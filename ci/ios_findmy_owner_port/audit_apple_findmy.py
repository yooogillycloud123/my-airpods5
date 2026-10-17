#!/usr/bin/env python3
"""Evidence-backed iOS Find My SOURCE/SYMBOL audit, no device IO, no GATT writes.

Provenance: pin third-party Apple IPSW symbol-diff and reconstructed
Objective-C pseudocode. These ARE NOT real Mach-O files, and an REA source
graph of them is NOT a Ghidra binary decompilation. Keep raw files privately
in runner temp; publish only safe symbol/selector and source hashes.
"""
from __future__ import annotations
import argparse, hashlib, json, re
from pathlib import Path

BLACKTOP_COMMIT="af456047befbab058df4c3d7c7094462ad4753d6"
ETHAN_COMMIT="90aa0cfe59d9682b4265e1354c8b19ec3c7823ab"
BLACKTOP_BASE=f"https://raw.githubusercontent.com/blacktop/ipsw-diffs/{BLACKTOP_COMMIT}/iOS/26_5_23F77_vs_27_0_24A5355q"
ETHAN_BASE=f"https://raw.githubusercontent.com/EthanArbuckle/iPhone18-3_26.1_23B85_Restore/{ETHAN_COMMIT}"
UPSTREAM={
  "FindMy_symbols.md": BLACKTOP_BASE+"/MACHOS/filesystem/private/var/staged_system_apps/FindMy.app/FindMy.md",
  "SPOwner_symbols.md": BLACKTOP_BASE+"/DYLIBS/System/Library/PrivateFrameworks/SPOwner.framework/SPOwner.md",
  "searchpartyd_symbols.md": BLACKTOP_BASE+"/MACHOS/filesystem/usr/libexec/searchpartyd.md",
  "SPOwnerSession.mm": ETHAN_BASE+"/System/Library/PrivateFrameworks/SPOwner.framework/SPOwner/SPOwnerSession.mm",
  "SPBTFindingSession.mm": ETHAN_BASE+"/System/Library/PrivateFrameworks/SPOwner.framework/SPOwner/SPBTFindingSession.mm",
  "SPAccessoryDiscoveryAndPairingSession.mm": ETHAN_BASE+"/System/Library/PrivateFrameworks/SPOwner.framework/SPOwner/SPAccessoryDiscoveryAndPairingSession.mm",
}
# These signatures were manually checked in published primary reverse
# engineering artifacts; assertions operate on exact content, not guesses.
SIGNATURES={
  "FindMy_symbols.md": {
    "ui_start":"FMFindingExperienceCoordinator.startPlaySound:",
    "ui_stop":"FMFindingExperienceCoordinator.stopPlaySound:",
    "local_beacon_sound":"startPlaySoundForBeacon:completion:",
  },
  "SPOwner_symbols.md":{
    "bt_finding_sound":"SPBTFindingSession startPlaySoundForBeacon:completion:",
    "owner_peripheral_material":"SPOwnerSession peripheralConnectionMaterialForAccessoryIdentifier:completion:",
  },
  "searchpartyd_symbols.md":{
    "backend_start":"startPlaySound(beacon:completion:)",
    "backend_stop":"stopPlaySound(beacon:completion:)",
  },
  "SPOwnerSession.mm":{
    "owner_command_wrapper":"- (id)executeUTPlaySoundCommand:(id)command",
    "owner_command_delegation":"[(SPOwnerSession *)self executeCommand:",
    "unauthorized_sound_path":"playUnauthorizedSoundOnBeaconUUID:",
    "connection_with_ltk":"connectUsingMACAddress:",
    "connection_material":"peripheralConnectionMaterialForAccessoryIdentifier:",
  },
  "SPBTFindingSession.mm":{
    "finding_xpc":"com.apple.searchparty.btfindingsession",
  },
  "SPAccessoryDiscoveryAndPairingSession.mm":{
    "pairing_xpc":"com.apple.icloud.searchpartyd.accessorydiscoverysession",
    "pairing_finalize":"finalizeProximityPairingForAccessoryIdentifier:",
  },
}

