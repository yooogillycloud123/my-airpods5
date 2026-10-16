#!/usr/bin/env python3
"""AirPods 5 charging case SOURCE-IDENTITY fail-closed gate.

Independent Apple-hosted publications currently CONFLICT on the mapping of
A3529/A3530 to the speaker-equipped wireless-charging model. Do not infer an
actual user's case model from generic AirPods 5 / BLE 0x3020. This only reports
documented public model claims; no device scan or network operation.
"""
from __future__ import annotations
import argparse
import json
import re
from dataclasses import dataclass
from pathlib import Path

@dataclass(frozen=True)
class PublicClaim:
    publisher: str
    url: str
    language: str
    wireless_case_model: str
    regular_case_model: str
    content_kind: str

# Pin source *claims*, not our conclusion. Read 2026-10-17.
# Some versions of official support 109525 are inconsistent even across regions.
OFFICIAL_CLAIMS = (
    PublicClaim(
        "Apple Korea product specifications",
        "https://www.apple.com/kr/airpods-5/specs/",
        "ko-KR", "A3529", "A3530", "manufacturer_product_specs",
    ),
    PublicClaim(
        "Apple Korea support case identification",
        "https://support.apple.com/ko-kr/109525",
        "ko-KR", "A3530", "A3529", "support_identification_article",
    ),
    PublicClaim(
        "Apple English support case identification",
        "https://support.apple.com/en-kg/109525",
        "en-KG", "A3529", "A3530", "support_identification_article",
    ),
)

SUPPORTED_MODELS = frozenset({"A3529", "A3530"})

def evaluate_target(
    case_label_model: str | None = None,
    owner_case_cryptographic_binding: bool = False,
    verified_case_audio_command: bool = False,
    closed_case_audible_test: bool = False,
) -> dict:
    # A third-party printed model assertion without independent case-ownership
    # authentication is still not authorization to operate a BLE endpoint.
    published = {c.wireless_case_model for c in OFFICIAL_CLAIMS}
    contradictory = len(published) > 1
    observed_printed = bool(re.fullmatch(r"A\d{4}", case_label_model or ""))
    physical_case_model = (
        case_label_model if observed_printed and case_label_model in SUPPORTED_MODELS
        else None
    )
    # Even a physical printed label confirms a model, not authenticated BLE
    # case identity and never that a random FD44 tracker belongs to the owner.
    owner_authenticated = bool(owner_case_cryptographic_binding and physical_case_model)
    command_verified = bool(verified_case_audio_command and owner_authenticated)
    case_beep_pass = bool(closed_case_audible_test and command_verified)
    return {
        "schema": "airpods5-charging-case-source-gate-v1",
        "source_publications": [
            {
                "publisher": x.publisher, "language": x.language,
                "reference": x.url, "wireless_case_claim": x.wireless_case_model,
                "standard_case_claim": x.regular_case_model,
                "kind": x.content_kind,
            }
            for x in OFFICIAL_CLAIMS
        ],
        "official_source_model_conflict": contradictory,
        "user_has_provided_printed_model_label": physical_case_model is not None,
        "case_model_confirmed_from_user_label": physical_case_model,
        "ble_0x3020_is_not_case_hardware_model": True,
        "own_case_ble_cryptographic_binding": owner_authenticated,
        "verified_a3529_or_a3530_case_play_stop": command_verified,
        "real_both_buds_docked_lid_closed_audio_verified": case_beep_pass,
        "test_apk_approved_for_case_sound": bool(
            not contradictory and case_beep_pass
        ),
        "next_engineering_gate": (
            "VERIFY_DEVICE_MODEL_AND_BLE_CASE_OWNER_BINDING_FIRST"
            if not owner_authenticated
            else "VERIFY_REAL_CLOSED_CASE_SOUND"
        ),
        "none_of_these_controls_bluetooth": True,
        "existing_v139_apk_or_personal_app_modified": False,
    }

def cli() -> None:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--out", type=Path, required=True)
    p.add_argument("--printed-case-model", default=None)
    a = p.parse_args()
    data = evaluate_target(case_label_model=a.printed_case_model)
    a.out.parent.mkdir(parents=True, exist_ok=True)
    a.out.write_text(json.dumps(data, ensure_ascii=False, indent=2)+"\n",encoding="utf-8")
    print("OFFICIAL_APPLE_MODEL_MAPPING_CONFLICT=" + ("YES" if data["official_source_model_conflict"] else "NO"))
    print("USER_OWN_CASE_MODEL=UNKNOWN" if not data["case_model_confirmed_from_user_label"] else "USER_OWN_CASE_MODEL=LABEL_ONLY")
    print("CASE_OWNER_GATT_VERIFIED=NO")
    print("CASE_SPEAKER_PHONE_BEEP=NOT_VERIFIED")
    print("EXISTING_APKS_UNCHANGED=YES")

if __name__ == "__main__":
    cli()
