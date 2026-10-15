# MY AIRPODS 5 v1.0.24 — ACTUAL INSTALLABLE CASE ADVERT LAB / BUILD CHECKPOINT (2026-10-15 KST)

## Verified engineering result, NOT device/sound success
- New **actual signed Android APK** built from original preserved v122 LAB source + separate read-only v124 case broadcast module.
- GitHub branch `airpods-personal-v124-case-advert-test-20261015`, CI head `9613bf941816c75759691e6bee2faca2861dda8c`.
- GitHub Actions **38719047092**, job **116368763969**, **SUCCESS**. Android Gradle `BUILD SUCCESSFUL in 2m 51s`. All v122 prerequisite patch steps + 12 new offline tests (5 HCI provenance + 7 broadcast format) PASS.
- APK release: https://github.com/yooogillycloud123/my-airpods5/releases/tag/my-airpods5-v124-case-advert-r1
- Install APK asset: https://github.com/yooogillycloud123/my-airpods5/releases/download/my-airpods5-v124-case-advert-r1/My_AirPods5_v1.0.24_CASE_INDEPENDENT_RADIO_LAB.apk
- APK exact SHA-256: `b23f0deaf7cd32a46a0a2668c3d520e950417d908a382455d7aac9b471d2ed14`.
- APK size: **51,425,113** bytes. Package: `com.myairpods5.personal.lab`; versionCode **10024**, versionName `1.0.24-independent-case-advert-lab-debug`.
- APK signing cert SHA256: `452370d8feda95849415c8a2dc9ebafd6240d14228d512de14c006fa504871a5`. **APK Signature Scheme v2 true**; ZIP integrity PASS. LAB v122 signer differs, so direct app upgrade is **NOT** signature compatible, no claim of seamless update. Do not remove baseline GOLDEN in response to signature mismatch.
- v1.0.10 partially-verified rollback baseline package `com.myairpods5.personal` and its original signer are unchanged; AA Android Auto Golden/daemon unchanged.

## What the installable new LAB APK actually adds
- In existing LAB Settings, one research entry **`내 케이스 찾기 연구 v1.0.24`** opens `CaseResearchMenuV124.kt`, preserving optional old `CaseOwnerProbeV120.open(a)` read-only path and integrating new user-triggered **10-second** passive source-based BLE investigation of a separate case-advertiser format.
- Match manufacturer data ID 0x004c, `07 11 06` and exactly 16 bytes of encrypted block. Decode only when *this LAB package's own* 16-byte stored AACP `ENC_KEY` exists; attempt no access to GOLDEN's separate private credentials. If no key, report shape-only candidate count rather than fake percent.
- 3-byte format check and byte decoding derived from actual **AirPods Pro 3** code `stuffz/airpods/src/aap/case_advert.hpp`; AirPods **5 case signal format is not confirmed**. No raw MACs/keys/signal bytes output; percentages reported only for decrypted format-matched candidate, **not** authenticated case identity. All-null battery is not 0%.
- No GATT writes, bond changes, audio playback, case speaker start/stop opcode, or invented success. Bluetooth scanning only after explicit user action and permissions; no automatic or repeated 35-second scans.
- **User's primary functional goal remains NOT IMPLEMENTED**: closed-lid AirPods5 case with two earbuds stored, A2DP off, Galaxy button makes physical CASE SPEAKER beep. LibrePods maintainer #20/#102 owner-case Find My GATT authentication and actual speaker sound command still unknown. Android compilation alone is not a ring test or completed app.
- This new APK is an actual narrow **transport diagnostic build**, NOT a case-sound test APK. Users need not install it if they want only audible beep, until auth/protocol is verified.

## Installation gate, because avoiding data loss matters
- The APK is a **separate LAB package** so user's v1.0.10 baseline can remain installed.
- Older LAB v120/v121/v122 were signed with unique ephemeral debug certs. **Do not uninstall v1.0.10 GOLDEN**. A device with old `com.myairpods5.personal.lab` probably cannot directly install v124 (Android signer mismatch). To run v124, user would have to remove **only LAB**, which loses LAB-only IRK/ENC_KEY data. This is precisely the known incomplete stable-OTA/signing problem; do not conceal it, nor instruct uninstall without warning/approval. New v124 cannot magically read GOLDEN private AACP data. Unless signer is preserved securely, subsequent LAB update-install cannot be guaranteed.
- Test value remains gated on matching observed AirPods5 case format and local ENC_KEY; the user's previous `07` scan returned 0. Avoid repeating unless user specifically accepts the narrow diagnostic question. Do not tell them the case will beep.

## Sources / verification
- Exact source: `ci/case_protocol_v124/CaseResearchMenuV124.kt`, `ci/case_protocol_v124/patch_lab_independent_case.py`.
- Exact build: `.github/workflows/v124-case-advert-lab.yml` on v124 branch.
- Prior v123 session+format synthetic CI **38718067357** PASS; new Android v124 run **38719047092** PASS.
- New Flutter/Android Auto helpers: NONE. Core service, original stable v1.0.10 package and AA app untouched.
- Next work must resolve AirPods5 **case owner-authenticated GATT** and *actual audible Find My case beep opcode*, plus stable signer in protected CI secret and one-app integration. No further blind advertising loops.

**FINAL STATUS: signed test APK available; physical case beep not achieved, not simulated, not claimed.**
