#!/usr/bin/env python3
"""Static Kotlin AST-free assertions + pure presentation truth-table regression."""
import pathlib,sys,unittest
BASE=pathlib.Path(sys.argv[1]) if len(sys.argv)>1 else None
class BatteryPresentationProvenanceTest(unittest.TestCase):
    @staticmethod
    def has_reading(levels):return any(isinstance(x,int) and 1 <= x <= 100 for x in levels if x is not None)
    def test_none_all_unknown(self):self.assertFalse(self.has_reading([None,None,None]))
    def test_stale_zero_all_unknown(self):self.assertFalse(self.has_reading([0,0,0]))
    def test_one_false_zero_unknown(self):self.assertFalse(self.has_reading([None,0,None]))
    def test_real_component_makes_reading_available(self):self.assertTrue(self.has_reading([0,55,None]))
    def test_case_only_battery(self):self.assertTrue(self.has_reading([None,None,81]))
    def test_bounded_integers_only(self):self.assertFalse(self.has_reading([255,-1,None]))
    def test_source_constraints(self):
        if BASE is None:self.skipTest("unit truth-table only")
        b=(BASE/"presentation/components/BatteryView.kt").read_text("utf8")
        p=(BASE/"presentation/overlays/PopupWindow.kt").read_text("utf8")
        settings=(BASE/"presentation/screens/AppSettingsScreen.kt").read_text("utf8")
        status=(BASE.parent.parent/"CaseSoundStatusV125.kt").read_text("utf8") if False else (BASE/"CaseSoundStatusV125.kt").read_text("utf8")
        self.assertIn("val hasNonzeroMeasuredBattery = listOfNotNull(left, right, case)",b)
        self.assertIn("!hasNonzeroMeasuredBattery",b)
        self.assertEqual(b.count("hasNonzeroMeasuredBattery &&"),3)
        self.assertIn("val hasNonzeroMeasuredBattery = batteryList.any",p)
        self.assertEqual(p.count("if (hasNonzeroMeasuredBattery && it.status"),3)
        self.assertIn("CaseSoundStatusV125.open(it)",settings)
        self.assertNotIn("CaseResearchMenuV124.open(it)",settings)
        for forbidden in ("startScan(", "connectGatt(", "writeCharacteristic", "createBond(", "sendRequestProximityKeys("):
            self.assertNotIn(forbidden,status)
if __name__=="__main__":
    # unittest consumes argv when not normalized
    sys.argv=[sys.argv[0]]
    unittest.main(verbosity=2)
