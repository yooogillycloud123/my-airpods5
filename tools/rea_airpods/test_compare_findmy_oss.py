import hashlib
import json
import subprocess
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch

from compare_findmy_oss import analyze, classify_case_proof, METHODS
from run_owned_apk_audit import preflight_apk

class TestFindMyCompare(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.apk=Path(self.temp.name)/"oss.apk"
        with zipfile.ZipFile(self.apk,"w") as z:
            z.writestr("AndroidManifest.xml",b"manifest")
            z.writestr("classes.dex",b"smali")
        self.sha=hashlib.sha256(self.apk.read_bytes()).hexdigest()

    def test_measured_user_results_not_invented(self):
        obs=classify_case_proof()
        self.assertEqual(obs["user_v139_findmy_nearby_1202"],13)
        self.assertEqual(obs["user_v139_findmy_separated_1219_connectable"],0)
        self.assertEqual(obs["user_v139_temp_addresses_observed"],5)
        self.assertFalse(obs["closed_case_beep_proven"])

    def test_digest_denied(self):
        with self.assertRaises(ValueError):
            preflight_apk(self.apk,"0"*64)

    def test_real_rea_method_path_exercised(self):
        calls=[]
        def mocked_cli(args,timeout=780):
            calls.append(args)
            if args[1]=="inspect-android-package":
                return {"normalized_result":{"engine":"jadx","package":"de.seemoo.at_tracking_detection"}}
            if args[1]=="search-android-classes":
                return {"normalized_result":{"matches":[{"class_name":args[3]}]}}
            if args[1]=="inspect-android-method":
                return {"normalized_result":{"body_status":"available","source":{"status":"complete","text":"this is decompiled source text with enough bytes and a method body","reported_total_bytes":64}}}
            raise AssertionError(args)
        out=Path(self.temp.name)/"safe.json"
        with patch("compare_findmy_oss.cli_json",side_effect=mocked_cli):
            result=analyze(self.apk,self.sha,out)
        self.assertEqual(result["rea_jadx_package_inspection"],"PASS")
        self.assertEqual(len(calls),1+2+len(METHODS))
        self.assertTrue(all(x[-1]=="--json" for x in calls))
        self.assertTrue(all(x=="DECOMPILED_BODY_OBSERVED" for x in result["methods"].values()))
        self.assertFalse(result["claim_user_closed_a3530_beep"])
        self.assertFalse(result["raw_decompilation_released"])
        self.assertNotIn('"source":',out.read_text())

    def test_bad_engine_fails_closed(self):
        with patch("compare_findmy_oss.cli_json",return_value={"normalized_result":{}}):
            with self.assertRaisesRegex(RuntimeError,"JADX provider"):
                analyze(self.apk,self.sha,Path(self.temp.name)/"no.json")

    def test_obfuscated_binary_is_unknown_not_unsound_false(self):
        def mocked_cli(args,timeout=780):
            if args[1]=="inspect-android-package":
                return {"normalized_result":{"engine":"jadx"}}
            return {"normalized_result":{"matches":[]}}
        with patch("compare_findmy_oss.cli_json",side_effect=mocked_cli):
            r=analyze(self.apk,self.sha,Path(self.temp.name)/"obfuscated.json")
        self.assertEqual(r["methods"]["tryStartProtocol"],"PROGUARD_OR_MISSING_CLASS_UNRESOLVED")
        self.assertFalse(r["claim_authenticated_case_findmy_start_stop"])

    def test_external_apk_never_executes(self):
        source=Path(__file__).with_name("compare_findmy_oss.py").read_text()
        self.assertNotIn("adb shell",source)
        self.assertNotIn("connectGatt(",source)
        self.assertNotIn("writeCharacteristic(",source)
        self.assertNotIn("createBond(",source)

if __name__=="__main__":
    unittest.main()
