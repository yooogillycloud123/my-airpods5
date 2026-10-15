#!/usr/bin/env python3
"""Source-backed v129 test: never promote chime ON/OFF to case PLAY."""
import unittest
from case_chime_evidence import classify_aap,classify_uuid,physical_usb_event,safe_report

class TestChimeTruth(unittest.TestCase):
    def test_on_is_config_not_ring(self):
        x=classify_aap(bytes.fromhex("12 3A 00 01 00 08 00"))
        self.assertEqual(x["category"],"CASE_CHIME_SETTING_HISTORIC")
        self.assertFalse(x["can_be_used_as_case_speaker_play_command"])
    def test_off_is_config_not_ring(self):
        x=classify_aap(bytes.fromhex("12 3A 00 01 00 08 01"))
        self.assertEqual(x["category"],"CASE_CHIME_SETTING_HISTORIC")
        self.assertFalse(x["physical_case_beep_from_this_frame_verified"])
    def test_unknown_setting_value_rejected(self):
        x=classify_aap(bytes.fromhex("12 3A 00 01 00 08 02"))
        self.assertEqual(x["category"],"UNKNOWN")
    def test_unknown_appended_frame_rejected(self):
        x=classify_aap(bytes.fromhex("12 3A 00 01 00 08 00 01"))
        self.assertEqual(x["category"],"UNKNOWN")
    def test_in_case_tone_31_is_not_charging_beep(self):
        x=classify_aap(bytes.fromhex("04 00 04 00 09 00 31 01 00 00 00"))
        self.assertEqual(x["category"],"EARBUD_IN_CASE_TONE")
        self.assertFalse(x["can_emit_unplugged_case_beep"])
    def test_0x40_not_sound(self):
        x=classify_aap(bytes.fromhex("04 00 04 00 09 00 40 40 00 00 00"))
        self.assertEqual(x["category"],"UNKNOWN")
    def test_other_accessory_owner_cp_not_airpods_case(self):
        x=classify_uuid("4F860002-943B-49EF-BED4-2F730304427A")
        self.assertEqual(x["category"],"OTHER_ACCESSORY_FINDMY_CP")
        self.assertFalse(x["case_identity_verified"])
    def test_other_accessory_nonowner_cp_not_case(self):
        x=classify_uuid("4F860003-943B-49EF-BED4-2F730304427A")
        self.assertFalse(x["can_be_used_as_case_speaker_play_command"])
    def test_physical_charge_chime_is_not_bt(self):
        x=physical_usb_event()
        self.assertTrue(x["speaker_sound_confirmed_only_during_real_power_event"])
        self.assertTrue(x["does_not_establish_BLE_trigger"])
    def test_report_never_claims_beep(self):
        x=safe_report(["123a0001000800","0400040009003101000000"],
                      ["4f860002943b49efbed42f730304427a"])
        self.assertFalse(x["case_speaker_play_implemented"])
        self.assertEqual(x["target_status"],"NO_VALIDATED_BLUETOOTH_PLAYBACK_COMMAND")
        self.assertTrue(all(not f["can_be_used_as_case_speaker_play_command"] for f in x["aap_frames"]))
    def test_no_radios_or_transmissions(self):
        import inspect,case_chime_evidence
        src=inspect.getsource(case_chime_evidence)
        for s in ["connectGatt(", "startScan(", "createBond(", "writeCharacteristic(", "socket.send(", "system("]:
            self.assertNotIn(s,src)
if __name__=="__main__":unittest.main(verbosity=2)
