## 2026-10-16 v139 실제 APK 완성: v138과 동일 앱 ID로 교체 / 설치 총개수 증가 금지
- 사용자는 개인 원본 1개+LAB 4개를 지적하면서 더 이상 6번째 앱 설치를 요구하지 말라고 명시. v139을 **v138과 동일 패키지 `com.myairpods5.personal.radiolab`**로 제작. CI `38904151411` SUCCESS, 릴리스 `my-airpods5-v139-one-lab-replace-r11`; SHA256 `380a26787fc0217ee0068dc24653ffb04731279f5e505e78b0d24deb683ca055`, 51,490,701 bytes, cert `6d73e4b2c659d6b3e5f38d83fe4fc6eed9e268254841c3664b14c3363a03ba39`. CI+zip/v2 signer/independent extracted APK hash PASS, v139 실기 미확인.
- 실험 LAB v139 **자체 AirPodsService 상시 BLE 스캔 OFF**(사용자 수동 12초 검색만 1개), 12:02/12:19/other12 중복 집계 수정, 12:19 비연결 신호와 임시 주소별 카운트 구분, 메인 설정 대화상자 `실험앱 관리`에서 기존 설치앱 역할과 Android 앱 정보 바로가기 표시. 백그라운드에서 다른 앱을 강제종료/삭제하지 않음.
- 설치 중 **중요 서명 제약**: v138/v139 CI debug 서명이 달라 v139 덮어쓰기 불가. **v138 `AirPods 신호 확인 · LAB`(`com.myairpods5.personal.radiolab`)만 한 번 삭제** 후 v139 설치 = 앱 수 증가하지 않음; v138 자체 데이터는 지워지므로 주의. 개인 v1.0.10, v134 lab(키 존재 가능성), AA Golden/데몬/페어링 변경 금지. v136 caseprobe/v137 identityprobe는 Android 설정에서 **강제 종료만**하여 중복 BLE 스캔을 차단하고, 사용자가 원하면 실기 성공 뒤 각각 데이터 손실 주의해 제거 가능.
- 다음 정규 업데이트 이전에 안정적인 **비공개 개인 LAB 서명키** 확보해야 하며, 이번 v139을 연속 update-install 성공이라고 주장 금지. 본인 닫힌 AirPods5 케이스 소리 인증/FindMy 삐삐 미구현.
- 상세 ROLE/절차/실측: `MY_AIRPODS5_V139_ONE_ACTIVE_LAB_REPLACEMENT_CHECKPOINT_20261016.md` (main).

---

## 2026-10-16 v138 사용자 최신 실측: BLE335/Apple34, 12:02=11, 12:19=0, 07:19=0
- 실제 사용자 캡처: BLE 콜백 335, Apple 제조사 콜백 34; **FindMy 12:02=11**, **Apple Nearby10형=22**, **12:19 연결가능=0**, **AirPods 07:19=0**, 모델 3020/3620=0. 전체 스캐너 작동 중이며 특정 광고형식만 미수신. 12:02와 과거 12:19가 동일 개체인지 RPA/키로 결속되지 않아 단정 금지.
- v138 보고 문자열 **`다른 Find My 12형=11`은 소스 `generic12++`가 12:02까지 포함하여 중복 집계한 레이블 버그**. Apple 광고 종류 11+22+기타1이며 11건 별도 12형이 아님.
- **새로 확인한 경쟁 스캐너 위험**: LAB가 별도 package로 증가할 때마다 LibrePods `AirPodsService.onCreate`가 `bleManager.startScanning()`을 호출. 사용자가 실행했던 LAB 프로세스들이 동시에 살아 있다면 앱별 상시 BLE scanner가 중복 등록될 수 있음. 실제 동시활성/12:19 소멸의 직접 원인은 아직 미증명. v138도 on-demand scanner를 별도로 시작함. 다음 개발은 LAB 자체 백그라운드 스캐너 격리와 고유한 케이스 신원 근거 확보 우선, 동일 8/12/35초 반복 신호실험 금지.
- 과거 v136 FD44(4F860003)=TRUE는 **비소유자 분리 추적기 후보의 서비스 조회**이며 본인 닫힌 AirPods5 케이스 증명 아님. 현 12:02=11도 소유 증명이 아님. 찾기 케이스 실제 삐삐 아직 미구현. 기존 원본/APP/AA 및 페어링 삭제·초기화 금지.
- 상세 후속: `MY_AIRPODS5_V138_REAL_335_BLE_34_APPLE_NEARBY_1100_OBSERVATIONS_20261016.md`.

---

## 2026-10-16 v138 실물 후보 0 원인 구조 수정 / 새 APK 빌드 PASS (사용자 실기 대기)
- v136 실측 12:19 후보 1개/FD44 TRUE/GATT 8개는 본인 AirPods5 케이스로 증명되지 않음. v137 설치 후 이전 v136까지 12:19=0, BLE callback=396. Android scanner 자체는 동작.
- **소스의 확정적 설계 결함**: v134~v137은 `12 19` 분리 Find My 추적기 광고만 '에어팟 후보'로 취급하여 **AirPods5 주변 연결용 `07 19` 광고를 아예 놓침**. 그렇다고 실측 12:19 신호가 왜 사라졌는지 특정되거나, 그 기기가 본인 AirPods 케이스였다고 입증되는 것은 아님.
- 신규 `v1.0.38`: `airpods-138-dual-airpods-findmy-observation-20261016`, Actions `38900664575` SUCCESS, release `my-airpods5-v138-split-radio-evidence-r9`. APK `My_AirPods5_v1.0.38_AIRPODS5_FINDMY_SPLIT_EVIDENCE_LAB.apk`, SHA256 `c2776b45ade28162206703cca6ea2ea2185a916ddb3bd95e931304e4fd4dd2a6`, package `com.myairpods5.personal.radiolab`, vCode 10038, signer `5fb1ae433e2c921ac129920c59fd5a1e83a306c83c4c0008e331f9b4d0b81379`. 물리 테스트 아직 안 함.
- 한번만 12초 수동 BLE 스캔하여 **07:19 AirPods 모델 광고(0x3020/0x3620)**, **12:19 분리 Find My**, 12:02 nearby, Pro3 07:11:06 등을 구분. 07:19 모델 표시 = 케이스 소유 인증이 아님. 12:19 옵션 GATT도 사용자 확인된 소유 기기에서만 READ-ONLY. 소리/페어링/GATT write 0.
- 기존 `com.myairpods5.personal` 부분 검증 v1.0.10 및 v134/v136/v137 LAB, AA Golden 모두 불변, 실험용 다른 패키지 설치. 같은 신호가 무선으로 송출되지 않는 시간에는 어떤 APK도 특정 후보를 강제로 찾을 수 없음. 추가 35초 반복 검색 요구 금지.
- 상세: `MY_AIRPODS5_V138_AIRPODS_PROXIMITY_VS_FINDMY_RADIO_FIX_20261016.md` (main). 사용자 v138 요약 결과 한 번 받은 뒤 **AirPods5 케이스 자체 신원/owner 인증/사운드 명령** 계속 연구. 실물 삐삐 아직 미구현.

---

## 2026-10-16 v1.0.37 새 읽기전용 GATT 신원 프로파일 실제 APK 빌드
- 사용자 v136 실측 PASS(후보행 선택/GATT FD44 비소유자 UUID), 신원 UNKNOWN. 이를 실제 사용자 소유 AirPods5 케이스로 단정 금지.
- v137 브랜치 `airpods-137-readonly-identity-profile-20261016`, GitHub Actions `38897400504` SUCCESS, 릴리스 `my-airpods5-v137-identity-readonly-r8`. APK `My_AirPods5_v1.0.37_READONLY_IDENTITY_PROFILE_LAB.apk`, SHA256 `19734234c523b58d8f75a6cfd7596793a9560ee76afbdcd187c1ceaec702b12f`, package `com.myairpods5.personal.identityprobe`, vCode 10037, v2 signer certificate `787d66a29fe20031f99cb9f873fea563b48832afa01085e63c22ad007f41b847`. 바이너리 ZIP/서명/source freeze PASS.
- 기존 v136/v134 lab, 개인 v1.0.10 및 AA Golden 아무것도 삭제/수정하지 않는 독립 LAB APK. 새 기능: `2A29` 제조사, `2A24` 모델, `2A00` 일반 이름, `6AA50003` FindMy 장치 표기가 READ 허용된 경우에만 짧게 읽고 원시 식별정보 없이 범주 표시. FD44 비소유자/소유자 point 존재 구분; owner point 존재≠인증. 시리얼/주소 값 출력 금지; GATT 쓰기/페어링/소리 명령 0.
- **v137 실기 아직 미검증**. 후보가 사용자 소유 기기인지 불확실하면 조회 취소. 12:19 다른 분리 추적기 가능성 유지. 원래 목표 AirPods5 두 이어버드 넣은 닫힌 케이스에서 audible Find My beep **미구현**.
- 체크포인트: `MY_AIRPODS5_V137_READONLY_IDENTITY_APK_20261016.md` main. 같은 RSSI/후보 수만 반복 측정하지 말고 신원/인증 프로토콜 자료로 계속 진행.

---

## 2026-10-16 사용자 v1.0.36 실제 스크린샷 — 후보 선택/GATT PASS, 케이스 신원 UNKNOWN
- 새 `com.myairpods5.personal.caseprobe` vCode 10036에서 후보 1개 목록을 실제 터치하고 `내 기기 서비스 확인` → GATT 결과창까지 성공. 기존 1개 후보 목록 미표시 버그는 실기 PASS.
- 실측: Apple Find My 12:19 후보 RSSI -85 dBm(주소 비공개), FD44 + `4F860003` 특성 TRUE, DULT(`15190001`+`8E0C0001`) FALSE, GATT 서비스 8개, 쓰기명령 0.
- **이것은 anti-stalking/non-owner service 확인이지 본인 AirPods5 A3530 케이스 인증이 아님.** 12:19는 Find My 규격에서 분리 광고이고, 제3자 Find My 태그 가능성 있음. AirPods5 자체의 등록/상태는 아직 원문 증명 없음. 서명 mismatch 및 미확인 대상 GATT write 위험 때문에 v137 신규 앱/소리명령으로 성급하게 진행하지 않는다.
- 최신 실제 실기 결과/후속 소스 범위: `MY_AIRPODS5_V136_PHYSICAL_FD44_NONOWNER_EVIDENCE_20261016.md` (main). 기존 v136/개인 v1.0.10/AA Golden 삭제·초기화 금지. 후보 추가 스캔 반복 요구 금지. 다음은 케이스 소유자 식별 및 인증 채널을 원본 소스로 확인.

