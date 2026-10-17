#!/usr/bin/env python3
"""Reject claims that a random BLE/RSSI correlation proves the user's
closed AirPods5 A3529 case or Apple FindMy owner authorization.
Only reads existing screenshot-derived structured observations: no radio IO.
"""
import json
from pathlib import Path

DATA=Path(__file__).with_name("REAL_USER_V142_PHYSICAL_AB_RESULT_20261017.json")

def validate(evidence):
    assert evidence["near_window_seconds"]==8
    assert evidence["far_window_seconds"]==8
    assert evidence["observed_apple_address_family_candidates_near"]==13
    assert evidence["observed_apple_address_family_candidates_far"]==5
    samples=evidence["displayed_ranked_12_02_candidates"]
    assert len(samples)==6
    assert len([x for x in samples if x["far_observation"]=="not_observed"])==4
    kept=[x for x in samples if x["far_observation"]=="observed"]
    assert len(kept)==2
    deltas=[x["near_rssi_mean_dbm"]-x["far_rssi_mean_dbm"] for x in kept]
    assert deltas==[5,-3],deltas
    assert evidence["prev_v140_user_physical_case_audible"]=="NO"
    assert evidence["individual_a3529_radio_identity"]=="UNVERIFIED"
    assert evidence["apple_findmy_owner_enrollment"]=="UNVERIFIED"
    assert evidence["case_owner_control_auth"]=="UNVERIFIED"
    assert evidence["case_audio_playback_in_v142"]=="NOT_IMPLEMENTED"
    return {
        "case_model_on_original_product_box": "A3529_CONFIRMED",
        "a_b_apple_candidate_near":13,
        "a_b_apple_candidate_distant":5,
        "shown_12_02_addresses_lost":4,
        "still_observed_12_02_mean_rssi_changes_db":deltas,
        "distinct_physical_devices_identified":0,
        "cryptographic_owned_case_binding":False,
        "apple_account_findmy_pairing_evidenced":False,
        "ring_control_implemented":False,
        "case_sound_physically_proven":False,
        "v140_anonymous_fd44_ack_zero_but_silent":True,
        "recommended_additional_identical_scan":False,
        "next_real_blocker":"OWNER_FINDMY_ENROLLMENT_AND_AUTHENTICATED_CASE_SPECIFIC_COMMAND",
        "result":"NO_OWN_CASE_IDENTIFICATION_NO_PLAYABLE_CASE_RING"
    }

def main():
    evidence=json.loads(DATA.read_text("utf8"))
    summary=validate(evidence)
    for key,val in summary.items():
        print(f"A3529_V142_{key.upper()}={str(val).upper()}")
    print("A3529_V142_EVIDENCE_CONSISTENCY=PASS")

if __name__=="__main__":
    main()
