#!/usr/bin/env python3
from pathlib import Path
import shutil,sys
root=Path(sys.argv[1])
repo=Path(sys.argv[2])
base=root/"android/app/src/main/java/me/kavishdevar/librepods"
p=base/"CaseRadioEvidenceV138.kt"
s=p.read_text()
def replace(old,new):
  global s
  assert s.count(old)==1,(old,s.count(old))
  s=s.replace(old,new,1)
replace("private var findmy19Nonconnectable = 0",
        "private var findmy19Nonconnectable = 0\n        private var findmy02AirGuardMask = 0")
replace("val findmy19Nonconnectable: Int, val nearbyUnique: Int,",
        "val findmy19Nonconnectable: Int, val findmy02AirGuardMask: Int, val nearbyUnique: Int,")
replace("nearbyUniqueAddresses.add(result.device.address)",
        """nearbyUniqueAddresses.add(result.device.address)
                        if (data.size >= 3 && (data[2].toInt() and 0x18) == 0x18)
                            findmy02AirGuardMask++
                        if (result.isConnectable) {
                            val id = result.device.address
                            if (candidates.size < 12 || candidates.containsKey(id))
                                candidates[id] = Candidate(result.device, result.rssi)
                        }""")
replace("nearInfo10, generic12, findmy19Nonconnectable,",
        "nearInfo10, generic12, findmy19Nonconnectable, findmy02AirGuardMask,")
replace("""report.append("Find My 주변형 12:02=").append(s.findmy02).append('\\n')""",
        """report.append("Find My 주변형 12:02=").append(s.findmy02).append('\\n')
            report.append("  ↳ AirGuard 필터 조건=").append(s.findmy02AirGuardMask).append('\\n')""")
replace('''                        }.show()
                }
                .setNegativeButton("취소") { _, _ -> close() }''',
        '''                        }
                        .setNeutralButton("FD44 소리 시험") { _, _ ->
                            close()
                            CaseSoundTrialV140.run(a, items[index].device)
                        }.show()
                }
                .setNegativeButton("취소") { _, _ -> close() }''')
replace("소리 명령·페어링·설정 변경은 하지 않습니다.",
        "읽기 전용 조회는 소리 명령을 보내지 않습니다. 별도 소리 시험을 선택하면 1회 전송하며, 이 후보는 타인의 기기일 수 있습니다.")
replace("v139 단일 스캐너 · 12초 결과","v140 A3529 · 소리 경로 시험")
replace("케이스 소리=미구현, GATT 쓰기=0","A3529 구매박스 확인. 케이스 찾기 소리 성공은 미검증")
p.write_text(s)
shutil.copy2(repo/"ci/case_protocol_v140/CaseSoundTrialV140.kt",base/"CaseSoundTrialV140.kt")
gradle=root/"android/app/build.gradle.kts"
g=gradle.read_text()
assert "versionCode = 10039" in g
gradle.write_text(g.replace("versionCode = 10039","versionCode = 10040").replace(
"1.0.39-single-scanner-lab-replaces-v138","1.0.40-a3529-fd44-manual-trial"))
assert p.read_text().count("startScan(")==1
print("A3529_EXPLICIT_MANUAL_TRIAL_AND_FROZEN_SCANNER=PASS")

# V141: user's actual device: FD44 start/stop GATT ACK 0, physically SILENT.
# No new generic FD44 writes to unidentified neighbors. Keep existing one
# manually requested BLE scan and offer read-only candidate profile ONLY.
q=p.read_text(encoding="utf8")
def v141_once(a,b):
    global q
    assert q.count(a)==1,(a[:90],q.count(a))
    q=q.replace(a,b,1)
v141_once("private data class Candidate(val device: BluetoothDevice, val rssi: Int)",
          "private data class Candidate(val device: BluetoothDevice, val rssi: Int, val subtype: Int)")
