import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from case_target_identity import (
    APPROVED_CASE_AUDIO_CAPTURE_HASHES,
    OFFICIAL_CLAIMS,
    SUPPORTED_MODELS,
    evaluate_target,
)

class ModelSourceConflictTests(unittest.TestCase):
    def test_apple_official_publications_disagree_on_wireless_case_model(self):
        official={c.wireless_case_model for c in OFFICIAL_CLAIMS}
        self.assertEqual(official,{"A3529","A3530"})
        self.assertTrue(evaluate_target()["official_source_model_conflict"])

    def test_user_has_not_supplied_printed_model(self):
        info=evaluate_target()
        self.assertFalse(info["user_has_provided_printed_model_label"])
        self.assertIsNone(info["case_model_confirmed_from_user_label"])
        self.assertFalse(info["own_case_ble_cryptographic_binding"])

    def test_earbud_advert_model_is_not_charge_case_model(self):
        info=evaluate_target("0x3020")
        self.assertIsNone(info["case_model_confirmed_from_user_label"])
        self.assertTrue(info["ble_0x3020_is_not_case_hardware_model"])

    def test_physically_read_model_does_not_prove_owner_ble_identity(self):
        for model in SUPPORTED_MODELS:
            with self.subTest(model=model):
                info=evaluate_target(model)
                self.assertEqual(info["case_model_confirmed_from_user_label"],model)
                self.assertFalse(info["own_case_ble_cryptographic_binding"])

    def test_proximity_frames_and_fd44_only_cannot_release_sound(self):
        info=evaluate_target()
        self.assertFalse(info["verified_a3529_or_a3530_case_play_stop"])
        self.assertFalse(info["real_both_buds_docked_lid_closed_audio_verified"])
        self.assertFalse(info["test_apk_approved_for_case_sound"])

    def test_all_user_asserted_flags_cannot_bypass_missing_real_capture(self):
        for model in SUPPORTED_MODELS:
            info=evaluate_target(
                case_label_model=model,
                owner_case_cryptographic_binding=True,
                verified_case_audio_command=True,
                closed_case_audible_test=True,
                reviewed_real_case_capture_sha256="f"*64,
            )
            self.assertFalse(info["test_apk_approved_for_case_sound"])
            self.assertFalse(info["real_both_buds_docked_lid_closed_audio_verified"])

    def test_reviewed_list_is_empty_until_evidence(self):
        self.assertFalse(APPROVED_CASE_AUDIO_CAPTURE_HASHES)

    def test_no_device_side_effects_claim(self):
        info=evaluate_target()
        self.assertTrue(info["none_of_these_controls_bluetooth"])
        self.assertFalse(info["existing_v139_apk_or_personal_app_modified"])

if __name__ == "__main__":
    unittest.main()
