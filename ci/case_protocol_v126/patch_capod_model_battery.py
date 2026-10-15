#!/usr/bin/env python3
"""v126: AirPods5 0x3020 CAPod-source-backed *existing* BLE parser hardening.
No new radio scan or CASE GATT calls. Transport still uses existing LibrePods path.
Model discriminator is public model metadata, NOT proof of case/Find My owner.
"""
from pathlib import Path
import sys
root=Path(sys.argv[1])
base=root/"android/app/src/main/java/me/kavishdevar/librepods"
p=base/"bluetooth/BLEManager.kt"
g=root/"android/app/build.gradle.kts"
def once(f,old,new,label):
    s=f.read_text("utf8")
    assert s.count(old)==1,f"V126_ABORT_{label}: occurrences={s.count(old)}"
    f.write_text(s.replace(old,new,1),"utf8")
    print(f"V126_{label}=PASS")
# CAPod AirPodsGen5Wireless.Factory reports device model 0x3020;
# its ordinary AirPodsGen5 model is separately 0x3620.
once(p, '        0x1F20 to "AirPods Max (USB-C)"',
      '''        0x1F20 to "AirPods Max (USB-C)",
        0x3020 to "AirPods 5 (무선 충전 케이스)",
        0x3620 to "AirPods 5"''',"EXACT_MODEL_MAP")
# CAPod ProximityPairing.Decoder first requires Apple Continuity type 07,
# length 25 and body prefix 01 to reject identity-address garbage.
# Only narrow frames claiming 0x3020/0x3620, leave older-device parser intact.
once(p, '            if (manufacturerData.size <= 20) return',
'''            if (manufacturerData.size <= 20) return
            val claimedModelId = ((manufacturerData[3].toInt() and 0xFF) shl 8) or
                (manufacturerData[4].toInt() and 0xFF)
            if (claimedModelId == 0x3020 || claimedModelId == 0x3620) {
                if (manufacturerData.size < 27 ||
                    (manufacturerData[0].toInt() and 0xFF) != 0x07 ||
                    (manufacturerData[1].toInt() and 0xFF) != 25 ||
                    (manufacturerData[2].toInt() and 0xFF) != 0x01) {
                    // CAPod: reject non-plaintext/incompatible 07 frame.
                    // No case identification inferred from manufacturer ID.
                    return
                }
            }''',"AIRPODS5_PLAINTEXT_CONTINUITY_GATE")
# The original LibrePods decoder returned 127% when 0xff means
# battery absent. Public CAPod decoder: ff or percentage>100 => UNKNOWN.
once(p,'''    private fun formatBattery(byteVal: Int): Pair<Boolean, Int> {
        val charging = (byteVal and 0x80) != 0
        val level = byteVal and 0x7F
        return Pair(charging, level)
    }''',
'''    private fun formatBattery(byteVal: Int): Pair<Boolean, Int?> {
        val charging = (byteVal and 0x80) != 0
        val level = byteVal and 0x7F
        if (byteVal == 0xFF || level > 100) return Pair(false, null)
        return Pair(charging, level)
    }''',"INVALID_ENCRYPTED_BATTERY_UNKNOWN")
once(p,'''        val caseBattery = if (rawCaseBatteryByte == 0xFF || (isCaseCharging && rawCaseBattery == 127)) {
            lastValidCaseBatteryMap[address]
        } else {
            lastValidCaseBatteryMap[address] = rawCaseBattery
            rawCaseBattery
        }''',
'''        // The 0xff "absent" value is NOT 127%. Only valid [0,100]
        // levels can populate the existing bounded per-address cache.
        val caseBattery = if (rawCaseBattery == null) {
            lastValidCaseBatteryMap[address]
        } else {
            lastValidCaseBatteryMap[address] = rawCaseBattery
            rawCaseBattery
        }''',"CASE_BATTERY_NO_127_CACHE")
# Keep the original v125 LAB history/ANC/AACP implementation unchanged.
once(g,'versionCode = 10025','versionCode = 10026',"VERSIONCODE")
once(g,'versionName = "1.0.25-battery-provenance-case-gate"',
      'versionName = "1.0.26-capod-model-battery-gate"',"VERSIONNAME")
src=p.read_text("utf8")
assert src.count("0x3020 to ")==1
assert src.count("0x3620 to ")==1
assert "rawCaseBattery == null" in src
assert 'applicationId = "com.myairpods5.personal.lab"' in g.read_text("utf8")
assert 'connectGatt(' not in src
assert 'writeCharacteristic' not in src
print("V126_NO_NEW_BLE_GATT_OR_SOUND_COMMAND=PASS")
