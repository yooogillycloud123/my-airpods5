#!/usr/bin/env python3
"""Static REA/JADX quality gate on OWN built Korean personal AirPods APK.

Absolutely no device execution, BLE scan, unpairing or data collection.
REA results are transient; only verification flags are published.
"""
import argparse,hashlib,json,re,subprocess,zipfile
from pathlib import Path

APP="com.myairpods5.personal"
TOOLS=("AirPodsService","BatteryView","PopupWindow","IslandWindow","FOSSBillingProvider")
def runjson(argv):
    r=subprocess.run(argv,capture_output=True,text=True,timeout=780)
    if r.returncode!=0:
        raise RuntimeError("pinned REA static stage failed exit="+str(r.returncode))
    try: out=json.loads(r.stdout)
    except (json.JSONDecodeError,TypeError):
        raise RuntimeError("REA did not return structured evidence") from None
    if not isinstance(out,dict) or out.get("isError") or out.get("success") is False:
        raise RuntimeError("REA static analysis returned error")
    return out
def main():
    p=argparse.ArgumentParser()
    p.add_argument("--apk",type=Path,required=True)
    p.add_argument("--expected-sha256",required=True)
    p.add_argument("--out",type=Path,required=True)
    a=p.parse_args()
    if not re.fullmatch("[0-9a-f]{64}",a.expected_sha256):
        raise ValueError("SHA256 pin required")
    actual=hashlib.sha256(a.apk.read_bytes()).hexdigest()
    if actual!=a.expected_sha256:
        raise ValueError("APK digest mismatch")
    with zipfile.ZipFile(a.apk) as z:
        n=sum(bool(re.fullmatch("classes[0-9]*[.]dex",x)) for x in z.namelist())
        if n<1 or "AndroidManifest.xml" not in z.namelist():
            raise ValueError("Invalid Android package")
    pkg=runjson(["rea","inspect-android-package",str(a.apk),"--json"])
    dump=json.dumps(pkg,sort_keys=True)
    if APP not in dump or "jadx" not in dump.lower():
        raise RuntimeError("REA package ownership and decompiler not verified")
    found={}
    for cls in TOOLS:
        r=runjson(["rea","search-android-classes",str(a.apk),cls,"--json"])
        found[cls]=(cls in json.dumps(r,sort_keys=True))
    if not all(found.values()):
        raise RuntimeError("Missing expected original engine/UI class in REA DEX results")
    method=runjson(["rea","inspect-android-method",str(a.apk),
        "me.kavishdevar.librepods.services.AirPodsService","onCreate","--json"])
    if "onCreate" not in json.dumps(method):
        raise RuntimeError("Service onCreate body not resolved by REA")
    out={
        "schema":"juhyeon-airpods5-ko-rea-static-v2",
        "apk_sha256":actual,"package":APP,"dex_count":n,
        "rea_version":"6.5.0","jadx_headless":"0.7.1",
        "own_apk_binary_inspected":True,
        "service_oncreate_decompiled":True,
        "required_classes":{k:"FOUND" for k in found},
        "original_bluetooth_commands_modified_by_personal_v2":False,
        "android_galaxy_physical_test_complete":False,
        "case_speaker_find_my_included":False,
        "raw_decompiled_material_shared":False,
        "gatt_written_or_ble_scanned":False,
    }
    a.out.parent.mkdir(parents=True,exist_ok=True)
    a.out.write_text(json.dumps(out,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
    print("JUHYEON_V2_REAL_REA_JADX_OWN_APK_STATIC=PASS")
    print("JUHYEON_V2_REA_DEX_COUNT="+str(n))
    print("JUHYEON_V2_PHYSICAL_AIRPODS5_GALAXY=NOT_YET_TESTED")
if __name__=="__main__":
    main()
