# MY AIRPODS5 v136 실측 — FD44 non-owner path confirmed on an UNKNOWN target (2026-10-16)

## 사용자 실제 4장 스크린샷 — 정확히 확인
- 설치 실행: `1.0.36-candidate-picker-parallel-safe-debug`, versionCode `10036`, `com.myairpods5.personal.caseprobe`.
- v134 회귀였던 후보 선택 UI: 후보 **1개** 목록 행 표시; 사용자가 행을 실제 선택했고 별도 승인 대화상자에서 `내 기기 서비스 확인`을 눌러 **GATT READ-ONLY 서비스 열거 결과까지 정상 진행했다**. `v136 USER_PHYSICAL_CANDIDATE_PICKER=PASS`. UI 단일 후보 선택 버그 종결.
- 관측 후보: Apple Find My 12:19 connectable frame, 관측 RSSI **-85 dBm**; BLE MAC/주소는 재기록하지 않는다. -85dBm 하나만으로 본인 기기/거리 판별 불가능.
- `Find My FD44 소리 특성=true` (실제 소스는 FD44 service + `4F860003` characteristic 확인)
- `DULT 소리 특성=false` (실제 소스는 `15190001` + `8E0C0001` 확인)
- `GATT 서비스 개수=8`
- `케이스 A3530 신원=미검증`; `소리 실행/소유자 인증=미검증`; `쓰기 명령=0`, 기존 앱 기능 변경=0.
- **Do not call this an AirPods5 owner/case sound success.** The screen is real GATT non-owner service detection on a 12:19 Find My candidate; it is NOT a proof of who owns the target or that the target is a charging case.

## Public primary source validation and new conclusion
1. `ci/case_protocol_v134/CasePathV134.kt` filters Apple manufacturer `12 19` connectable; `FD44=true` tests **4F860003 NON-OWNER** control point, NOT 4F860002 owner config. It never reads model, serial or challenge.
2. Apple Find My network accessory specification (third-party MFi) distinguishes `12 02` **nearby** vs `12 19` **separated**. This supports treating the selected signal as possibly a separated third-party tag, not automatically as this Galaxy-only AirPods case. **Third-party MFi spec does not prove proprietary AirPods 5 state.**
3. AirGuard current `AppleFindMy.kt` uses FD44 `4F860003` in anti-stalking non-owner separated mode, not an authenticated owner Find My speaker.
4. OpenTagViewer `docs/owner-connected-tags.md` documents this exact split: non-owner 4F860003 can function separated; authenticated owner 4F860002 required encrypted owner's LE key in third-party example. Android standard BLE GATT API has no user-supplied stored owner LTK injection API. Actual AirPods5 A3530 may use distinct protocol; no certified owner command.
5. Read-only model / manufacturer characteristics from `6AA5xxxx-6352-4D57-A7B4-003A416FBB0B` are public in *some* Find My accessories (MFi spec and AirGuard code). Their availability, value, relation to AirPods5 A3530, and whether model indicates ownership are **unknown**.
6. AirPods 5 wireless case speaker hardware exists per Apple; no Apple pairing history for user; nothing shown changes that blocker. No unsolicited beep-write to an unidentified Bluetooth device.

Primary public links (source origin):
- https://github.com/seemoo-lab/AirGuard/blob/main/app/src/main/java/de/seemoo/at_tracking_detection/database/models/device/types/AppleFindMy.kt
- https://github.com/parawanderer/OpenTagViewer/blob/main/docs/owner-connected-tags.md
- https://developer.apple.com/find-my/
- https://github.com/librepods-org/librepods/blob/main/README.md

## Next engineering gate — NO repetitive v136 user test required
- Preserve current v136 lab application/data (and original personal v1.0.10, original v134 lab, AA Golden) as installed. No resets or extra scans.
- Prepare SOURCE FIRST: explicit **read-only** accessory info characteristic model/manufacturer probe and enforce `identity=UNKNOWN` even if `FD44=true` unless a source-verifiable OWN AirPods5 case binding exists.
- Do not demand new APK install solely to repeat discovery. If this classification adds discriminating information and can be shared via stable signer/update path, prepare one isolated prototype; test only with target that the user can actually establish as owned.
- Need **case** evidence: AirPods 5 closed case identity and an authenticated Find My owner-case speaker command, or source-backed safe protocol clearly applicable to the same user-owned case. Observed GATT/FD44 is not enough. Writes=0 until verified.
- Physical gates: candidate UI PASS, GATT enumeration PASS, FD44 nonowner UUID PASS. Actual case model/ownership FAIL-TO-VERIFY (UNKNOWN), authenticated case speaker operation NOT_IMPLEMENTED.
