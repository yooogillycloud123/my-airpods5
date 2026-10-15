# Case-sound research step 1: AirPods 5 raw BLE evidence (v1.0.14)

2026-10-15 · Branch: airpods-personal-v111-updater-diagnostics · package com.myairpods5.personal.lab

## What the physical test v1.0.13 showed
- Single manufacturer ScanFilter 07 11 06 returned 0 frames.
- That counter was never the count of all Bluetooth devices or even all Apple BLE frames.
- Returning 0 from a narrow filter does **not** mean the case never advertises.
- The original AndroidManifest.xml uses BLUETOOTH_SCAN neverForLocation. Android documentation says that restriction can filter some BLE beacon results, especially those used to derive nearby location.
- Actual Find My case command/auth protocol still unknown; do NOT blindly transmit undocumented commands.

## v1.0.14 safe discovery change
- Physical device diagnostic use only: while app foreground, user selects 35-second scan.
- Remove `neverForLocation` from the .lab APK manifest and declare Android `ACCESS_FINE_LOCATION`; permission must be manually granted after clear explanation. No location coordinates are read, stored or uploaded by the app.
- Remove radio manufacturer's narrow filter: BLE callbacks now counted by stage.
- Report exact counters: all BLE callbacks, Apple manufacturer records, Apple 07 type, 07 11 06 case-shaped records, records passing user's own AES key checks, twice-observed identical own-case beacon, RSSI ONLY when verified (otherwise no measurement).
- No target candidate means no GATT connection or audio command. Candidate requires a stored own-device encryption key and two matching verified case advertisements.
- Existing v1.0.10 GOLDEN release SHA b46c8e38f28332f7af0f98bce7b8b27234f62a83d103363cf0697a2fd127445b remains immutable.
- v1.0.14 is NOT a successful beep and not an Apple Find My network client.

## Minimal user test
1. Keep working v1.0.10 installed. To replace .lab v1.0.13 APK, uninstall ONLY previous .lab if Android refuses update due to ephemeral CI debug signer. It may delete the .lab ENC_KEY only; reconnect the same AirPods to .lab once to reacquire own ENC_KEY, without repairing Bluetooth.
2. Grant Android precise location permission to .lab for nearby BLE beacon visibility (if requested). Local data only.
3. **Use user's desired real condition:** put both AirPods in charging case, close lid, music Bluetooth disconnected. Keep phone and case nearby. Use lab app Settings > 케이스 소리 시험 · 연구 > 35초 검색.
4. Copy result with on-screen 결과 복사. If 0 total BLE, Android scan/permission/location setting problem; if BLE>0 Apple=0, Apple frames filtered or out of range; if Apple>0 and case-shape=0, older format assumption unsupported; if shape>0 and own-key=0, encryption key/payload layout issue.
5. Based on actual evidence, next step investigate AirPods 5 protected case speaker control protocol/owner auth. No additional guess APK loops.
