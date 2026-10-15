#!/usr/bin/env python3
"""AirPods charging-case OWNER-TRANSPORT evidence: local btsnoop H4 ONLY.

Unlike prior ATT CID4-only parser, maps BR/EDR classic L2CAP signalling
(CID=1, including PSM 0x001F for ATT-over-classic) and LE L2CAP signalling
(CID=5, LE credit-based channels). This is NOT a discovered case service.
Each HCI connection epoch resets CID maps: no device/mac/keys/raw payloads leak.
L2CAP transport and encrypted HCI link are *not* case-owner auth, and a write
ACK is *not* actual physical CASE beep. No Bluetooth connection/radio/write.
"""
from __future__ import annotations
from collections import Counter, defaultdict
import argparse
import json
import struct
import sys
from pathlib import Path

sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"case_protocol_v119"))
from extract_att_hci import records

MAX_SDU=65535

class TransportAudit:
    def __init__(self):
        self.epoch=defaultdict(int)
        self.connected=defaultdict(bool)
        self.encrypted=defaultdict(bool)
        self.maps={}          # (hci, epoch, direction, CID) => psm hint
        self.fragments={}     # (hci, direction) => [needed, cid, data]
        self.requests={}      # (hci, epoch, direction, signal identifier) -> {psm,scid}
        self.counters=Counter()
        self.events=[]
    def event(self,frame,hci,kind,**kw):
        self.counters[kind]+=1
        self.events.append(dict(frame=frame,connection="hci_%04X"%hci,
            epoch=self.epoch[hci],event=kind,hci_encrypted=self.encrypted[hci],
            **kw))
    def reset(self,hci):
        self.epoch[hci]+=1
        self.encrypted[hci]=False
        self.connected[hci]=False
        for table in (self.maps,self.requests,self.fragments):
            for k in list(table):
                if k[0]==hci: del table[k]
    def process(self,frame,flags,raw):
        if len(raw)<1:return
        if raw[0]==4 and len(raw)>=3:
            if len(raw)<3+raw[2]:
                self.counters["HCI_TRUNCATED"]+=1;return
            code,p=raw[1],raw[3:3+raw[2]]
            if code==0x05 and len(p)>=4:
                handle=int.from_bytes(p[1:3],"little")&0xFFF
                self.reset(handle)
                self.event(frame,handle,"HCI_DISCONNECT")
            elif code==0x3E and len(p)>=4 and p[0] in (1,10):
                handle=int.from_bytes(p[2:4],"little")&0xFFF
                self.reset(handle)
                self.connected[handle]=p[1]==0
                self.event(frame,handle,"HCI_LE_CONNECTED" if p[1]==0 else "HCI_LE_CONNECT_FAILED")
            elif code==0x03 and len(p)>=11:
                # Classic BR/EDR Connection Complete; p= status,handle,BD_ADDR(6),link type, encryption
                handle=int.from_bytes(p[1:3],"little")&0xFFF
                self.reset(handle)
                self.connected[handle]=p[0]==0
                self.event(frame,handle,"HCI_CLASSIC_CONNECTED" if p[0]==0 else "HCI_CLASSIC_FAILED")
            elif code==0x08 and len(p)>=4:
                handle=int.from_bytes(p[1:3],"little")&0xFFF
                self.encrypted[handle]=p[0]==0 and p[3]!=0
                self.event(frame,handle,"HCI_ENCRYPTION_CHANGED",
                           link_cipher_active=self.encrypted[handle])
            return
        if raw[0]!=2 or len(raw)<5:return
        hf,acl_size=struct.unpack_from("<HH",raw,1)
        hci,pb=hf&0xFFF,(hf>>12)&3
        payload=raw[5:]
        if acl_size!=len(payload):
            self.counters["ACL_TRUNCATED"]+=1;return
        direction="rx" if flags&1 else "tx"
        key=(hci,direction)
        if pb==1:
            state=self.fragments.get(key)
            if state is None:self.counters["ACL_ORPHAN_FRAGMENT"]+=1;return
            n,cid,buf=state
            buf+=payload
            if len(buf)>MAX_SDU:self.fragments.pop(key,None);self.counters["ACL_OVERSIZE"]+=1;return
            if len(buf)>=n:
                self.fragments.pop(key,None)
                self.l2cap(frame,hci,direction,cid,buf[:n])
            else:self.fragments[key]=[n,cid,buf]
            return
        if pb not in (0,2,3) or len(payload)<4:return
        n,cid=struct.unpack_from("<HH",payload)
        if n>MAX_SDU:self.counters["L2CAP_OVERSIZE"]+=1;return
        body=payload[4:]
        if len(body)<n:self.fragments[key]=[n,cid,body]
        else:self.l2cap(frame,hci,direction,cid,body[:n])

    def l2cap(self,frame,hci,direction,cid,body):
        if cid in (1,5):
            k=0
            while k+4<=len(body):
                code,identifier,n=struct.unpack_from("<BBH",body,k)
                k+=4
                if n>len(body)-k:
                    self.counters["L2CAP_SIGNAL_TRUNCATED"]+=1;break
                data=body[k:k+n];k+=n
                self.signal(frame,hci,direction,cid,code,identifier,data)
            return
        kind=self.maps.get((hci,self.epoch[hci],direction,cid))
        if cid==4:
            opcode=body[0] if body else None
            if opcode in (0x12,0x52,0xD2,0x16):
                self.event(frame,hci,"ATT_WRITE_CID4",
                           direction=direction,att_opcode="0x%02x"%opcode,
                           payload_length=max(0,len(body)-3),target_owner_verified=False)
            else:self.counters["ATT_OTHER"]+=1
        elif kind is not None:
            self.event(frame,hci,"DYNAMIC_CHANNEL_DATA",
                       direction=direction,transport_psm="0x%04X"%kind,
                       sdu_size=len(body),owner_auth_verified=False)
        elif cid>=0x0040:
            self.counters["DYNAMIC_CID_UNCLASSIFIED"]+=1
    def signal(self,frame,hci,direction,cid,code,identifier,data):
        classic=cid==1
        opp="rx" if direction=="tx" else "tx"
        if classic and code==2 and len(data)>=4: # Connection Request PSM, source CID
            psm,scid=struct.unpack_from("<HH",data)
            self.requests[(hci,self.epoch[hci],direction,identifier)]=(psm,scid)
            self.maps[(hci,self.epoch[hci],direction,scid)]=psm
            self.event(frame,hci,"CLASSIC_PSM_REQUEST",
                       direction=direction,psm="0x%04X"%psm,request_source_cid="0x%04X"%scid,
                       classic_att_psm_001f_candidate=(psm==0x001F))
        elif classic and code==3 and len(data)>=8:
            dcid,scid,result,status=struct.unpack_from("<HHHH",data)
            req=self.requests.get((hci,self.epoch[hci],opp,identifier))
            if req and result==0:
                psm=req[0]
                self.maps[(hci,self.epoch[hci],opp,scid)]=psm
                self.maps[(hci,self.epoch[hci],direction,dcid)]=psm
            self.event(frame,hci,"CLASSIC_PSM_RESPONSE",direction=direction,
                       accepted=result==0,response_result=result,psm="0x%04X"%req[0] if req else None,
                       owner_authenticated=False)
        elif not classic and code in (0x14,0x17) and len(data)>=10:
            # LE credit-based connection request (0x14) / enhanced (0x17) uses PSM/MTU/MPS/credits
            psm,scid=struct.unpack_from("<HH",data)
            self.requests[(hci,self.epoch[hci],direction,identifier)]=(psm,scid)
            self.maps[(hci,self.epoch[hci],direction,scid)]=psm
            self.event(frame,hci,"LE_COC_REQUEST",direction=direction,
                       psm="0x%04X"%psm,source_cid="0x%04X"%scid)
        elif not classic and code in (0x15,0x18) and len(data)>=10:
            dcid,mtu,mps,credits,result=struct.unpack_from("<HHHHH",data)
            req=self.requests.get((hci,self.epoch[hci],opp,identifier))
            if req and result==0:
                self.maps[(hci,self.epoch[hci],opp,req[1])]=req[0]
                self.maps[(hci,self.epoch[hci],direction,dcid)]=req[0]
            self.event(frame,hci,"LE_COC_RESPONSE",direction=direction,
                       accepted=result==0,result=result,psm="0x%04X"%req[0] if req else None,
                       owner_authenticated=False)
        else:
            self.counters["OTHER_L2CAP_SIGNAL"]+=1

    def result(self):
        return {
            "origin":"OFFLINE_BT_H4_TRANSPORT_EVIDENCE",
            "model":"UNATTESTED",
            "actual_airpods5_case_identity":"NOT_PROVEN",
            "case_owner_authentication":"NOT_PROVEN",
            "physical_case_speaker_sound":"NOT_PROVEN",
            "br_edr_att_psm_001f_is_possible_transport_NOT_CASE_IDENTIFICATION":True,
            "link_encryption_is_not_case_owner_authentication":True,
            "no_private_addresses_or_payloads_exported":True,
            "never_issues_radio_or_gatt_commands":True,
            "counters":dict(self.counters),"events":self.events
        }

def audit(path:Path):
    a=TransportAudit()
    for frame,flags,raw in records(path):a.process(frame,flags,raw)
    a.counters["incomplete_acl_fragments"]=len(a.fragments)
    return a.result()

if __name__=="__main__":
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("btsnoop",type=Path)
    p.add_argument("--output",type=Path)
    a=p.parse_args()
    rendered=json.dumps(audit(a.btsnoop),ensure_ascii=False,indent=2)+"\n"
    if a.output:
        with a.output.open("x",encoding="utf8") as fp:fp.write(rendered)
    else:print(rendered)
