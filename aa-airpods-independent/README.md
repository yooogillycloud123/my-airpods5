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
