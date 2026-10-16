---
name: airpods-rea
description: Inspect authorized Android APKs with pinned Reverse Engineer Anything and JADX, using evidence rather than guesses.
---

Read `AGENTS.md` and `tools/rea_airpods/README_KO.md` first. The latest personal app handoff is `MY_AIRPODS5_PERMANENT_READ_FIRST_20261015.md`.

Use GitHub Actions `.github/workflows/rea-airpods-apk-source-audit.yml`, not a new user-installed APK, to run REA on the existing project-owned v139 release. Version and SHA details are pinned in `tools/rea_airpods/rea-upstream-lock.json`.

Analysis sequence: inspect package -> search service and case-related classes -> inspect selected classes/methods -> trace references -> distinguish observations, inferences, and unknowns. Preserve the full REA Evidence only in the private, temporary runner; publish an allowlisted summary, not decompiled text or identifiers.

ChatGPT GitHub access can edit/inspect this workflow and its reports. This repository skill does **not** install an MCP server in ChatGPT itself. An REA-capable local CLI agent may register REA separately using upstream setup. Do not promise case-beep success from static APK analysis.