---

## 2026-10-16 v1.0.36 후보 선택 오류 수정 · 기존 LAB 삭제 없는 병행 설치
- 사용자 실제 v1.0.34 화면: 12:19 후보 1개인데 선택 행이 없고 취소만 표시됨. Android `AlertDialog.Builder`에서 `.setMessage`와 `.setItems` 병용하여 목록이 숨겨지는 소스 결함 직접 확인.
- v1.0.35는 후보 행 선택 UI 수정 및 CI PASS였지만 기존 v1.0.34와 디버그 서명이 달라 정상 update-install 불가.
- **v1.0.36 안전 실험판**: `airpods-136-parallel-picker-preserve-lab-20261016`, Actions `38893990652` SUCCESS, release `my-airpods5-v136-parallel-picker-r4`, SHA256 `1f45f7a05d6e65448b45f99e78890298efc95d8a295b089550367256e52fa9a3`. 새로운 독립 package `com.myairpods5.personal.caseprobe`으로 배포해 기존 `.lab` v1.0.34의 저장소를 삭제/교체하지 않고 나란히 설치 가능. 개인 `com.myairpods5.personal` v1.0.10 일부 기능 검증 기준선, AA Golden은 변경 없음.
- 후보 탭 → 별도 사용자 확인 → **읽기 전용 GATT FD44/DULT 서비스 유무** 확인까지만 구현. `FD44=true`라도 본인 A3530 케이스/소유자 인증/실제 찾기 삐삐 성공은 입증되지 않음. 타인 소유 미확인 기기 GATT 연결 금지. 실제 사운드 기능 **미구현**.
- 최신 상세: `MY_AIRPODS5_V136_NON_DESTRUCTIVE_CANDIDATE_PICKER_HANDOFF_20261016.md` (main). 앞으로 기존 LAB 삭제 유도 금지; 영구 서명키 마련 전 매번 다른 서명으로 업데이트를 가정하지 않는다.

---

# 내 에어팟 5 · 영구 인수인계 / READ FIRST (2026-10-15)

## 2026-10-15 v1.0.24 실제 설치형 LAB APK 완성(빌드만 PASS) — 케이스 소리 미구현
- 사용자 요청으로 새 APK 실제 GitHub Actions 빌드. Branch `airpods-personal-v124-case-advert-test-20261015`, run **38719047092 SUCCESS**, signed artifact release **my-airpods5-v124-case-advert-r1**. SHA256 `b23f0deaf7cd32a46a0a2668c3d520e950417d908a382455d7aac9b471d2ed14`. pkg `com.myairpods5.personal.lab` versionCode **10024**, Debug signer SHA `452370d8feda95849415c8a2dc9ebafd6240d14228d512de14c006fa504871a5`.
- 새 Settings `내 케이스 찾기 연구 v1.0.24` → 사용자가 원할 때만 10초, Pro3 원본 출처 `07 11 06` 독립 케이스 광고 조사/이 LAB 자체 `ENC_KEY`가 있으면 AES 복호화 형식 판별, 없으면 건수만 보고. AirPods **5** 형식·케이스 소유자 인증·케이스 실제 소리 **전부 미확인/미구현**. 기존 읽기전용 v120 메뉴 별도 선택 보존, 음향/GATT 쓰기 없음. 기존 v116 07=0 실측 고려해 반복 시험을 강요하지 말 것.
- **IMPORTANT:** LAB v122와 v124 인증서가 달라 **덮어쓰기 불가능**할 수 있음. LAB 제거 시 LAB IRK/ENC_KEY 데이터 지워짐. `com.myairpods5.personal` v1.0.10 USER-PARTIAL-BASELINE 절대 삭제/재서명 금지. 당장 케이스 삐삐 기능을 원하면 이 APK가 그 기능을 제공한다고 말하지 말 것.
- Exact technical record: `MY_AIRPODS5_V124_INSTALLABLE_CASE_RADIO_LAB_20261015.md` on main. GitHub Actions v124 build PASS ≠ AirPods5 케이스 찾기 PASS.

---



**v123 확장/최신 CI:** 원본 `stuffz/airpods` **AirPods Pro 3** 케이스만 확인된 별도 `07 11 06` 광고+`ENC_KEY` AES 복호화 구현을 OFFLINE 파일 `ci/case_protocol_v123/case_broadcast_evidence.py`로 분리. 이어버드 `IRK`는 해당 케이스 주소를 판별하지 못함(해당 원본 캡처 한정). AirPods **5** 동일 방식 여부 미검증, 소유자 인증/실제 찾기 사운드와 무관. GitHub Actions **38718067357 SUCCESS**, 20 synthetic unit tests PASS; 실제 기기/케이스 소리 PASS 없음. 상세 `MY_AIRPODS5_V123_CASE_SESSION_SOURCE_PROOF_CHECKPOINT_20261015.md` 참조. 새 APK 없음.

## 2026-10-15 v123 source-only checkpoint — not an APK or case speaker success
- New **independent research branch** `airpods-case-session-proof-v123-20261015`, latest LAB v122 source frozen. `ci/case_protocol_v123/case_session_evidence.py` implements an offline btsnoop HCI handle reuse/encryption/session-epoch evidence guard. Raw bytes/addresses redacted. Actual case GATT owner authentication and beep **STILL NOT IMPLEMENTED**.
- CI Actions `38717404840` **SUCCESS**, synthetic no-device checks only, workflow job `116363341045`. **No new APK issued or installed**. Source step not marked physical pass.
- New public original source `stuffz/airpods/docs/ble-research.md`: independent case BLE address **not resolving against earbud IRK**, but tested on **AirPods Pro 3**, not user's AirPods 5. Prior v120 IRK-only GATT preflight therefore cannot safely be assumed to locate the user's separate closed case; no new blind BLE scan requested.
- v1.0.10 is a frozen **partially user verified baseline, not a finished app**. v122 LAB battery display/IRK source fix CI success still lacks actual device verification, and per-build signing prevents seamless LAB updates. Both projects' golden artifacts, AA daemon/pairing untouched.
- Full permanent checkpoint: `MY_AIRPODS5_V123_CASE_SESSION_SOURCE_PROOF_CHECKPOINT_20261015.md` in this repo's `main`. Source code resides **only** in the new v123 branch.
- Next actual blocker: **AirPods5 case owner pairing/auth + case speaker sound instruction/ack/proven audible output**. Do not confuse HCI encrypted link, BLE RPA or GATT write ACK with Find My case sound. No unsolicited test APK.

---



## 사용자 최종 정정 2026-10-15 18:50 KST — v1.0.10을 정식 완성본이라고 하지 말 것
- 사용자가 **현재 정식 완성본 AirPods 앱은 없다고 명확히 정정함**. v1.0.10은 앱 실행/초기 배터리 UI/일부 청취 모드 실물 동작을 확인한 임시 안전 기준선일 뿐, 전 기능 검증 완료·정식 통합 완성판이 아님. '정상 완성본', '기능 완성', '제품 출시'라고 쓰면 사용자 요구와 모순됨.
- 이후 LAB v1.0.20~22는 실제 찾기 소리 재생을 전혀 구현하지 못했고, LAB v1.0.21 IRK 미수신(음악용 연결 전후), LAB 배터리 0% 표시 오류가 관찰됨. v1.0.22 코드 수정 CI PASS는 실제 배터리 수신·IRK·소리 복구 실물 PASS와 다름.
- 사용자 최상위 목표는 **AirPods5 무선 충전 케이스 자체에서 Find My 찾기 소리 재생**: 양쪽 이어버드 넣음, 뚜껑 닫힘, 음악용 Bluetooth 연결 해제, Galaxy S26 Ultra에서 원터치. Apple iPhone/iPad 페어링 이력 없음. 목적 변경 금지.
- **시간/노력 소모 제한**: 35초 무분별 BLE 스캔·RSSI 근원거리 비교·무의미한 APK 교체/재설치·같은 테스트 중복 금지. 소스와 실제 성공한 구현 및 통신 근거 확인, 재현 가능한 로컬/CI 테스트를 개발자가 우선 수행. 정확한 후속 실기 검증 1회가 의미 있어질 때만 요청.
- **진짜 핵심 장벽**: AirPods5 충전 케이스 자체 owner GATT 인증 및 Find My speaker play/stop 통신 실증 소스 미확보, LAB 독립 앱 AACP IRK 미수신과 사용자 데이터 분리·매번 다른 디버그 서명. LibrePods #20/#102 직접 개발자 코멘트 기반. 배터리 설정용 AACP 키와 케이스 Find My owner authentication을 혼동하지 말 것.
- **최신 결과물 분류**: v1.0.10 사용자 일부 기능 PASS인 기준 앱 (frozen, NOT FINAL), v1.0.22 별도 LAB는 빌드 PASS이나 실물 기능 PASS 아님 (NOT FINAL). 실질적인 AirPods5 케이스 찾기 사운드 구현 완료 APK는 아직 없음.
- 다음 차례: 로그인·개인 키 등 비밀을 요구하지 말고 공개 성공 소스/펌웨어/프로토콜을 우선 확인한 후 케이스 GATT owner 인증을 실제 구현 가능한지 검증, 이어 케이스 발음 명령/ACK/실제 음향까지 단계별 구별. 최종 안정 앱을 계획하되 가짜 완성 선언 금지.
- 새 채팅에서 반드시 이 문서 및 루트 canonical을 우선 확인 후 중복 없이 이어서 진행.

---

