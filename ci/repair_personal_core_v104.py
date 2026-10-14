#!/usr/bin/env python3
"""Personal GPL AirPods v1.0.4: crash guards and no-dead-control presentation.

This script runs after repair_korean_onboarding_and_service.py on the checked-out
original source archive. It never patches APK bytecode or system Bluetooth.
"""
from pathlib import Path
import sys

root = Path(sys.argv[1])
base = root / "android/app/src/main/java/me/kavishdevar/librepods"

def replace(rel, old, new, label):
    p = base / rel
    s = p.read_text(encoding="utf-8")
    count = s.count(old)
    if count != 1:
        print("SOURCE_PREVIEW", label, s[:5000])
        raise RuntimeError(f"{label}: expected 1 exact source match, got {count}")
    p.write_text(s.replace(old, new, 1), encoding="utf-8")
    print("FIX_OK", label)

replace("MainActivity.kt",
    '        sendBroadcast(Intent(AirPodsNotifications.DISCONNECT_RECEIVERS))\n        super.onDestroy()',
    '        // Activity recreation must not unregister receivers owned by the running service.\n        super.onDestroy()',
    "activity_does_not_kill_service_receivers")

replace("services/AirPodsService.kt",
    '''                    showIsland(
                        this@AirPodsService,
                        batteryNotification.getBattery()
                            .find { it.component == BatteryComponent.LEFT }?.level!!.coerceAtMost(
                                batteryNotification.getBattery()
                                    .find { it.component == BatteryComponent.RIGHT }?.level!!
                            )
                    )''',
    '''                    // A single earbud may be reported without the other one.
                    val reportedLevels = batteryNotification.getBattery()
                        .filter { it.component == BatteryComponent.LEFT || it.component == BatteryComponent.RIGHT }
                        .map { it.level }
                    reportedLevels.minOrNull()?.let { showIsland(this@AirPodsService, it) }''',
    "single_earbud_popup_nullable_levels")

replace("services/AirPodsService.kt",
    '''                if (batteryNotification.getBattery()[0].status == BatteryStatus.CHARGING && batteryNotification.getBattery()[1].status == BatteryStatus.CHARGING) {''',
    '''                val leftBattery = batteryNotification.getBattery().find { it.component == BatteryComponent.LEFT }
                val rightBattery = batteryNotification.getBattery().find { it.component == BatteryComponent.RIGHT }
                if (leftBattery?.status == BatteryStatus.CHARGING && rightBattery?.status == BatteryStatus.CHARGING) {''',
    "single_earbud_battery_list_bounds")

replace("services/AirPodsService.kt",
    '''    private fun processEarDetectionChange(earDetection: ByteArray) {
        var inEar: Boolean''',
    '''    private fun processEarDetectionChange(earDetection: ByteArray) {
        if (earDetection.size < 2 || earDetectionNotification.status.size < 2) {
            Log.w(TAG, "Ignoring incomplete ear-state packet")
            return
        }
        var inEar: Boolean''',
    "incomplete_ear_detection_packet_guard")

replace("services/AirPodsService.kt",
    '''                    device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra("device", BluetoothDevice::class.java)!!
                    } else {
                        intent.getParcelableExtra("device") as BluetoothDevice?
                    }

                    if (config.deviceName''',
    '''                    device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra("device", BluetoothDevice::class.java)
                    } else {
                        intent.getParcelableExtra("device") as BluetoothDevice?
                    }
                    if (device == null) {
                        Log.w(TAG, "Ignoring missing AirPods device from connection broadcast")
                        return
                    }

                    if (config.deviceName''',
    "nullable_bluetooth_broadcast_device")

replace("bluetooth/BLEManager.kt",
    '        0x1B20 to "AirPods 4 (ANC)",',
    '        0x1B20 to "AirPods 4 (ANC)",\n        0x3020 to "AirPods 5",',
    "airpods5_ble_model_3020_label")

