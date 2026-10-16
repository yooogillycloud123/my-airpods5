# AirPods 5 + REA: agent instruction
Read `MY_AIRPODS5_PERMANENT_READ_FIRST_20261015.md` first, then `tools/rea_airpods/README_KO.md`.
Do not replace the user’s Galaxy-only closed AirPods 5 wireless charging case Find My audible beep target (physical A-number not yet authenticated; Apple A3529/A3530 primary sources conflict).
Existing personal `com.myairpods5.personal` v1.0.10, v139 diagnostic `com.myairpods5.personal.radiolab`, older LAB storage and all Android Auto Golden assets are protected. Never delete/re-sign/unpair them.
Use REA **on owned or expressly authorized APK only**, locally/offline; treat JADX output as static observations, not runtime events or owner authentication.
Upstream official `morluto/rea` and npm `rea-agents@6.5.0`; JADX headless JAR SHA check as in lockfile.
For reproducible GitHub execution, modify source on a branch and trigger `.github/workflows/rea-airpods-apk-source-audit.yml`. Compare reported SHA and static class findings, keep raw decompiled bodies on ephemeral runner only.
Never post device MACs, Apple Account tokens, IRK/ENC_KEY, Bluetooth raw captures or private app dex to GitHub logs/artifacts.
Missing AirPods 5 wireless case actual GATT owner-auth + case-speaker play/stop means success gate CLOSED. Do not treat FD44 nonowner, 12:02, AirPods proximity model 0x3020, charge-start ding or AACP 0x31 as Find My case beep.
This document guides REA-capable coding agents/ChatGPT reading the GitHub repo; **it does not itself install an MCP connector into ChatGPT**.
