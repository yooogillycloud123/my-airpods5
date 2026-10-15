import unittest
from extract_att_hci import AttExtractor

class Mapping(unittest.TestCase):
    def test_uuid(self):
        x=AttExtractor()
        x.l2cap(1,7,"tx",4,bytes.fromhex("080100ffff0328"))
        x.l2cap(2,7,"rx",4,bytes.fromhex("09070100020300002a"))
        x.l2cap(3,7,"tx",4,bytes.fromhex("120300bf"))
        self.assertEqual(x.result()["events"][2]["observed_uuid"],"0x2A00")
        self.assertEqual(x.result()["case_identity"],"UNKNOWN")
    def test_service(self):
        x=AttExtractor()
        x.l2cap(1,7,"rx",4,bytes.fromhex("1106010005000018"))
        self.assertEqual(x.result()["events"][0]["observed_services"][0]["uuid"],"0x1800")
if __name__=="__main__":unittest.main()
