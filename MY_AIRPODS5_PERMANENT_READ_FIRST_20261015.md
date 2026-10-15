# 내 에어팟 5 · 영구 인수인계 / READ FIRST (2026-10-15)

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
