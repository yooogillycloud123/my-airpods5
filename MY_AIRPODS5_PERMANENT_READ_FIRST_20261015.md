# 내 에어팟 5 · 영구 인수인계 / READ FIRST (2026-10-15)

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
