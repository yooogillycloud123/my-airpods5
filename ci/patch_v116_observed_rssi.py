#!/usr/bin/env python3
from pathlib import Path
import sys
base=Path(sys.argv[1])
k=base/"android/app/src/main/java/me/kavishdevar/librepods/FindMyRadioAudit.kt"
g=base/"android/app/build.gradle.kts"
s=k.read_text("utf-8")
changes=[
("        private var type12 = 0","        private var type12 = 0\n        private var short12 = 0\n        private var long12 = 0\n        private val shortSignal = mutableListOf<Int>()\n        private val longSignal = mutableListOf<Int>()"),
("                type12 += 1\n                if (result.isConnectable)","                type12 += 1\n                if (subtype == 2 && data.size == 4) { short12++; shortSignal.add(result.rssi) }\n                if (subtype == 2 && data.size == 23) { long12++; longSignal.add(result.rssi) }\n                if (result.isConnectable)"),
('            val body = "전체 BLE 콜백: " + all +','            fun median(v: List<Int>): String = if (v.isEmpty()) "없음" else v.sorted()[v.size/2].toString()\n            val body = "12:02 크기4: " + short12 + " / 중앙 RSSI: " + median(shortSignal) + " dBm" +\n                "\\n12:02 크기23: " + long12 + " / 중앙 RSSI: " + median(longSignal) + " dBm" +\n                "\\n실제 케이스 식별은 아직 되지 않았습니다.\\n" +\n                "전체 BLE 콜백: " + all +')
]
for a,b in changes:
    assert s.count(a)==1,a
    s=s.replace(a,b,1)
k.write_text(s,"utf-8")
a=g.read_text("utf-8")
assert a.count("versionCode = 10015")==1
assert a.count('versionName = "1.0.15-findmy-radio-audit"')==1
g.write_text(a.replace("versionCode = 10015","versionCode = 10016").replace('versionName = "1.0.15-findmy-radio-audit"','versionName = "1.0.16-findmy-rssi"'),"utf-8")
print("V116_AGGREGATE_RSSI_DIAGNOSTICS=PASS")
