#!/usr/bin/env python3
"""No-device, synthetic-HCI checks: old UUID/encryption must never leak between sessions."""
import struct, tempfile, unittest
from pathlib import Path
from case_session_evidence import audit_file, difference, control

def evt(code, body):
    return bytes((4,code,len(body))) + body

def connect(h=7, extended=False):
    return evt(0x3E, bytes((10 if extended else 1,0)) + struct.pack("<H",h) + bytes(15))

def disconnect(h=7):
    return evt(5, b"\x00" + struct.pack("<H",h) + b"\x13")

def encrypted(h=7, on=1):
    return evt(8, b"\x00" + struct.pack("<H",h) + bytes((on,)))

def att(data,h=7):
    block = struct.pack("<HH",len(data),4) + data
    return b"\x02" + struct.pack("<HH",0x2000|h,len(block)) + block

def store(path,seq):
    with path.open("wb") as fp:
        fp.write(b"btsnoop\0"+struct.pack(">II",1,1002))
        for item,flag in seq:
            fp.write(struct.pack(">IIIIQ",len(item),len(item),flag,0,0))
            fp.write(item)

class Tests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory()
        self.path=Path(self.tmp.name)/"synthetic.btsnoop"
    def tearDown(self):
        self.tmp.cleanup()
    def test_disconnect_handle_reuse_clears_uuid_and_encryption(self):
        store(self.path, [
            (connect(),1),(encrypted(),1),
            (att(b"\x05\x01\x34\x12\xCD\xAB"),1), # FIND_INFO response: UUID 0xABCD
            (att(b"\x12\x34\x12\xAA"),0),
            (disconnect(),1),(connect(),1),
            (att(b"\x12\x34\x12\xBB"),0)])
        r=audit_file(self.path)
        self.assertEqual(len(r["writes"]),2)
        old,new=r["writes"]
        self.assertEqual(old["characteristic_uuid_this_session"],"0xABCD")
        self.assertTrue(old["hci_link_encryption_observed"])
        self.assertIsNone(new["characteristic_uuid_this_session"])
        self.assertFalse(new["hci_link_encryption_observed"])
        self.assertNotEqual(old["session_epoch"],new["session_epoch"])
        self.assertEqual(r["case_speaker_sound"],"NOT_ESTABLISHED")
        self.assertFalse(old["owner_auth_verified"])
        self.assertNotIn("payload_hex",repr(r))
    def test_failed_encryption_is_not_credential(self):
        store(self.path, [(connect(),1),(encrypted(on=0),1),
                          (att(b"\x12\x01\x00\x99"),0)])
        self.assertFalse(audit_file(self.path)["writes"][0]["hci_link_encryption_observed"])
    def test_encrypted_att_is_not_owner_auth(self):
        store(self.path, [(connect(),1),(encrypted(),1),
                          (att(b"\x52\x01\x00\x99"),0)])
        r=audit_file(self.path)
        self.assertEqual(r["owner_authentication"],"NOT_ESTABLISHED")
        self.assertFalse(r["writes"][0]["case_speaker_beep_verified"])
        self.assertEqual(difference({"writes":[]},r)["candidates_only"],[])
    def test_only_mapped_uuid_can_appear_as_nonproof_candidate(self):
        store(self.path, [(connect(),1),(encrypted(),1),
                          (att(b"\x05\x01\x34\x12\xCD\xAB"),1),
                          (att(b"\x12\x34\x12\x99"),0)])
        r=audit_file(self.path)
        comp=difference({"writes":[]},r)
        self.assertEqual(comp["candidates_only"][0]["uuid"],"0xABCD")
        self.assertFalse(comp["case_beep_proof"])
    def test_enhanced_connection(self):
        self.assertEqual(control(connect(15,True)),("connect",15,True))
if __name__=="__main__":
    unittest.main(verbosity=2)
