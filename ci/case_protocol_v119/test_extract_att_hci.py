import struct
import tempfile
import unittest
from pathlib import Path
from extract_att_hci import extract

def hci(cid, payload):
    block = struct.pack("<HH",len(payload),cid)+payload
    return bytes([2])+struct.pack("<HH",0x2007,len(block))+block

def saved(path, seq):
    data=b"btsnoop\x00"+struct.pack(">II",1,1002)
    for value,flag in seq:
        data+=struct.pack(">IIIIQ",len(value),len(value),flag,0,0)+value
    path.write_bytes(data)

class BtsnoopEvidenceTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory()
        self.path=Path(self.temp.name)/"log"
    def tearDown(self):
        self.temp.cleanup()
    def test_att_write_and_notifications(self):
        saved(self.path,[(hci(4,b"\x12\x34\x12\xaa\xbb"),0),
                         (hci(4,b"\x13"),1),
                         (hci(4,b"\x1b\x35\x12\x99"),1)])
        got=extract(self.path)
        self.assertEqual([e["type"] for e in got["events"]],
                         ["WRITE_REQ","WRITE_RSP","NOTIFY"])
        self.assertEqual(got["events"][0]["attribute_handle"],"0x1234")
        self.assertEqual(got["events"][0]["value_length"],2)
        self.assertNotIn("aabb",repr(got))
        self.assertEqual(got["case_identity"],"UNKNOWN")
    def test_smp_not_att(self):
        saved(self.path,[(hci(6,b"\x11\x22\x33"),0)])
        self.assertEqual(extract(self.path)["events"],[])
    def test_owner_auth_not_assumed(self):
        saved(self.path,[(bytes([4,8,4,0,7,0,1]),3)])
        got=extract(self.path)
        self.assertEqual(got["events"][0]["type"],"ENCRYPTION_CHANGE")
        self.assertEqual(got["case_owner_authentication"],"UNKNOWN")
    def test_explicit_local_reveal(self):
        saved(self.path,[(hci(4,b"\x52\x34\x12\xaa\xbb"),0)])
        got=extract(self.path,private=True)
        self.assertEqual(got["events"][0]["private_value_hex"],"aabb")
        self.assertEqual(got["case_sound_effect"],"NOT_OBSERVED")
    def test_bad_magic(self):
        self.path.write_bytes(b"wrong")
        with self.assertRaises(ValueError):extract(self.path)
    def test_service_discovery_no_identity(self):
        saved(self.path,[(hci(4,b"\x10\x01\x00\xff\xff\x00\x28"),0)])
        got=extract(self.path)
        self.assertEqual(got["events"][0]["type"],"READ_BY_GROUP_REQ")
        self.assertEqual(got["case_identity"],"UNKNOWN")

if __name__=="__main__":
    unittest.main(verbosity=2)
