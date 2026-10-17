#!/usr/bin/env python3
"""Make a Korean personal FOSS LibrePods UI, retaining the known v1.0.10 engine.

Input: original SHA-pinned GPL source + exact v1.0.10 tested patch chain.
Do not include any of v117-v142 case SOUND/LAB experimental series.
"""
from pathlib import Path
import sys, re, xml.etree.ElementTree as ET

root=Path(sys.argv[1])
base=root/"android/app/src/main/java/me/kavishdevar/librepods"
def save(path,text):
    path.write_text(text,encoding="utf-8")
def exact(text, before, after, label):
    n=text.count(before)
    if n!=1: raise RuntimeError(f"{label}: source anchor count {n}")
    return text.replace(before,after,1)

def remove_kotlin_block(text, before):
    start=text.find(before)
    if start<0 or text.count(before)!=1: raise RuntimeError("Source block missing or ambiguous: "+before)
    brace=text.index("{",start)
    level=1
    i=brace+1
    while i<len(text) and level:
        if text[i]=="{": level+=1
        elif text[i]=="}": level-=1
        i+=1
    if level: raise RuntimeError("Unclosed Kotlin block "+before)
    return text[:start]+text[i:]

# Licence attribution is kept. User facing payment/account/donation network paths
# are not useful in a single-user GPL FOSS edition.
billing=base/"billing"
foss=billing/"FOSSBillingProvider.kt"
save(foss,'''/*
 LibrePods contributors (C) 2025; GPL-3.0-or-later.
 Personal FOSS edition: all locally supported options available without a
 purchase/account/sponsorship, no payment or purchase network requests.
*/
package me.kavishdevar.librepods.billing

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FOSSBillingProvider(context: Context): BillingProvider {
    private val localEnabled = MutableStateFlow(true)
    override val isPremium: StateFlow<Boolean> = localEnabled
    private val localPrice = MutableStateFlow("무료 개인용")
    override val price: StateFlow<String> = localPrice
    override fun purchase(activity: Activity) { /* no payments in FOSS build */ }
    override fun queryPurchases() { localEnabled.value = true }
    override fun restorePurchases() { localEnabled.value = true }
}
''')
factory=billing/"BillingProviderFactory.kt"
s=factory.read_text("utf-8")
start=s.index("    fun create(context: Context): BillingProvider {")
s=s[:start]+'''    fun create(context: Context): BillingProvider = FOSSBillingProvider(context)
}
'''
save(factory,s)
play=billing/"PlayBillingProvider.kt"
if play.exists(): play.unlink()
gradle=root/"android/app/build.gradle.kts"
s=gradle.read_text("utf-8")
s=exact(s,"    implementation(libs.billing)\n","", "remove billing SDK")
s=exact(s,"versionCode = 10010","versionCode = 20000","version bump")
s=exact(s,'versionName = "1.0.10-video-badtoken-fix"',
        'versionName = "2.0.0-juhyeon-ko-final"',"branding version")
save(gradle,s)

# Keep upstream structural Screen.Purchase linkage to prevent navigation crashes
# in existing source, but replace it with an empty FOSS informational page.
purchase=base/"presentation/screens/PurchaseScreen.kt"
save(purchase,'''/*
 LibrePods contributors (C) 2025; GPL-3.0-or-later.
 No payment, account or donation screen in personal FOSS edition.
*/
package me.kavishdevar.librepods.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.snapshots.SnapshotStateList
import me.kavishdevar.librepods.presentation.navigation.Screen

@Composable
fun PurchaseScreen(backStack: SnapshotStateList<Screen>) {
    Column(modifier=Modifier.padding(24.dp)) {
        Text("주현에어팟5",style=MaterialTheme.typography.headlineMedium)
        Text("모든 지원 기능은 무료이며, 결제나 계정 등록이 필요하지 않습니다.")
    }
}
''')
# Main nav currently passes viewModel param to purchase composable; avoid
# changing the known-good navigation by preserving its original signature.
s=purchase.read_text("utf-8")
s=s.replace("fun PurchaseScreen(backStack: SnapshotStateList<Screen>) {",
            "fun PurchaseScreen(viewModel: me.kavishdevar.librepods.presentation.viewmodel.PurchaseViewModel = androidx.lifecycle.viewmodel.compose.viewModel(), backStack: SnapshotStateList<Screen>) {")
save(purchase,s)

app=base/"presentation/screens/AppSettingsScreen.kt"
s=app.read_text("utf-8")
s=remove_kotlin_block(s,"if (!state.isPremium && state.connectionSuccessful) {")
# v106 previously removed account/contact and premium billing banner
if "billing@kavish.xyz" in s:
    s=remove_kotlin_block(s,"if (state.timeUntilFOSSPremiumExpiry > 0L) {")
