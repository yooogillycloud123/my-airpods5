#!/usr/bin/env python3
# Evidence gate for AirPods5 lost-CASE speaker protocol.
# Source boundary: checked documentation only, NEVER sends BLE writes.
from dataclasses import dataclass
import unittest

@dataclass(frozen=True)
class CaseSpeakerEvidence:
    case_identity_verified: bool
    airpods5_case_command_verified: bool
    authorization_available: bool
    closed_case_transport_verified: bool
    audible_case_result_verified: bool
    external_source: str

    def can_ship_case_find_button(self) -> bool:
        return all((
            self.case_identity_verified,
            self.airpods5_case_command_verified,
            self.authorization_available,
            self.closed_case_transport_verified,
            self.audible_case_result_verified,
            bool(self.external_source.strip()),
        ))

# Measured state of user Galaxy-only AirPods 5, as of 2026-10-15.
MEASURED = CaseSpeakerEvidence(False, False, False, False, False, "")

# Other projects' real sources: purposefully must NOT be mistaken for user's case.
AACP_IN_CASE_TONE_31 = CaseSpeakerEvidence(False, False, False, False, False, "LibrePods docs/AAP Definitions.md")
AIRGUARD_NON_OWNER_SEPARATED = CaseSpeakerEvidence(False, False, False, False, False, "AirGuard AppleFindMy.kt")
OPENTAGVIEWER_OWNER_ON_THIRD_PARTY = CaseSpeakerEvidence(False, False, False, False, False, "OpenTagViewer owner-connected-tags.md")

class TestCaseSpeakerEvidenceGate(unittest.TestCase):
    def test_real_user_has_no_verified_case_sound_path(self):
        self.assertFalse(MEASURED.can_ship_case_find_button())

    def test_charging_chime_toggle_is_not_findmy_sound(self):
        self.assertFalse(AACP_IN_CASE_TONE_31.can_ship_case_find_button())

    def test_nonowner_separated_tracker_is_not_own_closed_case(self):
        self.assertFalse(AIRGUARD_NON_OWNER_SEPARATED.can_ship_case_find_button())

    def test_third_party_owner_protocol_is_not_airpods5_proof(self):
        self.assertFalse(OPENTAGVIEWER_OWNER_ON_THIRD_PARTY.can_ship_case_find_button())

    def test_each_independent_evidence_requirement_must_be_met(self):
        base = dict(case_identity_verified=True, airpods5_case_command_verified=True,
                    authorization_available=True, closed_case_transport_verified=True,
                    audible_case_result_verified=True, external_source="airpods5 physical proof")
        for key in ["case_identity_verified","airpods5_case_command_verified",
                    "authorization_available","closed_case_transport_verified",
                    "audible_case_result_verified"]:
            fail = dict(base)
            fail[key] = False
            with self.subTest(missing=key):
                self.assertFalse(CaseSpeakerEvidence(**fail).can_ship_case_find_button())
        fail=dict(base)
        fail["external_source"]=""
        self.assertFalse(CaseSpeakerEvidence(**fail).can_ship_case_find_button())

    def test_synthetic_full_evidence_qualifies_only_after_real_proof(self):
        # Satisfiable gate, but this is SYNTHETIC data and not AirPods5 proof.
        synthetic=CaseSpeakerEvidence(True,True,True,True,True,"synthetic fixture - NOT reality")
        self.assertTrue(synthetic.can_ship_case_find_button())

if __name__=="__main__":
    unittest.main(verbosity=2)
