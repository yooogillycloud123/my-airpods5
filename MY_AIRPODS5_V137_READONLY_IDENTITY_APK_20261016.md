# My AirPods5 v137 read-only candidate identity profile — installable gate (2026-10-16)

## Baseline and reason
- Actual v136 user evidence: candidate picker and on-demand GATT connect real-device PASS; 12:19 Apple Find My connectable candidate, -85dBm, FD44 non-owner 4F860003 present, DULT absent, total services=8. Device identity and case speaker **UNKNOWN**.
- A 12:19 separated accessory is **not automatically the user's Galaxy-only AirPods 5 charging case**. For an unowned peripheral, even a read-only attempt should not be described as an owner identification; user must cancel if the candidate cannot be recognized as owned. No Find My Play Sound writes.
- Developer source checked: AirGuard `AppleFindMy.kt` manufacturer/service read, OpenTagViewer owner-connected-tags.md non-owner vs owner control distinction; neither proves AirPods5 owner case sound.

## New code / frozen domains
- Branch `airpods-137-readonly-identity-profile-20261016`.
- New Kotlin: `ci/case_protocol_v137/CaseIdentityReadV137.kt`; new patch: `ci/case_protocol_v137/patch_case_identity_v137.py`.
- Base is entire `airpods-136-parallel-picker-preserve-lab-20261016` v136 known source / all original AACP & audio source frozen.
- Existing v136 candidate selection preserved, adds sequential GATT READ (only characteristics advertising PROPERTY_READ), timeout and no retry storm. DIS model 2A24; manufacturer 2A29; generic device name 2A00; documented Find My vendor side/name 6AA50003 if available. No serial 2A25 reads, raw address/values not displayed. Output coarse evidence categories only; neither Apple-ish string nor 4F860002 presence is an authenticated owner proof.
- Continues reporting FD44 non-owner/owner UUID presence separately, DULT, total GATT services. Strict zero `writeCharacteristic`, zero `writeDescriptor`, zero `createBond`, zero sound commands, no connection to random devices without user manual selection.
- User's existing `com.myairpods5.personal` partially verified v1.0.10, `com.myairpods5.personal.lab` v134, `com.myairpods5.personal.caseprobe` v136, and AA Golden all untouched. Debug signer differs per Actions CI run. To avoid uninstall/data loss, v137 installs in new isolated `com.myairpods5.personal.identityprobe`. This is temporary laboratory-only, not final product. Never ask to remove old data/apps to test.

## Actual build / release
- GitHub Actions run `38897400504` build SUCCESS; after two early workflow assertion errors corrected, successful APK build `BUILD SUCCESSFUL in 3m 31s`.
- Release `my-airpods5-v137-identity-readonly-r8`.
- APK `My_AirPods5_v1.0.37_READONLY_IDENTITY_PROFILE_LAB.apk` SHA-256 `19734234c523b58d8f75a6cfd7596793a9560ee76afbdcd187c1ceaec702b12f`, bytes `51457929`.
- Package `com.myairpods5.personal.identityprobe`, versionCode 10037, versionName 1.0.37-readonly-case-identity-debug; signer cert SHA256 `787d66a29fe20031f99cb9f873fea563b48832afa01085e63c22ad007f41b847`.
- GitHub CI source assertions: `V137_READONLY_IDENTITY_CLASSIFIER=PASS`, no writes/pair changes/serial text PASS, core source hashes PASS, `apksigner verify --verbose` v2 true, unzip compressed integrity PASS. Artifact local byte SHA from downloaded Actions ZIP verified matches release, and APK ZIP test PASS. No Android hardware v137 test yet; built != device function pass.

## User physical test (single meaningful round)
1. Install new v137 standalone app; **do not uninstall any** old LAB/personal/AA app; no BT unpair/reset.
2. Use new launcher `케이스 신원 확인 · LAB` -> Settings -> `내 케이스 소리 경로 확인 · LAB`.
3. Search once for 8 seconds, select a peripheral **ONLY** if actually known to be owned; don't infer ownership from RSSI or 12:19 count. If unknown, cancel and report that; never force GATT access to unknown devices.
4. If safe to select, accept read-only identity check. Return final dialog screenshot or its text: manufacturer/model/name/FindMy side category, FD44 owner/non-owner presence, total services. No private addresses or raw data.
5. Result showing AirPods-looking text alone is NOT AirPods5 A3530 case identity, nor case authentication. Actual AirPods5 closed-case beep still **NOT IMPLEMENTED**.

## Remaining engineering blocker
- Source-verifiable owner ↔ specific AirPods5 wireless case BLE transport authentication and actual Find My play/stop command must be proved. Do not add generic FD44 non-owner write or spoof Find My commands against unidentified third-party tags. For future app updates build reproducible stable signer at a protected location; do not publish signing secrets.