def evaluate(files:dict[str,str])->dict:
    if sorted(files)!=sorted(UPSTREAM):
        raise ValueError("missing or unexpected iOS evidence source")
    matched={}
    for filename,queries in SIGNATURES.items():
        content=files[filename]
        if not content or len(content)<300:
            raise ValueError("empty/truncated iOS evidence: "+filename)
        matched[filename]={key:(needle in content) for key,needle in queries.items()}
        if not all(matched[filename].values()):
            raise ValueError("source changed; named iOS claim not verified: "+filename)
    owner=files["SPOwnerSession.mm"]
    # Validate TWO DIFFERENT code paths, not conflated owner-side commands:
    # executeUTPlaySoundCommand -> executeCommand, and unauthorized tracking
    # -> userAgentProxy playUnauthorizedSound. This distinction matters because
    # v140 generic FD44 writes succeeded yet user A3529 CASE was silent.
    start=owner.find("- (id)executeUTPlaySoundCommand:(id)command\n{")
    end=owner.find("\n- (void)",start+1)
    if start<0 or "executeCommand:" not in owner[start:end]:
        raise ValueError("owner command wrapper delegation not verified")
    unauthorized_start=owner.find("- (void)playUnauthorizedSoundOnBeaconByUUID:(id)d completion:(id)completion\n{")
    unauthorized_end=owner.find("\n- (void)",unauthorized_start+1)
    if unauthorized_start<0 or "userAgentProxy" not in owner[unauthorized_start:unauthorized_end]:
        raise ValueError("unauthorized playback delegation not verified")
    # Apple decompiled wrapper does NOT expose raw BLE sound packet or owner
    # enrollment. A method named 'executeUTPlaySoundCommand' alone is NOT
    # proof that our OWN A3529 case was provisioned.
    return {
      "schema":"rea-apple-findmy-port-source-v1",
      "evidence_types":["iOS Mach-O SYMBOL_DIFF (not bytecode)","third-party reconstructed Objective-C (not our Ghidra run)"],
      "pinned_commits":{"blacktop_ipsw_diffs":BLACKTOP_COMMIT,"ethan_iphone_26_1":ETHAN_COMMIT},
      "source_sha256":{name:hashlib.sha256(text.encode()).hexdigest() for name,text in files.items()},
      "source_lengths":{name:len(text) for name,text in files.items()},
      "verified_signatures":matched,
      "observed_owner_session_wrappers":True,
      "observed_unauthorized_tracking_sound_separate":True,
      "observed_explicit_ltk_connect_wrapper":True,
      "observed_apple_internal_xpc_boundaries":True,
      "apple_internal_xpc_callable_from_normal_android":False,
      "actual_macho_bytes_analyzed_with_ghidra":False,
      "true_call_graph_reconstructed":False,
      "raw_a3529_case_ble_owner_auth_available":False,
      "raw_a3529_authenticated_speaker_opcode_available":False,
      "android_findmy_case_ring_implemented":False,
      "user_v140_fd44_ack_but_physically_silent":True,
      "v141_user_app_or_pairing_modified":False,
      "next_gate":"REAL_A3529_OWNER_SESSION_PROVISIONING_AND_VERIFIED_CASE_COMMAND",
      "raw_decompile_published":False,
    }

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--sources",type=Path,required=True)
    p.add_argument("--output",type=Path,required=True)
    a=p.parse_args()
    paths={k:(a.sources/k).read_text(encoding="utf8") for k in UPSTREAM}
    info=evaluate(paths)
    a.output.parent.mkdir(parents=True,exist_ok=True)
    a.output.write_text(json.dumps(info,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
    print("APPLE_FINDMY_OWNER_AND_NONOWNER_SOURCE_PATHS=PASS")
    print("APPLE_INTERNAL_XPC_AND_LTK_WRAPPERS=PASS")
    print("ACTUAL_APPLE_MACHO_GHIDRA_DECOMP=NOT_PERFORMED")
    print("ANDROID_A3529_OWNER_CASE_RING=BLOCKED_NO_OWNER_AUTH_AND_OPCODE")

if __name__=="__main__":
    main()
