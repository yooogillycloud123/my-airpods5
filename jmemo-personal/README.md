# J Memo Personal — private development branch

A separate offline Windows sticky-note manager based on Memoit-style **user experience**, not a patched copy of Memoit.

## Status

- Python standard-library Tkinter sticky-note desktop application with drag/fold/resize/colors/search/groups/favorites/backup/trash
- Automatic atomic JSON saving, backup on startup, fail-closed on corrupted data
- First GitHub Actions Windows EXE build **pending CI confirmation**
- Original Memoit executable was **not** decompiled using REA yet. REA environment and Windows/Ghidra provider validation are separate steps.
- No installation of original Memoit, import of memodb, cloud transfer, or account login.
- Original AA AirPods and workexcel sources on main are not modified; this branch is isolated.

## Run Windows

Python 3.11+:

```powershell
python src/jmemo.py
python -m unittest discover -s tests -v
```

Windows EXE is produced by `.github/workflows/jmemo-windows.yml` with PyInstaller from exactly this source on push to this branch. Download only if Actions run reports SUCCESS and passes tests.

## Data

Dedicated `%APPDATA%\JMemoPersonal\notes.json` and `backups/`. For a separate test folder set `JMEMO_HOME`. This shares the v0.1 basic schema, but real upgrade compatibility on Windows needs testing and backup. It NEVER opens or overwrites Memoit's original `memodb`.

## REA role

REA is an **investigation tool**: installed target inventory, importing/strings/GUI traces (with Ghidra native provider) and evidence. It does not turn original proprietary EXE into directly editable source. Use `tools/Analyze-Memoit.ps1` locally when the user chooses to inspect their original installer. That script does not launch the installer. **Only the installer**, not the application's installed primary binary, was provided initially.

## Workflow caution

Any proprietary/copyright-protected application executable and personal notes must stay **out of this repository**; keep evidence redacted, do not commit the installer, passwords, tokens or memodb.

Planned: tray icon, keyboard parity, native WinForms/WPF alternative, alarms/calendar/formatting, import only after read-only format evidence, true Windows UI tests.
