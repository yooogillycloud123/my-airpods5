import hashlib
import json
import subprocess
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch
from run_owned_apk_audit import cli_json, preflight_apk, run, CLASSES, OWN_PACKAGE

class IntegrationTests(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.dir.cleanup)
        self.path = Path(self.dir.name) / "personal.apk"
        with zipfile.ZipFile(self.path, "w") as z:
            z.writestr("AndroidManifest.xml", b"test-manifest")
            z.writestr("classes.dex", b"dex-placeholder")
            z.writestr("classes2.dex", b"dex-placeholder")
        self.digest = hashlib.sha256(self.path.read_bytes()).hexdigest()

    def test_preflight_validates_owned_input_digest(self):
        status = preflight_apk(self.path, self.digest)
        self.assertEqual(status["dex_file_count"], 2)
        self.assertEqual(status["sha256"], self.digest)

    def test_different_sha_denied_before_any_tool(self):
        with self.assertRaisesRegex(ValueError, "digest mismatch"):
            preflight_apk(self.path, "0"*64)

    def test_missing_manifest_denied(self):
        with zipfile.ZipFile(self.path, "w") as z:
            z.writestr("classes.dex", b"fixture")
        digest = hashlib.sha256(self.path.read_bytes()).hexdigest()
        with self.assertRaisesRegex(ValueError, "manifest"):
            preflight_apk(self.path, digest)

    def test_owns_cli_arg_escaping_no_shell(self):
        def fake_run(cmd, **kwargs):
            self.assertEqual(cmd[0], "rea")
            self.assertFalse(kwargs["check"])
            self.assertTrue("stdout" in kwargs)
            self.assertNotIn("shell", kwargs)
            return subprocess.CompletedProcess(cmd, 0, '{"tool":"jadx"}', '')
        with patch("run_owned_apk_audit.subprocess.run", fake_run):
            self.assertEqual(cli_json(["rea", "inspect-android-package", str(self.path), "--json"])["tool"], "jadx")

    def test_real_stages_need_consistent_own_package_not_just_dex(self):
        calls = []
        def mock_rea(cmd, **kwargs):
            calls.append(cmd)
            if "inspect-android-package" in cmd:
                data = {"normalized_result":{"package":OWN_PACKAGE,"provider":"jadx"}}
            else:
                data = {"normalized_result":{"matches":[{"class_name":cmd[3]}]}}
            return subprocess.CompletedProcess(cmd, 0, json.dumps(data), "")
        out = Path(self.dir.name) / "summary.json"
        with patch("run_owned_apk_audit.subprocess.run", mock_rea):
            result = run(self.path, self.digest, out)
        self.assertEqual(result["rea_inspect_package"], "PASS")
        self.assertEqual(len(calls), 1 + len(CLASSES))
        self.assertTrue(all(c[-1] == "--json" for c in calls))
        self.assertFalse(result["case_speaker_command_recovered"])
        self.assertFalse(result["closed_case_speaker_sound_verified"])
        self.assertNotIn("normalized_result", out.read_text())
        self.assertFalse(result["raw_evidence_uploaded"])

    def test_untrusted_provider_response_rejected(self):
        def mock_rea(cmd, **kwargs):
            return subprocess.CompletedProcess(cmd, 0, '{"normalized_result":{}}', "")
        with patch("run_owned_apk_audit.subprocess.run", mock_rea):
            with self.assertRaisesRegex(RuntimeError, "unverified"):
                run(self.path, self.digest, Path(self.dir.name)/"out.json")

    def test_failure_cannot_dump_raw_apk_data(self):
        secret="PRIVATE_BLE_PAIRING_MATERIAL_123"
        def mock_rea(cmd, **kwargs):
            return subprocess.CompletedProcess(cmd, 1, secret, secret)
        with patch("run_owned_apk_audit.subprocess.run", mock_rea):
            with self.assertRaises(RuntimeError) as ctx:
                cli_json(["rea", "inspect-android-package", str(self.path)])
        self.assertNotIn(secret, str(ctx.exception))

if __name__ == "__main__":
    unittest.main()
