# 2026-10-16 A3530 v141 offline research result

User physical result preserved: v139 BLE callback=307, Apple=29, Find My 12:02=13 packets from five temporary addresses, Find My 12:19=0, 07:19=0. An older anonymous 12:19 candidate exposed FD44 nonowner characteristic and eight GATT services, but was never identified as the user's own A3530 case.

Current source-bound gate: AirPods 5 wireless A3530 has a speaker, and user physically confirmed a *charging-start chime* after AC power OFF, LED completely dark, then ON with USB-C connected. No case-specific authenticated owner GATT session or reliable start/stop Find My playback opcode is known for Android-only use without Apple Find My enrollment. Apple Find guide documents initial Apple Account pairing; LibrePods issue #102 says own case GATT authentication is yet unknown.

Local source package `AirPods5_A3530_PROOF_RESEARCH_v141_20261016.zip` was independently tested offline: existing 14 v140 unit tests + 6 new sanitized manifest tests = 20 PASS. It includes a v139 evidence manifest and explicit fail-closed program verdict `release_allowed=false`; does not connect to phone, reveal device credentials, update APK, or claim actual beep.

**Next physical APK release requires** same A3530 case's model-specific identity, owner provisioning, authenticated case command, and verified audible case-speaker playback with two buds docked/lid closed/A2DP disconnected. Keep current installed v139 and original personal/AA Golden untouched; no repetitive BLE scan tests.

Sources: https://github.com/librepods-org/librepods/issues/102 ; https://github.com/librepods-org/librepods/issues/20 ; https://support.apple.com/ko-kr/guide/airpods/dev8e8b93d71/web ; https://github.com/d4rken-org/capod/blob/main/app/src/main/java/eu/darken/capod/pods/core/apple/ble/devices/airpods/AirPodsGen5Wireless.kt .