## 2026-10-15 18:30+ 사용자 스크린샷 IRK/배터리 회귀 원인 확인 및 v1.0.22 빌드
- 실제 사용자 스크린샷: LAB 앱 배터리 영역 0%, 진단에서 이전 LAB BLE콜백 906/939, 이어버드 IRK 주소 일치 0; 수정 LAB v1.0.21은 블루투스 연결 전/후 `내 AirPods IRK 미수신`. 사용자 지적 타당.
- 실제 LibrePods `BatteryView.kt`에서 left/right null을 0%로 렌더링한 버그 확인. 정상 v1.0.10 사용자 실제 배터리 표시 PASS와 별도로 LAB에는 독립 패키지 prefs 저장소가 있고 CI ephemeral debug signer라 이전 LAB 제거·재설치 시 `IRK`가 사라짐. 음악용 블루투스 연결만으로 LAB AACP L2CAP 키수신 보장하지 않음.
- v1.0.22 LAB 패치 `ci/case_protocol_v122/patch_lab_irk_battery.py`: 없어진 배터리 0% 가짜 수치 제거하고 `배터리 데이터 미수신` 표기, 수신된 값은 그대로 유지, 자체 AACP 이미 연결된 경우에 한해 사용자가 `AACP 키 재요청` 선택 가능(기존 LibrePods 0x05 요청/6초 로컬 응답 확인). 자동 BLE GATT/사운드 명령 없음.
- GitHub Actions `38714212956` SUCCESS, `BUILD SUCCESSFUL in 3m34s`, signed APK `my-airpods5-v122-lab-r1/My_AirPods5_v1.0.22_BATTERY_IRK_REPAIR_LAB.apk` size 51,408,721, SHA256 `db4916999080b60c2d0efc6c72fee8f967e973e7d041620c8a5bae6ba60cd056`, pkg `com.myairpods5.personal.lab` versionCode 10022.
- **이 APK가 LAB의 실제 L/R/케이스 배터리 통신 복구나 케이스 Find My beep 성공을 증명하지는 않음.** 서명 변경으로 LAB uninstall 시 또 IRK 초기화됨. 사용자에게 근무 중 재설치 요구하지 말고, 정상 GOLDEN v1.0.10에서 배터리/ANC 사용. 추가 BLE 반복테스트 금지. 앞으로는 LAB 앱 독립 데이터와 Golden key 공유를 무작정 기대하지 않으며 인증 근거 확보 우선.
- 상세 `MY_AIRPODS5_V122_LAB_BATTERY_IRK_REGRESSION_REPAIR_20261015.md`. 사용자 GOLDEN v1.0.10 및 AA Golden 불변.

---

## 2026-10-15 18시 v1.0.20 '검색 권한 부족' 실제 원인과 v1.0.21 수정본 빌드 PASS
- 사용자 실제 LOG: SM-S948N Android 17 sdk37, LAB v1.0.20 시작/ServiceConnection.init COMPLETE, `NO_JAVA_FATAL_CAPTURED`, 메뉴가 `검색 권한 부족`에 차단. **GATT 시도/케이스 사운드 단계 도달 전, 앱의 런타임 권한 가드에서 종료**. 원래 source `CaseOwnerProbeV120.open`은 BLUETOOTH_SCAN, BLUETOOTH_CONNECT, FINE LOCATION만 검사하고 앱 내 권한 요청 API가 전혀 없음.
- Android 공식: Android12+ FINE 런타임 권한은 COARSE와 함께 요청해야 함. 새 `ci/case_protocol_v121/patch_lab_permission.py`는 LAB IRK 체크를 먼저 하고, 권한 부족 유형 표시, Nearby 기기 및 COARSE+FINE 정확 위치 승인 버튼, 앱 정보 화면 바로가기, 권한 승인 시 검색 재확인 대화상자(자동 스캔 없음) 구현. 소리/GATT 쓰기 미구현, 개인정보/좌표 수집 없음.
- **기존 v1.0.20 설치본으로 즉시 해결**: 설정→애플리케이션→`내 에어팟 5 실험실`→권한→주변 기기 허용·위치 `앱 사용 중 허용`+`정확한 위치` ON. 실험실 앱으로 돌아가 동일 메뉴 누르면 permission gate 통과 가능. 이후 IRK 미수신 표출 시 LAB 앱에서 에어팟 한번 정상 연결하면 됨. GOLDEN 삭제 금지.
- **새 설치형 LAB v1.0.21**: release `my-airpods5-v121-lab-r1`, APK `My_AirPods5_v1.0.21_BLE_PERMISSION_RECOVERY_LAB.apk`, SHA256 `da355e062cf14ecb044e3fc8d7809b7076f7964b06ad56953bc4f493a4fed874`, signer SHA256 `c3355af70901af41a6ceb3ec2fdc7641fc24e18234f27af31aec38dc7fa409a9`, size 51,408,729, App ID `com.myairpods5.personal.lab`, versionCode 10021. Actions `38710016440` actual SUCCESS, `BUILD SUCCESSFUL in 3m 26s`, ZIP/package/signature/static/engine sha gate PASS.
- v1.0.20→v1.0.21 LAB signing differs. **새 APK 설치 강요하지 않음**; current v120 manual permission grant is quickest and avoids LAB uninstall/IRK loss. GOLDEN v1.0.10 and AA Golden untouched.
- Source details `MY_AIRPODS5_V121_PERMISSION_GATE_STATUS_20261015.md`, workflow `.github/workflows/v121-ble-permissions.yml`. 케이스 실제 beep 여전히 미구현/미검증. 다음 실측은 permission then IRK/gatt gate only, not repeat 35s BLE or reset pairing.

---

## 2026-10-15 오후 v1.0.20 설치가능 LAB APK 실제 릴리스 / 사용자 실험 대기
- 이번 사용자 '말이 아니라 결과물' 명시 요청으로 별도 설치 실험용 실제 APK 제작/서명/배포. 정상 v1.0.10 GOLDEN과 AA Golden은 변경·삭제하지 않음.
- `my-airpods5-v120-lab-r2/My_AirPods5_v1.0.20_OWNER_CASE_GATT_PREFLIGHT_LAB.apk` SHA256 `3f6702b23179561648f30f4c189573f163992cc60af89ce4ff635059ff60d134`, 패키지 `com.myairpods5.personal.lab`, 크기 51392317 bytes.
- GitHub Actions `38707003529` 실제 SUCCESS, Android Kotlin + APK build 3m48s, 사인/zip/core SHA gate 모두 PASS. 
- LAB 설정 `내 케이스 제어 경로 확인 · 연구`: LibrePods 실소스 BLE RPA/IRK 검증 12초, 내 이어버드 IRK와 일치하며 연결 가능한 후보만 별도 동의하에 GATT UUID를 읽고 FD44/4F860002/4F860003 확인. 원시 MAC/키 공유 안 함. **단순 RPA 일치는 케이스 신원/소유자 인증이 아니며 케이스 소리 명령은 보내지 않음**.
- 앞선 LAB과 signer 달라 설치 충돌하면 **LAB만** 삭제하고 다시 설치. 그러면 LAB에 IRK 없음 → LAB에서 AirPods 이어버드 1회 정상 연결하여 키 수신 필요. Golden 삭제/페어링 초기화 절대 요구 금지.
- 새 근거·절차 문서 `MY_AIRPODS5_V120_INSTALLABLE_GATT_GATE_ACTUAL_BUILD_20261015.md`. 실기 결과는 아직 없음. 실제 케이스 스피커 소리는 **NOT IMPLEMENTED**. 다음은 `IRK 없음/0 matches/GATT available` 중 어느 gate인지 물리 결과 1회 확인하고 case owner authentication protocol verification 지속.

---

## 2026-10-15 v119 오프라인 GATT 실제 추출 + 비교 기능 완료
- LAB 브랜치 `ci/case_protocol_v119/extract_att_hci.py`에 실제 btsnoop H4/ACL/L2CAP/ATT 기록에서 서비스·특성 UUID, ATT 쓰기/응답/알림, BLE 암호화 변경을 추출하는 파서를 구현.
- 추가 `ci/case_protocol_v119/compare_case_sound_sessions.py`: 인증된 케이스의 실제 소리 버튼 실행 전/후 HCI 캡처가 있으면 관찰된 GATT 제어 명령 후보의 차이만 계산(특성 UUID 우선, 세션별 핸들 무시). 실제 케이스 캡처 **아직 없음**. 현재 BLE RSSI 로그로 케이스 사운드 opcode를 뽑았다고 주장하면 안 됨.
- 두 소스 모두 **오프라인**, 다른 기기 접속/원격 제어 없음, 기본 출력 개인정보·원문값 제외. GitHub Actions `38693194281` SUCCESS(소스 파싱/합성 데이터 테스트). 상세 문서 `MY_AIRPODS5_V119_HCI_ATT_SOURCE_CHECKPOINT_20261015.md`.
- 케이스 owner 인증/실제 sound opcode 확보 후에만 설치용 LAB APK/실기 1회. 회사 사용자에게 추가 검사·재설치 요구 금지. 정상 v1.0.10 Golden 불변.

---

## 2026-10-15 v1.0.19 실코드 소스 기반 ATT/HCI 데이터 추출 도구 완료
- 사용자의 회사 근무와 이전 과도한 반복을 고려해 신규 앱 설치·35초 BLE/RSSI 반복·재페어링 요청 **없음**. 정상 v1.0.10 GOLDEN 유지.
- 실물 AirPods5 case Find My 스피커 제어 프로토콜 추정 금지. 실제 소리 재생은 여전히 미구현.
- 새 LAB source `airpods-personal-v111-updater-diagnostics/ci/case_protocol_v119/extract_att_hci.py`: Android `btsnoop_hci.log`(H4) → ACL → L2CAP → ATT/GATT 실제 캡처 프레임 오프라인 해석기. ATT 서비스/특성 UUID 발견 기록과 쓰기/응답/알림, 링크 암호화 이벤트를 구분해서 기록. 신호의 소유 케이스 여부는 UNKNOWN으로 유지. 별도 장치 연결이나 명령 전송 불가능.
- 개인식별정보 및 raw ATT 값은 기본 출력 제외. 로컬 명시적 private mode만 원문 포함. 과거 사용자 로그에는 authentic case-owner handshake trace가 존재하지 않음.
- 실험코드 테스트 `test_extract_att_hci.py`, `test_uuid_map.py`, reproducible workflow `case-att-trace-extractor-v119.yml`: GitHub Actions `38692379215` **SUCCESS**, fixture 2종 OK. 정확한 산출물/다음 인증 통신 게이트 `MY_AIRPODS5_V119_HCI_ATT_SOURCE_CHECKPOINT_20261015.md`.
- 공식 자료: Apple A3530 케이스 Find My 스피커 지원. LibrePods #20/#102는 case GATT owner auth/play case sound 모두 미완료. CAPod v118 case ATT는 케이스 설정이며 AirPods Pro3 CASE_INFO에 제한. AirGuard/OpenTagViewer 성공 제어 예제는 사용자 AirPods5 사례와 다르다. 새 APK 성공이라 말하지 말 것.

---

