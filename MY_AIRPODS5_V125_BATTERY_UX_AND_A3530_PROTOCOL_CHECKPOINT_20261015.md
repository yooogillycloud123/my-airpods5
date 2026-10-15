# AirPods 5 v1.0.25 — 실제 signed LAB APK + 배터리 0% 실측 회귀 수정, Case Find My 미구현 (2026-10-15 KST)

## ONE-CLICK outcome in this iteration
- 사용자 마지막 요청: "가장 좋은 방향성으로 진행하자 그리고 결과물도". 기존 v124 actual Galaxy 캡처에서 Apple BLE 광고 **4/33**, Pro3 독립 케이스 유형 `07 11 06` **0/0**, LAB ENC_KEY 없음, 홈 배터리 **0%** 잘못 표출.
- New real signed Android APK created on branch `airpods-personal-v125-model-specific-ux-20261015`, Actions **38724810662 SUCCESS** (job `116387918769`), Gradle **BUILD SUCCESSFUL in 4m 5s**, release tag `my-airpods5-v125-battery-gate-r2`.
- APK link https://github.com/yooogillycloud123/my-airpods5/releases/download/my-airpods5-v125-battery-gate-r2/My_AirPods5_v1.0.25_BATTERY_PROVENANCE_LAB.apk
- sha256 `47abddb83ff85de6cbc89853a2b4039b35af2376b778ffd69463f4546709b18c`, size **51,425,109 bytes**.
- package `com.myairpods5.personal.lab`; versionCode **10025**, versionName `1.0.25-battery-provenance-case-gate-debug`.
- cert SHA256 `0244df2d9b5a6fe9f1e92fadbd248f5f682fda939ea57c8268d02948d8020e7b`; debug signer differs from **v124** cert `452370d8feda95849415c8a2dc9ebafd6240d14228d512de14c006fa504871a5` -> **not update-install compatible** with user's currently installed LAB. They must NOT remove v1.0.10 or existing LAB data just to test speculative case audio.
- ZIP/APK signature v2 PASS, original baseline source SHA verified in CI, original Bluetooth/ATT/AACP engine unchanged.

## Real code improvements; not a case speaker completion
- Actions **38724253324 SUCCESS** previously unpacked the exact frozen source and showed v122 patched BatteryView still permitted an all-zero but non-disconnected, non-null battery packet to become one prominent **0% ring**. The user's screenshot precisely matches this symptom; do not claim this proves which system sent zero.
- v125 `ci/case_protocol_v125/patch_lab_safe_battery.py` applies after v124 patches: `BatteryView.kt` if no recorded component in 1..100 then **'배터리 정보 확인 안 됨 (0%로 단정하지 않음)'** rather than presenting false 0% circle; checks 3 icon conditional branches. `PopupWindow.kt` also protects all-three zero falsely reported in popup; known actual data with at least one nonzero component remains visible. A physically true all-component 0% cannot be distinguished without raw provenance; conservatively display unknown for all-zero.
- v125 `CaseSoundStatusV125.kt` read-only transparent status dialog replaces old `CaseResearchMenuV124.open` settings link; no repetitive AirPodsPro3 format scans, GATT writes or automatic BLE probing. The v124 legacy source remains in compiled module unused (no engine removal/refactor). Case Find My beep still unimplemented.
- Local source tests **7** + inherited v123 tests **5+7** synthetic PASS. All actual model-specific A3530 case owner auth/play/stop commands remain unknown and NOT transmitted.
- Prior separate partially user verified v1.0.10 `com.myairpods5.personal` must be preserved. Android Auto AA app/daemon untouched.

## Source references and model-specific next work
- https://github.com/d4rken-org/capod/blob/main/app/src/main/java/eu/darken/capod/pods/core/apple/ble/devices/airpods/AirPodsGen5Wireless.kt model **0x3020** supports device status/battery history, but **no Find My A3530 case speaker**.
- https://github.com/d4rken-org/capod/blob/main/app/src/main/java/eu/darken/capod/monitor/core/aap/AapKeyPersister.kt handles AAP keys IRK/ENC_KEY; Bluetooth buds proximity encryption keys **not the same** as separate case owner authorization.
- https://github.com/librepods-org/librepods/issues/102 maintainer explicitly indicates separate **case GATT server + owner auth** unresolved.
- https://github.com/wrfwdd/Pods-control/blob/main/core-aacp/src/main/java/com/airpods/control/core/aacp/AacpFrameBuilder.kt `0x0C` was named earbud tone but no actual independent A3530 case-auth evidence; code may contain inconsistent button-to-side enum mapping. No random replay.
- For real functional completion the next REQUIRED external artifact is permissioned/proven sample session of same-model A3530 closed case performing Find My sound *with owner authenticated* or a manufacturer-confirmed case GATT specification; then implement case identity/owner auth/play/stop and confirm actual audible case.
- **NEVER** ask user to rerun the 10s scan, reinstall experimental LAB, share credential, buy iPhone, erase pairing, or delete v1.0.10.
- RESEARCH SOURCE ✅; APK compile/sign ✅; actual battery fix physical PASS ❓; case sound implementation ❌; case sound physical PASS ❌.