# This is a personal derivative distributed under GPL, using the existing FOSS
# implementation, not a Google Play purchase receipt or paid cloud entitlement.
# The immutable archived source already enables the locally licensed GPL feature
# switches by default. Preserve its existing FOSS contract, without fake billing.
p = base / "billing/FOSSBillingProvider.kt"
foss = p.read_text(encoding="utf-8")
assert 'private val _isPremium = MutableStateFlow(true)' in foss
assert 'override fun queryPurchases()' in foss
assert 'https://github.com/sponsors/' not in foss
print("FIX_OK", "already_premium_enabled_in_verified_original_source")

p = base / "presentation/screens/AppSettingsScreen.kt"
s = p.read_text(encoding="utf-8")
start = s.index('        StyledList(title = stringResource(R.string.contact)) {')
end = s.index('        Spacer(modifier = Modifier.height(20.dp))', start)
s = s[:start] + s[end:]
start = s.index('    StyledBottomSheet(\n        visible = contactBottomSheet.value,')
s = s[:start] + '}\n'
s = s.replace('                        "Save",', '                        "저장",')
s = s.replace('                        "Cancel",', '                        "취소",')
p.write_text(s, encoding="utf-8")
assert 'https://discord.gg/' not in s
assert 'https://github.com/kavishdevar/librepods/issues/new' not in s
assert 'contact@kavish.xyz' not in s
print("FIX_OK", "remove_external_contact_email_discord_github_ui")

# Do not imply that a button can send ANC commands with no AACP socket.
replace("presentation/screens/AirPodsSettingsScreen.kt",
    'if (capabilities.contains(Capability.LISTENING_MODE)) {',
    'if (capabilities.contains(Capability.LISTENING_MODE) && state.isLocallyConnected) {',
    "only_offer_listening_mode_control_with_live_aacp")

gradle = root / "android/app/build.gradle.kts"
g = gradle.read_text(encoding="utf-8")
assert g.count('versionCode = 10003') == 1
g = g.replace('versionCode = 10003', 'versionCode = 10004', 1)
assert g.count('versionName = "1.0.3-ko-personal"') == 1
g = g.replace('versionName = "1.0.3-ko-personal"', 'versionName = "1.0.4-personal-guard"', 1)
gradle.write_text(g, encoding="utf-8")
print("FIX_OK", "source_version_10004")

# The original service has three more cross-device island call sites that
# force-unwrap BOTH bud levels. They are triggered by takeover/disconnect and
# would still crash when only one bud has reported a battery level.
import re
service_path = base / "services/AirPodsService.kt"
service = service_path.read_text(encoding="utf-8")
unsafe = (r"batteryNotification\\.getBattery\\(\\)\\s*"
          r"\\.find \\{ it\\.component == BatteryComponent\\.LEFT \\}\\?\\.level!!\\.coerceAtMost\\(\\s*"
          r"batteryNotification\\.getBattery\\(\\)\\s*"
          r"\\.find \\{ it\\.component == BatteryComponent\\.RIGHT \\}\\?\\.level!!\\s*\\)")
service, remaining = re.subn(unsafe, "safeVisibleBatteryLevel()", service)
if remaining != 3:
    raise RuntimeError(f"Expected 3 additional one-bud takeover crash paths, found {remaining}")
helper = '''    private fun safeVisibleBatteryLevel(): Int {
        return batteryNotification.getBattery()
            .filter { it.component == BatteryComponent.LEFT || it.component == BatteryComponent.RIGHT }
            .map { it.level }
            .minOrNull() ?: 0
    }

'''
needle = '    fun isBluetoothSocketExempted(): Boolean {'
assert service.count(needle) == 1
service = service.replace(needle, helper + needle, 1)
service_path.write_text(service, encoding="utf-8")
print("FIX_OK", "three_remaining_single_ear_takeover_null_guards")

# Fail closed if an actual one-ear crash vector still exists.
svc = (base / "services/AirPodsService.kt").read_text(encoding="utf-8")
assert '?.level!!' not in svc and 'getBattery()[1]' not in svc
print("STATIC_SOURCE_GATE=PASS")
