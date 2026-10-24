import json
import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from importlib.util import spec_from_file_location, module_from_spec

p = Path(__file__).resolve().parents[1] / "src" / "jmemo.py"
spec = spec_from_file_location("jmemo", p)
mod = module_from_spec(spec)
spec.loader.exec_module(mod)


class StorageTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.folder = Path(self.tmp.name)
        self.db = mod.Store(self.folder)

    def tearDown(self):
        self.tmp.cleanup()

    def test_new_note(self):
        note = self.db.create()
        self.assertEqual(note["group"], "기본")
        self.assertEqual(len(self.db.notes), 1)

    def test_korean_unicode_roundtrip(self):
        self.db.create()["text"] = "한글 입력\n두 번째 줄"
        self.db.save()
        db2 = mod.Store(self.folder)
        self.assertEqual(db2.notes[0]["text"], "한글 입력\n두 번째 줄")

    def test_existing_v01_shape(self):
        note = mod.new_note()
        note.update({"due": "", "notified": False, "favorite": True, "opacity": .7})
        self.db.data["notes"].append(note)
        self.db.save()
        self.assertTrue(mod.Store(self.folder).notes[0]["favorite"])

    def test_corrupt_does_not_get_overwritten(self):
        broken = "{garbled json"
        self.db.path.write_text(broken, encoding="utf-8")
        with self.assertRaises(RuntimeError):
            mod.Store(self.folder)
        self.assertEqual(self.db.path.read_text(encoding="utf-8"), broken)

    def test_duplicate_id_rejected(self):
        note = mod.new_note()
        self.db.data["notes"] = [note, dict(note)]
        with self.assertRaises(ValueError):
            self.db.save()

    def test_startup_backup(self):
        self.db.create()
        mod.Store(self.folder)
        self.assertEqual(len(list((self.folder/"backups").glob("*startup.json"))), 1)

    def test_never_reads_memoit_path(self):
        self.assertNotIn("memodb", str(mod.data_folder()).lower())
        self.assertEqual(self.db.path.name, "notes.json")


if __name__ == "__main__":
    unittest.main()
