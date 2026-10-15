# Galaxy Find My observations: same phone, case-distance change (2026-10-15)

| Signal | Before distance change | After distance change |
|---|---:|---:|
| All BLE callbacks | 1,484 | 1,809 |
| Apple manufacturer | 48 | 99 |
| Find My type 0x12 | 48 | 14 |
| 0x12 / subtype 0x02 / data size 4 | 31 | 8 |
| 0x12 / subtype 0x02 / data size 23 | 17 | 6 |
| Apple type 0x10, subtype 0x07, size 9 | 0 | 85 |
| AirGuard AirPods type/status mask | 0 | 0 |
| FD44 advertised | 0 | 0 |
| 0x12 connectable | 17 | 6 |

Two 35-second scan windows; shorter 12:02 frame occurrences decreased ~74.2%; overall 12 occurrences decreased ~70.8%. This is **consistent with** movement of an Apple advertiser, but no cryptographic proof identifies the owner's case and unrelated device activity can confound. On a Galaxy-only, never Apple Find My paired accessory, proximity beacons from surrounding Apple devices must be considered.

No GATT service discovery was performed and no case audio command was sent. Next gate: anonymous *RSSI, packet length, connectability, repeated A/B/A case-distance controlled samples* before owner attribution; do not automatically ring any 0x12 advertiser. The owner's working v1.0.10 GOLDEN and Android Auto router are frozen.
