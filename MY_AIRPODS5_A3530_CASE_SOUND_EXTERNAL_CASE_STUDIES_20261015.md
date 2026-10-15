# AirPods 5 무선 충전 케이스 A3530 — GitHub / Reddit / MaterialPods 유사 성공 사례 근거 감사 (2026-10-15)

## 실제 사용자 요구 (최종 기능 시험 기준)
- Galaxy S26 Ultra / Android 17 / AirPods 5 with Wireless Charging Case: earbud BLE model **0x3020**, 독립 케이스 모델 **A3530** (Apple 지원 기준).
- AirPods 양쪽 모두 케이스 안, 덮개 닫힘, A2DP 끊김, Apple Find My 소유자 계정 등록 이력 없음. 갤럭시 앱에서 버튼 한 번으로 **물리적 케이스 자체 스피커에 Find My 비프음**이 들릴 것.
- user v124 두 번 결과: Apple manufacturer advertisements 4 / 33, `07 11 06` **0 / 0**, 다른 07 0 / 0, LAB ENC_KEY 없음, 앱 UI 잘못된 0% 표출. 광고 반복 검사 중단. User-uploaded product page and MaterialPods blog screenshots 직접 참조.

## A. 제조사 실제 하드웨어 입증 (speaker EXISTS)
1. Apple AirPods 5 specs: https://www.apple.com/kr/airpods-5/specs/ — 무선 충전 모델만 Find My용 케이스 스피커.
2. Apple case identification: https://support.apple.com/ko-kr/109525 — 무선 케이스 **A3530**; 기본 일반 케이스 **A3529**, 외부 스피커 구분. User's screenshot shows wireless case purchase.
3. Apple Find My official: https://support.apple.com/en-by/guide/airpods/dev8e8b93d71/web — Apple 앱 (iPhone/iPad/Mac/Watch) 케이스 소리 가능. **iCloud.com/find 에서는 케이스 자체 소리 불가**라는 공식 별도 주의. User screenshot’s generic '웹 iCloud에서 [사운드 재생]으로 케이스 찾기' is incorrect for this target.

## B. MaterialPods의 실제 성공 ≠ 닫힌 케이스 스피커
- Google Play: https://play.google.com/store/apps/details?id=com.pryshedko.materialpods — app advertises Search. **2023-05-02 Piper Waite verified review**: “make the airpods ring so you can find them if lost **out of the case**.” Earbud sound success anecdote, not case sound.
- https://beebom.com/best-airpods-apps-for-android/ — specifically says ring **each bud** (이어버드). User-uploaded tutorial screenshots literally show **left/right/both** and **two earbuds outside/open case** from AirPods 3-era example. Neither proves closed A3530 case sound.
- MaterialPods Play listing currently does not list AirPods 5 model among specifically supported models in feature description. Never port such GUI as a speaker-protocol proof.

## C. Unverified 'Find My' app source uncovered — Pods-control needs audit
- https://github.com/wrfwdd/Pods-control — README asserts 'Find My - Play sounds on your AirPods'. Real source inspected:
  * `core-aacp/.../AacpProtocol.kt` names **0x0C** `PLAY_FIND_SOUND`;
  * `core-aacp/.../AacpFrameBuilder.kt` builds through EARBUＤ AACP and comments *0=both, 1=left, 2=right*;
  * `feature-home/.../HomeScreen.kt` instead passes *left=0, right=1, both=2* (self-contradiction).
  * `AacpCommander.playFindSound()` returns `transport.sendCommand(...) != null`, **not case beep or hardware verification**.
  * No identified distinct **A3530 CASE GATT owner authentication** or physical case play/stop sound flow. README promises/0x0C command **NOT an A3530 success proof**, do not send blindly.
- Exact source:
  https://github.com/wrfwdd/Pods-control/blob/main/core-aacp/src/main/java/com/airpods/control/core/aacp/AacpFrameBuilder.kt
  https://github.com/wrfwdd/Pods-control/blob/main/feature-home/src/main/java/com/airpods/control/home/HomeScreen.kt
- Apple earbud AACP control and independent Find My case GATT must remain separate.

