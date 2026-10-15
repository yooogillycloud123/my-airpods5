#!/usr/bin/env python3
"""Read-only CI source exploration to locate false 0% UI; prints SOURCE ONLY, no secrets."""
from pathlib import Path
import re,sys
root=Path(sys.argv[1])
base=root/"android/app/src/main/java/me/kavishdevar/librepods"
for p in sorted((base/"presentation").rglob("*.kt")):
    data=p.read_text("utf-8")
    lines=data.splitlines()
    interesting=[i for i,x in enumerate(lines) if (
        re.search(r'BatteryView\(|batteryPercent|batteryLevel|batteryCase|battery(\?|\.)\.?(level|status)|\.level\s*\?:\s*0|(?:Text\(|text = ).*[0-9]?%|%\\\"|0%',x,re.I)
        or re.search(r'BatteryStatus|Battery\b|BatteryInfo',x)
    )]
    if not interesting:continue
    picks=set()
    for i in interesting:
        picks.update(range(max(0,i-3),min(len(lines),i+5)))
    if len(picks)>160:picks=set(sorted(picks)[:160])
    print("=== SOURCE",p.relative_to(root),"total_lines",len(lines),"===")
    for i in sorted(picks): print(f"{i+1:04d} | {lines[i][:180]}")
print("V125_INSPECT_NO_DEVICE_NO_PAYLOAD=PASS")
