# AirPods 5 Find My 0x12:02 near/far user measurements (2026-10-15)

User tests on Galaxy S26 Ultra with the same read-only v1.0.15 app, before and after moving their own closed charging case farther away. The condition that only the case was moved was requested; confirmation of exact surrounding RF conditions is unavailable.

| Observed 35-second scan | near | case farther |
| --- | ---: | ---: |
| Total BLE callbacks | 1,484 | 1,809 |
| Apple manufacturer advertisements | 48 | 99 |
| Find My type 0x12 | 48 | 14 |
| Apple 12:02 length 4 | 31 | 8 |
| Apple 12:02 length 23 | 17 | 6 |
| Apple 10:07 length 9 | 0 | 85 |
| 0x12 advertisements marked connectable | 17 | 6 |
| AirGuard AirPods-filter matches | 0 | 0 |
| FD44 service UUID advertisement | 0 | 0 |

- Find My 0x12 count dropped ~71%, short frames ~74%, and long 12:02 frames ~65%, while total BLE increased. This is suggestive of an RF correlation if measurement conditions were controlled, **not proof of own-case identity**.
- Apple 0x10 advertisements are documented Apple Continuity Nearby Info, not the case's Find My sound endpoint. Their 85 new packets explain why total Apple frames increased.
- Apple's Find My Network Accessory Spec v2 describes 12:02 length 4 as *nearby state* advertisement, and 12:19 as a separated frame. The 12:02 length 23 is not a documented standard nearby format; interpretation unknown. The specification says Find My network payload normally begins after owner pairing, and the user reports never pairing to an Apple account. The 0x12 packets therefore could all be unrelated Apple devices.
- 0 FD44 advertisements does not prove the actual service is absent. 0 AirGuard mask does not prove the user's case cannot play sound.
- **No owner case verification, no authorized GATT channel, no beep**. Never attempt GATT writes to these anonymous packets.
- Next experimental source on lab branch: v1.0.16 records only anonymous RSSI median per the two 12:02 frame lengths. The user's normal v1.0.10 (physically verified listening modes) remains untouched.
- No raw addresses, Find My ephemeral keys, Apple account tokens or RF packet payloads are stored or published.

Evidence and format references:
- https://www.frandroid.com/wp-content/uploads/2020/06/Find_My_network_accessory_protocol_specification.pdf
- https://github.com/furiousMAC/continuity/blob/master/messages/nearby_info.md
- https://github.com/seemoo-lab/AirGuard/blob/main/app/src/main/java/de/seemoo/at_tracking_detection/database/models/device/types/AirPods.kt
