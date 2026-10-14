#!/usr/bin/env python3
"""GPL source-only Korean first-run and Compose lifecycle repair (no APK patch)."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

root=Path(sys.argv[1])
base=root/"android/app/src/main/java/me/kavishdevar/librepods"
def replace(path, old, new):
    p=base/path
    s=p.read_text(encoding="utf-8")
    if s.count(old)!=1: raise RuntimeError(f"{path} anchor count {s.count(old)} for {old[:75]}")
    p.write_text(s.replace(old,new,1),encoding="utf-8")
    print("PATCH_OK", path)

# Binding Android foreground service in @Composable body called it on EVERY
# recomposition. Change to a one-shot LaunchedEffect driven by observable state.
p="MainActivity.kt"
replace(p,'val onboardingComplete = sharedPreferences.getBoolean("onboarding_complete", false)','val onboardingComplete = remember { mutableStateOf(sharedPreferences.getBoolean("onboarding_complete", false)) }')
replace(p,'    if (onboardingComplete) {\n        bindService()\n    }','    LaunchedEffect(onboardingComplete.value) {\n        if (onboardingComplete.value) bindService()\n    }')
replace(p,'showOnboarding = !onboardingComplete,','showOnboarding = !onboardingComplete.value,')
replace(p,'sharedPreferences.edit { putBoolean("onboarding_complete", true) }\n            bindService()','sharedPreferences.edit { putBoolean("onboarding_complete", true) }\n            onboardingComplete.value = true')
replace(p,'''    override fun onStop() {
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
    }
''',"")
replace(p,'        context.startForegroundService(Intent(context, AirPodsService::class.java))', '''        if (android.os.Build.VERSION.SDK_INT >= 31 &&
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("MainActivity", "Bluetooth permission missing, service not started")
            return
        }
        try {
            context.startForegroundService(Intent(context, AirPodsService::class.java))
        } catch (e: RuntimeException) {
            Log.e("MainActivity", "AirPods service start failed", e)
            return
        }''')
replace(p,'''        context.bindService(
            Intent(context, AirPodsService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )''', '''        try {
            if (!context.bindService(
                    Intent(context, AirPodsService::class.java),
                    serviceConnection,
                    Context.BIND_AUTO_CREATE
                )) Log.e("MainActivity", "AirPods service bind returned false")
        } catch (e: RuntimeException) {
            Log.e("MainActivity", "AirPods service bind failed", e)
        }''')

# The English June-2026 LibrePods sponsorship policy is neither a Korean UI
# nor an appropriate mandatory consent flow for this independent FOSS app.
# Keep true first-run permission notice and GPL provenance, remove fake consent.
privacy = '''package me.kavishdevar.librepods.presentation.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyPolicyPage(onForward: () -> Unit) {
    Box(modifier = Modifier.background(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(42.dp)
    )) {
        Column(modifier = Modifier.padding(horizontal=16.dp)
            .verticalScroll(rememberScrollState()),
            verticalArrangement=Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier=Modifier.height(24.dp))
            Text("내 에어팟 5 이용 안내",style=MaterialTheme.typography.titleLarge)
            Text("이 앱은 LibrePods 오픈소스(GPL-3.0)를 바탕으로 만든 개인용 한국어 앱입니다. 앱을 사용하는 데 결제나 후원이 필요하지 않습니다.",style=MaterialTheme.typography.bodyMedium)
            Text("기기 권한",style=MaterialTheme.typography.titleMedium)
            Text("에어팟과 통신하려면 Android의 '근처 기기/블루투스' 권한이 필요합니다. 알림, 통화, 다른 앱 위에 표시 권한은 해당 기능을 사용할 때 선택할 수 있습니다.",style=MaterialTheme.typography.bodyMedium)
            Text("정보 처리",style=MaterialTheme.typography.titleMedium)
            Text("에어팟 연결 정보와 설정은 휴대전화에서 처리됩니다. 이 개인용 빌드에는 별도 광고·분석·추적 SDK를 추가하지 않았습니다. 사용자가 직접 GitHub나 이메일 등 외부 서비스를 열면 해당 서비스의 정책이 적용됩니다.",style=MaterialTheme.typography.bodyMedium)
            Text("오픈소스 안내",style=MaterialTheme.typography.titleMedium)
            Text("원본 LibrePods 개발자와 오픈소스 기여자의 저작권 및 GPL 라이선스는 그대로 유지됩니다. 다음 버튼은 개인정보 제공 동의가 아니라 권한 설정 화면으로 이동하는 버튼입니다.",style=MaterialTheme.typography.bodyMedium)
            Button(onClick=onForward, modifier=Modifier.fillMaxWidth()) {
                Text("권한 설정으로",style=MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier=Modifier.height(24.dp))
        }
    }
}
'''
policy=base/"presentation/screens/onboarding/PrivacyPolicyPage.kt"
if "Last updated: 20 June 2026" not in policy.read_text(encoding="utf-8"):raise RuntimeError("unexpected privacy policy source")
policy.write_text(privacy, encoding="utf-8")
print("PATCH_OK Korean personal use information, no third-party consent")

permissions=base/"presentation/screens/onboarding/PermissionsPage.kt"
s=permissions.read_text(encoding="utf-8")
translations={
"Required Permissions":"필수 권한",
"Bluetooth":"블루투스",
"Required to communicate with AirPods":"에어팟 연결과 제어에 필요합니다.",
"Optional Permissions":"선택 권한",
"Notifications":"알림",
"Show battery status":"배터리 상태를 알림으로 표시합니다.",
"Phone":"전화",
"Respond to phone calls with head gestures":"머리 제스처로 전화를 받거나 거절할 때 사용합니다.",
"Display over other apps":"다른 앱 위에 표시",
"Show popups when AirPods are nearby or audio switches to them.":"에어팟 연결 시 팝업을 표시합니다.",
"Grant all":"모든 권한 요청",
}
for before,after in translations.items():
    old='"'+before+'"'
    if old not in s: raise RuntimeError("Missing permission string "+before)
    s=s.replace(old,'"'+after+'"')
s=s.replace("import androidx.compose.runtime.remember\n","import androidx.compose.runtime.remember\nimport androidx.compose.runtime.setValue\n")
s=s.replace("    var grantingAll = false\n","    var grantingAll by remember { mutableStateOf(false) }\n")
permissions.write_text(s,encoding="utf-8")
print("PATCH_OK Korean permissions + remembered grant sequence")

replace("presentation/screens/onboarding/OnboardingScreen.kt",'text = "Welcome to",','text = "환영합니다",')
strings=root/"android/app/src/main/res/values-ko/strings.xml"
s=strings.read_text("utf-8")
for old,new in [
('name="privacy_policy">개인정보 처리 안내','name="privacy_policy">앱 이용 안내'),
('name="i_agree">동의','name="i_agree">계속')]:
    if s.count(old)!=1:raise RuntimeError("Missing locale anchor "+old)
    s=s.replace(old,new)
strings.write_text(s,encoding="utf-8")
ET.parse(strings)
print("PATCH_OK Korean locale XML parsed")

gradle=root/"android/app/build.gradle.kts"
s=gradle.read_text("utf-8")
for old,new in [('versionCode = 10002','versionCode = 10003'),
                ('versionName = "1.0.2-source-clean"','versionName = "1.0.3-ko-personal"')]:
    if s.count(old)!=1:raise RuntimeError("Missing version anchor "+old)
    s=s.replace(old,new)
gradle.write_text(s,encoding="utf-8")
print("PATCH_OK version 10003 source-only")
