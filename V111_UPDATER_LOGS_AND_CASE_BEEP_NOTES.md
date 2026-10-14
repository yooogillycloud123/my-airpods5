# My AirPods 5 v1.0.11 — updater / diagnostics lab, NOT user GOLDEN

Source branch `airpods-personal-v111-updater-diagnostics`, derived from v1.0.10 success commit `629f45dcc7b4189b713876660ddc2e18cf13f38a`.
Frozen source APK: `my-airpods5-test-r21` / SHA256 `b46c8e38f28332f7af0f98bce7b8b27234f62a83d103363cf0697a2fd127445b`.
2026-10-15 user confirms ALL four actual listening modes function (끔, 주변음, 적응형, 노이즈 캔슬링). This is the GOLDEN's physical PASS. Never delete/replace r21 to try lab candidates.

## Implemented in lab candidate (pending CI and physical tests)
- `Settings → 내 에어팟 5 관리 → 앱 업데이트 확인` checks HTTPS GitHub pinned update-channel.json **ONLY when user taps**, not an always-on monitor. Returns current version if no newer release.
- Checks target package / versionCode, HTTPS release allowlist, published APK SHA256, and local installed signing cert. If installed cert != release signer, it **refuses** destructive/unsafe in-place update.
- Only if all gates PASS, downloads into isolated private update cache, verifies APK hash AND PackageManager apk archive signer/package/version, then invokes Android Package Installer after user confirms; may require one-time 'unknown apps' install permission.
- `Settings → 내 에어팟 5 관리 → 오류 기록` moves Java stack / lifecycle viewer and copy/share control; **no startup dialog**. Reporter stays installed, corrected header to installed BuildConfig version.
- Original AirPods AACP/ANC/BLE/media error fix remain frozen.
- Main public `update-channel.json` is pinned to r21 v1.0.10. **Do not point to unsigned or differing-certificate build; never automatically broadcast insecure file.**

## Fundamental Android signing hard stop
The v1.0.10 r21 APK was signed by a GitHub-run ephemeral debug key; the key's private half was not saved. Android WILL NOT allow a different signed APK to update `com.myairpods5.personal` in place. Coding an updater does not fix cryptographic identity.
To achieve FUTURE seamless updates:
1. Generate and securely back up a durable private signing keystore. NEVER commit it or its password to the public source repo. Prefer encrypted GitHub Actions Secrets or other private signing service.
2. Configure build to sign each subsequent release with EXACTLY that key; record certificate SHA256 and SHA256 in signed manifest after verifying APK and physical regressive tests.
3. The already-installed r21 app CANNOT be silently upgraded to the new identity. A *single user-approved migration* of that personal app is required unless its original private key can be recovered. Do not promise no-reinstall from r21.
4. Keep a separate beta branch; do not encourage users to delete working r21 merely to try a beta. Genuine LibrePods and Android Auto AA projects unaffected.

## Planned case speaker find-my action (next research stage, NOT implemented)
- Apple states only AirPods 5 **with Wireless Charging Case** has Find My speaker; ordinary USB-C case does not. Use case model to verify: Apple case A3530 (wireless speaker) vs A3529 (ordinary) per apple support.
- Apple Find My can trigger sound from supported case; LibrePods upstream README specifically lists `Play sound through charging case to find it` under **planned, further reverse engineering required, may need root**. Therefore do not invent Bluetooth opcode or add a fake 'Find case' button.
- Research exact packet/protocol/capability and without-root feasibility before isolated implementation. Distinguish *find-my locator beep* vs *charge-complete chirp setting*; not the same feature.