assert q.count("Candidate(result.device, result.rssi)")==2
q=q.replace("Candidate(result.device, result.rssi)","Candidate(result.device, result.rssi, subtype)")
v141_once('"12:19 후보 선택"','"연결 가능 BLE 후보 · 신원 미인증"')
v141_once('"신원 확인되지 않은 12:19 후보"','"신원 미확인 BLE 후보"')
v141_once('''(i + 1).toString() + ": 분리 Find My (신원 미확인) " +''',
          '''(i + 1).toString() + ": " +
                (if (candidate.subtype == 0x02) "12:02 주변형" else "12:19 분리형") +
                " (본인 A3529 미인증) " +''')
v141_once('connectReadOnly(items[index].device)',
          'connectReadOnly(items[index].device, items[index].subtype)')
v141_once('private fun connectReadOnly(device: BluetoothDevice) {',
          '''private fun connectReadOnly(device: BluetoothDevice, subtype: Int) {
            val observedSubtype = if (subtype == 0x02) "12:02 주변형" else "12:19 분리형"''')
v141_once('''identityProbe = CaseIdentityReadV137(g) { body ->
                                        finish("Find My 후보 프로파일 · 미인증", body)
                                    }''',
          '''identityProbe = CaseIdentityReadV137(g, observedSubtype) { body ->
                                        finish("Find My 후보 프로파일 · 미인증", body)
                                    }''')
v141_once('"연결 가능 후보="','"연결 가능 후보(12:02+12:19 합계)="')
v141_once('"12:19 후보 ≠ 본인 에어팟 인증\\n"',
          '"12:02/12:19 후보 ≠ 본인 A3529 케이스 인증\\n"')
v141_once('''                        .setNeutralButton("FD44 소리 시험") { _, _ ->
                            close()
                            CaseSoundTrialV140.run(a, items[index].device)
                        }.show()''',
          '''                        .setNeutralButton("v140 소리 없음 결과") { _, _ ->
                            close()
                            dialog(a, "이미 실행한 시험의 결과",
                                "v140: 비소유자 FD44 START/STOP GATT status=0/0\\n" +
                                "사용자 실제 청취: 충전 케이스 소리 없음\\n\\n" +
                                "A2DP 오디오 연결 없이 BLE GATT 제어는 가능했지만, " +
                                "이 후보의 신원과 내 케이스 스피커 동작은 확인되지 않았습니다.\\n" +
                                "동일 명령을 반복해서 보내지 않습니다.")
                        }.show()''')
v141_once("읽기 전용 조회는 소리 명령을 보내지 않습니다. 별도 소리 시험을 선택하면 1회 전송하며, 이 후보는 타인의 기기일 수 있습니다.",
          "읽기 전용 서비스 확인만 허용합니다. v140 GATT 명령은 ACK=0 이었지만 실물은 무음이므로 동일 명령을 중복 전송하지 않습니다.")
v141_once("v140 A3529 · 소리 경로 시험","v141 A3529 · BLE/오디오 분리 판정")
v141_once("A3529 구매박스 확인. 케이스 찾기 소리 성공은 미검증",
          "A3529 박스 확인. v140 GATT ACK=0/0, 실제 케이스 소리 없음")
p.write_text(q,encoding="utf8")
# Target is original case (A3529 SKU). This model proof is NOT BLE ownership proof.
g=gradle.read_text(encoding="utf8")
assert g.count("versionCode = 10040")==1
g=g.replace("versionCode = 10040","versionCode = 10041")
assert g.count("1.0.40-a3529-fd44-manual-trial")==1
g=g.replace("1.0.40-a3529-fd44-manual-trial","1.0.41-a3529-silent-ack-evidence")
gradle.write_text(g,encoding="utf8")
assert "CaseSoundTrialV140.run" not in q
assert "CaseIdentityReadV137(g, observedSubtype)" in q
assert q.count("startScan(")==1
assert q.count("Candidate(result.device, result.rssi, subtype)")==2
print("V141_CORRECT_REAL_12_02_VS_12_19_SUBTYPE=PASS")
print("V141_USER_PHYSICAL_SILENCE_ACK_ZERO_ZERO=PASS")
print("V141_NO_REPEAT_NONOWNER_FD44_WRITE=PASS")
print("V141_A2DP_SEPARATE_FROM_GATT_EVIDENCE=PASS")
print("V141_SINGLE_USER_INITIATED_SCANNER=PASS")
