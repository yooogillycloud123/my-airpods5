#!/usr/bin/env python3
"""Source-only v1.0.3 onboarding Korean and binding lifecycle repairs.
GPL-3.0-or-later derivative of LibrePods. No APK/DEX binary modification."""
from pathlib import Path
import re
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: python3 repair_ko_onboarding.py input/app_source")
root = Path(sys.argv[1])
src = root / "android/app/src/main/java/me/kavishdevar/librepods"

def patch(path, changes):
    data=path.read_text(encoding="utf-8")
    for before, after, label in changes:
        n=data.count(before)
        if n!=1:
            raise RuntimeError(f"{path.name} {label} anchor count={n}")
        data=data.replace(before,after,1)
    path.write_text(data,encoding="utf-8")
    print("PATCHED",path.name,len(changes))

p=src/"presentation/screens/onboarding/OnboardingScreen.kt"
s=p.read_text(encoding="utf-8")
s=s.replace("itemCount = { 4 }", "itemCount = { 3 }")
s=s.replace("""        stringResource(R.string.privacy_policy),
        stringResource(R.string.not_supported),
        stringResource(R.string.permissions),""","""        stringResource(R.string.not_supported),
        stringResource(R.string.permissions),""")
s=s.replace("if (isSupported && state.currentItem == 3) 1 else state.currentItem - 1","if (isSupported && state.currentItem == 2) 0 else state.currentItem - 1")
s=s.replace('text = "Welcome to"','text = "환영합니다"')
s=s.replace('contentDescription = "forward"','contentDescription = "다음"')
s=s.replace("state.animateScrollToItem(1)\n                                                }","state.animateScrollToItem(if (isSupported) 2 else 1)\n                                                }")
start=s.index("                            1 -> {\n                                PrivacyPolicyPage(")
end=s.index("\n                        }\n                    }\n                }\n            }",start)
s=s[:start]+"""                            1 -> {
                                NotSupportedPage(
                                    bypassCompatibilityCheck = {
                                        bypassDeviceCheck(sharedPreferences)
                                    }
                                )
                            }
                            2 -> {
                                PermissionsPage(
                                    onBackward = {
                                        animationScope.launch {
                                            state.animateScrollToItem(if (isSupported) 0 else 1)
                                        }
                                    },
                                    onForward = onOnboardingComplete
                                )
                            }"""+s[end:]
assert "PrivacyPolicyPage(" not in s
assert "3 -> {" not in s
p.write_text(s,encoding="utf-8")
print("PATCHED OnboardingScreen.kt, removed forced old LibrePods privacy-consent gate")

p=src/"presentation/screens/onboarding/PermissionsPage.kt"
patch(p, [
 ('title = "Required Permissions"', 'title = "필수 권한"', "required"),
 ('name = "Bluetooth"', 'name = "블루투스"', "bluetooth name"),
 ('description = "Required to communicate with AirPods"', 'description = "에어팟 연결과 제어를 위해 필요합니다"', "bluetooth desc"),
 ('title = "Optional Permissions"', 'title = "선택 권한"', "optional"),
 ('name = "Notifications"', 'name = "알림"', "notif"),
 ('description = "Show battery status"', 'description = "배터리 상태를 알림으로 표시합니다"', "notif desc"),
 ('name = "Phone"', 'name = "전화"', "phone"),
 ('description = "Respond to phone calls with head gestures"', 'description = "머리 동작으로 전화를 받거나 거절합니다"', "phone desc"),
 ('name = "Display over other apps"', 'name = "다른 앱 위에 표시"', "overlay"),
 ('description = "Show popups when AirPods are nearby or audio switches to them."', 'description = "에어팟 연결 또는 오디오 전환 팝업을 표시합니다"', "overlay desc"),
 ('text = "Grant all"', 'text = "모든 권한 요청"', "grant all"),
 ('contentDescription = "backward"', 'contentDescription = "이전"', "previous"),
 ('contentDescription = "forward"', 'contentDescription = "다음"', "next"),
 ])

p=src/"MainActivity.kt"
data=p.read_text(encoding="utf-8")
old="""    override fun onStop() {
        try {
            unbindService(serviceConnection)
            Log.d("MainActivity", "Unbound service")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error while unbinding service: $e")
        }
        try {
            unregisterReceiver(connectionStatusReceiver)
            Log.d("MainActivity", "Unregistered receiver")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error while unregistering receiver: $e")
        }
        super.onStop()
    }"""
assert data.count(old)==1, "onStop original missing"
data=data.replace(old,"""    override fun onStop() {
        // Stay bound while activity is backgrounded. OnDestroy performs cleanup.
        // Premature unbinding led to lost service references after navigating away.
        super.onStop()
    }""")
old="""    fun bindService() {
        context.startForegroundService(Intent(context, AirPodsService::class.java))"""
assert data.count(old)==1
data=data.replace(old,"""    val serviceStarted = remember { mutableStateOf(false) }
    fun bindService() {
        // Never perform an unguarded bind during recomposition.
        if (serviceStarted.value) return
        serviceStarted.value = true
        try {
            context.startForegroundService(Intent(context, AirPodsService::class.java))""")
old="""        context.bindService(
            Intent(context, AirPodsService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    if (onboardingComplete) {
        bindService()
    }"""
assert data.count(old)==1
data=data.replace(old,"""            val bound = context.bindService(
                Intent(context, AirPodsService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
            if (!bound) {
                serviceStarted.value = false
                Log.e("MainActivity", "AirPods service bind returned false")
            }
        } catch (e: Exception) {
            serviceStarted.value = false
            Log.e("MainActivity", "Cannot start or bind AirPods service", e)
        }
    }

    LaunchedEffect(onboardingComplete) {
        if (onboardingComplete) bindService()
    }""")
p.write_text(data,encoding="utf-8")
print("PATCHED MainActivity.kt; no service bind during Compose recomposition, keep bound when backgrounded")

p=src/"LibrePodsApplication.kt"
data=p.read_text(encoding="utf-8")
old="""    override fun onCreate() {
        XposedServiceHelper.registerListener(this)"""
assert data.count(old)==1
data=data.replace(old,"""    override fun onCreate() {
        // Preserve the last uncaught failure for later diagnosis without network access.
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                getSharedPreferences("myairpods5_diagnostics", MODE_PRIVATE)
                    .edit()
                    .putString("last_error", android.util.Log.getStackTraceString(error))
                    .putLong("last_error_time", System.currentTimeMillis())
                    .apply()
            } catch (_: Exception) {
                // Do not interfere with Android's original fatal-error handler.
            }
            previousHandler?.uncaughtException(thread, error)
        }
        XposedServiceHelper.registerListener(this)""")
p.write_text(data,encoding="utf-8")
print("PATCHED LibrePodsApplication.kt: local-only fatal stacktrace capture")

# Inspect FOSS build only; main permissions kept so working controls survive.
gradle=root/"android/app/build.gradle.kts"
data=gradle.read_text(encoding="utf-8")
assert 'applicationId = "com.myairpods5.personal"' in data
assert 'versionCode = 10002' in data
data=data.replace('versionCode = 10002','versionCode = 10003',1).replace('versionName = "1.0.2-source-clean"','versionName = "1.0.3-ko-crashfix"',1)
gradle.write_text(data,encoding="utf-8")
print("VERSION 1.0.3")