## 2026-10-15 v118 CAPod 실제 소스 신규 교차검증
- CAPod Kotlin `DefaultAapDeviceProfile.kt`: **케이스 효과음은 ATT를 통해 제어**되며 AACP IN_CASE_TONE (0x31) 효과 미확인이라고 직접 표기. `CASE_INFO_REQUEST=0x22` / `CASE_INFO=0x23` 실제 구현은 AirPods Pro3 단독 allowlist (AirPods5 미검증); 케이스 Find My 소리 opcode가 아님.
- LibrePods #20/#102의 CASE GATT owner 인증 문제와 일치. 따라서 v117 RPA/IRK 코드만으로 사용자의 AirPods5 케이스를 인증하거나 소리 재생할 수 없음.
- 상세 신규 파일 `MY_AIRPODS5_V118_CASE_ATT_PRIMARY_SOURCE_AUDIT_20261015.md`. 다음 **소스 연구는 케이스 자체 ATT/GATT owner 인증 및 Find My 오디오 명령의 기종별 실증 추적**에 집중. 근거 확보 전 테스트용 새 APK 설치·BLE 스캔·사용자 실기 요구 금지. Golden v1.0.10 불변.

---

## 2026-10-15 v1.0.17 Android 실제 소스 빌드 PASS (실기 요구 없음)
- 동작 입증된 LibrePods의 BLE 주소확인(AES-128 IRK RPA)을 Kotlin 순수 모듈로 이식. KeySource EARBUD_AACP vs CASE_OWNER_HANDSHAKE_VERIFIED 구분, 어떤 주소 일치도 Find My 소리 제어 성공이나 케이스 소유자 인증으로 판정하지 않음.
- GitHub 소스 브랜치 `airpods-personal-v111-updater-diagnostics`, `ci/case_protocol_v117/CaseRpaIdentity.kt`, 테스트 `CaseRpaIdentityTest.kt`.
- Kotlin 독립 8개 오프라인 테스트 GitHub Actions `38683459680` SUCCESS; Android LAB 통합 실제 빌드 GitHub Actions `38683666006` SUCCESS, `BUILD SUCCESSFUL in 2m53s`, 앱 ID `com.myairpods5.personal.lab` v10017, APK SHA256 `e15e76d50798997f66f050ee88caaa0a7c9c8673f0e1cc49c309e12bcd2a6ba0`.
- APK는 **내부 컴파일 검증 아티팩트로만 보관**(새 사운드 기능 없음), 사용자 설치·테스트 요구하지 않는다. 최초 실기 v1.0.10 Golden 보호.
- 상세 소스 검증 및 다음 실제 차단점 문서 `MY_AIRPODS5_V117_CASE_OWNER_IDENTITY_SOURCE_CHECKPOINT_20261015.md`. 후속은 AirPods5 **케이스 자체** Find My GATT 소유자 인증/제어 전송 소스 또는 성공한 실물 trace 찾기이며, 확보 전 추정 opcode 시도 금지.

---

## 2026-10-15 회사 추가테스트 없이 v1.0.17 실제 소스 단계 진행
- 소스 중심 다음 단계 실제 수행: LibrePods 원본 Android BluetoothCryptography/BLEManager/AACPManager 및 독립 LinuxPods AAP IRK/ENC_KEY 구현을 비교 검증. 이어버드 proximity IRK 및 ENC_KEY와 케이스 Find My owner 인증키는 용도가 달라 혼동하면 안 됨.
- 별도 LAB 브랜치 `airpods-personal-v111-updater-diagnostics/ci/case_protocol_v117/CaseRpaIdentity.kt`에 Bluetooth AES-128 `ah` 및 RPA 주소 판별을 Kotlin으로 구현, KEY_SOURCE provenance 단계 분리. 8개 독립 오프라인 테스트 PASS(독립 Python cryptography 기준 AES 벡터 포함). GitHub Actions source gate `38683459680` SUCCESS.
- 신규 `ci/case_protocol_v117/patch_android_lab_case_identity.py`, `.github/workflows/compile-case-rpa-android-v117.yml`에서 LAB 전용 Android 빌드에도 위 Kotlin 파일 통합. 사용자의 앱에 강제 설치하거나 새 소리 기능이라고 말하지 말 것.
- 인증된 **케이스 자체 IRK / GATT owner auth / 실제 Find My sound characteristic**은 여전히 미확보. 소리 기능 실기 PASS 아님. 결론/소스 경로 `MY_AIRPODS5_V117_CASE_OWNER_IDENTITY_SOURCE_CHECKPOINT_20261015.md` 참조.
- 회사 사용자에게 반복 BLE 스캔, RSSI 측정, APK 삭제/재설치 요구 금지. 정상 GOLDEN v1.0.10 4모드/배터리, AA Golden 불변.

---

## 2026-10-15 13:03+ 명확한 기술자료 재검토 완료 — 근거 없는 실기 반복 중단
- 사용자 회사 근무 중이며, 기존 BLE/RSSI/인식 시험에 이미 많은 시간 소비함. **추가 35초 신호 검색, 거리 실험, APK 재설치, 디버깅 설정 변경 요청 금지**. 과거 수치는 버리지 않고 근거로 사용.
- Librepods README는 케이스 Find My 소리를 계획/추가 RE 필요라고 표기; AAP Definitions 및 PR764에서 확인한 케이스 소리 명령은 충전 알림 효과음 on/off이지 분실 케이스 삐삐가 아님. Issue718 'AACP play tone' 제안은 closed as not planned이고 실증 소스 없음.
- AirGuard AppleFindMy.kt는 비소유자 분리 상태 액세서리 GATT 사운드 경로만 지원. OpenTagViewer owner-connected-tags 연구에서는 제3자 Find My 액세서리 owner-sound에서 전용 링크 암호화 필요, 평문 GATT ACK만으로는 재생되지 않음. 둘을 사용자의 AirPods5 실증 성공으로 착각하면 안 됨.
- v116 실물 재검증 RSSI 12:02 길이4 근/원거리 4/-95dBm vs 46/-94dBm, case identity FALSE/UNKNOWN. BLE 광고만으로 이름/MAC/소유자 확인 불가. Golden v1.0.10 변경 금지.
- 종합 기술 감사 새 문서: `MY_AIRPODS5_CASE_SOUND_SOURCE_AUDIT_NO_MORE_BLIND_TESTS_20261015.md`. 이 문서에 검증된 원본 링크/해시, 성공과 미구현의 구분, 다음 프로토콜 구현 Go/No-Go 기록. `ci/check_case_speaker_evidence.py` 소스 증거 게이트 + GitHub Action `case-speaker-proof-gate.yml` 소스 테스트. **케이스 실제 스피커 사운드 아직 작동하지 않음**.
- 다음 개발은 본인 AirPods5의 실제 케이스 제어 채널+인증+사운드 실제 재생 패킷의 source-verified 증거를 먼저 확보한 뒤 LAB 이식. 사용자에게 추측성 실기 검증 떠넘기지 말 것. 단순 docs/GitHub CI PASS ≠ 케이스 소리 PASS.

---

## 2026-10-15 v1.0.16 실측 재검증 — Find My 케이스 신원 연결 실패
- 가까이 12:02 길이4=4건 RSSI -95dBm, 멀리 46건 RSSI -94dBm. 기존 '케이스 멀어지니 감소' 결과 재현 실패. 내 케이스라고 결론 내릴 근거 없음.
- Apple 02:15 길이23 720/795건은 iBeacon 표준 광고 형태이며 Apple 제품이나 케이스 Find My 기능의 증거 아님. 12:19 길이27 1/3건, FD44 광고 0, AirGuard 일치 0.
- 추가 무작위 BLE 스캔과 다른 기기 GATT 연결 반복하지 말 것. 실제 케이스 신원 확인 및 소유자 소리 재생 인증 경로에 대한 검증된 자료가 확보되어야 다음 개발 진행. Golden v1.0.10 불변.

---

## 2026-10-15 v1.0.16 실제 APK 릴리스
- 사용자 v1.0.15 실측 근처/먼거리: Apple FindMy 12형 48→14, 세부 12:02 크기4 31→8, 크기23 17→6, 새 Apple 10:07 크기9 광고 85. 전체 BLE 1484→1809. 이는 사용자 케이스 연관 가능성에 대한 통계 단서이지 신원 증거는 아님.
- 새 실험실 APK `my-airpods5-v116-lab-r1/My_AirPods5_v1.0.16_FINDMY_RSSI_EVIDENCE_LAB.apk`, 실제 GitHub Actions `38675198218` SUCCESS, APK 크기 51375870 bytes, SHA256 `ab7af15139206e957b22d37d2099e60e94468c5c0b1a2ac422938753ea53522d`.
- v1.0.16은 12:02 길이4/23 RSSI 중앙값을 각각 추가해 표시. 읽기 전용 BLE 스캔이며 케이스 사운드 기능 미구현/미검증. 앱ID `com.myairpods5.personal.lab`. 정상 1.0.10 GOLDEN 보호.
- 이전 실험실 APK와 서명 불일치 시 LAB 앱만 제거. 기존 정상 앱과 원본 페어링에 영향 주지 않음. 사용자 보고 값을 바탕으로 BLE 케이스 식별 경로의 가능성을 판단할 것.

---

## 2026-10-15 v115 Find My 거리 비교 실물 결과
- 가까운 상태: BLE 1484/Apple 48/FindMy 12:02 크기4=31 크기23=17/연결 가능 표시17/FD44=0.
- 케이스만 8~10m 멀리 옮기는 실험 결과: BLE 1809/Apple 99/FindMy 12:02 크기4=8 크기23=6/연결가능 표시6/FD44=0, 새 Apple 10:07 크기9=85.
- Find My 12형은 48→14(약71% 감소); 10형 Apple 근처 정보 광고 증가로 전체 Apple 신호는 48→99로 증가.
- 0x10은 Apple Continuity Nearby Info 유형이며 Find My 소리 명령 근거가 아님. 12형 감소만으로 본인 케이스 확정 금지. Find My 네트워크 통신은 Apple 소유자 페어링 이력 없음.
- 근거 기록 `MY_AIRPODS5_V115_FINDMY_NEAR_FAR_EVIDENCE_20261015.md`, 추가 실험 소스 별도 LAB v116 RSSI 중앙값 진단. 정상 v1.0.10 GOLDEN 불변.

---

