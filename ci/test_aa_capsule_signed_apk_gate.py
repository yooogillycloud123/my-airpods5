#!/usr/bin/env python3
"""Synthetic contract tests: only original helper bytes may pass, no fake PASS."""
import hashlib, importlib.util, io, json, tempfile, unittest, zipfile
from pathlib import Path
p=Path(__file__).with_name("aa_capsule_signed_apk_gate.py")
spec=importlib.util.spec_from_file_location("gate",p);g=importlib.util.module_from_spec(spec);spec.loader.exec_module(g)
class TestReleaseContract(unittest.TestCase):
    def test_exact_helper_required(self):
        with tempfile.TemporaryDirectory() as d:
            zpath=Path(d)/"bundle.zip"
            with zipfile.ZipFile(zpath,"w") as z:
                for key in g.HELPERS:
                    z.writestr(key,b"synthetic-no-private-original")
            with self.assertRaisesRegex(ValueError,"ORIGINAL_HELPER_DIGEST_DRIFT"):
                g.inspect_original_helper_bundle(zpath)
    def test_missing_helper_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            zpath=Path(d)/"bundle.zip"
            with zipfile.ZipFile(zpath,"w") as z:z.writestr("manifest.json",'{}')
            with self.assertRaisesRegex(ValueError,"MISSING_ORIGINAL_HELPER"):
                g.inspect_original_helper_bundle(zpath)
    def test_empty_apk_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            path=Path(d)/"fake.apk";path.write_bytes(b"bad")
            with self.assertRaisesRegex(ValueError,"APK_FILE_SIZE"):
                g.inspect_signed_apk(path)
    def test_const_matches_golden(self):
        self.assertEqual(len(g.HELPERS),3)
        self.assertEqual(g.PKG,"com.aaairpods.r25fix")
        self.assertEqual(len(g.CERT),64)
        self.assertEqual(len(g.CONTROLLER_SHA),64)
if __name__=="__main__":unittest.main(verbosity=2)
