#!/usr/bin/env python3
"""Synthetic L2CAP signaling regression; never checks actual Apple hardware."""
import struct
import unittest
from l2cap_owner_trace import TransportAudit

def acl(h,cid,p):
    body=struct.pack("<HH",len(p),cid)+p
    return bytes([2])+struct.pack("<HH",0x2000|h,len(body))+body
def cmd(op,ident,p):
    return struct.pack("<BBH",op,ident,len(p))+p

class Tests(unittest.TestCase):
    def test_classic_att_psm_detection(self):
        a=TransportAudit()
        a.process(1,0,acl(3,1,cmd(2,7,struct.pack("<HH",0x1f,0x44))))
        self.assertEqual(a.result()["events"][-1]["psm"],"0x001F")
    def test_classic_response_and_dynamic_cid(self):
        a=TransportAudit()
        a.process(1,0,acl(3,1,cmd(2,7,struct.pack("<HH",0x1f,0x44))))
        a.process(2,1,acl(3,1,cmd(3,7,struct.pack("<HHHH",0x55,0x44,0,0))))
        a.process(3,1,acl(3,0x55,b"hello"))
        self.assertEqual(a.result()["events"][-1]["transport_psm"],"0x001F")
    def test_ble_coc_connection(self):
        a=TransportAudit()
        a.process(1,0,acl(3,5,cmd(0x14,6,struct.pack("<HHHHH",0x80,0x41,120,120,12))))
        self.assertEqual(a.result()["events"][-1]["event"],"LE_COC_REQUEST")
    def test_ble_coc_response(self):
        a=TransportAudit()
        a.process(1,0,acl(3,5,cmd(0x14,6,struct.pack("<HHHHH",0x80,0x41,120,120,12))))
        a.process(2,1,acl(3,5,cmd(0x15,6,struct.pack("<HHHHH",0x55,120,120,12,0))))
        a.process(3,1,acl(3,0x55,b"test"))
        self.assertEqual(a.result()["events"][-1]["transport_psm"],"0x0080")
    def test_false_owner_not_claimed(self):
        a=TransportAudit()
        a.process(1,0,acl(3,1,cmd(2,7,struct.pack("<HH",0x1f,0x44))))
        self.assertEqual(a.result()["case_owner_authentication"],"NOT_PROVEN")
        self.assertEqual(a.result()["physical_case_speaker_sound"],"NOT_PROVEN")
    def test_session_epoch_reset(self):
        a=TransportAudit()
        a.process(1,0,acl(3,1,cmd(2,7,struct.pack("<HH",0x1f,0x44))))
        a.reset(3)
        self.assertEqual(len(a.maps),0)
        self.assertEqual(a.epoch[3],1)
    def test_att_write_no_beep_claim(self):
        a=TransportAudit()
        a.process(1,0,acl(3,4,b"\x52\x10\x00\x50\x51"))
        self.assertEqual(a.result()["events"][-1]["event"],"ATT_WRITE_CID4")
        self.assertFalse(a.result()["events"][-1]["target_owner_verified"])
    def test_bad_l2cap_signal_suppressed(self):
        a=TransportAudit()
        a.process(1,0,acl(3,1,b"\x02\x04\xff\xff"))
        self.assertEqual(a.result()["counters"]["L2CAP_SIGNAL_TRUNCATED"],1)
    def test_not_export_application_bytes(self):
        a=TransportAudit()
        a.process(1,0,acl(3,4,b"\x52\x10\x00payload_example"))
        self.assertNotIn("payload_example",str(a.result()))
    def test_no_radio_functions(self):
        import inspect
        src=inspect.getsource(TransportAudit)
        for api in ("startScan(", "connectGatt(", "writeCharacteristic("):
            self.assertNotIn(api,src)

if __name__=="__main__":unittest.main(verbosity=2)
