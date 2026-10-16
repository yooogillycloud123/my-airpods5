# My AirPods5 v1.0.39 ONE-ACTIVE-LAB replacement (2026-10-16)

## User directive and latest physical evidence
- User reports one personal app plus four increasingly proliferated LAB clones, demands **NO fifth LAB** and a real, testable correction with exact cleanup instructions.
- v138 actual scan: 335 BLE callbacks / 34 Apple; 07:19=0, FindMy nearby 12:02=11, separated 12:19 connectable=0, Nearby10=22. v136 once got 12:19=1 and 8 GATT services / FD44 non-owner true, never proven to be user's AirPods case.
- v138 "other 12-type=11" duplicated nearby 12:02 because generic12++ was inclusive; real code defect.
- Every full LibrePods LAB package has AirPodsService, whose onCreate registers a background BLE scan. Some might still be running; no actual scan-competition root-cause proof. v138 also starts a one-shot scanner.
- Case Find My A3530 closed-case beep remains **NOT IMPLEMENTED** (device identity, auth and speaker command unverified). No automatic GATT writes or tag sound without authenticated owned device.

## Exactly five installed app roles before replacement
1. **Keep** personal `com.myairpods5.personal`, partially verified v1.0.10. Never uninstall, reset, change Bluetooth bond, or edit AA Golden.
2. **Preserve data for now** v1.0.34 `com.myairpods5.personal.lab`, may have LAB-local IRK/ENC_KEY; force-stop when not in use; don't delete until data preservation consciously decided.
3. **Obsolete LAB** v1.0.36 `com.myairpods5.personal.caseprobe`; user verified candidate chooser + FD44 read; can **force-stop** to avoid background scanner; optional uninstall later ONLY after user confirms data not needed.
4. **Obsolete LAB** v1.0.37 `com.myairpods5.personal.identityprobe`; on-demand read-only identity probe; can **force-stop**; optional removal later with warning app data lost.
5. **Active one test package** v1.0.38 `com.myairpods5.personal.radiolab`; v139 uses **same package** and launcher identity — it is not a sixth app. But v138 and v139 *GitHub-generated debug signing certificates differ*, so standard update-install is **NOT possible**. To replace, user must uninstall **only v1.0.38** and install v1.0.39. This clears v138-local data if any; it does not touch the personal app / old LAB packages / paired AirPods. A stable private signer is required before claiming future update-install.

## Actual v139 changes and build
- Branch `airpods-139-singlelab-replace-v138-20261016`, commit `14e078f6163e6bd6cf8a67a10c3c2a6c05ea5667`.
- Source additions `ci/case_protocol_v139/patch_lab_single_scanner.py` and `CaseLabInventoryV139.kt`; same v138 CaseRadioEvidenceV138 and original frozen audio/proximity engine.
- Cloned **v139 own** `AirPodsService.kt` no longer calls `bleManager.startScanning()` on app startup. One user-started temporary 12-second BLE scanner only; other installed LAB scanners are not remotely stopped by app due Android sandbox.
- Radio counts disjoint: FindMy nearby 12:02 vs separated 12:19 connectable/nonconnectable vs OTHER 12, plus in-memory unique advertiser address count per type and same-ephemeral-address overlap; no raw address displayed/saved.
- First modal **실험앱 관리** button shows named inventory plus Android system app-info links. Doesn't auto-force-stop/uninstall anything or clear data. Chooser `.setItems()` still has no conflicting `.setMessage()`.
- Same `applicationId=com.myairpods5.personal.radiolab` as v138, versionCode 10039, versionName `1.0.39-single-scanner-lab-replaces-v138-debug`.
- Signed GitHub Actions `38904151411` SUCCESS, release `my-airpods5-v139-one-lab-replace-r11`, APK `My_AirPods5_v1.0.39_ONE_LAB_SINGLE_SCANNER_REPLACE_V138.apk`, **51,490,701 bytes**, SHA256 `380a26787fc0217ee0068dc24653ffb04731279f5e505e78b0d24deb683ca055`, signer SHA256 `6d73e4b2c659d6b3e5f38d83fe4fc6eed9e268254841c3664b14c3363a03ba39`.
- Build `BUILD SUCCESSFUL in 3m45s`, source contract PASS, raw source hashes PASS, ZIP test PASS, Android APK v2 signature PASS. Downloaded official artifact and independently extracted and hashed matching expected SHA; APK internal ZIP + all classes DEX container integrity PASS. **Phone v139 verification pending**.
- Public release: https://github.com/yooogillycloud123/my-airpods5/releases/tag/my-airpods5-v139-one-lab-replace-r11

## One useful user test only / exact non-destructive steps
1. In Android Settings > Apps, **force-stop ONLY older LABs** `com.myairpods5.personal.lab` v134, `.caseprobe` v136 and `.identityprobe` v137. **Do NOT force-stop/delete personal normal v1.0.10 or AA Golden.** "Force stop" preserves LAB data and bonding. The user can leave LAB icons; no unwanted side-effects claimed.
2. To avoid a sixth app: one-time uninstall **only** installed `com.myairpods5.personal.radiolab` v1.0.38 (launcher `AirPods 신호 확인 · LAB`), accept that its **own** app data would be removed. Direct over-install v139 fails due signing mismatch. Install v139 APK (same package/launcher). Thus app count stays same, not +1.
3. v139 app > Settings > `AirPods / Find My 진단 v139 · LAB`. Tap `실험앱 관리` to see roles if useful; tap `한 번 확인` to perform **one** 12-second scan and send screenshot; no need to tap unknown 12:19 GATT candidates, no repeat 35s/RSSI.
4. Report separate 12:02 distinct addresses, 12:19 total connectable/nonconnectable and 07:19. If 12:19 still 0, do NOT invent failure; the packet was not observed. If 12:02 address overlap exists, it's only same temporary BLE address observed in one session, **not** owner/case identity or beep support.
5. After successful v139 trial, obsolete v136/v137 may be uninstalled **only at user's discretion** with corresponding data-loss warning; v134 is intentionally preserved due possible keys. Current "one test app" goal is one ACTIVE experiment rather than unsafe automatic uninstalls.

## Important unresolved issue
- Current v139 debug signer is again ephemeral. Future same-package APKs signed on fresh GitHub runner cannot be installed as updates without either original signer/private key or prior one-time migration to a **secure persistent signer**. Do not misrepresent replacement as seamless update. Implement persistent protected signing before next version; no private signer in public repo.
