# MY AIRPODS 5 — USER-VERIFIED GOLDEN LOCK v1.0.10
Date: 2026-10-15 KST
Scope: LibrePods-derived personal application only, NOT AA Android Auto audio router.
Status: BASELINE LOCKED — user observed stable app + actual listening-mode control.
Previous state: v1.0.3~1.0.9 recurrent launcher crash or inoperative controls.

## User physical evidence, 2026-10-15 08:24 +0900
- Galaxy S26 Ultra (SM-S948N), sdk=37, personal app `com.myairpods5.personal`.
- User explicitly reports: "오케이 청취모드도 작동하고" and screen of app, battery `100%`, mode buttons `끔 / 주변음 허용 / 적응형 / 노이즈 캔슬링`, left/right long-press entries. App stays open.
- User-supplied in-app diagnostics (v1.0.9 recorder retained in v1.0.10) at 08:24:15.628+0900:
  - `NO_JAVA_FATAL_CAPTURED`.
  - Same process `pid=4046` starts `08:21:52.868` and persists until at least `08:24:13.313`.
  - `08:21:52.881 Activity.onCreate`
  - `08:22:04.325 ServiceConnection.onServiceConnected`, `AirPodsViewModel.init COMPLETE`
  - `08:23:58.561 Activity.onDestroy` (normal lifecycle event; NO FATAL).
  - `08:24:13.238 Activity.onCreate`; `08:24:13.313 AirPodsViewModel.init COMPLETE`; no Java crash.
- Confirmed on user's device **app survives, displays battery, and listening mode works**. NOT YET physical PASS: independently accurate L/R/case battery, one-ear+open/close regression, quick tile/widget, adaptive strength, all original premium functions, OTA signer.

## IMMUTABLE BASE APK — NO TOUCH / NO OVERWRITE
- GitHub: `yooogillycloud123/my-airpods5`
- Stable release to PRESERVE: `my-airpods5-test-r21`
- Download: `https://github.com/yooogillycloud123/my-airpods5/releases/download/my-airpods5-test-r21/My_AirPods5_v1.0.10_BADTOKEN_VIDEO_URI_FIX.apk`
- Original frozen build commit (workflow/source patch at time of build): `629f45dcc7b4189b713876660ddc2e18cf13f38a`
- Workflow run: `38652773288` / job `116155300268` SUCCESS.
- APK filename: `My_AirPods5_v1.0.10_BADTOKEN_VIDEO_URI_FIX.apk`
- APK SHA-256: `b46c8e38f28332f7af0f98bce7b8b27234f62a83d103363cf0697a2fd127445b`
- APK signing certificate SHA-256: `f063dddd4b2621e89fde407cc2e66c6ce8a2ef69b892b54861545e6c166c77d0`
- Package: `com.myairpods5.personal` versionCode `10010`
- Size: `51,310,230` bytes.
- CI PASS: DEX crash recorder, VideoView old URI removal & dynamic resource authority, crash-safe playback error handlers, manifest/package/signature/unzip.
- Root cause of prior crash, **physically confirmed resolved for this observed path**: `WindowManager.BadTokenException` from `VideoView$5.onError` after hard-coded `android.resource://me.kavishdevar.librepods/` mismatch. Fixed to `context.packageName` and non-dialog onError in `PopupWindow.kt` + `IslandWindow.kt`.

## CHANGE/FUTURE WORK PROTECTION
1. Never modify/re-release this r21 asset or confuse it with newer experimental APKs. Treat it as rollback GOLDEN for personal AirPods, maintain real download and SHA, and do not modify user's installed copy for planning-only requests.
2. Work on a separately versioned test candidate; compare results against r21; only promote candidate when same device physically verifies app launch + real battery + real listening mode; fail → return to this r21 APK.
3. Changes to Bluetooth, AACP, ATT, service, lifecycle, packet, battery, popup require diff and regression evidence, no speculation.
4. UI strings, battery component labeling, notification, widget, quick tile, one-ear logic, premium source capabilities, permanent signing/OTA are planned assessment **not** all physically proven.
5. Existing debug signer is per-CI-run ephemeral. APK rebuilds cannot silently update signed r21. Never claim seamless update until a stable private signing key is configured in a secure secret store; never upload key to public repository.
6. Do not uninstall/remove verified original LibrePods, change BT pairing, or modify AA/Golden Android Auto daemon/app.
7. Device logs are personal; diagnostics saved on device only; do not auto-upload.

## Verification truth table
- Installation/launch and no new fatal: USER-PASS (2026-10-15).
- Initial battery UI: USER-SCREEN-PASS (100% displayed; accuracy and all three components unverified).
- Listening mode switching: USER-PASS.
- One-ear/case changes: NOT YET TESTED ON THIS GOLDEN.
- All premium capabilities/ANC secondary settings: SOURCE EXISTS, NOT ALL TESTED.
- Quick settings tile and widgets: SOURCE EXISTS, NOT USER TESTED.
- Stable signature upgrades: NOT IMPLEMENTED.
