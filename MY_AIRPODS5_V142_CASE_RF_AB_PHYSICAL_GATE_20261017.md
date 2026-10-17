# A3529 CASE PHYSICAL SIGNAL EXPERIMENT — v1.0.42 (2026-10-17)

## Why this is different from 12:02-count-only LABs
User's real Galaxy has picked up random Apple 12:02 broadcasts and has even sent 0/0-success nonowner FD44 start/stop writes, yet their actual AirPods 5 wireless A3529 case was silent. We still do not know if *any* scanned temporary address belonged to the case. Counting more anonymous packets is not a solution.

This single opted-in **controlled near-vs-shielded radio perturbation** experiment measures Apple 0x004C manufacturer advertisements **from the user's physical closed case** in two short windows. Android does NOT need A2DP connected. The user intentionally changes ONLY the position/shielding of the own case while the room and phone stay fixed.

1. Put both buds inside, lid closed; put A3529 case alongside Galaxy S26 Ultra, user taps START. Read 8 s.
2. App STOPS scan and waits. Move only the closed case 4–5m away (or in an unpowered closed metal box), leave room/other devices still, tap CONTINUE. Read 8 s.
3. In-memory ephemeral address groups `12:02`, `12:19`, `07:19`, other Apple `07` compared across the windows. Show only anonymized group, RSSI near/far and change. No address/UUID/raw manufacturer bytes emitted, exported or persisted.

**Physical-correlation result can suggest the case's radio signal; it is NOT cryptographic proof of owner identity.** A false signal/rotating address/multipath can produce a false positive or negative. Positive means stage-1 of user-requested case identity narrowed, NOT FindMy owner provisioning.

Sound testing remains BLOCKED until a **separate independent case crypto-binding + valid authenticated control session + reproducible A3529-specific play/stop** have proof. The third-party RAVEMEN ABF01 `4F860002 / 01 00 02` encrypted owner command is NOT established as applicable to Apple's A3529 case. Sending another anonymous 4F860003 command was already physically refuted.

## Implementation
- Branch: `airpods-v143-targeted-case-rf-correlation-20261017`
- Source: `ci/case_protocol_v143/CaseEntryV143.kt`, `CaseRfCorrelationV143.kt`, `patch_v142.py`.
- Both old v141 read-only diagnosis and new physical correlation are selectable. Original Android services/settings/Bluetooth pairings protected.
- Same LAB package `com.myairpods5.personal.radiolab`, not a sixth app. But CI-generated signing certificate is new, **so direct update over installed v141 is impossible**; uninstall only radiolab *if accepting loss of its local data*, then install the new LAB. DO NOT remove original personal v1.0.10, old key-bearing LAB, AA Golden/daemon or unpair buds.
- No automatic scans. Only this one user-triggered guided session consists of two 8-second windows.
- Case speaker cannot be activated by this build; it purposefully does not send a single control/write instruction.
