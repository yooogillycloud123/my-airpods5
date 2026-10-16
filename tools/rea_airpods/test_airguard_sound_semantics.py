import unittest
from airguard_sound_semantics import inspect_sources,masked_match

PODS = """setManufacturerData(
  0x4C,
  byteArrayOf((0x12).toByte(),(0x19).toByte(),(0x18).toByte()),
  byteArrayOf((0xFF).toByte(), (0x00).toByte(), (0x18).toByte())
)
listOf(SoundProtocol.FINDMY, SoundProtocol.DULT, SoundProtocol.AIRTAG)
"""

FINDMY = """
internal const val FINDMY_SOUND_SERVICE = "fd44"
UUID.fromString("4F860003-943B-49EF-BED4-2F730304427A")
internal val FINDMY_START_SOUND_OPCODE = byteArrayOf(0x01, 0x00, 0x03)
internal val FINDMY_STOP_SOUND_OPCODE  = byteArrayOf(0x01, 0x01, 0x03)
writeCharacteristic(gatt, characteristic, FINDMY_START_SOUND_OPCODE, enableNotifications = true)
soundStarted = true
"""

class SourceContractTests(unittest.TestCase):
    def test_upstream_public_contract_with_proof_limits(self):
        facts=inspect_sources(PODS,FINDMY)
        self.assertEqual(facts["fd44_nonowner_characteristic"],"4F860003")
        self.assertFalse(facts["fd44_user_wireless_case_auth_proven"])
        self.assertFalse(facts["fd44_case_sound_observed_by_user"])
        self.assertFalse(facts["gatt_write_ack_implies_physical_case_sound"])
        self.assertTrue(all(facts["checked_contracts"].values()))

    def test_ble_airpods_filter_is_not_only_1219(self):
        target=bytes.fromhex("12 19 18")
        mask=bytes.fromhex("ff 00 18")
        self.assertTrue(masked_match(bytes.fromhex("12 02 18"),target,mask))
        self.assertTrue(masked_match(bytes.fromhex("12 19 18"),target,mask))

    def test_1202_is_not_automatically_a_match_without_third_byte(self):
        self.assertFalse(masked_match(bytes.fromhex("12 02 00"),bytes.fromhex("12 19 18"),bytes.fromhex("ff 00 18")))

    def test_reject_missing_sound_protocol_source(self):
        with self.assertRaises(ValueError):
            inspect_sources(PODS,"unsupported source")

    def test_bad_frame_lengths_fail(self):
        self.assertFalse(masked_match(b"\x12",bytes.fromhex("12 19 18"),bytes.fromhex("ff 00 18")))

if __name__=="__main__":
    unittest.main()
