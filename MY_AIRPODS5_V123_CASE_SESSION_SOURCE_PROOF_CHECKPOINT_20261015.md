# MY AIRPODS5 v1.0.23 — CASE SESSION SOURCE-PROOF CHECKPOINT (2026-10-15 19:05+ KST)

## Status / explicit limits
- **No release APK, NO AirPods 5 case speaker Find My beep.** No case owner-auth protocol/credential or model-verified start/stop command was discovered; not an Android hardware PASS.
- The user-proven v1.0.10 is a **PARTIAL TEST BASELINE, NOT FINISHED/FINAL**. Keep original package `com.myairpods5.personal`, release `my-airpods5-test-r21`, signer SHA256 `f063dddd4b2621e89fde407cc2e66c6ce8a2ef69b892b54861545e6c166c77d0`, and APK SHA256 `b46c8e38f28332f7af0f98bce7b8b27234f62a83d103363cf0697a2fd127445b` untouched.
- Previous LAB v1.0.22 `com.myairpods5.personal.lab` (workflow 38714212956, release `my-airpods5-v122-lab-r1`) remains latest *installable lab*, but fixes to missing-value UI and AACP conditional key re-request are **not physically verified**. Independent app UID/storage, AACP socket absent, and per-build debug signing remain separate unresolved update/IRK constraints.
- Android Auto Golden/daemon/app unchanged. No BLE scans, extra installs, pairing resets, case writes, private keys, or other on-device interactions this stage.

## Specific newly implemented source: v123 offline transport provenance
- Branch: `airpods-case-session-proof-v123-20261015` based on the **latest v122 LAB code branch**, intentionally **not merged to main**, not installed.
- Source: `ci/case_protocol_v123/case_session_evidence.py` and `ci/case_protocol_v123/test_case_session_evidence.py`.
- Technical fix: existing v119 HCI ATT extractor associated discovery UUIDs with **reusable 12-bit HCI connection handles**; comparing an old GATT UUID after reconnect to a new peripheral could generate bogus characteristic/protocol conclusions. Also an encryption-change on an old connection must not imply the new one is secure. New offline audit:
  - resets parsed GATT UUID and pending fragments on successful HCI disconnection and LE connection-complete;
  - tracks per-handle **session epochs** and encryption-change status for each write;
  - suppresses unmapped UUIDs from cross-capture candidate differences;
  - distinguishes HCI encrypted transport from **case owner authentication**, and ATT writes from **audible speaker beep**;
  - strips MAC, key, raw ATT bytes and never issues Bluetooth commands.
- Workflow: `.github/workflows/case-session-proof-v123.yml`; GitHub Actions **38717404840 SUCCESS**, job **116363341045 SUCCESS**, 5 synthetic no-device tests + legacy v119 parser regression checks. Branch commit (CI head): `f14a61e7467af30fa6ce881f6ce85755406bf683`.
- Result is a reproducible forensic *false-positive guard*, not implementation of Find My case owner-auth or sound playback. Do NOT promote this code to a working find-case APK.

## New public primary-source lead (different hardware, not AirPods5 success)
- `stuffz/airpods` September 2026 real BLE research on **AirPods Pro 3**: `docs/ble-research.md` records a distinct case advertisement format `07 11 06` + encrypted 16-byte block, independently rotating case address that **does not resolve with earbud proximity IRK**, with occasional host HCI loss despite sniffed BLE emissions. Their `README.md` explicitly limits tested model/firmware.
- Source: https://github.com/stuffz/airpods/blob/main/docs/ble-research.md
- Implication for existing LAB v120: exclusive `CaseRpaIdentity.assess(... EARBUD_AACP_PROXIMITY)` scan may systematically **miss an independent case GATT device**, even if the earbud IRK is valid. This is a research warning, NOT permission to attribute unknown advertisements to the user's AirPods 5. The user's v116 `07` not observed and `12:02` near/far inconclusive results remain intact; do not repeat 35-sec scans.
- LibrePods maintainer #20/#102 separately states **CASE** GATT Find My owner auth unsolved. AirGuard `4F860003` non-owner separated tracker sound, OpenTagViewer owner accessory encryption examples, LinuxPods/FindMy.py do not provide a demonstrated Galaxy-only AirPods5 closed-case beep. Nothing found to justify a guessed GATT write.
- New research must locate a **device/fingerprint-specific case BLE identity**, **working case owner registration/handshake**, and **real case sound opcode and confirmation trace**. An AACP earbud IRK is not this owner credential.

## Gate before an actual user APK
1. Independent case channel and AirPods5 model-specific signature supported by real code/capture.
2. Owner-auth procedure with proven ownership state, including users without prior Apple-device Find My enrollment.
3. Authenticated GATT/L2CAP write and case speaker play/stop response shown by real device, **not just ACK/encryption**.
4. Source-only/CI tests; reproducible stable signing via protected repo secrets, not an exposed key. Do not delete v1.0.10 for signer mismatch.
5. Only then one strictly necessary closed-lid/buds-in/A2DP-disconnected physical audible-beep test.

**Next checkpoint must not claim 'case found/playing' unless actual case-speaker sound is user confirmed.**

## v123 SECOND SOURCE IMPLEMENTATION — independent-case BLE encrypted envelope (offline only)

A newly inspected public **AirPods Pro 3** primary implementation `stuffz/airpods/src/aap/case_advert.hpp` (blob SHA `19c57b85c0ca181d2156e1f564d386ebbe8a2e0d`) and `src/aap/encrypted_payload.hpp` (SHA `9d00ff154e44a9e70af0f6a950409ad5959aa9d2`) reveal an overlooked independent case path:
- `Apple 0x004C` manufacturer-data exactly `07 11 06 + 16-byte AES-ECB-encrypted block` in their AirPods **Pro 3**, even while lid shut.
- The case advertiser BLE address does **not** resolve against AACP bud IRK, but the independent case block was decrypted using bud AACP **ENC_KEY**. The two keys are different. This invalidates the **assumption** that an RPA match is required for any independent case discovery; it does **not** contradict the user AirPods5 v116 `07=0` logs or prove their model uses Pro3 protocol.
- The project validates only three zero slots of decrypted block: bytes 8/10/11. This **cannot cryptographically authenticate ownership** and should be treated as format-compatible *candidate*, especially for untested AirPods5. Header/firmware variant/counter must not be hardcoded to one captured value.

Implemented isolated `ci/case_protocol_v123/case_broadcast_evidence.py`: passive bytes-only Pro3 case envelope + optional *in-memory* ENC_KEY decryption, lid, battery values/absent/charging decoding. It never scans, pairs, writes GATT, connects hardware, exposes raw keys/addresses, or claims AirPods 5/owner/speaker functionality. Source unit tests use **synthetic encrypted data only**, not a real user's device. Source `ci/case_protocol_v123/test_case_broadcast_evidence.py`.

GitHub Actions **38718067357**, job **116365524807**, SHA `62c8c6406ae5276b3af87ca1ecc8a1c89fdb9096` **SUCCESS**: preexisting v119 tests 6+2, v123 HCI session 5, new Pro3-envelope 7 => **20 synthetic/offline tests PASS**. This confirms source logic only; **AirPods5 case identification, authenticated sound command, case beep: NOT IMPLEMENTED/NOT VERIFIED.**

Next source gate: AirPods 5-specific independent advertiser format confirmation *from open, real-source captures*; no repeated user-device scans, v1.0.10 untouched. If same format truly supported, a future stable single-app module may reuse a safely obtained ENC_KEY (in **same UID**) to recognize potential case adverts, while separately solving the case GATT owner-auth and real Find My sound command.