## 2026-10-15 v115 거리 비교 실기 의미
- 가까울 때 12:02 길이4 31건/길이23 17건, 멀리 8건/6건. 12형 합계 48→14(70.8% 감소).
- 반면 Apple 전체 48→99인 것은 먼 거리 실험에서 Apple 10:07 길이9 광고가 85건 관찰됐기 때문. 환경의 광고 구성이 변하므로 이 비교만으로 에어팟 케이스 신원 확정 불가.
- 표준 Find My accessory 문서에서 nearby 12:02 길이4는 owner pairing 뒤 광고 형식. 사용자는 Apple 소유자 페어링 경험이 없어 주변 다른 기기일 가능성을 배제할 수 없음.
- 12형 연결가능 표시 17→6은 12:02 길이23 빈도와 수치상 일치하지만 어떤 광고 기기인지 알 수 없음. FD44 광고 0건. 소리 재생 명령 미시도.
- 다음 유의미한 검증은 소유권을 증명할 수 있는 실제 케이스 고유 프로토콜 증거이며 무작위 Apple 기기 GATT 연결 금지. 신규 APK의 케이스 소리 성공을 주장하지 말 것.

---

## 2026-10-15 v1.0.15 Find My 케이스 거리 비교 실측
- 가까울 때 Apple 12형 총 48 = `12:02 size4` 31회 + `12:02 size23` 17회, BLE 1484 / Apple 48.
- 케이스만 멀리 옮긴 후 12형 총 14 = size4 8회 + size23 6회, BLE 1809 / Apple 99. 멀리 두었을 때 Apple 10:07 size9 신호가 85회 추가 관찰됨.
- type12 약 70.8% 감소, size4 약 74.2%, size23 약 64.7% 감소. 같은 시간 측정상 케이스 거리에 따른 변화일 가능성은 있지만 **내 케이스의 신원은 미확인**, 다른 Apple 기기/스캔변동 배제 불가. AirGuard AirPods mask 0 / FD44 0 / 실제 소리 명령 미시도.
- 공개 Find My 액세서리 규격상 12:02 짧은 nearby frame은 Find My pairing 이후의 형식임. 네 AirPods는 Galaxy만 페어링했으므로 주변 **타기기 광고** 가능성을 반드시 고려. AirPods5의 광고를 규격만으로 확정하지 말 것.
- 같은 v115 측정 반복 요구보다 RSSI 기반 앱 내 대조/소유 기기 식별 검증이 다음 실험상 의미 있음. 임의 connectable 기기 6개에 GATT 접속 금지. GOLDEN v1.0.10 유지.
- 최신 측정 근거 파일 `MY_AIRPODS5_FINDMY_V115_NEAR_FAR_PHYSICAL_EVIDENCE_20261015.md`.

---

## 2026-10-15 v1.0.15 Find My 조사 실험판 실물검증 대기
- Galaxy만 사용한 소유자 조건 유지, v1.0.10 GOLDEN 앱 보존.
- 새 실험용 APK GitHub release `my-airpods5-v115-lab-r1`, 파일 `My_AirPods5_v1.0.15_FINDMY_BEACON_EVIDENCE_LAB.apk`.
- GitHub Actions `38669531352` SUCCESS; 크기 51375874 bytes, SHA256 `58a1b8db50d50600db78604c39ff42b287566edeb03753618b04bd10e57df7f9`.
- Android LAB 별도 패키지 `com.myairpods5.personal.lab`, 기존 앱에 영향 없음.
- 앱 설정의 Find My 신호 조사 메뉴에서 35초 측정 후 결과 복사. Apple 12형, AirGuard AirPods 조건, FD44, 유형별 총계만 확인.
- 아직 실제 케이스를 울리는 기능은 미구현. 원격 사운드 제어 성공 주장 금지.

---

## 2026-10-15 외부 연구 재검토 결과
- 기존 닫힘/열림 실기 결과 모두 Apple 07형 0건. 배터리 광고 형식만으로 케이스 찾기 소리를 구현한다는 기존 방향 중단.
- AirGuard 공개 코드는 별도로 Find My Apple 12형 신호를 다룬다. 비소유자 분리된 추적기 소리 기능과 사용자의 소유자 케이스 소리 명령을 구분해야 한다.
- Apple 공식 자료: AirPods 5 무선 충전 케이스 스피커 지원. iCloud.com 웹에서는 케이스 소리 재생 불가.
- 기술 근거와 후보 경로 비교는 `MY_AIRPODS5_CASE_FINDMY_EVIDENCE_RESEARCH_DECISION_20261015.md`를 참고. 새 APK 추측 빌드 전 해당 문서를 읽는다.
- 정상 v1.0.10 GOLDEN 불변. 실물 케이스 소리 성공은 아직 미검증.

---

## 2026-10-15 v1.0.14 실물 BLE 측정 결과
- 사용자 실제 측정: BLE 전체 2570회, Apple 제조사 광고 93회, Apple 07형 0, 구형 케이스형 0, 본인 키에 맞는 케이스로 판별된 신호 0.
- **블루투스 검색 기능은 작동한다.** Apple 93건이 어떤 기기 것인지는 확인되지 않았다. 07형 신호가 없으므로 실제 케이스 탐지나 암호화 검사가 실행될 조건이 갖춰지지 않았다. `암호화키 일치 0`은 키가 틀렸다는 근거가 아님.
- 다음 데이터 중심 검증: 설치된 v1.0.14로 케이스 뚜껑만 연 상태(이어버드 두 개는 계속 넣은 상태)에서 동일 35초 검색. 이전 닫힘 상태와 Apple 07형 숫자를 비교하여 다음 단계의 광고 포맷 확인. 이 검증은 최종 기능의 이어버드 연결 요구와 구별되는 일회성 개발 진단.
- 현재 APK는 케이스 소리를 실제로 울린 적이 없고, 기존 v1.0.10 GOLDEN 정상 앱은 반드시 보존한다.

---

## 2026-10-15 10:04+ KST — v1.0.14 실험실 BLE 측정 APK 컴파일 PASS / 실기 확인 필요

- 사용자 승인: 끊지 말고 단계별로 빠르게 개발. **진짜 목표는 두 이어버드 모두 케이스 안, 뚜껑 닫힌 상태, 음악용 블루투스 연결 해제 상태에서도 충전 케이스 자체 스피커의 Find My 소리 호출**. 현재 Android custom 앱 미구현이며 이 사실 숨기지 말 것.
- v1.0.13 사용자 로그: `기존 Apple 케이스형 07 11 06=0`. 원래 ScanFilter가 Apple 특정 포맷만 전달했고 수신 `total` 숫자도 필터링 후 Apple packet만 세었음. `0`을 전체 Bluetooth 광고가 없다고 해석하면 안 됨.
- Android Bluetooth 공식문서: 기존 upstream Manifest `BLUETOOTH_SCAN usesPermissionFlags=neverForLocation`은 **일부 BLE beacon 필터링** 가능. v1.0.14 **실험실 패키지만** 해당 플래그를 제거하고 명시적 `ACCESS_FINE_LOCATION`을 선언, 앱 내 한국어 안내 뒤 사용자 동의로 런타임 권한 요청. 앱은 위치 좌표를 조회/저장/전송하지 않음.
- 새로운 scanner는 35초 `BluetoothLeScanner.startScan(null, low latency)`, 단계별 정직한 집계: **전체 BLE 콜백 → Apple manufacturer(76) → Apple type07 → 과거 케이스형 07 11 06 → 개인 앱 AACP에서 받은 암호화키 구조 복호화 → 2회 동일 BLE 주소 검증**. 개인정보 주소 화면·로그 제외. 실제 사용자 케이스 입증 없는 GATT/소리 명령 금지.
- 소스 `ci/patch_v114_unfiltered_case_ble.py` 독립 LAB 브랜치 `airpods-personal-v111-updater-diagnostics`에 저장. Workflow run `38663174580`, job `116188954901` **SUCCESS**, `BUILD SUCCESSFUL in 3m 59s`, 소스/패키지/ZIP/서명 PASS. 릴리스 `my-airpods5-v114-lab-r31`.
- APK `https://github.com/yooogillycloud123/my-airpods5/releases/download/my-airpods5-v114-lab-r31/My_AirPods5_v1.0.14_CASE_BLE_FULL_SCAN_DIAGNOSTICS_LAB.apk`; `com.myairpods5.personal.lab` vCode 10014; 51,359,502 bytes, APK SHA256 `2400760bbea523fabf6add6d7eb09523266b22c3927e3bac0713ffdd72e550b4`; signer SHA256 `41979a9b3d6b8b916415ac508f2e6826c0483274da5702a0dae579481b145f74`.
- **실물 소리 재생 PASS 아님**. 골든 `v1.0.10` SHA `b46c8e38f28332f7af0f98bce7b8b27234f62a83d103363cf0697a2fd127445b`는 변경/삭제 금지. 기존 .lab v1.0.13과 서명이 달라 설치 충돌 시에만 LAB 삭제 후 새 LAB 설치; 이때 LAB 저장 `ENC_KEY`가 삭제되므로 신규 LAB에서 이어버드 AACP 1회 연결하여 암호화키 재확보 필요. 이후 케이스 닫고 음악용 BT 연결 해제 후 스캔하는 것이 핵심 목표 검증.
- 사용자에게 요청할 **단 한 번의 보고**: 「전체 BLE/Apple/07/케이스형/내 암호화키 일치」 35초 검사 결과창 복사. 증거별 후속 단계는 BT 스캔/케이스 광고 포맷/암호화 키/Find My 소리 인증 경로로 차례 좁힐 것.
- `v1.0.14`은 개인정보와 연결 민감 자료를 외부 업로드하지 않는다. 공개 GitHub에 원시 AACP 키, 광고 MAC, 계정정보 절대 커밋 금지.

---

## 2026-10-15 케이스 찾기 실기 실패 기록
- v1.0.13 실험실에서 사용자 보고: 케이스 신호 0건, 실제 케이스 소리 미발생.
- 현재 Android 앱의 케이스 찾기 소리 기능은 **미구현/미검증**. Apple 나의 찾기의 실물 스피커 지원과 우리 Android 앱에서 제어 가능한지는 별개의 문제.
- 사용자 목표: 이어버드가 양쪽 모두 케이스 안에 있고 Bluetooth 오디오 연결이 없어도 케이스 소리로 찾기. 먼저 이어버드를 빼거나 연결하라는 방안은 목표 미충족.
- BLE 0건은 앱이 설정한 좁은 검색 조건에 맞는 수신이 없었다는 뜻이지 주변 모든 Apple 신호가 없다는 증거가 아님. RSSI 0 역시 유효 측정값이 아니라 초기값.
- 기존 정상 v1.0.10 GOLDEN 변경하지 말 것. 다음 배포는 실질적인 근거와 검증 가능한 개선을 갖춘 경우에만 진행할 것.

