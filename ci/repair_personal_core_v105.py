#!/usr/bin/env python3
"""Source-preserving v1.0.5 repair on top of the compiled v1.0.4 guard.
Only lifecycle/ear-state/empty protocol data and user-facing strings.
"""
from pathlib import Path
import sys
root=Path(sys.argv[1])
base=root/"android/app/src/main/java/me/kavishdevar/librepods"
def replace(path, old, new, why):
    p=base/path
    s=p.read_text("utf-8")
    count=s.count(old)
    if count != 1:
        raise RuntimeError(f"{why}: count={count}")
    p.write_text(s.replace(old,new,1),"utf-8")
    print("PERSONAL_V105_PATCH=PASS",why)

# Service's one-time on-boarding attempt was never retried after Bluetooth
# permission was granted and its Compose LaunchedEffect had already returned.
replace("MainActivity.kt",
    '    fun bindService() {\n        if (android.os.Build.VERSION.SDK_INT >= 31',
    '    val bindingAttempted = remember { mutableStateOf(false) }\n    fun bindService() {\n        if (bindingAttempted.value) return\n        if (android.os.Build.VERSION.SDK_INT >= 31',
    "no_duplicate_bind")
replace("MainActivity.kt",
    '        try {\n            context.startForegroundService(Intent(context, AirPodsService::class.java))',
    '        bindingAttempted.value = true\n        try {\n            context.startForegroundService(Intent(context, AirPodsService::class.java))',
    "record_one_bind_attempt")
replace("MainActivity.kt",
    'Log.e("MainActivity", "AirPods service start failed", e)\n            return',
    'Log.e("MainActivity", "AirPods service start failed", e)\n            bindingAttempted.value = false\n            return',
    "allow_retry_after_start_failure")
replace("MainActivity.kt",
    '''                )) Log.e("MainActivity", "AirPods service bind returned false")''',
    '''                )) {
                    bindingAttempted.value = false
                    Log.e("MainActivity", "AirPods service bind returned false")
                }''',
    "retry_failed_bind")
replace("MainActivity.kt",
    '''Log.e("MainActivity", "AirPods service bind failed", e)
        }''',
    '''Log.e("MainActivity", "AirPods service bind failed", e)
            bindingAttempted.value = false
        }''',
    "retry_bind_exception")
replace("MainActivity.kt",
    '''    LaunchedEffect(onboardingComplete.value) {
        if (onboardingComplete.value) bindService()
    }

    NavigationRoot(''',
    '''    LaunchedEffect(onboardingComplete.value) {
        if (onboardingComplete.value) bindService()
    }
    // The user can grant NEARBY_DEVICES in Android settings AFTER the first
    // on-boarding attempt. React to Activity RESUME, never per recomposition.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, onboardingComplete.value) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME &&
                onboardingComplete.value && !bindingAttempted.value) bindService()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    NavigationRoot(''',
    "permission_grant_reconnect_when_resumed")

replace("services/AirPodsService.kt",
    '''            inEar = data[0] == 0x00.toByte() && data[1] == 0x00.toByte()''',
    '''            // Either AirPod worn is an in-ear state. Requiring both buds
            // incorrectly pauses audio whenever just one AirPod is in use.
            inEar = data[0] == 0x00.toByte() || data[1] == 0x00.toByte()''',
    "one_ear_wearing_is_playable")
replace("presentation/viewmodel/AirPodsViewModel.kt",
    '''ancMode = controlRepo.getValue(ControlCommandIdentifiers.LISTENING_MODE)?.get(0)?.toInt() ?: 1,''',
    '''ancMode = controlRepo.getValue(ControlCommandIdentifiers.LISTENING_MODE)?.getOrNull(0)?.toInt() ?: 1,''',
    "ignore_empty_listening_mode_packet")

# Avoid displaying a static AirPods image or 0% as proof of a connected live
# AACP session. Real control methods remain the original LibrePods code.
p=base/"MainActivity.kt"
s=p.read_text("utf-8")
assert "bindingAttempted.value" in s and "LocalLifecycleOwner" in s

gradle=root/"android/app/build.gradle.kts"
g=gradle.read_text("utf-8")
assert g.count("versionCode = 10004")==1
assert g.count('versionName = "1.0.4-personal-guard"')==1
g=g.replace("versionCode = 10004","versionCode = 10005",1)
g=g.replace('versionName = "1.0.4-personal-guard"', 'versionName = "1.0.5-ear-resume-connection"',1)
gradle.write_text(g,"utf-8")
print("PERSONAL_V105_SOURCE_GATE=PASS")
