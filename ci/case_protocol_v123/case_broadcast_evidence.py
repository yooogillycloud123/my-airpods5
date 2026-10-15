"""Read-only offline decoder for a *source-observed AirPods Pro 3* case BLE format.

Origin: https://github.com/stuffz/airpods/blob/main/src/aap/case_advert.hpp
This is NOT a verified AirPods 5 format and NOT a Find My sound control path.
Case BLE address is independent of the buds' RPA IRK in that project's captures.
No BLE operations, no credential storage or logging, no device connections.
"""
from __future__ import annotations
from dataclasses import dataclass
from typing import Optional
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

APPLE_COMPANY_ID = 0x004C
PRO3_CASE_ADVERT_PREFIX = bytes.fromhex("071106")
FORMAT_FIXED = ((8, 0), (10, 0), (11, 0))

@dataclass(frozen=True)
class Component:
    level: Optional[int]
    charging: bool

@dataclass(frozen=True)
class CaseObservation:
    # Parsed battery fields are only a model-research layout hypothesis.
    case: Component
    left: Component
    right: Component
    lid_closed: bool
    lid_counter: int
    header: str
    variant: str
    format_anomalies: tuple[str, ...]
    claimed_model: str = "AIRPODS_PRO_3_CAPTURE_FORMAT_ONLY"
    independent_case_identity_verified: bool = False
    apple_findmy_owner_authenticated: bool = False
    findmy_case_sound_command_known: bool = False
    actual_case_beep_observed: bool = False

def decode_component(value: int) -> Component:
    if value == 0xFF:
        return Component(None, False)
    level = value & 0x7F
    if level > 100:
        return Component(None, False)
    return Component(level, bool(value & 0x80))

def decode_pro3_case_format(company_id: int, manufacturer_data: bytes,
                            proximity_encryption_key: Optional[bytes]) -> Optional[CaseObservation]:
    """Return a *format-matched* decoded record; NEVER an owner/case authentication.

    The provided key is the separate AACP ENC_KEY, *not* the earbud RPA IRK.
    Do not search by IRK only or assume this Pro3 syntax holds for AirPods 5.
    Caller must not save, upload or log raw advertisements or keys.
    """
    if company_id != APPLE_COMPANY_ID or not isinstance(manufacturer_data, bytes):
        return None
    if len(manufacturer_data) != 19 or manufacturer_data[:3] != PRO3_CASE_ADVERT_PREFIX:
        return None
    if not isinstance(proximity_encryption_key, bytes) or len(proximity_encryption_key) != 16:
        return None
    encrypted_block = manufacturer_data[3:]
    decryptor = Cipher(algorithms.AES(proximity_encryption_key), modes.ECB()).decryptor()
    plain = decryptor.update(encrypted_block) + decryptor.finalize()
    # A 3-byte static shape filter is NOT integrity validation or authentication.
    if any(plain[offset] != expected for offset,expected in FORMAT_FIXED):
        return None
    anomalies = []
    if plain[:2] != b"\x29\x20":
        anomalies.append("unknown_header")
    if plain[6:8] not in (bytes.fromhex("510b"), bytes.fromhex("5a86")):
        anomalies.append("unknown_firmware_variant")
    if any(v != 0xFF and (v & 0x7F) > 100 for v in (plain[3],plain[4],plain[5])):
        anomalies.append("invalid_battery_field")
    return CaseObservation(
        case=decode_component(plain[3]), left=decode_component(plain[4]),
        right=decode_component(plain[5]), lid_closed=bool(plain[2] & 0x08),
        lid_counter=plain[2] & 0x07,
        header=plain[:2].hex(), variant=plain[6:8].hex(),
        format_anomalies=tuple(anomalies),
    )
