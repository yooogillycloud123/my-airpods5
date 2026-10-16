# MY_AIRPODS5 v139 REAL PHYSICAL PASS / CASE IDENTITY BLOCKED — 2026-10-16

## Exact user screenshot after v139 install (authoritative real phone result)
- Title: `v139 단일 스캐너 · 12초 결과`. This is **user physical confirmation v139 starts, UI displays 12-second report and manual BLE callbacks exist**. It does not prove app exclusive global scanner, case sound, or Bluetooth traffic isolation from all other applications.
- `전체 BLE 콜백=307`
- `Apple 제조사 콜백=29`
- `AirPods 주변 광고 07:19=0`; model tags `0x3020=0`, `0x3620=0`, Apple07 other `0`, Pro3 `07:11:06=0`.
- `Find My 주변형 12:02=13`
- `서로 다른 주소(임시)=5` — these are **five transient advertiser addresses within that session**, not proof of five distinct physical devices or five owned cases (RPA can rotate).
- `Find My 분리형 12:19 연결가능=0`, `12:19 연결불가=0`, separated unique addresses `0`, same-address 12:02+12:19 overlap `0`; no 12:19 device to GATT query.
- `그 외 12형(중복 제외)=0`; fixes real v138 duplicated classification bug. `Apple Nearby 10형=0`. Other Apple packets `29-13=16` by subtraction, but their actual formats were not separately classified by this UI. Do not call these 16 packets AirPods or case signals.
- `이 LAB 상시 BLE 검색=OFF / 수동 검색만 ON` reported from source setting; original personal AirPods app / other Android processes not proven scanner inactive.
- No live case-specific owner proof, GATT case protocol or audible closed-case beep. The actual user goal remains **NOT_IMPLEMENTED / PHYSICAL_UNKNOWN**.

## New supported conclusions / previously invalid causal claims
1. v138 `12:02=11, 12:19=0, 07:19=0` and v139 `12:02=13 (5 transient addresses), 12:19=0, 07:19=0`. The previous 1-off v136 candidate `12:19=1, FD44 4F860003 non-owner` has **NOT** been reproduced and its identity has NEVER been established.
2. v139 disables only **its own** `AirPodsService.bleManager.startScanning()` in source and reports external BLE callbacks from only 1 manual scan registration; it **does not** disable other packages' scanners by itself. The screenshots show no 12:19 after this isolation attempt; this does not identify radio emission state or prove other LABs had stopped.
3. `12:02` frames show *Apple manufacturer type 12 nearby format*, not case ownership. RPA changes mean five addresses are not five distinct physical trackers; no stable identity mapping to earlier `12:19`. Zero `07:19` may be expected for closed case disconnected but cannot prove why; do not assert inability to beep.
4. Official Apple KR spec (https://www.apple.com/kr/airpods-5/specs/) states AirPods5 **wireless charging case** supports external **Find My speaker**; Apple support `https://support.apple.com/ko-kr/109525` labels wireless charging case model **A3530**. Hardware capability is real (do not conflate with software control).
5. Public LibrePods README (https://github.com/librepods-org/librepods/blob/main/README.md) still lists **Play sound through charging case** as planned/reverse engineering required. Maintainer on issue #102 (https://github.com/librepods-org/librepods/issues/102) explicitly said charging case feature involves **owner-authenticated case GATT server**, not ordinary earbud AACP sound packet. No verified Android case authentication/closed-case play/stop command has been obtained. Issue #718 proposing AACP sound was closed and is not tested evidence of case beep.
6. User has already performed sufficient repeated 8/12/35-scan experiments; DO NOT request another APK or identical scan. Existing `com.myairpods5.personal.radiolab` v139 remains active; v134 data preserved; v136/v137 deprecated/force-stop only; Golden personal v1.0.10 and AA untouched. Signing-key rotation remains a separate obstacle to seamless APK updates.

## Source-first next engineering gate
- **No v140 APK just to chase 12:19 candidate.** First investigate verifiable **AirPods 5 A3530 CLOSED charging case** BLE radio identity and authentic owner association / GATT authentication / audible case-play/stop commands, not Find My generic nonowner FD44 or proximity AACP from earbuds.
- Retain a pass/fail gate in source: proprietary case GATT auth or source-backed case speaker opcode is REQUIRED before enabling a user-facing beep button. Explicitly classify unknown packet as not owner-bound.
- Future LAB should be same package; stable private signing identity needed BEFORE update-install claim; never publish private key in public repo. Existing v139 physically displays stats correctly, no further phone tests needed for this passive scan.

STATUS: v139_MANUAL_SCAN_AND_UI_PHYSICAL_PASS, v139_EXCLUSIVE_GLOBAL_BLE_SCANNER_UNPROVEN, AIRPODS5_A3530_CLOSED_CASE_OWNER_ID=UNKNOWN, CASE_GATT_AUTH=UNKNOWN, CASE_SPEAKER_COMMAND=NOT_IMPLEMENTED.
