# v138 physical result and decision — 2026-10-16 (no redundant APK yet)

## Exact user evidence
- User tested v1.0.38 and shared real screenshot. 12 s:
  - `전체 BLE 콜백=335`
  - `Apple 제조사 콜백=34`
  - `07:19 AirPods 주변 광고=0`
  - `0x3020=0`, `0x3620=0`, `Apple 07 기타=0`, `07:11:06=0`
  - `Find My 12:02=11`
  - `Find My 12:19 연결가능=0`, unique separated candidates=0
  - `다른 Find My 12형=11`
  - `Apple Nearby 10형=22`.
- Older v136 captured 1 12:19 connectable candidate and FD44 4F860003=true, 8 GATT services, DULT=false, WITHOUT proven own device identity. Later both v136 and v137 observed 0 12:19, while scanner returned BLE callbacks=396. User remembers AirPods in hand, but presence does not establish advertising packet as same device.

## Source-certain findings
1. V138 categorization source `ci/case_protocol_v138/CaseRadioEvidenceV138.kt` increments `generic12++` for every kind=0x12 including subtype=0x02. UI label `다른 Find My 12형` is misleading: 11 includes SAME 11 nearby type 12:02 observations, not 11 extra devices nor a separate type. Fix reporting before future shipment.
2. 11 `12:02` callbacks and 0 `12:19` indicate DIFFERENT ADVERTISEMENT FRAME types in this snapshot. Public OpenTagViewer `docs/owner-connected-tags.md` and relevant Apple Find My docs discuss Nearby `12:02` vs separated `12:19` in Find My accessory protocols. They do not prove this is user's AirPods 5, nor that v136's earlier device transitioned to nearby. No address continuity was recorded, so cannot match earlier 12:19 to current 12:02; assigning identity from counts is prohibited.
3. The v136/137 discovery is *filtered*: Apple 12:19 + connectable; v138 now counts both 07:19 and 12:02. New v138 shows Apple packets, so global BLE scan is working. AirPods ordinary 07:19 may be silent when closed/not pairing; neither 07:19 zero nor model zero proves hardware failure.
4. **Additional source-confirmed engineering interference risk:** each LAB clone bundles LibrePods `AirPodsService` foreground/background lifecycle. Upstream `services/AirPodsService.kt` creates `BLEManager` in `onCreate`, then launches `bleManager.startScanning()` around line 798; stops in `onDestroy` around 3131. `MainActivity`/personal v107 starts/binds `AirPodsService` on UI startup. Multiple separately packaged v134, v136, v137, v138 clones, if their services are *simultaneously active*, can register multiple BLE scans. This is a concrete design hygiene issue and possible scan competition but has NOT been established as the reason 12:19 vanished. Running scanner registrations were not captured.
5. V138 test app additionally registers a 12-second ad-hoc `startScan(null)` while its own LibrePods service is normally scanning. v139 should avoid this internal duplication by isolating TEST MODE scan without altering Golden app.
6. Find My FD44 non-owner `4F860003` discovered once may be from any separated tag. No source ties it to Galaxy-only AirPods5 wireless charging case A3530. No owner credentials or case speaker sound command currently available.

## Immediate engineering next steps
- Preserve actual v136 positive and v138 negative evidence; DO NOT treat 12:19=0 as new APK regression or 12:02 as proof of owner nearby.
- Source-first controlled improvement: create a test-only scanner **that does not automatically start the cloned foreground LibrePods BLE scan**, or uses exactly one registered BLE listener, for one-shot passive classification. Do not send user a new APK just to repeat the same data.
- Correct disjoint category counts, separate observations vs unique MAC-derived ephemeral counts without displaying addresses or persisting identity. Add scan callback failures, scanner start/stop status, model frame type and non-connectable separated frames separately. Show bounded recording in memory only.
- Before physical retest, define one discriminating hypothesis: verify an AirPods5 case-specific (NOT generic 12:02/12:19) association via authenticated keys or verified open-source closed-case transport. Unknown case ownership prevents automatic GATT calls.
- No uninstalls / Bluetooth toggle / pair reset / Golden edits. If app activity needs to be cleared for a future controlled trial, only force-stop *LAB* clones, never personal v1.0.10 or AA Golden, and explicitly tell user this preserves data.
- Main objective remains Galaxy standalone: both buds in closed AirPods5 wireless case, playback disconnected, audible CASE Find My beep; unimplemented until real case identity/authenticated speaker opcode is sourced.
