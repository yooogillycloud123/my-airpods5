#!/usr/bin/env python3
"""v1.0.8 minimal safety guards around incomplete single-ear packets.

Preserve original AACP/ATT/BLE managers, noise-control commands and ear-state
meaning. Only remove definite unsafe force-unwrapping of optional battery
components and [0]/[1] access when packet is short.
"""
from pathlib import Path
import sys, re
root=Path(sys.argv[1])
p=root/"android/app/src/main/java/me/kavishdevar/librepods/services/AirPodsService.kt"
s=p.read_text(encoding="utf-8")
pattern=(r"batteryNotification\.getBattery\(\)\s*"
         r"\.find \{ it\.component == BatteryComponent\.LEFT \}\?\.level!!\.coerceAtMost\(\s*"
         r"batteryNotification\.getBattery\(\)\s*"
         r"\.find \{ it\.component == BatteryComponent\.RIGHT \}\?\.level!!\s*\)")
s,n=re.subn(pattern,"safeVisibleBatteryLevel()",s)
if n != 4: raise RuntimeError(f"Expected 4 non-null assertion pairs in original service, got {n}")
helper='''    private fun safeVisibleBatteryLevel(): Int =
        batteryNotification.getBattery()
            .filter { it.component == BatteryComponent.LEFT || it.component == BatteryComponent.RIGHT }
            .map { it.level }
            .minOrNull() ?: 0

'''
needle='    fun isBluetoothSocketExempted(): Boolean {'
assert s.count(needle)==1
s=s.replace(needle,helper+needle,1)

before='''                if (batteryNotification.getBattery()[0].status == BatteryStatus.CHARGING && batteryNotification.getBattery()[1].status == BatteryStatus.CHARGING) {'''
after='''                val batteryEntries = batteryNotification.getBattery()
                val leftStatus = batteryEntries.find { it.component == BatteryComponent.LEFT }?.status
                val rightStatus = batteryEntries.find { it.component == BatteryComponent.RIGHT }?.status
                if (leftStatus == BatteryStatus.CHARGING && rightStatus == BatteryStatus.CHARGING) {'''
assert s.count(before)==1
s=s.replace(before,after,1)

before='''    private fun processEarDetectionChange(earDetection: ByteArray) {
        var inEar: Boolean'''
after='''    private fun processEarDetectionChange(earDetection: ByteArray) {
        if (earDetection.size < 2 || earDetectionNotification.status.size < 2) {
            Log.w(TAG, "Incomplete ear-state packet ignored")
            return
        }
        var inEar: Boolean'''
assert s.count(before)==1
s=s.replace(before,after,1)
assert '?.level!!' not in s
assert 'batteryNotification.getBattery()[1]' not in s
p.write_text(s,encoding="utf-8")
g=root/"android/app/build.gradle.kts"
txt=g.read_text(encoding="utf-8")
assert txt.count('versionCode = 10007')==1
assert txt.count('versionName = "1.0.7-lifecycle-stable"')==1
txt=txt.replace('versionCode = 10007','versionCode = 10008',1)
txt=txt.replace('versionName = "1.0.7-lifecycle-stable"','versionName = "1.0.8-single-ear-safe"')
g.write_text(txt,encoding="utf-8")
print("V108_ONE_EAR_BATTERY_NULL_GUARD=PASS")
print("V108_EAR_STATE_SHORT_PACKET_GUARD=PASS")
