# AirPods 5 CASE authentication R&D — v1.0.17 source implementation checkpoint (2026-10-15)

## User-visible outcome
- USER ACTION NOW: NONE. No Bluetooth scan, no pairing reset, no APK install.
- v1.0.10 GOLDEN personal ANC/battery app unchanged.
- User's goal NOT reduced: two buds IN case, lid CLOSED, A2DP DISCONNECTED, Galaxy sends command to CASE speaker and it actually beeps.
- Physical case beep: NOT IMPLEMENTED, not claimed.

## Newly implemented / validated (code, not hypothetical)
- Source branch `airpods-personal-v111-updater-diagnostics`, `ci/case_protocol_v117/CaseRpaIdentity.kt` and `CaseRpaIdentityTest.kt`.
- Implements the exact Bluetooth LE resolvable-private-address (RPA) resolution algorithm upstream LibrePods uses (Bluetooth AES-128 `ah`, byte order accounted for). This is a **real reusable device-identity prerequisite**. No BLE traffic or GATT writes are made.
- Separates key provenance: `EARBUD_AACP_PROXIMITY` vs `CASE_OWNER_HANDSHAKE_VERIFIED`. It does **NOT** equate the earbud AACP `IRK` / `ENC_KEY` to an authenticated identity of the charging case; even an RPA match never grants authority to send a sound command.
- Positive/negative tests: known IRK test vector independently verified with Python cryptography AES-ECB; mismatch IRK, mutated address, wrong address kind, null key, invalid MAC, role gate. **8 offline checks PASS** (not user physical PASS).
- GitHub CI `.github/workflows/case-protocol-v117-offline.yml`, run `38683459680` SUCCESS. Device required: NO.

## 1st-party original implementations audited
1. LibrePods `AACPManager.kt` uses `PROXIMITY_KEYS_REQ (0x30)`, `PROXIMITY_KEYS_RSP (0x31)` and `ProximityKeyType.IRK (0x01)` / `ENC_KEY (0x04)`. BLEManager loads IRK from private app settings and calls `BluetoothCryptography.verifyRPA(address, irk)`. This is implemented for AirPods proximity/earbuds; **NO evidence those keys grant independent case Find My authorization**.
   - https://github.com/librepods-org/librepods/blob/main/android/app/src/main/java/me/kavishdevar/librepods/bluetooth/AACPManager.kt
   - https://github.com/librepods-org/librepods/blob/main/android/app/src/main/java/me/kavishdevar/librepods/bluetooth/BLEManager.kt
   - https://github.com/librepods-org/librepods/blob/main/android/app/src/main/java/me/kavishdevar/librepods/utils/BluetoothCryptography.kt
2. LibrePods author in #20/#102: the **case** exposes a separate Find My GATT server and owner authentication is unsolved; `Play sound through case` remains unchecked. The earbud AACP chime toggle `IN_CASE_TONE_CONFIG 0x31` is not find-case sound.
   - https://github.com/librepods-org/librepods/issues/20
   - https://github.com/librepods-org/librepods/issues/102
3. AirGuard AppleFindMy `4F860003` / DULT sound is NON-OWNER separated tracker mode, not verified owner nearby case sound.
   - https://github.com/seemoo-lab/AirGuard/blob/main/app/src/main/java/de/seemoo/at_tracking_detection/database/models/device/types/AppleFindMy.kt
4. OpenTagViewer owner `4F860002` BLE encrypted link is proven for third-party tracker, not AirPods5. It is not a drop-in case command and supplying `ENC_KEY` is NOT a replacement for owner bond LTK.
   - https://github.com/parawanderer/OpenTagViewer/blob/main/docs/owner-connected-tags.md

## Exact next implementation (source first, no user labor)
- **Case owner pairing / GATT auth** is the next priority, not scanning all 0x12 advertisements.
- Locate verifiable case-only owner authentication handshake in upstream source, Apple Mac Bluetooth logs/public annotated traces, or peer-reviewed reverse engineering. Classify which data is AirPods-case-specific; never substitute generic third-party Find My accessory opcodes.
- When authenticated case credential/IRK provenance exists, integrate this RPA resolver in an isolated experimental source branch with **only explicitly verified owner case** permitted. Carry key in memory/app Keystore; no MAC or key to GitHub.
- Then add command start/stop transport with explicit user consent + timeout + verified full response.
- Only then ONE planned physical closed-case/no-A2DP beep test. Source CI PASS is not device success.

## Negative result retained
v1.0.16 12:02 size4 near 4 (-95dBm) / far 46 (-94dBm), owner-case identity NOT proven. Repeated BLE count experiments are not useful. All user's old evidence and Golden are preserved.
