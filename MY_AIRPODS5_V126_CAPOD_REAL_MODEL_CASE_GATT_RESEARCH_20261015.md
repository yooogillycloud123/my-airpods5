# AirPods 5 0x3020 / A3530 — v126 source-backed development checkpoint (2026-10-15)

## User's actual final acceptance test
- Samsung Galaxy S26 Ultra + user's AirPods 5 Wireless Charging Case (buds BLE Continuity model **0x3020**, case hardware **A3530**). Both buds docked, lid closed, Bluetooth A2DP not connected, no Apple device enrollment, Android Find My button plays **audible beep from physical case speaker**.
- Do not change/merge v1.0.10 partially user-verified baseline, Android Auto Golden, ADB daemon, or user's pairings/data. Do not ask for repeated v124 zero-result 10-second scans.
- Physical v124 screen showed 4 and 33 Apple advertisements, 0 `07 11 06` frames, LAB `ENC_KEY` missing, fake 0% battery; v125 UI-only correction built PASS, physical correction unverified.

## New original research verified today
- `d4rken-org/capod` official active source `app/src/main/java/eu/darken/capod/pods/core/apple/ble/devices/airpods/AirPodsGen5Wireless.kt` has `DEVICE_CODE=0x3020` and separate `AirPodsGen5.kt` has `DEVICE_CODE=0x3620`. This resolves **model identity classification**, NOT cryptographic ownership of the *separate case*.
- CAPod `ProximityPairing.kt` requires manufacturer Apple 0x004c Continuity `07`, length 25, public prefix `01` (full 27-byte frame). Nonstandard `07` garbage must be rejected rather than interpreted as battery/nearby.
- CAPod `DualApplePods.kt` decodes public nibble batteries to 10% steps (0xF absent), private AES decrypted whole percentages only when valid ENC_KEY is present; 0xFF is absent. CAPod `AapKeyPersister.kt` persists IRK/ENC_KEY received from connected **earbud** AAP; does not provide case owner authentication.
- `librepods-org/librepods` actual `BLEManager.kt` model map lacked distinct 0x3020/0x3620 in externally inspected upstream branch. Its encrypted `formatBattery` historically masked 0xff to 127%, mistakenly treating it as valid for buds and risking case cache pollution. Its BLE manager already uses AirPods IRK to verify RPA from earbuds, and **that RPA cannot identify an independent A3530 case**.
- Apple official AirPods 5 specs https://www.apple.com/kr/airpods-5/specs/ says wireless case has Find My speaker. https://support.apple.com/ko-kr/109525 explicitly identifies hardware model A3530 as wireless speaker case. https://support.apple.com/ko-kr/guide/airpods/dev8e8b93d71/27/web/27 explicitly says web iCloud cannot ring this case; Apple Find My app can.
- `librepods-org/librepods/issues/102` maintainer describes **separate case GATT server, owner authentication unknown**, distinct from AAP charge alert tone 0x31. `librepods-org/librepods/issues/20` marks CASE-authentication & CASE-sound unchecked.
- `parawanderer/OpenTagViewer/docs/owner-connected-tags.md` shows authenticated owner `4F860002` sound on **third-party RAVEMEN ABF01**, not A3530. `4F860003` nonowner separation/tracking control for eligible devices, NOT transferable to this case without source proof.
- No publicly confirmed same-model **Galaxy-only + Apple account not enrolled + closed A3530 case Find My beep** successful program/protocol/packet transcript found in current searches. Lack of public proof is not proof of hardware impossibility.

## Actual work committed, independent branch
- Branch: `airpods-personal-v126-capod-model-battery-gatt-gate-20261015`.
- `ci/case_protocol_v126/patch_capod_model_battery.py` updates only the **existing isolated LAB BLEManager.kt**:
  1. Label 0x3020 `AirPods 5 (무선 충전 케이스)`, 0x3620 `AirPods 5`.
  2. For only these two models require `07 19 01` with at least 27 bytes; do not treat foreign plaintext as own case.
  3. Encrypted battery 0xFF and byte with 7bit percentage >100 become `null`, never 127% or fake case charge; case value cache stores only valid 0..100.
  4. Existing Bluetooth scanner, ANC modes, L2CAP socket, device pairing and key storage algorithm remain unchanged. This gives no new case-radio scans or GATT writes and cannot produce Find My case sound.
- `ci/case_protocol_v126/test_model_battery_contract.py`: 11 offline public-model/battery sanity tests, plus preexisting v125/v123 suites.
- `.github/workflows/v126-capod-model-battery.yml`: actual Android build on independent branch, preserving original source bundle SHA256 and hashes for all untouched engine files; `BLEManager.kt` is narrowly exempted from bytewise frozen comparison and source gated.
- v125 signed LAB APK remains available and uninstalled/updater incompatibility persists; each debug CI build generates a new **signer identity**. DO NOT force install/uninstall, else isolated LAB keys destroyed.

## Specific next case-GATT breakthrough gate
A credible trace or source implementation must show **A3530 case separate BLE identity** (not 0x3020 earbud RPA), **owner enrollment credentials/authentication handshake for Galaxy-only owner**, **Find My case-specific UUID/characteristic**, **start+stop command with valid response**, and **user/heard/recorded real case beep**. Distinguish BLE link encrypted ≠ owner authenticated ≠ speaker audible. Never blindly send other-device commands or manufacture auth success. Use preexisting CI `ci/case_protocol_v119/extract_att_hci.py`, `ci/case_protocol_v123/case_session_evidence.py` for sourced offline traces when present.

**WORK STATUS: model/battery code changed on LAB branch, synthetic/build tests separate from real user physical confirmation. A3530 owner-auth and sound commands remain UNSOLVED; no truthful successful ringtone APK.**

## CI result transparency (preliminary)
- Initial branch action 38733312258 compiled signed LAB but exposed a duplicate 0x3020 mapping from the frozen source; its APK r3 is superseded and must NOT be recommended.
- Fix commit 10b3132105dc63edaa25b7259a94957771760c5b corrects original 0x3020 label **in-place**, no duplicate. Replacement action 38734032043 is the authoritative candidate; ensure its completed success and signature/hash before offering any APK.
