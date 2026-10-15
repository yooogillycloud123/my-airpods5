"""Synthetic Pro3 case cipher test; no real user device, MAC, IRK or sound commands."""
import unittest
from cryptography.hazmat.primitives.ciphers import Cipher,algorithms,modes
from case_broadcast_evidence import decode_pro3_case_format, decode_component

KEY=bytes(range(16))
# Example DECRYPTED layout in stuffz/airpods docs/ble-research.md, not actual cipher.
PLAIN=bytes.fromhex("2920083ddedd510b00000000f7821600")

def make(payload=PLAIN, prefix=bytes.fromhex("071106"), key=KEY):
    encryptor=Cipher(algorithms.AES(key),modes.ECB()).encryptor()
    return prefix + encryptor.update(payload) + encryptor.finalize()

class BroadcastEvidence(unittest.TestCase):
    def test_synthetic_closed_case_and_percent(self):
        found=decode_pro3_case_format(0x004C,make(),KEY)
        self.assertIsNotNone(found)
        self.assertEqual((found.case.level,found.left.level,found.right.level),(61,94,93))
        self.assertTrue(found.lid_closed)
        self.assertEqual((found.case.charging,found.left.charging,found.right.charging),(False,True,True))
        self.assertFalse(found.independent_case_identity_verified)
        self.assertFalse(found.apple_findmy_owner_authenticated)
        self.assertFalse(found.findmy_case_sound_command_known)
        self.assertFalse(found.actual_case_beep_observed)
    def test_wrong_irk_is_not_encryption_key(self):
        self.assertIsNone(decode_pro3_case_format(0x004C,make(),bytes(reversed(KEY))))
    def test_other_advert_format_never_accepted(self):
        self.assertIsNone(decode_pro3_case_format(0x004C,make(prefix=bytes.fromhex("071906")),KEY))
        self.assertIsNone(decode_pro3_case_format(0x1234,make(),KEY))
        self.assertIsNone(decode_pro3_case_format(0x004C,b"\x07\x11",KEY))
        self.assertIsNone(decode_pro3_case_format(0x004C,make(),None))
    def test_lid_and_absence(self):
        p=bytearray(PLAIN)
        p[2]=0x09
        p[5]=0xff
        found=decode_pro3_case_format(0x004C,make(bytes(p)),KEY)
        self.assertTrue(found.lid_closed)
        self.assertEqual(found.lid_counter,1)
        self.assertIsNone(found.right.level)
        self.assertFalse(found.right.charging)
    def test_variant_change_is_anomaly_not_discard(self):
        p=bytearray(PLAIN)
        p[6:8]=bytes.fromhex("5a86")
        self.assertEqual(decode_pro3_case_format(0x004C,make(bytes(p)),KEY).format_anomalies,())
        p[6:8]=bytes.fromhex("0001")
        self.assertEqual(decode_pro3_case_format(0x004C,make(bytes(p)),KEY).format_anomalies,("unknown_firmware_variant",))
    def test_bad_fixed_bytes_rejected(self):
        p=bytearray(PLAIN)
        p[8]=3
        self.assertIsNone(decode_pro3_case_format(0x004C,make(bytes(p)),KEY))
    def test_invalid_percent_never_faked_as_zero(self):
        self.assertIsNone(decode_component(0xff).level)
        self.assertIsNone(decode_component(0x7e).level)
        self.assertEqual(decode_component(0x00).level,0)
if __name__=="__main__":
    unittest.main(verbosity=2)