---

## 2026-10-15 09:28+KST user's v1.0.12 case GATT error 147, corrected v1.0.13 radio targeting

- User runtime: `BLE 연결 종료 상태코드: 147` twice on case-beep v1.0.12 LAB; **Android official BluetoothGatt.GATT_CONNECTION_TIMEOUT = 147/0x93** (not permission denied). Phone's ordinary ANC/AACP/Bluetooth still works; this is a *separate case GATT link*, no evidence of bad Bluetooth stack.
- Exact root defect in v112 `ci/CaseSpeakerLab_v112.kt`: used `settings.mac_address` from paired **earbuds (Classic address)** and `device.connectGatt(...,TRANSPORT_LE)`; source-backed 2026 research `stuffz/airpods/docs/ble.md` / `ble-research.md` confirms charging **case emits distinct rotating LE advertisements** `Apple manufacturer 0x004c, 07 11 06 plus 16 AES block` and does not resolve with buds IRK. Wrong target could cause GATT 147. This explains test *without proving all other causes ruled out*.
- v1.0.13 `ci/CaseBeaconFinder_v113.kt` scans 35s for case 07 11 06 ads and decrypts with `ENC_KEY` extracted by its own AACP connection. Requires fixed bytes 8,10,11 zero, proper length and battery bounds, >=2 same-address receptions; does not display/log target address or connect to arbitrary nearby BLE devices. No key or case adverts -> explicit evidence report, no GATT connection. Only detected connectable OWN case advert permits GATT service probe; no sound command unless characteristic + SECOND user permission. The old 147 label corrected to exact timeout meaning in `ci/patch_v113.py`.
- **Not verified**: scanned case presence, actual FD44 owner endpoint/authorization, real speaker beep. A case can broadcast without allowing GATT or may require Apple-proprietary owner authentication. Do not claim Find My solved based on APK compile.
- v1.0.13 separate LAB package `com.myairpods5.personal.lab`, branch `airpods-personal-v111-updater-diagnostics`, Actions run `38660885487` SUCCESS, release `my-airpods5-v113-lab-r30`; APK `My_AirPods5_v1.0.13_CASE_BLE_ADDRESS_SCAN_LAB.apk`, 51,359,482 bytes, SHA256 `aee4fc3cb276c00dba28b7d22f71def3005e8660f21a859b0a8f372d55d6bdf1`, signer SHA256 `e40f873f1e565f100a55c51760d5d54ca84f9d09957d58ed1cbbeb0f99638257`. GOLDEN v1.0.10 r21 remains unchanged.
- If v1.0.12 lab already installed, distinct GitHub Actions debug signer means v1.0.13 cannot install over v1.0.12 lab. **Only lab package** may require user-approved removal; no removal of the fully working GOLDEN v1.0.10 or original LibrePods, no BT unpair or phone resets. For no key: establish AirPods controls in LAB first; a standalone .lab app cannot read private encrypted prefs of GOLDEN.
- Next test: LAB Settings > 케이스 소리 시험 · 연구 -> 35s search, keep case nearby (opened/closed) and return result dialog text (not raw MAC). For real beep need independently verified AirPods5 speaker endpoint + owner auth; random opcodes forbidden.

---

## 2026-10-15 v1.0.12 실제 APK 및 다음 기준
- GOLDEN v1.0.10 r21 정상 앱 보존, 삭제·수정 금지.
- v1.0.12 LAB는 별도 패키지 `com.myairpods5.personal.lab`, 기존 앱과 나란히 설치 가능.
- Settings `케이스 소리 시험 · 연구`: 사용자가 페어링한 AirPods만 GATT 기능 확인, 인증된 사용자용 특성이 있을 때 별도 동의 후 짧은 소리 재생 시험. 소리 실제 발생 여부는 아직 검증하지 못함.
- ACTIONS `38657605679` 컴파일 PASS, release `my-airpods5-v112-lab-r27`, APK `My_AirPods5_v1.0.12_CASE_SOUND_RESEARCH_LAB.apk`, SHA256 `920e0fd3479c1b2a72ee144d90fbce7153fb0693016291c0edc89046585942f9`.
- 자세한 실험 프로토콜·사용법은 LAB 브랜치 `V112_CASE_RESEARCH.md` 확인. 기존 청취 모드와 원본 소스 보존. 다음 진행은 실기에서 나온 정확한 결과 기준으로 수정.

---

## 2026-10-15 v1.0.11 개발 후보 실제 APK 빌드 성공 — GOLDEN 승격 금지

- user-verified GOLDEN r21 v1.0.10 remains unchanged. User confirmed 4/4 listening modes real PASS.
- Candidate branch `airpods-personal-v111-updater-diagnostics`, CI run `38656036301` PASS. Also first baseline candidate CI `38655706739` PASS.
- **Lab APK**: `https://github.com/yooogillycloud123/my-airpods5/releases/download/my-airpods5-v111-lab-r23/My_AirPods5_v1.0.11_UPDATE_SETTINGS_DIAGNOSTICS_BETA.apk`; package `com.myairpods5.personal`, versionCode 10011, APK bytes 51,326,686, SHA256 `2f35bbe14f0adf4b497a75f67133894853517a8b8c9feb093897923b815d3b22`, signer SHA256 `9281f7ccbfe5e17c3fc9471661b62740a3d56e91bf0cb497540268f65a72b675`.
- Golden r21 signer SHA256 `f063dddd4b2621e89fde407cc2e66c6ce8a2ef69b892b54861545e6c166c77d0` **DIFFERS** from v1.0.11 beta. **Cannot upgrade GOLDEN in place!** Signing key was not preserved in r21. As the user wishes to keep their verified installation, DO NOT propose deleting it merely to run candidate. User must explicitly authorize one-time personal app migration after secure permanent signing is configured.
- v1.0.11 implementation `Settings→내 에어팟 5 관리→앱 업데이트 확인` checks channel `main/update-channel.json`, compares package/version/signature, optionally downloads and SHA-256 + APK cert verifies then launches Android installer; on signer mismatch refuses rather than attempt destructive install.
- `Settings→내 에어팟 5 관리→오류 기록` enables on-demand log copy/share; crash listener still alive, **every-launch diagnosis popup removed**; on-device reporter header now shows actual versionName.
- GitHub Actions branch optional secure signing via `AIRPODS_SIGNER_BASE64`, `AIRPODS_SIGNER_STORE_PASSWORD`, `AIRPODS_SIGNER_KEY_ALIAS`, `AIRPODS_SIGNER_KEY_PASSWORD` secrets. **Repository connectors in this chat do not support secrets administration**; no claim that secrets are present. The release includes `SIGNING_STATE.txt`, and actual certificate mismatch proves seamless upgrade NOT DONE.
- Apple case speaker Find My researched: only AirPods 5 Wireless Charging Case (case A3530) has speaker, standard case A3529 no speaker. Upstream Find My case play sound planned, not implemented. PR #764 charging case sound toggle (`IN_CASE_TONE_CONFIG`) ≠ Find My locator beep. No false feature claims.
- **Required next technical stage**: securely create and persist release signing key under private GH Actions secret; ensure same signer across >=2 signed test APKs, one-time authorized migration from r21, real in-app update success on Galaxy; only then label OTA PASS and consider case speaker opcode research. No AUTO-DELETION.

---

## 2026-10-15 다음 우선순위 확정 — 안전한 앱내 업데이트 → 로그 메뉴 이전 → 케이스 찾기 소리

- USER-PASS 업데이트: **4개 청취 모드 전부 실제 작동** (`끔/주변음 허용/적응형/노이즈 캔슬링`). 기존 v1.0.10 GOLDEN release `my-airpods5-test-r21` 그대로 고정. 사용자의 현재 설치본 삭제/데이터 제거/서명 변경 요청 금지.
- 사용자 지정 개발순서: **(1) 앱 안에서 업데이트 확인·업데이트 설치**와 업데이트마다 반복 설치·삭제 부담 감소, **(2) 매번 자동 표시되는 진단 다이얼로그 없애고 `설정→오류 기록`에서 수동 로그 확인**, 이후 **(3) '나의 찾기' 케이스 스피커 삐삐삐 소리 유도** 별도 개발.
- v1.0.11 전용 **독립 작업 브랜치** `airpods-personal-v111-updater-diagnostics` 시작(원본 v1.0.10 성공 커밋 `629f45d...` 기준), 여기에 `ci/PersonalUpdateManager_v111.kt`, `ci/patch_personal_v111_updater_logs.py`, 기능 설명 `V111_UPDATER_LOGS_AND_CASE_BEEP_NOTES.md`; **GOLDEN APK와 main 빌드 흐름을 변경하지 않음**.
- 인앱 업데이터는 사용자 클릭 시만 공개 GitHub `main/update-channel.json` 확인, SHA256/동일 패키지/높은 versionCode/동일 서명 인증서 검증 후 APK 다운로드·설치 화면 호출. 서명 불일치 시 보호 중단, 자동 강제삭제/설치 금지. 해당 채널은 **r21 v1.0.10 버전으로 잠금**; 검증된 신버전만 승격.
- 진단 로그 수집은 유지하되 매 실행의 자동 AlertDialog를 제거하고 앱 설정 `오류 기록`에서만 로그 복사·공유. 정적 v1.0.9 로그 헤더를 BuildConfig.VERSION_NAME으로 변경.
- **가장 중요한 Android 제한:** r21의 개인 signing key가 CI 임시 debug key였고 private key가 보존되지 않아 현재 설치된 r21은 다른 서명 APK로 **데이터 유지 자동 업그레이드 불가**. private keystore를 공개 저장소에 저장하지 말고 GitHub Actions 비밀정보로 고정 장기 서명을 갖춰야 향후 업데이트가 매번 작동함. 최초 서명 교체는 사용자 승인 후 1회 이관 필수; 이 제약 해결 전 '무삭제 업데이트 완성'이라고 주장하지 말 것.
- 케이스 찾기 소리: Apple 공식 정보상 **AirPods 5 무선 충전 케이스 모델**에 Find My speaker가 있음. 일반 USB-C 케이스는 해당 스피커 없음. LibrePods 상류 README의 `Play sound through charging case to find it`는 **계획 중 / 역공학 필요 / 루트 요구 가능성**이고 구현 기능 아님. 상류 PR #764의 `IN_CASE_TONE_CONFIG (0x31)`은 **충전 케이스 동작음 켜기/끄기**용으로 **분실 찾기 삐삐 호출 명령과 다름**. 정체불명 Opcode 송신 금지, 하드웨어 정확한 모델 및 프로토콜 입증부터.
- beta CI/릴리스 결과가 나오면 해당 커밋/다운로드 URL/서명과 GOLDEN 차이를 기록할 것. **사용자가 명시적으로 요청하기 전까지 r21 실사용 앱 설치 제거·전환 권유하지 않음**.

