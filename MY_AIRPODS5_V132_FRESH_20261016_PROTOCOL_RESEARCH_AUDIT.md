# AirPods5 A3530 case sound — live research freshness audit (2026-10-16 ~17:16 KST)

**Acceptance unchanged:** rooted-none Galaxy app, no prior Apple registration, both buds docked, closed unplugged A3530 CASE physically beeps on Android button. No implementation or validated case speaker packet found in these sources. **Do not infer impossible**.

## LibrePods original record vs ACTUAL latest
- LibrePods `#102` open **2025-04-17**, maintainer states separate case GATT server needs owner authentication, exact procedure unknown. Last issue activity **2025-07-10**. Old unresolved evidence, NOT current 2026 new experiment. https://github.com/librepods-org/librepods/issues/102
- Main README inspected LIVE 2026-10-16 still identifies Find My 'add to Find My', 'play sound through charging case', and 'toggle case charging sounds' as **PLANNED, further RE needed**. https://github.com/librepods-org/librepods/blob/main/README.md
- `#20` 2025-01-06 TODO (last changed 2026-04-30) still unchecked **authenticate with case / play through case**. https://github.com/librepods-org/librepods/issues/20
- Real **fresh versions**: Android **v1.0.2 published 2026-10-15 13:05 UTC** (mainly other-model visual assets); **v2.0.0-alpha.1 rewrite published 2026-10-15 18:48 UTC** (explicit unfinished refactor/heart-rate); **nightly d27b87c 2026-10-16 07:34 UTC**, commits up to **2026-10-16 07:31 UTC**, mostly UI/CI. Source `android/rewrite` ATTManager uses hearing-aid/transparency ATT, NOT owner case play implementation. No case speaker play in reviewed release notes or main/rewrite paths. https://github.com/librepods-org/librepods/releases
- `#718` created **2026-08-11** suggested RSSI+generic AACP sound, closed **2026-08-30 NOT_PLANNED** without implemented case code; not an actual test result. `#717` Apple ID network location similar closed NOT_PLANNED. https://github.com/librepods-org/librepods/issues/718

## Newer researchers, DIRECT measurements
- **2026-09-13 to 2026-10-13 (NOT AirPods, a RAVEMEN ABF01 third-party Find My accessory)**: OpenTagViewer #193 author captured successful iOS HCI+sysdiagnose and exact service/characteristic writes:
  - `4F860002` owner point, iOS writes `01 09 02` status, `01 00 02` START, `01 01 02` STOP (this supersedes incorrect early order).
  - Android reproduced writes, ATT success but speaker silent without prior **owner encrypted LE link**; iOS used a known LTK. Android public API cannot inject a supplied LTK.
  - **2026-09-26** researcher captured HCI command injection through root/Frida, packet reaches controller but BT HAL crashes, still silent; owner encryption not proven sufficient.
  - **2026-10-13** maintainer offered new iCloud keychain search tool; account login was then subject to a server failure. Issue remains open.
  - **CRITICAL:** These bytes, LTK behavior, advertised UUID, and root experience are from RAVEMEN, **NOT AirPods5 A3530**, do NOT blindly write them to user case.
  https://github.com/parawanderer/OpenTagViewer/issues/193
- **2026-09-13** OpenTagViewer #201 tracks owner-connected sound path as unresolved. **2026-08-25** #166 tracks iPhone-free initial Find My registration unresolved for AirTag (not A3530). https://github.com/parawanderer/OpenTagViewer/issues/201 ; https://github.com/parawanderer/OpenTagViewer/issues/166
- **2026-09-09 through 09-22** stuffz/airpods independently reverse engineered **AirPods Pro 3** encrypted **separate charging CASE** BLE advertisement format even with lid shut; HCI scan can miss while radio sends. This is a genuine recent case-research advancement, but only Pro3 observation, **NOT proven 5 A3530 CASE identity, auth, or sound**. https://github.com/stuffz/airpods/blob/main/docs/ble-research.md
- **2026-10-15 CAPod** repo active; `DefaultAapDeviceProfile` says `0x31 IN_CASE_TONE` setting effect unknown; real case charging sounds use ATT, not this control. User has not obtained the CASE authenticated command.
- **2026-10-15 MagicPodsCore** repo active, open AACP tone volume setter and AirPods5 model registration; **not physically ring**. Same distinction applies.
- **2026-10-16 status**: active researchers *do* exist, old LibrePods issue cannot be used alone to claim no progress. Latest direct third-party owner-auth HCI evidence is Sep26, maintainer update Oct13. No independently verified rootless Galaxy-only no-Apple-setup closed/unplugged A3530 CASE audible beep implementation found.

## Consequence for project
Stop repeat scans/clone APKs. Next proof must be **A3530-specific Apple successful case speaker trace and original provisioning/auth** from consent-based independent research, not RAVEMEN opcode reuse, then determine whether stock Android can create/obtain the required owner key and encrypted link without Apple account. Preserve v1.0.10 partially verified, v126 lab, Android Auto Golden.