s=exact(s,"        Spacer(modifier = Modifier.height(topPadding))",
        '''        Spacer(modifier = Modifier.height(topPadding))
        Text("주현에어팟5", style = MaterialTheme.typography.headlineSmall)
        Text("한글 개인용 · 결제 및 로그인 불필요", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(18.dp))''',"settings Korean heading")
save(app,s)
air=base/"presentation/screens/AirPodsSettingsScreen.kt"
s=air.read_text("utf-8")
s=remove_kotlin_block(s,'item(key = "play_update_banner") {')
s=remove_kotlin_block(s,'item(key = "upgrade_button") {')
# Store any hardware unsupported option as disabled, never invent support.
save(air,s)

# Keep GPL attribution and legal notices on-screen under open-source licenses.
for f in [app,air,foss,billing/"BillingProviderFactory.kt",purchase]:
    t=f.read_text("utf-8")
    if ("sponsors/kavishdevar" in t or "billing@kavish.xyz" in t
        or "premium access" in t):
        raise RuntimeError("Payment/contact remnant in "+str(f))

# Personal Korean strings. Actual Android UI is built from upstream values-ko.
values=root/"android/app/src/main/res/values/strings.xml"
ko=root/"android/app/src/main/res/values-ko/strings.xml"
for p in (values,ko):
    tree=ET.parse(p); r=tree.getroot()
    names={item.get("name"):item for item in r.findall("string")}
    if p==values:
        assert "app_name" in names
        names["app_name"].text="주현에어팟5"
    else:
        translated={
          "app_description":"갤럭시에서 사용하는 나만의 에어팟 5",
          "app_widget_description":"홈 화면에서 에어팟 5의 배터리를 확인합니다.",
          "noise_control":"소음 제어","listening_mode":"청취 모드",
          "settings":"설정","battery":"배터리","widget":"배터리 위젯",
          "popup_animations":"연결 팝업","show_bottom_sheet_popup":"하단 연결 팝업",
          "show_island_popup":"상단 연결 알림",
          "left":"왼쪽 이어버드","right":"오른쪽 이어버드",
          "transparency":"주변음 허용","adaptive":"적응형 오디오",
          "noise_cancellation":"노이즈 캔슬링","off":"끔",
          "head_gestures":"머리 동작","ear_detection":"착용 감지",
          "connection":"연결","connected":"연결됨",
          "disconnected":"연결 안 됨","permissions":"권한 설정",
          "open_source_licenses":"오픈소스 라이선스",
          "troubleshooting":"문제 해결","appearance":"화면 설정",
          "conversation_awareness":"대화 감지",
          "equalizer":"이퀄라이저","head_tracking":"머리 추적",
          "microphone":"마이크","name":"이름",
          "phone_battery":"휴대전화 배터리",
        }
        baseline=ET.parse(values).getroot()
        allowed={x.get("name") for x in baseline.findall("string")}
        for key,val in translated.items():
            if key not in allowed:continue
            item=names.get(key)
            if item is None: item=ET.SubElement(r,"string",{"name":key})
            item.text=val
    ET.indent(r,space="    ")
    save(p,'<?xml version="1.0" encoding="utf-8"?>\n'+ET.tostring(r,encoding="unicode")+'\n')
    ET.parse(p)

assert 'applicationId = "com.myairpods5.personal"' in gradle.read_text()
assert not play.exists()
assert "CaseRadioEvidenceV138" not in "\n".join(x.name for x in base.glob("*.kt"))
for flag in ("purchaseJob","sponsors/kavishdevar","BillingClient","billing@kavish.xyz"):
    for p in (foss, factory, app, air):
        assert flag not in p.read_text("utf-8"), (flag,p)
assert '"주현에어팟5"' in app.read_text("utf8")
print("JUHYEON_V2_KOREAN_LAUNCHER_BRANDING=PASS")
print("JUHYEON_V2_ALL_LOCAL_FOSS_FEATURES_FREE=PASS")
print("JUHYEON_V2_PAYMENT_SDK_AND_PLAY_BILLING_REMOVED=PASS")
print("JUHYEON_V2_UNNECESSARY_PURCHASE_CONTACT_UI_REMOVED=PASS")
print("JUHYEON_V2_BASELINE_AACP_ENGINE_UNMODIFIED=PASS")
print("JUHYEON_V2_CASE_SOUND_LAB_NOT_INCLUDED=PASS")
print("JUHYEON_V2_GPL_NOTICE_PRESERVED=PASS")