---

## 2026-10-15 08:24 KST — PERSONAL GOLDEN PHYSICAL PASS: v1.0.10 LOCKED

- **가장 최신 최우선 상태:** 사용자가 `오케이 청취모드도 작동하고`라고 직접 확인. 새 스크린샷: `주현팟` 개인용 앱 정상 표시, 배터리 100%, 청취 모드 4버튼. 기존 즉시 홈 튕김 없음.
- 기기 로컬 로그: 한 프로세스가 약 2분 넘게 살아 있고 `NO_JAVA_FATAL_CAPTURED`, 반복 App/Service 초기화 완료 기록; `Activity.onDestroy`는 정상 종료 이벤트일 수 있으므로 그 자체를 크래시로 오인 금지.
- **`v1.0.10` 사용자 검증 GOLDEN 기준 영구 고정**: 정확한 릴리스 `my-airpods5-test-r21`, APK SHA256 `b46c8e38f28332f7af0f98bce7b8b27234f62a83d103363cf0697a2fd127445b`, 빌드 commit `629f45dcc7b4189b713876660ddc2e18cf13f38a`. 별도 확정 매니페스트 `MY_AIRPODS5_V1_0_10_USER_VERIFIED_GOLDEN_LOCK_20261015.md` 필독. APK 수정·릴리스 바꿔치기·재서명·삭제 금지, 향후 개발은 별도 버전 후보에서 시작하고 실물 회귀 성공 후 승격.
- 이 앱의 `AirPodsCrashDiary.kt` 보고서 헤더에 있는 `My AirPods 5 1.0.9 local diagnostics`는 **진단 코드의 정적 문자열**이며 실제 설치 APK가 v1.0.9라는 증거가 아님. 별도 개발 버전에서 BuildConfig 기반으로 개선 가능하나 GOLDEN은 불변.
- **USER PHYSICAL PASS**: 앱 정상 실행/유지, 청취 모드 전환 작동. **DISPLAY PASS / ACCURACY UNKNOWN**: 배터리 100%. **SOURCE ONLY or UNKNOWN**: 한쪽 사용/세 요소 동시 배터리, widget/QS, 완전 한글화, 온디바이스 premium per-model, 팝업/오디오 핸드오프, 영구 서명/OTA.
- 사용자 의사: "일단 이 버전 픽스해놓고 수정발전 시킬 부분들을 공유해보자". 즉 여기서는 **새 APK 업데이트 강행 금지, 안정본 보존 + 실제 소스 기능 조사 및 우선순위 논의** 우선.

---

## 2026-10-15 08:11 실측 Java Fatal 확정 → v1.0.10 정확한 크래시 수정 / APK 산출

- 사용자가 v1.0.9 앱 `로그 복사`로 보고한 원문: `WindowManager$BadTokenException: Unable to add window -- token null is not valid`, `android.widget.VideoView$5.onError(VideoView.java:615)` → `android.app.AlertDialog$Builder.show`, **main thread**, pid 28747/29518/29804/29995/30137에서 반복. `AirPodsViewModel.init COMPLETE` 직후 1~2초 후 동일 종료가 재현됨. **원인은 배터리 값·AACP 통신 크래시가 아니라 Android VideoView 오류 팝업이 Activity token 없는 서비스 오버레이에서 표시되려다가 발생한 것으로 직접 입증**.
- 원본 소스 `presentation/overlays/PopupWindow.kt`과 `IslandWindow.kt`의 **정확한 결함:** 각각 `android.resource://me.kavishdevar.librepods/`를 비디오 URI에 하드코딩. 복제 앱의 실제 `applicationId=com.myairpods5.personal`과 달라 raw video resource를 찾지 못하는 직접적인 패키지 불일치. `VideoView` 기본 `onError` 처리로 토큰 없는 `AlertDialog` 표시 시도 → BadTokenException.
- 수정 `ci/fix_evidenced_videoview_badtoken_v110.py`: 두 VideoView의 URI를 `android.resource://${context.packageName}/${R.raw.connected/island}`로 변경, 각각 `setOnErrorListener { ...; true }`로 **기본 오류 대화상자 생성 경로를 완전히 차단**하고 비디오 에러는 로그+비디오 숨김으로 격리. 블루투스/AACP/ATT/BLE/노이즈 명령과 배터리 로직은 이 변경에서 미수정. 기존 v1.0.9 앱 내부 Java fatal 로그 수집 기능 유지.
- CI 빌드 `38652773288`, job `116155300268` PASS; `BUILD SUCCESSFUL in 3m 3s`, `VIDEO_FATAL_PATCH_PASS` 두 곳 PASS, `REAL_BADTOKEN_VIDEO_URI_AND_HANDLER=PASS`, APK ZIP+cert+package 검사 PASS, recorder DEX 검사 PASS.
- 결과 APK `https://github.com/yooogillycloud123/my-airpods5/releases/download/my-airpods5-test-r21/My_AirPods5_v1.0.10_BADTOKEN_VIDEO_URI_FIX.apk`. 릴리스 `my-airpods5-test-r21`, package `com.myairpods5.personal`, versionCode `10010`, 51,310,230 bytes, APK SHA256 `b46c8e38f28332f7af0f98bce7b8b27234f62a83d103363cf0697a2fd127445b`, signer SHA256 `f063dddd4b2621e89fde407cc2e66c6ce8a2ef69b892b54861545e6c166c77d0`.
- **사용자 실물 검증 대기**: 이 특정 `VideoView$5` → `BadToken` Java Fatal 경로를 차단한 것은 소스/CI 직접 검증; Galaxy에서 실제 앱 유지, ANC, 배터리 정상 동작은 아직 PASS 아님. 이번 빌드도 임시 debug signer가 이전과 다르므로 기존 불량 개인 앱 덮어쓰기 불가할 수 있음; 정상 원본 LibrePods 앱 삭제 금지.
- 다음 문제 발생 시 v1.0.10에 포함된 `AirPodsCrashDiary`로 **새 스택을 확보한 뒤** 해당 원인만 수정. 이전 v1.0.3~v1.0.8 반복 추정/전면개조는 금지.

---

## 2026-10-15 08:10 KST — 사용자 v1.0.8 여전히 동일 오류 → **증거 수집 우선 고정**

- 사용자 새 스크린샷에서 실제 배터리 100%/96%, 알림 `주현팟` 및 청취 모드 메뉴는 나타나지만 앱을 누르면 다시 홈으로 튕김. **UI/알림이 보인다는 이유로 제어 성공 또는 종료 원인을 확정하지 말 것.**
- 이전 v1.0.3~1.0.8의 원인을 로그 없이 단정하며 반복 패치한 것은 회귀를 해결하지 못했다. **추가 서비스/오디오 수정 전에 AndroidRuntime FATAL EXCEPTION 또는 ActivityManager 프로세스 종료 근거를 먼저 수집한다.**
- 실제 빌드된 `My_AirPods5_v1.0.9_CRASH_LOG_CAPTURE.apk`는 'FIX'가 아니라 진단용. `my-airpods5-test-r20` GitHub Release: `https://github.com/yooogillycloud123/my-airpods5/releases/tag/my-airpods5-test-r20`. CI run `38651819222` PASS, original Bluetooth/AACP/ATT/BLE frozen, `AirPodsCrashDiary` Android DEX 포함 게이트 PASS.
- 최종 APK SHA256 `42febe6220fb63f44ede1b60708a812f4a445b390a54e83ebd20c84b982964cd`; package `com.myairpods5.personal` versionCode `10009`; debug signer SHA256 `1fd80e747f04782ad3fa9a9e2caad9b245d474201650e9e9cae6e5da84eb20dd`; byte size `51293838`; source/build/ZIP/signature gates PASS.
- `ci/add_personal_diagnostics_v109.py`: Application 진입 즉시 Java UncaughtExceptionHandler 설치, 디스크에 최근 fatal stack trace 및 간단한 Activity/service/ViewModel lifecycle 기록; 매 실행 시 한국어 `앱 실행 / 로그 복사 / 로그 공유` 다이얼로그 제공. 자동 인터넷 전송/권한상승 없음.
- 사용자가 설치 후 `앱 실행`→이전 강제종료 재현→다시 열고 `로그 복사`→채팅에 붙여넣기. 만약 `NO_JAVA_FATAL_CAPTURED`라면 정상으로 판정하지 말고 Android 시스템 종료 로그 확보.
- 동일 Release에 `Capture-MyAirPods5-SystemCrash.ps1` 제공: 연결된 PC의 ADB에서 `-b crash` 및 관련 시스템 로그를 **읽기 전용**으로 추출하는 보조 방법. 데이터 초기화/페어링 재설정/원본 정상 LibrePods 삭제는 금지. 개인정보 포함 가능성을 고지.
- 매 GH Actions 실행마다 debug signer 변경. 기존 개인 앱과 서명이 달라 덮어쓰기는 불가할 수 있으며 사용자가 개인용 앱 재설치를 승인해야 함. 정상 LibrePods는 남긴다.
- **v1.0.9는 로그 확보 목적이며 실제 크래시 원인 해결/ANC PASS가 아니다.** 후속 패치는 stack trace 패키지/클래스/라인을 확인한 뒤 최소 범위만 수정, 실제 APK 빌드 링크까지 전달.

---

## 2026-10-15 사용자 신규 영상 → 실제 버그 수정 APK v1.0.8 CI 성공