## D. Useful NEW model-specific code: CAPod AAP ENC_KEY and 0x3020
- https://github.com/d4rken-org/capod/blob/main/app/src/main/java/eu/darken/capod/pods/core/apple/ble/devices/airpods/AirPodsGen5Wireless.kt — `DEVICE_CODE=0x3020`, explicitly AirPods **5 wireless** device model; recovers snapshot battery/lid from proximity packets + history. No Pro 3 07 11 06 assumption.
- https://github.com/d4rken-org/capod/blob/main/app/src/main/java/eu/darken/capod/monitor/core/aap/AapKeyPersister.kt — AAP key exchange `IRK` and `ENC_KEY` persisted to own device profile, enabling precise encrypted battery decoding.
- https://github.com/d4rken-org/capod/wiki/airpod-Keys — CAPod 5.0+ can *usually* fetch keys automatically on supported fixed Android Bluetooth stacks (platform/model dependent, not guaranteed).
- This is high-quality *battery and nearby identity* code foundation for user v124 broken 0% and missing key, NOT a physical speaker beep protocol. CAPod README does not claim charging case Find My beep.
- Do NOT ask user to install CAPod/test or delete personal baseline. Source adaptation only after patch-size/engine compatibility audit.

## E. GATT case-auth blocker in original author source
- https://github.com/librepods-org/librepods/issues/102 — maintainer `kavishdevar` directly explains target is separate CASE GATT server; **case must authenticate an owner**; technique is unknown.
- https://github.com/librepods-org/librepods/issues/20 — authenticate case and play sound through case still unchecked.
- https://github.com/librepods-org/librepods/blob/main/README.md — Find My case speaker marked planned / more RE required.
- AACP charging-case alert setting `0x31` and 0x0C earbud sound proposals are NOT authenticated A3530 Find My command.

## F. Reddit anecdotes that do NOT meet user criterion
- https://www.reddit.com/r/airpods/comments/ycykt8/ — Android users report case automatically chimes on motion/anti-stalking. This proves case *may* emit non-Find-My sounds by its own triggers, **not Android commanding closed case**.
- https://www.reddit.com/r/airpods/comments/1w2hd1x/ — Android iPhone switchers hear FindMy-associated chirp if previously registered to Apple ID, opposite of this user's no-iDevice/no-account history.
- https://www.reddit.com/r/airpods/comments/1wnhwhq/ — AirPods 5 wireless case owners report Apple Find My functionality exists; not Android remote speaker command.
- https://www.reddit.com/r/galaxys26ultra/comments/1wt50q7/ — S26 Ultra OneUI9 LibrePods ANC/battery successful but **case ring not claimed**.
- https://github.com/parawanderer/OpenTagViewer/blob/main/docs/owner-connected-tags.md and https://github.com/seemoo-lab/AirGuard — distinct 3rd-party FindMy owner DULT/nonowner accessory sounds; **not AirPods5 case owner path**, may guide authentication architecture only.

## Verdict and useful engineering shift
- **NO documented reproducible public success found (as of source review) for this exact condition**: Galaxy-only / no Apple ownership enrollment / A3530 case / both earbuds docked / lid shut / BT A2DP disconnected / **case audible Find My speaker**.
- Found **genuine partial successes**: MaterialPods external earbud ringtone; LibrePods native AACP ANC/precise battery on Samsung; CAPod **0x3020** BLE and AAP key exchange; Android anti-stalking *automatic* case sounds. None constitutes user target sound command.
- Stop Pro3 format scanning after two user negative measurements. Do NOT re-run v124, re-pair, or reinstall. No guessing/writing arbitrary opcodes.
- **High-value next technical route:** (1) use CAPod AirPods5 model 0x3020 parser/key persister to repair native battery/identity source in *isolated source branch* without affecting frozen personal and AA app, (2) obtain **real A3530 case sound session** trace in a permitted source research setting from authentic Apple Find My owner session (public trace/maintainer collaboration rather than repeated user tests), (3) derive **owner case BLE identity → GATT auth → case-specific sound play/stop → actual audible confirmation**, (4) only then user-visible one-app APK, with stable signer/rollback.
- Lack of published proof does **not** mean hardware impossibility, and iCloud login, Shizuku or root do not independently solve missing proprietary owner-auth protocol.
