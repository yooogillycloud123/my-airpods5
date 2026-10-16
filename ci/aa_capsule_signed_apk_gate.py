#!/usr/bin/env python3
"""AA AirPods R25 fail-closed release gate. Pure stdlib except Android SDK CLIs.
Inspects artifacts only. Never installs, executes shell payloads or edits Android audio.
"""
from __future__ import annotations
import argparse, hashlib, json, re, shutil, struct, subprocess, sys, zipfile, zlib
from pathlib import Path

PKG = "com.aaairpods.r25fix"
CERT = "be9ec29b5384912c2c75c283615ccc78ee17f694133375e087e8df08319e4f23"
CONTROLLER_SHA = "49364923ba83acaa02d4e6a2b58251df7697182ff82a4b33ae22abbc94764623"
HELPERS = {
    "bridge_original.apk": "a901afd91ba72cde0a27f05f40c70987085a5271d6ea3b941a574ddcfbc6b739",
    "edge23_original.apk": "c520e7bcfe8f248b6c8e0e13a4c693bcd1afdac588de628d5bed69c3cb337ad3",
    "edge24_original.apk": "6bbac432829f627563f57cd0cfa2f5f6f05e61f27ed25895d68b19e253dc1db3",
}

def sha(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()

def need(ok: bool, label: str) -> None:
    if not ok: raise ValueError(label)

def inspect_original_helper_bundle(path: Path) -> dict:
    with zipfile.ZipFile(path) as z:
        need(z.testzip() is None, "HELPER_BUNDLE_ZIP_CRC_FAIL")
        for name, digest in HELPERS.items():
            need(name in z.namelist(), "MISSING_ORIGINAL_HELPER_"+name)
            actual=sha(z.read(name))
            need(actual==digest, "ORIGINAL_HELPER_DIGEST_DRIFT_"+name)
        return {"original_helper_bytes":3,"crc":"PASS","hash":"PASS",
                "sha256":sha(path.read_bytes()),"installable_apk":False}

def _run(tool:str, *args:str)->str:
    executable=shutil.which(tool)
    need(bool(executable), "REQUIRES_ANDROID_SDK_TOOL_"+tool)
    p=subprocess.run([executable,*args],capture_output=True,text=True,check=False)
    need(p.returncode==0, "SDK_TOOL_FAILED_"+tool)
    return p.stdout+"\n"+p.stderr

def inspect_signed_apk(path: Path, *, min_version:int=26080,
                       require_helper_assets:bool=False)->dict:
    """Require original signer, APK v2+, R25 identity, genuine DEX, immutable B.
    An existing signed APK can be audited; this method cannot make an APK final.
    """
    need(path.is_file() and 100_000<path.stat().st_size<100_000_000,"APK_FILE_SIZE")
    with zipfile.ZipFile(path) as z:
        need(z.testzip() is None,"APK_CRC_FAIL")
        names=z.namelist()
        need("AndroidManifest.xml" in names,"NO_MANIFEST")
        need("resources.arsc" in names,"NO_RESOURCES")
        resources=z.getinfo("resources.arsc")
        need(resources.compress_type==zipfile.ZIP_STORED,"RESOURCES_NOT_STORED")
        need(resources.header_offset>=0,"RESOURCES_BAD_OFFSET")
        dexes=sorted(n for n in names if re.fullmatch(r"classes(?:[2-9][0-9]*)?\\.dex",n))
        need(bool(dexes),"NO_CLASSES_DEX")
        for dex in dexes:
            b=z.read(dex)
            need(len(b)>=112 and b[:4]==b"dex\\n".replace(b"\\n",b"\n"),"DEX_MAGIC_"+dex)
            need(b[12:32]==hashlib.sha1(b[32:]).digest(),"DEX_SHA1_"+dex)
            need(struct.unpack_from("<I",b,8)[0]==(zlib.adler32(b[12:])&0xffffffff),
                 "DEX_ADLER32_"+dex)
        golden="assets/golden_controller_b_493_IMMUTABLE_REFERENCE.sh"
        need(golden in names,"ORIGINAL_GOLDEN_B_MISSING")
        payload=z.read(golden)
        need(len(payload)==22454 and sha(payload)==CONTROLLER_SHA,"GOLDEN_B_NOT_EXACT")
        if require_helper_assets:
            for name,digest in HELPERS.items():
                asset="assets/golden_helpers/"+name
                need(asset in names,"CAPSULE_MISSING_HELPER_"+name)
                need(sha(z.read(asset))==digest,"CAPSULE_HELPER_SHA_DRIFT_"+name)
    signature=_run("apksigner","verify","--verbose","--print-certs",str(path))
    need("Verified using v2 scheme (APK Signature Scheme v2): true" in signature,
         "APK_V2_SIGNATURE_REQUIRED")
    fingerprint=re.search(r"Signer #1 certificate SHA-256 digest:\\s*([0-9A-Fa-f:]+)",signature)
    need(bool(fingerprint),"NO_SIGNER_FINGERPRINT")
    got=fingerprint.group(1).replace(":","").lower()
    need(got==CERT,"NOT_ORIGINAL_R25_SIGNER")
    badge=_run("aapt","dump","badging",str(path))
    matched=re.search(r"^package:\\s+name='([^']+)'\\s+versionCode='([0-9]+)'",badge,re.MULTILINE)
    need(bool(matched) and matched.group(1)==PKG,"NOT_R25_PACKAGE")
    version=int(matched.group(2))
    need(version>=min_version,"APK_VERSION_TOO_LOW")
    return {"package":PKG,"versionCode":version,"signer":got,"apk_v2":"PASS",
            "zip_crc":"PASS","dex_sha1_adler":"PASS","golden_b":"EXACT",
            "embedded_helpers":("3_EXACT" if require_helper_assets else "NOT_REQUIRED"),
            "sha256":sha(path.read_bytes()),"car_physical_test":"NOT_TESTED"}

def main()->int:
    p=argparse.ArgumentParser()
    g=p.add_mutually_exclusive_group(required=True)
    g.add_argument("--helpers-zip",type=Path)
    g.add_argument("--signed-apk",type=Path)
    p.add_argument("--min-version",type=int,default=26080)
    p.add_argument("--require-helper-assets",action="store_true")
    a=p.parse_args()
    try:
        report=inspect_original_helper_bundle(a.helpers_zip) if a.helpers_zip else (
            inspect_signed_apk(a.signed_apk,min_version=a.min_version,
                               require_helper_assets=a.require_helper_assets))
        print(json.dumps({"gate":"PASS",**report},indent=2,ensure_ascii=False))
        return 0
    except (ValueError, OSError, zipfile.BadZipFile) as e:
        print(json.dumps({"gate":"FAIL","reason":str(e)},ensure_ascii=False))
        return 1

if __name__=="__main__":sys.exit(main())
