# AirPods5 v1.0.22 — screenshot-proven LAB battery / IRK regression fix checkpoint (2026-10-15)

## Incident based on user's exact device report
- User screenshot shows AirPods page with a single **0%** battery ring and no L/R/case levels. This is NOT proof the battery itself is at 0%; it is a UI fallback failure.
- Their older LAB run showed **906 / 939 total BLE callbacks with 0 earbud-IRK-matched devices**. This confirms the scan did not prove a matched address or case identity, even when an IRK was available.
- Their replacement LAB (v1.0.21 permission fix) reports **`내 AirPods IRK 미수신` both before and after connecting the AirPods in standard Bluetooth**.
- Evidence-based root cause 1: original `presentation/components/BatteryView.kt` uses `left?.level ?: 0`, `right?.level ?: 0` and `left?.status == right?.status`: when both data entries are null, it displays a misleading 0%. Verified against upstream code.
- Evidence-based root cause 2: `com.myairpods5.personal.lab` uses different Android per-UID storage from user-proven `com.myairpods5.personal`. Each GitHub CI debug build has a **different signing key**, forcing LAB-only uninstall/reinstall and deleting LAB `settings` / IRK. Physical A2DP Bluetooth connection ≠ separate LAB AACP L2CAP socket session; a connected UI/mode button is not evidence of IRK acquisition. Upstream LibrePods actually requests IRK+ENC_KEY on an AACP socket and only persists `IRK` on the received AACP packet callback. Retrieved from `AirPodsService.kt` lines around 1083 and 2747.
- No evidence the golden original normal application's battery has broken; do not uninstall it. Two separate apps attempting concurrent AirPods sockets may cause contention, not proven on user device.

## Actual fix in code
- LAB branch `airpods-personal-v111-updater-diagnostics`, patch `ci/case_protocol_v122/patch_lab_irk_battery.py`.
- When both earbuds have no valid battery entry, UI now explicitly shows **`배터리 데이터 미수신`** instead of fictitious 0%. Case and individual levels also require actual battery entries. Actual received levels are preserved without alteration.
- On IRK missing, distinguishes LAB's **own** AACP socket from ordinary Bluetooth connection. When LAB has its own already-connected AACP session and local service, user can explicitly request original LibrePods `sendRequestProximityKeys(0x05)`; local settings are rechecked for up to 6 s. No automatic GATT, unexpected Bluetooth reconnection, extra BLE scan, case beep writes or key export.
- When LAB has no connected AACP, explains why the separate LAB cannot read the Golden key or battery. Existing Golden usable, and no additional scan required.
- Proven Bluetooth/AACP/service processing core kept hash frozen. Existing read-only GATT consent gate unchanged.

## GitHub CI / installable LAB artifact
- Workflow `.github/workflows/v122-irk-and-battery.yml` run **38714212956**, job 116352807564 **SUCCESS**.
- Android Gradle `BUILD SUCCESSFUL in 3m 34s`; APK ZIP integrity, separate package, app signer, engine hash checks PASS.
- LAB APK `My_AirPods5_v1.0.22_BATTERY_IRK_REPAIR_LAB.apk`, GitHub prerelease `my-airpods5-v122-lab-r1`, 51,408,721 bytes.
- SHA256 `db4916999080b60c2d0efc6c72fee8f967e973e7d041620c8a5bae6ba60cd056`.
- Signer SHA256 `795a5ec33de18d642b890b5e7603fbcb2bc66e8622344d9f83568102a57d893c`.
- Package `com.myairpods5.personal.lab`, versionCode 10022, versionName `1.0.22-lab-irk-and-battery-recovery-debug`.
- Asset: https://github.com/yooogillycloud123/my-airpods5/releases/download/my-airpods5-v122-lab-r1/My_AirPods5_v1.0.22_BATTERY_IRK_REPAIR_LAB.apk
- **Not yet user physically verified**. Fix is truthful missing-value UI and conditional AACP IRK re-request, not proof that LAB can read Golden battery or that it can ring case.
- Existing LAB version and v122 signer differ; installing requires removing **LAB only** and would once again clear LAB IRK. **Do not force a new installation or ask user to run repetitive tests during work.** They may continue using user-verified v1.0.10 for accurate practical battery/ANC. Stop/force-stop LAB app to avoid possible socket contention; don't delete Golden.
- Golden v1.0.10 SHA256 `b46c8e38f28332f7af0f98bce7b8b27234f62a83d103363cf0697a2fd127445b` unchanged.

## Still unsolved
- Galaxy-only **separate closed AirPods5 case Find My speaker**, with buds stored, no A2DP, still not implemented.
- Current LAB's earbud IRK 0 matches do not prove case Bluetooth unreachable; case owner key and authenticated Find My control point unverified.
- Next architecture: stop making multiple full-featured LibrePods clones fight over AACP. Integrate research only with authenticated case control after source proof, and establish secure stable signer before any further user-facing LAB update. Never infer battery from 0 placeholder; never read another app's private storage without an authorized opt-in sharing mechanism.
