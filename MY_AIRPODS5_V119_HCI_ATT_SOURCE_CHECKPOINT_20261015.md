# AirPods 5 case sound — v119 source extraction checkpoint (2026-10-15)

TARGET unchanged: Galaxy S26 Ultra, owner AirPods 5 wireless case A3530, both buds inside, lid closed, A2DP disconnected, **actual audible case ring**. Still NOT IMPLEMENTED or physically verified.

## Work finished
Branch `airpods-personal-v111-updater-diagnostics` now contains `ci/case_protocol_v119/extract_att_hci.py`. It is a real offline Android btsnoop HCI H4 -> ACL -> L2CAP -> ATT decoder: GATT service and characteristic discovery, ATT write/read/notify and HCI encryption-change status. It maps **observed** handle to UUID where the discovery exchange appears. It never connects to or sends commands to any Bluetooth device.
Synthetic offline tests in `ci/case_protocol_v119/test_extract_att_hci.py` and `test_uuid_map.py`. GitHub CI `38692379215` SUCCESS. Default output excludes MACs, secret keys, and raw ATT values. Optional explicit private values are for locally held research only; never publish or upload raw traces.

## Factual boundaries
- Apple A3530 case speaker physically exists: https://support.apple.com/ko-kr/109525.
- LibrePods maintainers case GATT owner authentication and play-sound still TODO: https://github.com/librepods-org/librepods/issues/20 and https://github.com/librepods-org/librepods/issues/102.
- CAPod observed ATT case setting semantics and Pro3-specific CASE_INFO requests. Does NOT document AirPods 5 Find My sound.
- AirGuard nonowner separated-tracker sound and OpenTagViewer encrypted third-party Find My owner command cannot be treated as AirPods5 case protocols.
- Source format verified against AOSP btsnoop and Bluetooth SIG ATT specification.
- v116 12:02 near 4/-95 dBm vs far 46/-94 dBm disproves earlier guessed owner-case attribution.

## Next gates
(1) Credible AirPods 5 charging-case-specific GATT endpoint and identity; (2) owner authentication handshake; (3) actual case-speaker sound request and response with acoustic proof; (4) guarded LAB Android integration; (5) one final physical test. Do not fabricate answers or issue writes to nearby unrelated devices.

USER ACTION: NONE. No repeated BLE/RSSI scans, no installs, no repairement. Golden v1.0.10 immutable; this source utility is not a working case ring APK.