- 사용자 최신 화면 녹화 `Screen_Recording_20261015_071445_One UI Home.mp4`, 20.24초: 앱 열기 → 연결 안 됨 → 배터리 100%와 노이즈 제어 UI가 잠깐 보임 → 홈 화면으로 앱이 반복 종료. 원본 LibrePods에서는 동일 AirPods 정상. 단순 '원본 정상 코드라서 문제 없음'이라는 진단 금지.
- 소스에서 반복 재진입의 직접적인 구조적 원인 확인: 이전 v1.0.6은 원본 `MainActivity.kt`를 복원하면서 `@Composable Main()` **본문의 `if(onboardingComplete) bindService()`**를 함께 복원함. Compose 배터리/UI 업데이트 시 매번 `startForegroundService+bindService` 호출, ServiceConnection→ViewModel.init→UI state 변경→recomposition→bindService라는 자기증폭 경로 발생.
- 추가 안정성 결함: `AirPodsService.kt`에서 한쪽 이어버드 배터리 정보가 없는 경우에도 `?.level!!` 4개 지점과 `getBattery()[1]`을 강제 접근함. 한쪽 착용 이벤트에서 예외 발생 가능.
- `ci/repair_personal_activity_v107.py`: onStop 중복 unbind 방지, Activity onDestroy가 서비스 receiver 해제하던 동작 제외, Composable side effect를 LaunchedEffect 1회로 제한, ServiceConnection 중복 init 방지, 빈 ANC packet 안전 접근.
- `ci/repair_personal_oneear_v108.py`: 4개 null assert 안전 변경 + battery component별 status + 짧은 ear-state packet guard. 실제 ANC 명령/이어버드 동작 로직은 건드리지 않음.
- **CI SUCCESS:** `38647015563` job `116136259445`; `BUILD SUCCESSFUL in 3m 51s`; Original `bluetooth/*`, 기타 서비스·데이터·receiver, Application SHA source PASS; targeted source patch gates PASS, APK ZIP/서명 PASS.
- **APK:** `https://github.com/yooogillycloud123/my-airpods5/releases/download/my-airpods5-test-r16/My_AirPods5_v1.0.8_SINGLE_EAR_LIFECYCLE_BATTERY_GUARD.apk`, versionCode `10008`, size 51,293,838 bytes, SHA-256 `eeada9a7f01a52c841913da59b5c547b816ceafa051d7a7be47791eab88d08bb`, signer SHA256 `269f9dcd50054019f13e54919c8f0bd54129d778a2a8134dd7fc931cdc2fb81f`.
- **IMPORTANT:** Galaxy 실물 회귀/배터리/ANC PASS는 아직 아님. 단말에 동시 설치된 진짜 LibrePods 서비스가 L2CAP 제어 채널을 이미 점유할 수 있음; 원본 삭제하지 않고 **일시적으로 강제종료**한 상태에서만 비교 시험하도록 권고. Debug APK마다 다른 서명 → 덮어쓰기 불가. 영구 서명 및 OTA 미해결.
- 다음 요청은 이 v1.0.8 결과를 출발점으로 이어가며, 더 이상 v1.0.6 'pristine'이라는 이유만으로 Composable 원본 바인드 반복 결함을 되돌리지 말 것.

---

## 2026-10-15 실제 결과물 추가 / CI VERIFIED (실물 작동 검증과 구분)
- v1.0.6 개인용 APK: `My_AirPods5_v1.0.6_PRISTINE_CORE_KOREAN_UI.apk`, GitHub release `my-airpods5-test-r14` (`https://github.com/yooogillycloud123/my-airpods5/releases/tag/my-airpods5-test-r14`).
- CI run `38644369485` job `116127479121` **SUCCESS**. 원본 소스 아카이브 SHA PASS, 원본 `MainActivity.kt`, Bluetooth/AACP/ATT/BLE, 서비스/receiver/data/ViewModel/Application **원본 대비 소스 바이트 SHA256 PASS**, 변경점은 한국어 온보딩/앱 설정 UI/문자열/버전 뿐.
- APK SHA-256 `f7788b3df715e75efac10d41dd7987d131ac04a87257a88ac8a46368efbc0d9c`, size `51,293,846` bytes, package `com.myairpods5.personal`, versionCode `10006`, signature cert `1c590b0ca3afece10e85159c4120b544fa4169739c3e7811f289b096f40b5be6`, v2/ZIP PASS.
- **실물 앱 연결/한쪽 착용/배터리/ANC는 아직 테스트되지 않았으며 PASS 아님**. 기존 v1.0.3/v1.0.5와 다른 CI 디버그 서명이므로 업데이트 설치가 거부될 수 있음. 원본 정상 앱을 삭제하지 말 것.


**최상위 사용자 확정 지시: 설명/분석/소스만 제출하고 종료하지 않는다. 이 프로젝트의 모든 실제 수정 요청은 가능한 범위에서 소스 패치 → 실제 Android CI 빌드 → APK ZIP·패키지·서명·SHA 검사 → 실제 설치 가능한 APK 링크 전달까지 같은 작업 안에서 수행한다. 빌드 실패 시 로그로 원인 교정하고 재실행한다. 빌드 전/실기 검증 전 상태를 FINAL 또는 작동 PASS라고 표현하지 않는다.**

## 1. 실제 물리 검증 사실 / 목표
- Galaxy S26 Ultra (SM-S948N), Android 17, AirPods 5, 사용자 별칭 주현팟/주현에어팟5.
- **사용자가 원본 LibrePods에서 실제 배터리 표시와 노이즈 캔슬링 등 원래 기능이 전부 정상 동작한다고 반복 확인했다.** 이를 사용자 최상위 사실로 취급할 것.
- 문제는 AI가 변형한 `com.myairpods5.personal` 한국어 개인용 앱: 한쪽 착용과 연결 상태 변경 시 홈으로 튕김, 연결이 풀림, 0% 배터리·ANC 조작 불능. 2026-10-14 사용자 제공 실제 10초 화면 녹화의 2~5초에 재연결→100% 잠깐 표출→홈 강제종료→재연결 반복.
- 작업 목표: **원본에서 실제 성공하는 Bluetooth/ATT/AACP/BLE/배터리/ANC/착용/연결 로직을 온전히 보존한 채 개인용 명칭·한국어 UI·불필요한 개발자 문의 메뉴 제거.** 기술적 목표 하향, 원본 앱 탓/기기 탓 금지.

## 2. 절대 지켜야 할 회귀 방지
1. 원본 엔진 재작성/서비스 생명주기 무근거 교체 금지. 원본 `MainActivity.kt`, `AirPodsService.kt`, `bluetooth/*`, `data/*`, `receivers/*`, `AirPodsViewModel.kt`, `LibrePodsApplication.kt`의 SHA-256 바이트 동일성을 CI에서 검증할 것. 사용자 기기 로그로 입증된 개별 버그만 별도 승인 변경.
2. `ci/repair_personal_core_v104.py`와 `ci/repair_personal_core_v105.py`는 이전 실험으로 서비스/착용 상태까지 수정했다. **현재 복원 후보 v1.0.6에서는 실행하지 않는다.** 두 스크립트를 원본 성공 동작의 기준으로 재사용 금지.
3. `ci/repair_korean_onboarding_and_service.py`는 한국어 온보딩뿐 아니라 MainActivity까지 변경한다. 이 경우 CI에서 원본 MainActivity를 백업/복원하고 엔진 SHA 게이트가 PASS해야 한다.
4. 한쪽/양쪽 착용·케이스 열림·꺼짐·재연결·배터리 왼쪽/오른쪽/케이스·실제 ANC 명령까지 원본에 대조하는 실물 회귀 테스트가 끝나야 FINAL.
5. UI에서 이메일, 후원, 디스코드, GitHub 이슈제보, 유료 업그레이드, 불필요한 원본 개발자 링크는 제거. GPL/오픈소스 라이선스와 저작권 고지는 유지. 모든 실사용 UI 한국어.
6. 기존 실제 정상 원본 LibrePods 절대 삭제/데이터 초기화/페어링 해제 요구 금지. AA Android Auto 라우팅 앱과 복구 데몬/보조 앱도 절대 변경하지 않는다.
7. 사용자 데이터·기존 개인 앱 업그레이드 가능 여부는 **인증서와 versionCode 정확 검증**. GitHub Actions ephemeral debug 키는 매번 다르므로 정상 덮어쓰기 업데이트가 안 된다. 서명 일치하지 않는 APK를 업데이트 가능하다고 속이지 않는다. 공개 저장소에 개인 서명용 비밀키를 커밋하거나 올리지 않는다. 실용적 영구 서명은 안전하게 유지되는 비공개 저장소/CI 보안 시크릿을 통해 별도 확보해야 한다.
8. Android 앱에서 배터리/청취 모드가 화면에 나타났다는 사실만으로 **실제 AACP 제어 정상 PASS**라 하지 않는다.
9. 어떤 작업이든 **생성한 설치 가능한 실제 APK를 사용자에게 바로 제공**하고 정적 PASS와 물리 PASS를 분리해서 상태 표기할 것. 화면이 보인다는 이유로 기능 정상 주장 금지.

## 3. GitHub 및 빌드
- 저장소: `yooogillycloud123/my-airpods5`, 브랜치 `main`.
- 소스 압축본: `My_AirPods5_Clean_Standalone_Korean_SourceBuild_20261014-2.zip` SHA-256 `0403d20dcc83ae02a974d69b1340e5686ac408b43f77e341e2068fe9991829da`.
- 빌드 워크플로: `.github/workflows/build-my-airpods5.yml`; Android SDK36, NDK 28, Java21, `assembleFossDebug`. 빌드 뒤 원본 엔진 코드 SHA gate, APK package, ZIP, signer, SHA 기록.
- 2026-10-15 우선 사용 후보: `ci/repair_personal_ui_v106.py`에서 앱 UI/한국어 문자열만 변경하는 v1.0.6 작업. **GitHub 최신 CI 상태를 조회하여 PASS/FAIL을 구분**하고 유효하게 완성된 APK를 전달한다.
- 이전 v1.0.3(원본 기능 불능)·v1.0.4·v1.0.5는 기능 복귀 증거 없음. 단순 재배포 금지.
- APK 배포 뒤 우선 시험 순서: 원본 LibrePods 동작 확인(완료 사실) → 개인 앱 실행(홈 튕김 없음) → 한쪽 착용→ 양쪽 → 한쪽 빼기 → 배터리 실제값 → ANC on/off/주변음/적응형 실제 효과 → 백그라운드 복귀 → 빠른 재연결; 모든 실제 확인은 사용자 측 물리 검증 전까지 UNKNOWN.

## 4. 답변 원칙
사용자는 소스 설명이 아닌 **실제 APK 결과물**을 요청한다. 불필요한 추론·반복 확인 질문·버그 신고 요구·장황한 설명으로 완료를 대체하지 않는다. 원본은 작동한다는 사실을 재차 의심하지 말고 **변형 앱/서명/패키징** 문제를 좁혀서 해결한다. 빌드 실패는 가능한 범위에서 직접 수정해 재빌드한다.
