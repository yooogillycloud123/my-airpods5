# My AirPods5 v1.0.36 — 2026-10-16 candidate picker non-destructive handoff

## User's latest physical report
- Installed v1.0.34 `com.myairpods5.personal.lab`: BLE 12:19 scan reported 1 candidate but modal only showed descriptive text and Cancel; no candidate row could be tapped.
- Exact submitted APK `My_AirPods5_v1.0.34_FINDMY_DULT_CASE_PATH_PREFLIGHT_LAB.apk`: SHA256 `606bd01fc523cfa4a1a6c205875208374d817a19a857175cced86c64ba454765`, 51441493 bytes, ZIP PASS; matches release v134 r2.
- Root cause confirmed: Android `AlertDialog.Builder` with `.setMessage(...)` and `.setItems(...)` in same dialog shows the message but hides selectable candidate list.

## Fixed source already in v135
- Branch `airpods-135-fix-candidate-picker-20261016`; commit `fc41c43451fcafb3461c626442f121cee19691df`; run `38891828684` SUCCESS, APK SHA `b22107630bc30747ae1fc75213266fa8d6e25b041fbdd9eacab91564cfeacf08`.
- Source `CasePathV134.kt` now uses `.setTitle(...).setItems(labels)` without `.setMessage` in list dialog. Tap row -> second explicit confirmation -> READ-ONLY GATT services (FD44/DULT) only.
- **v135 cannot update-install v134**: original installed v134 signing certificate SHA256 `282ed5ca9a37220fa3bd48355dcd2e82db2584256659837675df7614a27bb7de`; v135 cert `1bbe82693624735eb7dfeec07a7ada860e4dcb26f9b3b7a4911110c5787e842d`. Never require uninstall of v134 to verify a picker.

## v136 safe parallel candidate, installable without removing v134
- Branch `airpods-136-parallel-picker-preserve-lab-20261016`, commit `71986a39ac246240e5de05ebfa82adc5527aa770`.
- CI `38893990652`: SUCCESS, complete Android build + source gate + ZIP + APK v2 signature. Release `my-airpods5-v136-parallel-picker-r4`.
- APK `My_AirPods5_v1.0.36_SAFE_PARALLEL_CASE_PICKER_LAB.apk`, size 51441545 bytes; SHA256 `1f45f7a05d6e65448b45f99e78890298efc95d8a295b089550367256e52fa9a3`.
- Unique package ID `com.myairpods5.personal.caseprobe`, vCode 10036, vName `1.0.36-candidate-picker-parallel-safe-debug`. Distinct launcher label `케이스 후보 확인 · LAB`. Debug signer SHA256 `0f7b22224316cb221cb769d6a5bfd7c2c63a84208e1d6e53258c3d98b8383916`.
- Existing personal `com.myairpods5.personal` v1.0.10 partially verified baseline and v134 `com.myairpods5.personal.lab` remain untouched, as do all AA routing Golden/daemon components.
- Android source `ci/case_protocol_v136/patch_safe_parallel.py`: only applicationId, vCode, vName, label changed following v135 patch; enforces picker rows and ZERO characteristic writes / no createBond. Main frozen engine hashes checked in CI.
- Artifact downloaded and extracted to local sandbox; ZIP integrity and SHA verified.
- Release URL: https://github.com/yooogillycloud123/my-airpods5/releases/tag/my-airpods5-v136-parallel-picker-r4

## Gate / next meaningful physical check
1. Install ONLY new v136 parallel caseprobe; DO NOT delete original v134 LAB or v1.0.10 or reset BT pairing.
2. In new LAB app Settings -> `내 케이스 소리 경로 확인 · LAB`, manual 8-second scan; when candidate row shown, tap it. Do not connect to candidates that cannot be confirmed as owned. Selection -> separate confirmation -> GATT service enumeration.
3. Report exact final dialog (FD44 / DULT flags, number of GATT services, or GATT status error), no raw address/keys. This is for evidence only.
4. **CASE FIND MY BEEP IS NOT IMPLEMENTED.** Even `FD44=true` or `DULT=true` isn't user-owned AirPods5 A3530 identity / case owner auth / speaker command proof.
5. Do not call a merely built APK fully tested. User physical confirmation pending.
6. A permanent/private GitHub Actions signer is still needed for seamless future LAB update-install without per-build identity churn. Never commit a signer private key to this public repo.
