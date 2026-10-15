# LibrePods maintainer evidence — 2026-10-15

Original issue #20: https://github.com/librepods-org/librepods/issues/20 — 'Authenticate with the case' and 'Play sound through case' are BOTH unchecked TODOs under case Find My GATT.

Original issue #102: https://github.com/librepods-org/librepods/issues/102 — project maintainer says case chime configuration must be sent to the CASE GATT server (not the earbud AACP connection), requires authentication as owner, and the method of authenticating the case was unknown. This is not a working find-case sound protocol.

User Galaxy-only AirPods 5 has no Apple Find My ownership registration. Existing Golden v1.0.10 untouched. Do not ask user for more BLE/RSSI scans; further work requires confirmed AirPods5 case owner-auth and audible-sound control implementation, not opcode guesses.
