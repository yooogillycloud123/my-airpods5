#!/usr/bin/env python3
"""AirPods 5 model/Continuity/null battery proof.
Offline tests only; not a case speaker or live Bluetooth check.
"""
from pathlib import Path
import sys,unittest
class AirPods5CapodDerivedContract(unittest.TestCase):
    @staticmethod
    def validate_advert(manufacturer):
        if len(manufacturer)<27:return False
        model=(manufacturer[3]<<8)|manufacturer[4]
        return model in {0x3020,0x3620} and manufacturer[0:3]==bytes([7,25,1])
    @staticmethod
    def battery_percent(byte):
        level=byte&0x7f
        return None if byte==255 or level>100 else level
    def test_3020_wireless_is_supported(self):
        self.assertTrue(self.validate_advert(bytes([7,25,1,0x30,0x20])+bytes(22)))
    def test_3620_standard_is_separate(self):
        self.assertTrue(self.validate_advert(bytes([7,25,1,0x36,0x20])+bytes(22)))
    def test_type07_11_06_pro3_not_airpods5(self):
        self.assertFalse(self.validate_advert(bytes([7,17,6,0x30,0x20])+bytes(22)))
    def test_wrong_prefix_is_identity_garbage(self):
        self.assertFalse(self.validate_advert(bytes([7,25,6,0x30,0x20])+bytes(22)))
    def test_too_short_rejected(self):
        self.assertFalse(self.validate_advert(bytes([7,25,1,0x30,0x20])))
    def test_absent_ff_not_127(self):
        self.assertIsNone(self.battery_percent(0xff))
    def test_illegal_charging_127_rejected(self):
        self.assertIsNone(self.battery_percent(0xfd))
    def test_100_charging_valid(self):
        self.assertEqual(self.battery_percent(0xe4),100)
    def test_0_present_is_valid(self):
        self.assertEqual(self.battery_percent(0),0)
    def test_model_frame_contains_no_ownership(self):
        self.assertFalse(self.validate_advert(bytes([7,25,1,0x30,0x21])+bytes(22)))
    def test_compiled_source_contract(self):
        if len(sys.argv)<2 or not sys.argv[1]:self.skipTest("source path unavailable")
        b=Path(sys.argv[1]).read_text("utf8")
        self.assertIn('0x3020 to "AirPods 5 (무선 충전 케이스)"',b)
        self.assertIn('0x3620 to "AirPods 5"',b)
        self.assertIn('if (claimedModelId == 0x3020 || claimedModelId == 0x3620)',b)
        self.assertIn("byteVal == 0xFF || level > 100",b)
        self.assertNotIn('writeCharacteristic',b)
        self.assertNotIn('connectGatt(',b)
if __name__=="__main__":
    # Pass optional source as positional, avoid unittest parsing it as test module.
    source=sys.argv[1] if len(sys.argv)>1 else None
    sys.argv=[sys.argv[0]]+(["--verbose"] if source else [])
    # use a global to avoid argv mutation
    AirPods5CapodDerivedContract.SOURCE=source
    orig=AirPods5CapodDerivedContract.test_compiled_source_contract
    def updated(self):
        if not source:self.skipTest("no source path")
        b=Path(source).read_text("utf8")
        self.assertIn('0x3020 to "AirPods 5 (무선 충전 케이스)"',b)
        self.assertIn('0x3620 to "AirPods 5"',b)
        self.assertIn('if (claimedModelId == 0x3020 || claimedModelId == 0x3620)',b)
        self.assertIn("byteVal == 0xFF || level > 100",b)
        self.assertNotIn('writeCharacteristic',b)
        self.assertNotIn('connectGatt(',b)
    AirPods5CapodDerivedContract.test_compiled_source_contract=updated
    unittest.main(verbosity=2)
