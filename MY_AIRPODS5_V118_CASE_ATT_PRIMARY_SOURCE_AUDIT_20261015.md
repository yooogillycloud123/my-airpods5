# AirPods 5 case-control v118: CAPod source audit (2026-10-15)

* Scope: Galaxy S26 Ultra, own AirPods 5 wireless case. Target remains two earbuds stored, closed lid, A2DP disconnected, actual case speaker sound. Apple owner account enrollment absent.
* CAPod 2026 Kotlin source `DefaultAapDeviceProfile.kt` says case tones use ATT, not the AACP IN_CASE_TONE setting, whose effect is unknown. Reference: https://github.com/d4rken-org/capod/blob/main/app/src/main/java/eu/darken/capod/pods/core/apple/aap/protocol/DefaultAapDeviceProfile.kt
* CAPod has CASE_INFO_REQUEST 0x22 and CASE_INFO reply 0x23 with a verified allowlist limited to AIRPODS_PRO3. It does not prove support on AirPods 5 and does not ring the case. Its raw reply is not yet fully decoded.
* LibrePods authors identify case-specific GATT with owner authentication as an unresolved prerequisite, issues #20 and #102. AirGuard sound is for separated nonowner trackers, not demonstrated for this particular nearby case.
* Engineering decision: investigate the real case ATT owner-session and case-speaker command from case-specific captured traces. Do not guess commands. Existing v117 BLE key resolver only proves an RPA matches a given key; earbud proximity IRK is not case owner authorization.
* User time protected: no more unrelated BLE scans or installation requests. v1.0.10 Golden and Android Auto functions untouched. Physical beep still UNPROVEN.
