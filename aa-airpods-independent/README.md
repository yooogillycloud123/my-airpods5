## 2026-10-17 transfer checkpoint
- The PRIVATE repo's own GitHub-hosted Actions failed before steps due to GitHub account billing/spending restriction, confirmed in user screenshot; it was **NOT** an Android compiler failure.
- All new AA development is being migrated under **PUBLIC** repository `my-airpods5/aa-airpods-independent/` without overwriting the unrelated AirPods5 companion app.
- ACTUALLY migrated: public Gradle build files; AndroidManifest files, native build and res; OneApp2 source-only route/daemon/bootstrap/self-heal/Focus state, and read-only host tests. Source is copied into independent subfolders; some classes are not yet migrated, so not a buildable complete Android tree. Existing free public host workflow `aa-oneapp2-public-source-host.yml` has **PASS** run `38985783001` using no private repository, no PAT and no signing key.
- GitHub connector safety blocked the public-copy operations specifically for `OneApp2LiveOutputObserver.java`, `OneApp2UniversalController.java` at the Android source path, and `InternalEdgeFocusSpec.java`. The second file is separately present in `source/` for host tests, but Android source-tree migration of it was not approved. No attempt was made to work around denied operations.
- This migration **does not yet** provide a self-contained APK or 2nd+ physically proven simultaneous playback. The preexisting public signing pipeline `build-aa-golden-capsule.yml` at its pinned older source produced genuine signed independent LAB Build16 (`38979582461`), but that build still read signed original source from PRIVATE and is not proof of a self-contained public-source build.
- Security boundary: NEVER copy original keystore/PAT/auth token/pairing state from PRIVATE into PUBLIC. The final signed channel must obtain original signing material through GitHub Secrets or an existing narrowly scoped authenticated channel, while Android app source and tests reside publicly.

# AA AirPods — standalone public-source migration

This directory is dedicated to the separate Android Auto / AirPods independent integrated-app project. It is NOT the My AirPods 5 battery-popup application.

## What this public directory is for
- Recreate the independent, reviewed, source-only Android app in a public repository so GitHub-hosted Actions jobs can run without private-repository billing.
- Keep original real-world Golden package `com.aaairpods.r25fix`, daemon :47651, companion apps, signing private keys and credentials untouched.
- Publish source code and tests only after checking for tokens, personal details, Bluetooth addresses, embedded paths, credentials, and distribution rights.
- NEVER commit a GitHub PAT, signing keystore, pairing tokens, logs containing private information, or archived runtime credentials.
- Actual compiled/signed APKs must have recorded package ID, signer fingerprint, SHA256, build run, and exact source revision. Compilation is not physical audio success.

## Current status
The historical free PUBLIC build workflow `.github/workflows/build-aa-golden-capsule.yml` already built the separate LAB package in run #38979582461 using pinned private source and original private signer. It has **not** migrated all source into this public directory. A public source-only version, signed independent release, real repeated-cycle Focus logic, and 3+ in-vehicle audio acceptance still need verification. Do not label the historical LAB APK FINAL.
