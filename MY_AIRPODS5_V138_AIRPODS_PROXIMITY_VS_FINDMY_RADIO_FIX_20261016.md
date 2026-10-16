# 2026-10-16 v1.0.38 physical-candidate regression repair — shipped CI APK, user test pending

## Current user observation (do not discard)
- v136 actual physical test: Apple 12:19 connectable candidate 1, RSSI -85 dBm, GATT FD44 / non-owner 4F860003 TRUE, DULT FALSE, 8 services. Was NOT proven to be the user's AirPods5 case.
- After v137 installed, both v136 and v137 reported 0 candidates. v137 screenshot showed 396 total BLE callbacks and 12:19 candidate 0. Scanner was receiving radio events; disappearance of this specific packet cannot be attributed to v137's GATT read modification alone.
- User insists there were two AirPods with them when previously 1 candidate was shown. Preserve as test-context evidence; without device-bound identifier, that co-occurrence is NOT identity proof.

## Root cause in actual code, not speculation
- Original v134-137 `CasePathV134.kt` scanner increments total then ONLY accepts Apple manufacturer `0x12 0x19` with `result.isConnectable`. That is a separated Find My accessory format, not AirPods5's ordinary Apple Continuity proximity type `07 19`. Labels that said "AirPods candidate" were unjustified. So AirPods5 advertisements could be plentiful while app reported zero.
- Upstream LibrePods `Bluetooth/BLEManager.kt` scans manufacturer first bytes `07 19`, model fields at bytes 3 and 4. Our source-backed v126 CAPod map classifies `0x3020` as AirPods5 wireless-case variant and `0x3620` as general AirPods5. **Model label ≠ identifying the closed charging case or user ownership**.
- Lost 12:19 is not necessarily a code regression: both apps used same filter and separated advertising may be transient or from unrelated nearby accessory. Cannot guarantee radio emits an absent packet.

## Actual v138 code and frozen scope
- Branch `airpods-138-dual-airpods-findmy-observation-20261016` head `122e7641305e3eb17dfa648b7cf0c4174c9d4ed3`.
- New source `ci/case_protocol_v138/CaseRadioEvidenceV138.kt`; patch `ci/case_protocol_v138/patch_case_radio_v138.py`. Existing v137 case identity source retained for user-confirmed 12:19 only; original AirPods AACP/ANC/battery and AA Golden unaffected.
- One manual unfiltered Apple manufacturer BLE collection, 12 seconds, no automatic retry or BLE toggles. Separately count all BLE callbacks, Apple, AirPods proximity `07:19`, strict model-tag `3020/3620` (length >=27, flag byte2=1), FindMy separated `12:19` *and connectable*, FindMy nearby `12:02`, case-research Pro3 `07:11:06`, other Apple. Never equate 12:19 with user's case.
- If explicitly known OWN 12:19 candidate found, user may optionally tap row (list/dialog collision fixed) and consent to one READ-ONLY GATT identity check. Do NOT select unknown devices. No characteristic/descriptor writes, bond changes, sound opcode, hidden MAC or serial in report.
- Existing `com.myairpods5.personal` partially verified v1.0.10, older `.lab` v134, `.caseprobe` v136, `.identityprobe` v137, and Android Auto components untouched. New temporary parallel package `com.myairpods5.personal.radiolab` because prior per-CI debug signers do not support update-install. Future stable signing needed, never publish private signing material.

## Build/release verifications
- GitHub Actions run `38900664575` SUCCESS, APK built in 3m37s.
- Release `my-airpods5-v138-split-radio-evidence-r9`.
- `My_AirPods5_v1.0.38_AIRPODS5_FINDMY_SPLIT_EVIDENCE_LAB.apk` bytes `51474305`, SHA256 `c2776b45ade28162206703cca6ea2ea2185a916ddb3bd95e931304e4fd4dd2a6`.
- Package `com.myairpods5.personal.radiolab`, versionCode 10038, vName `1.0.38-airpods-findmy-split-evidence-debug`, signer cert SHA256 `5fb1ae433e2c921ac129920c59fd5a1e83a306c83c4c0008e331f9b4d0b81379`, APK v2 signature PASS. Source gates V138_* PASS; immutable engine hashes PASS; downloaded Actions ZIP / APK ZIP both PASS; downloaded APK hash matches release. CI success != physical scan success.

## Meaningful next physical test, once only
- Install new `AirPods 신호 확인 · LAB` in parallel without removing any existing apps or unpairing. Settings -> `AirPods 5 / Find My 신호 분리 · LAB` -> `한 번 확인`, wait ~12 seconds, share resulting summary screenshot, not identifiers.
- Interpret separately `AirPods 주변 광고 07:19`, `0x3020/0x3620`, `12:19` and `12:02`. 07:19=0 does not prove absence; 12:19=0 does not prove AirPods5 incompatibility.
- No need to tap 12:19 unknown candidate: first result is enough. Avoid repeated scans/35sec tests.
- Ultimate goal remains actual audible FindMy sound from two buds inside CLOSED AirPods5 wireless charging case with Galaxy only, music Bluetooth disconnected. Authentic case identity/bonding/owner authentication/speaker play/stop still UNIMPLEMENTED/UNVERIFIED.
