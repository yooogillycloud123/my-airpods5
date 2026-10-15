# v128: A3530 CASE sound protocol transport groundwork (NOT a sound APK)

**Actual accepted feature**: AirPods 5 wireless charging case A3530 emits audible Find My beep when both buds are docked and lid closed, controlled by Galaxy S26 Ultra without an Apple device. Not implemented yet.

## Verified source (not Apple AirPods5 case evidence)
- Apple AAP/AACP transport reverse-engineering shows L2CAP `PSM=0x1001` is accessory AAP, `PSM=0x001F` corresponds to ATT over BR/EDR (classic GATT). Neither alone proves case identity or Find My owner channel. Source: https://gist.github.com/R00tKited/4fc75e67280768976476fc5c341c69f1 (unverified secondary writeup); https://github.com/BorgoGustavo/airpods-ctl/blob/master/docs/aap-protocol-notes.md
- LibrePods CASE Find My GATT owner authentication and physical speaker play still unchecked: https://github.com/librepods-org/librepods/issues/20 and https://github.com/librepods-org/librepods/issues/102
- OpenTagViewer issue #201 describes owner encrypted vs nonowner DULT control on third-party accessories, which CANNOT be copied as A3530 case command: https://github.com/parawanderer/OpenTagViewer/issues/201
- Android-only Find My provisioning unknown: https://github.com/parawanderer/OpenTagViewer/issues/166
- User AirGuard physical run: Safe Tracker, Locate Tracker only, NO Play Sound.

## What is real code
`l2cap_owner_trace.py` improves previous v119 CID4-only HCI analysis to include Classic signalling CID1 and LE signalling CID5, dynamic channel PSMs, HCI session epochs, encryption vs claimed case owner authorization, no raw addresses, payloads or keys exported. The audit output always marks `actual_airpods5_case_identity: NOT_PROVEN`, `case_owner_authentication: NOT_PROVEN`, `physical_case_speaker_sound: NOT_PROVEN`.

Offline use if independent, redacted, consented HCI capture material becomes available:
`python3 ci/case_protocol_v128/l2cap_owner_trace.py /local/btsnoop_hci.log --output /local/transport.json`
Never publicly share original HCI traces; they may include private addresses and keys. This is not a phone guide or new test request.

## Gate to a REAL case sound APK
Require source-confirmed A3530 CASE identity (not earbuds' 0x3020), owner enrollment/authentication, case-side authenticated Find My start/stop packets, and physical CASE beep validation. A Bluetooth write ACK or working earbud noise cancelling is insufficient. Never issue speculative writes to someone's tracker.

## Protected artifacts
Personal v1.0.10 user partially verified baseline untouched. Android Auto YTM/daemon untouched. Last signed model/battery LAB v1.0.26 stays at its existing release; v128 is an independent protocol research source branch and has NO installable Android case-beep functionality.

This package intentionally does not make users install another ineffective helper app.
