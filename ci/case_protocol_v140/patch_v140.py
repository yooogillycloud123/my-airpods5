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
