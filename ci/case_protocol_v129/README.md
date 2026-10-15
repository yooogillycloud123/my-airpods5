# AirPods 5 A3530 — charging sound remote playback evidence gate, v129

## User acceptance: still NOT working
Galaxy Android, AirPods5 A3530 standalone case with two earbuds docked, lid closed, no charger, no Apple device, no root: physical CASE speaker audible beep from Android button. This acceptance condition remains unimplemented.

## Exact primary source discoveries
Apple A3530 case speaker: https://support.apple.com/ko-kr/109525
User physical experiment A: USB-C removed/reinserted 2-3s later yields charging ding.
User physical experiment B: leave USB-C in case; remove/restore outlet power; only after red case LED fades all the way dark does renewed mains power yield new ding. This is real POWER, not a Bluetooth command.
CAPod upstream DefaultAapDeviceProfile.kt (lines 289-294) explicitly says AACP 0x31 IN_CASE_TONE effect unknown, real charging case tone control is via ATT rather than that command: https://github.com/d4rken-org/capod/blob/main/app/src/main/java/eu/darken/capod/pods/core/apple/aap/protocol/DefaultAapDeviceProfile.kt
LibrePods docs/AAP Definitions.md lists 12 3A 00 01 00 08 00 (enable) and 01 (disable) for future charging sounds, tested ONLY Pro2, not AirPods5 and NOT PLAY NOW: https://github.com/librepods-org/librepods/blob/main/docs/AAP%20Definitions.md
More decisively LibrePods maintainer #102 says original raw commands sent to earbuds do nothing, original bytes are not the full GATT characteristic payload: must write to separate CASE GATT server and authenticate as owner: https://github.com/librepods-org/librepods/issues/102
LibrePods TODO #20 still marks authenticating with CASE and Find My case speaker unimplemented: https://github.com/librepods-org/librepods/issues/20
AirGuard actual Galaxy hardware shows only Safe Tracker and Locate Tracker, no Play Sound. Third party Find My accessories owner CP 4F860002 vs nonowner 4F860003 do not prove A3530 command: https://github.com/parawanderer/OpenTagViewer/issues/201
Stuffz Pro3 separate case BLE radio encrypted charging flag is telemetry CASE-to-phone; NOT a phone-to-case charging event trigger and not verified AirPods5: https://github.com/stuffz/airpods/blob/main/docs/ble-research.md

## Actual v129 deliverable
Read-only offline source evidence classifier distinguishes physical USB ding, charge chime enable/disable, unknown AACP in-case tone, third party Find My control. All reject incorrect claim that a source packet currently causes the unplugged A3530 case to ring. No BLE command, no pairing, no account material. 11 synthetic unit tests; CI and source ZIP.
No new ineffective Android APK is published; user's personal v1.0.10 partially tested baseline and Android Auto Golden and pairing are unchanged.

## What would unblock real physical ring
Actual A3530-only verified CASE connection/identity, case owner enrollment/authentication, true speaker PLAY+STOP opcode from measured independent source, implemented in Android app and a real audible CASE confirmation. GATT ACK or charger-setting toggle is not enough.