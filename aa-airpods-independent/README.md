## 2026-10-17 — Independent repeated-cycle executor implementation attempt (not merged)
- User requested an actually executable single-app ACTIVE_MEDIA_OWNER -> Generic Focus rearm -> vehicle evidence -> next AirPods chain, with a signed vehicle test APK. Existing signed Build16 is not this candidate.
- Checked true PUBLIC source HEAD 7bfa5d56772915fc626881263e9a25e9bd75c926 and successful public host-only run 38985897319. The Android Gradle source is incomplete; prior GitHub tool safety rejected transferring OneApp2LiveOutputObserver, OneApp2UniversalController in Android source tree and InternalEdgeFocusSpec. The existing source-only Focus planner is not an operating external AudioPolicy owner.
- Attempted to add original-independent `aa-airpods-independent/source/OneApp2RepeatTransaction.java` implementing route->owner->Focus rearm->positive vehicle evidence->Focus release via an injected platform backend, on PUBLIC main using official GitHub create_file. Tool returned exactly `OpenAI의 안전 검사에서 이 도구 요청을 차단했습니다. 전송하는 항목을 재차 확인하세요.` No commit or build exists for that operation, no retry via different tooling. Internal reason not supplied. This executable bridge was NOT added.
- Confirmed existing PRIV source has only shell uid=2000 GenericFocusMain in the frozen original and not a proven equivalent separate OneApp2 runtime; last signer build remains run 38979582461 signed Build16 LAB (original SHA a51f62bd6568198c9a8256570719eb841b36d2db0afa9cc44d7b99b408714958). Separately recorded private Actions billing spending restriction; public source-only host runner works.
- Status: no NEW_SIGNED_APK; no fully independent external Focus Policy / ACTIVE_MEDIA_OWNER / edge UID equivalence; original Golden intact. Do not advertise source-only tests as car-ready. Next supported work requires an allowed fully integrated source modification and independently signed Android compile (without bypassing rejected operations), plus user-controlled safe single-owner cutover and real car verification.

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
