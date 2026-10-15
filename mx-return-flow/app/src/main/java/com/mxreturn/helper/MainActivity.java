package com.mxreturn.helper;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQ_BT = 100;
    private static final String ROLE_HOME = "집 PC";
    private static final String ROLE_PHONE = "스마트폰";
    private static final String ROLE_WORK = "회사 PC";
    private static final String ROLE_NONE = "사용안함";

    private HidHostBridge bridge;
    private TextView status;
    private Spinner[] mouseRole = new Spinner[3];
    private Spinner[] keyboardRole = new Spinner[3];
    private Spinner autoRoleSpinner;
    private Switch autoSwitch;
    private Switch actionRingSwitch;
    private Switch chatGptSwitch;
    private Spinner[] ohoKeyboardSlot = new Spinner[3];
    private Spinner[] ohoMouseSlot = new Spinner[3];
    private Switch flowSwitch;
    private Spinner flowTopSpinner;
    private Spinner flowBottomSpinner;
    private Spinner flowLeftSpinner;
    private Spinner flowRightSpinner;
    private Spinner flowThicknessSpinner;
    private Spinner flowDwellSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initDefaults();
        buildUi();
        ensureBluetoothPermissionAndOpen();
    }

    private void initDefaults() {
        SharedPreferences p = getSharedPreferences("cfg", MODE_PRIVATE);
        if (!p.contains("kb_role_0")) p.edit()
                .putString("kb_role_0", ROLE_HOME)
                .putString("kb_role_1", ROLE_PHONE)
                .putString("kb_role_2", ROLE_WORK)
                .putString("mouse_role_0", ROLE_WORK)
                .putString("mouse_role_1", ROLE_PHONE)
                .putString("mouse_role_2", ROLE_HOME)
                .putString("auto_follow_role", ROLE_WORK)
                .putBoolean("auto", false)
                .putBoolean("mouse_return_gesture", true)
                .putBoolean("global_return_hotkey", true)
                .apply();

        if (!p.contains("oho_kb_0")) {
            p.edit()
                    .putInt("oho_kb_0", 2)
                    .putInt("oho_mouse_0", 0)
                    .putInt("oho_kb_1", 1)
                    .putInt("oho_mouse_1", 1)
                    .putInt("oho_kb_2", 0)
                    .putInt("oho_mouse_2", 2)
                    .apply();
        }

        if (!p.contains("flow_enabled")) {
            p.edit()
                    .putBoolean("flow_enabled", false)
                    .putInt("flow_top_slot", 0)
                    .putInt("flow_bottom_slot", -1)
                    .putInt("flow_left_slot", -1)
                    .putInt("flow_right_slot", -1)
                    .putInt("flow_edge_dp", 2)
                    .putInt("flow_dwell_ms", 180)
                    .putInt("flow_cooldown_ms", 1200)
                    .apply();
        }
    }

    private void buildUi() {
        int pad = dp(18);
        SharedPreferences p = getSharedPreferences("cfg", MODE_PRIVATE);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("MX Return v0.4.0 FLOW");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        body.addView(title);

        TextView intro = new TextView(this);
        intro.setText("\n슬롯 번호가 아니라 '역할'을 기준으로 키보드·마우스가 함께 움직이도록 설정합니다.\n" +
                "현재 연결된 장치의 HID++ 0x1814 실제 host도 진단에서 확인합니다.");
        intro.setTextSize(15);
        body.addView(intro);

        body.addView(section("키보드 슬롯 역할"));
        String[] roleItems = {ROLE_HOME, ROLE_PHONE, ROLE_WORK, ROLE_NONE};
        for (int i = 0; i < 3; i++) {
            keyboardRole[i] = addRoleRow(body, "키보드 슬롯 " + (i + 1), roleItems,
                    p.getString("kb_role_" + i, defaultKbRole(i)));
        }

        body.addView(section("마우스 슬롯 역할"));
        for (int i = 0; i < 3; i++) {
            mouseRole[i] = addRoleRow(body, "마우스 슬롯 " + (i + 1), roleItems,
                    p.getString("mouse_role_" + i, defaultMouseRole(i)));
        }

        Button save = new Button(this);
        save.setText("슬롯 역할 저장");
        save.setOnClickListener(v -> {
            saveMappings();
            refreshDiagnostics();
            toast("슬롯 역할 설정을 저장했습니다.");
        });
        body.addView(save);

        body.addView(section("OHO+ 바로가기 슬롯 매핑"));
        TextView ohoHint = new TextView(this);
        ohoHint.setText("One Hand Operation+에서는 MX Return - 슬롯 1 / 2 / 3만 선택하면 됩니다.\n" +
                "각 논리 슬롯이 실제 키보드 채널과 마우스 채널 몇 번을 사용할지 여기서 바꿉니다.");
        ohoHint.setTextSize(14);
        body.addView(ohoHint);

        String[] channelItems = {"1", "2", "3"};
        for (int i = 0; i < 3; i++) {
            ohoKeyboardSlot[i] = addRoleRow(body,
                    "슬롯 " + (i + 1) + " - 키보드 채널",
                    channelItems,
                    String.valueOf(p.getInt("oho_kb_" + i, defaultOhoKeyboard(i)) + 1));
            ohoMouseSlot[i] = addRoleRow(body,
                    "슬롯 " + (i + 1) + " - 마우스 채널",
                    channelItems,
                    String.valueOf(p.getInt("oho_mouse_" + i, defaultOhoMouse(i)) + 1));
        }

        Button saveOho = new Button(this);
        saveOho.setText("OHO+ 슬롯 매핑 저장");
        saveOho.setOnClickListener(v -> {
            saveMappings();
            toast("OHO+ 슬롯 매핑을 저장했습니다.");
        });
        body.addView(saveOho);

        body.addView(section("MX Return FLOW"));
        TextView flowHint = new TextView(this);
        flowHint.setText("마우스를 지정한 화면 끝으로 밀면 키보드 + 마우스를 함께 슬롯으로 보냅니다.\n" +
                "회사 기본 배치: 스마트폰 위쪽 → 슬롯 1(회사 PC). OHO+는 비상용으로 그대로 남습니다.");
        flowHint.setTextSize(14);
        body.addView(flowHint);

        String[] flowItems = {"사용안함", "슬롯 1", "슬롯 2", "슬롯 3"};
        flowTopSpinner = addRoleRow(body, "스마트폰 위쪽", flowItems,
                flowLabel(p.getInt("flow_top_slot", 0)));
        flowBottomSpinner = addRoleRow(body, "스마트폰 아래쪽", flowItems,
                flowLabel(p.getInt("flow_bottom_slot", -1)));
        flowLeftSpinner = addRoleRow(body, "스마트폰 왼쪽", flowItems,
                flowLabel(p.getInt("flow_left_slot", -1)));
        flowRightSpinner = addRoleRow(body, "스마트폰 오른쪽", flowItems,
                flowLabel(p.getInt("flow_right_slot", -1)));

        flowThicknessSpinner = addRoleRow(body, "감지띠 두께", new String[]{"1", "2", "3", "4", "6", "8"},
                String.valueOf(p.getInt("flow_edge_dp", 2)));
        flowDwellSpinner = addRoleRow(body, "끝에서 유지(ms)", new String[]{"100", "140", "180", "220", "300", "400"},
                String.valueOf(p.getInt("flow_dwell_ms", 180)));

        Button overlayPermission = new Button(this);
        overlayPermission.setText("FLOW 화면 가장자리 권한 열기 (최초 1회)");
        overlayPermission.setOnClickListener(v -> openOverlaySettings());
        body.addView(overlayPermission);

        Button saveFlow = new Button(this);
        saveFlow.setText("FLOW 배치 저장 / 적용");
        saveFlow.setOnClickListener(v -> {
            saveMappings();
            refreshFlowService();
            toast("FLOW 화면 배치를 적용했습니다.");
        });
        body.addView(saveFlow);

        flowSwitch = new Switch(this);
        flowSwitch.setText("\n마우스 이동만으로 자동 전환");
        flowSwitch.setTextSize(16);
        flowSwitch.setChecked(p.getBoolean("flow_enabled", false));
        flowSwitch.setOnCheckedChangeListener((v, checked) -> {
            saveMappings();
            p.edit().putBoolean("flow_enabled", checked).apply();
            if (checked) {
                if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                    toast("FLOW 사용을 위해 '다른 앱 위에 표시' 권한을 한 번 허용해 주세요.");
                    openOverlaySettings();
                } else {
                    startEdgeFlowService();
                }
            } else {
                stopService(new Intent(this, EdgeFlowService.class));
            }
        });
        body.addView(flowSwitch);

        body.addView(section("자동추종 기준 역할"));
        autoRoleSpinner = new Spinner(this);
        ArrayAdapter<String> autoAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{ROLE_WORK, ROLE_HOME, ROLE_PHONE});
        autoRoleSpinner.setAdapter(autoAdapter);
        autoRoleSpinner.setSelection(roleIndex(p.getString("auto_follow_role", ROLE_WORK)));
        body.addView(autoRoleSpinner);

        TextView autoHint = new TextView(this);
        autoHint.setText("한 장치의 host 변화가 확인되면 다른 장치도 이 역할의 슬롯으로 맞춥니다.\n" +
                "기본값은 회사 PC입니다.");
        autoHint.setTextSize(13);
        body.addView(autoHint);

        body.addView(section("현재 host 확인"));
        Button probe = new Button(this);
        probe.setText("0x1814 실제 host 다시 확인");
        probe.setOnClickListener(v -> probeHosts());
        body.addView(probe);

        Button workReturn = new Button(this);
        workReturn.setText("역할 기준: 회사 PC로 키보드 + 마우스 복귀");
        workReturn.setOnClickListener(v -> {
            saveMappings();
            switchBothToRole(ROLE_WORK);
        });
        body.addView(workReturn);

        autoSwitch = new Switch(this);
        autoSwitch.setText("\n기존 자동복귀 + 슬롯 자동추종");
        autoSwitch.setTextSize(16);
        autoSwitch.setChecked(p.getBoolean("auto", false));
        autoSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            saveMappings();
            p.edit().putBoolean("auto", checked).apply();
            if (checked) startWatchService();
            else stopService(new Intent(this, WatchService.class));
        });
        body.addView(autoSwitch);

        actionRingSwitch = new Switch(this);
        actionRingSwitch.setText("\nOHO+ SAFE 모드: 마우스 클릭/휠 직접 캡처 안 함");
        actionRingSwitch.setTextSize(16);
        actionRingSwitch.setChecked(false);
        actionRingSwitch.setEnabled(false);
        actionRingSwitch.setOnCheckedChangeListener((v, checked) -> { });
        body.addView(actionRingSwitch);

        Button actionAccessibility = new Button(this);
        actionAccessibility.setText("키보드 Ctrl+Alt+P 접근성 설정 열기 (선택)");
        actionAccessibility.setOnClickListener(v -> openAccessibilitySettings());
        body.addView(actionAccessibility);

        TextView routineInfo = new TextView(this);
        routineInfo.setText("\n삼성 루틴: MX Return [자동 복귀 켜기] / [자동 복귀 끄기] 동작을 그대로 사용할 수 있습니다.\n");
        routineInfo.setTextSize(14);
        body.addView(routineInfo);

        TextView chatTitle = new TextView(this);
        chatTitle.setText("ChatGPT 물리 키보드 보정");
        chatTitle.setTextSize(19);
        chatTitle.setTypeface(Typeface.DEFAULT_BOLD);
        body.addView(chatTitle);

        chatGptSwitch = new Switch(this);
        chatGptSwitch.setText("Enter = ChatGPT 전송\nShift + Enter = 줄바꿈");
        chatGptSwitch.setTextSize(16);
        chatGptSwitch.setChecked(p.getBoolean("chatgpt_pc_enter", true));
        chatGptSwitch.setOnCheckedChangeListener((v, checked) ->
                p.edit().putBoolean("chatgpt_pc_enter", checked).apply());
        body.addView(chatGptSwitch);

        Button accessibility = new Button(this);
        accessibility.setText("ChatGPT 기능: 접근성 설정 열기 (최초 1회)");
        accessibility.setOnClickListener(v -> openAccessibilitySettings());
        body.addView(accessibility);

        TextView privacy = new TextView(this);
        privacy.setText("ChatGPT 보정은 com.openai.chatgpt에서만 동작합니다. 다른 앱의 Enter 동작은 건드리지 않습니다. 인터넷 권한은 없습니다.\n");
        privacy.setTextSize(13);
        body.addView(privacy);

        status = new TextView(this);
        status.setText("\n진단 대기 중...");
        status.setTextSize(13);
        status.setTypeface(Typeface.MONOSPACE);
        status.setPadding(0, dp(12), 0, dp(40));
        body.addView(status);

        ScrollView sc = new ScrollView(this);
        sc.addView(body);
        setContentView(sc);
    }

    private TextView section(String text) {
        TextView t = new TextView(this);
        t.setText("\n" + text);
        t.setTextSize(19);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private Spinner addRoleRow(LinearLayout body, String label, String[] values, String selected) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(15);
        l.setPadding(0, dp(4), dp(8), dp(4));
        row.addView(l, new LinearLayout.LayoutParams(0, -2, 1f));
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, values);
        s.setAdapter(a);
        s.setSelection(arrayIndex(values, selected));
        row.addView(s, new LinearLayout.LayoutParams(dp(170), -2));
        body.addView(row);
        return s;
    }

    private void saveMappings() {
        SharedPreferences.Editor e = getSharedPreferences("cfg", MODE_PRIVATE).edit();
        for (int i = 0; i < 3; i++) {
            e.putString("kb_role_" + i, String.valueOf(keyboardRole[i].getSelectedItem()));
            e.putString("mouse_role_" + i, String.valueOf(mouseRole[i].getSelectedItem()));
        }
        e.putString("auto_follow_role", String.valueOf(autoRoleSpinner.getSelectedItem()));
        e.putInt("keyboard_pc_channel", slotForRole(false, ROLE_WORK));
        e.putInt("mouse_pc_channel", slotForRole(true, ROLE_WORK));
        for (int i = 0; i < 3; i++) {
            if (ohoKeyboardSlot[i] != null) e.putInt("oho_kb_" + i, selectedChannel(ohoKeyboardSlot[i]));
            if (ohoMouseSlot[i] != null) e.putInt("oho_mouse_" + i, selectedChannel(ohoMouseSlot[i]));
        }
        if (flowTopSpinner != null) e.putInt("flow_top_slot", flowSlot(flowTopSpinner));
        if (flowBottomSpinner != null) e.putInt("flow_bottom_slot", flowSlot(flowBottomSpinner));
        if (flowLeftSpinner != null) e.putInt("flow_left_slot", flowSlot(flowLeftSpinner));
        if (flowRightSpinner != null) e.putInt("flow_right_slot", flowSlot(flowRightSpinner));
        if (flowThicknessSpinner != null) e.putInt("flow_edge_dp", parseInt(flowThicknessSpinner, 2));
        if (flowDwellSpinner != null) e.putInt("flow_dwell_ms", parseInt(flowDwellSpinner, 180));
        e.apply();
    }

    private int slotForRole(boolean mouse, String role) {
        Spinner[] source = mouse ? mouseRole : keyboardRole;
        for (int i = 0; i < 3; i++) {
            if (role.equals(String.valueOf(source[i].getSelectedItem()))) return i;
        }
        return -1;
    }

    private void switchBothToRole(String role) {
        if (bridge == null || !bridge.isReady()) {
            toast("HID Host 준비 중입니다.");
            ensureBluetoothPermissionAndOpen();
            return;
        }
        int mouseTarget = slotForRole(true, role);
        int kbTarget = slotForRole(false, role);
        HidHostBridge.SwitchResult mouse = mouseTarget >= 0
                ? bridge.switchMouseToHost(mouseTarget)
                : new HidHostBridge.SwitchResult(false, "마우스 역할 슬롯 없음");
        HidHostBridge.SwitchResult kb = kbTarget >= 0
                ? bridge.switchKeyboardToHost(kbTarget)
                : new HidHostBridge.SwitchResult(false, "키보드 역할 슬롯 없음");
        status.setText("역할 복귀 결과\n" +
                "마우스 " + (mouse.ok ? "성공" : "실패") + ": " + mouse.message + "\n" +
                "키보드 " + (kb.ok ? "성공" : "실패") + ": " + kb.message);
    }

    private void probeHosts() {
        if (bridge == null || !bridge.isReady()) {
            ensureBluetoothPermissionAndOpen();
            return;
        }
        status.setText("0x1814 실제 host 확인 중...");
        bridge.probeCurrentHosts((text) -> runOnUiThread(() -> status.setText(text)));
    }

    private void ensureBluetoothPermissionAndOpen() {
        if (Build.VERSION.SDK_INT >= 31 &&
                checkSelfPermission("android.permission.BLUETOOTH_CONNECT") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.BLUETOOTH_CONNECT"}, REQ_BT);
            return;
        }
        openBridge();
    }

    private void openBridge() {
        if (bridge != null) bridge.close();
        bridge = new HidHostBridge(this);
        status.setText("HID Host 연결 준비 중...");
        bridge.open((ok, msg) -> runOnUiThread(() -> {
            status.setText((ok ? "준비됨\n" : "준비 실패\n") + msg + "\n\n" + (ok ? bridge.diagnostics() : ""));
            if (ok && getSharedPreferences("cfg", MODE_PRIVATE).getBoolean("auto", false)) startWatchService();
        }));
    }

    private void refreshDiagnostics() {
        if (bridge == null || !bridge.isReady()) {
            ensureBluetoothPermissionAndOpen();
            return;
        }
        status.setText(bridge.diagnostics());
    }

    private void startWatchService() {
        Intent i = new Intent(this, WatchService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
            else startService(i);
        } catch (Throwable t) {
            status.setText("자동 감시 서비스 시작 실패: " + t);
        }
    }

    private void openAccessibilitySettings() {
        try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
        catch (Throwable t) { toast("접근성 설정을 열 수 없습니다."); }
    }

    private void openOverlaySettings() {
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(i);
            }
        } catch (Throwable t) { toast("다른 앱 위에 표시 설정을 열 수 없습니다."); }
    }

    private void startEdgeFlowService() {
        Intent i = new Intent(this, EdgeFlowService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
            else startService(i);
        } catch (Throwable t) {
            toast("FLOW 서비스 시작 실패: " + t.getClass().getSimpleName());
        }
    }

    private void refreshFlowService() {
        SharedPreferences p = getSharedPreferences("cfg", MODE_PRIVATE);
        if (!p.getBoolean("flow_enabled", false)) return;
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) return;
        Intent i = new Intent(this, EdgeFlowService.class);
        i.setAction(EdgeFlowService.ACTION_REFRESH);
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
            else startService(i);
        } catch (Throwable ignored) { }
    }

    @Override protected void onResume() {
        super.onResume();
        SharedPreferences p = getSharedPreferences("cfg", MODE_PRIVATE);
        if (p.getBoolean("flow_enabled", false) &&
                (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this))) {
            startEdgeFlowService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_BT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) openBridge();
            else status.setText("근처 기기/Bluetooth 연결 권한이 필요합니다.");
        }
    }

    private String defaultKbRole(int i) { return new String[]{ROLE_HOME, ROLE_PHONE, ROLE_WORK}[i]; }
    private String defaultMouseRole(int i) { return new String[]{ROLE_WORK, ROLE_PHONE, ROLE_HOME}[i]; }
    private int defaultOhoKeyboard(int i) { return new int[]{2, 1, 0}[i]; }
    private int defaultOhoMouse(int i) { return new int[]{0, 1, 2}[i]; }
    private String flowLabel(int slot) { return slot < 0 ? "사용안함" : "슬롯 " + (slot + 1); }
    private int flowSlot(Spinner s) {
        String v = String.valueOf(s.getSelectedItem());
        if ("슬롯 1".equals(v)) return 0;
        if ("슬롯 2".equals(v)) return 1;
        if ("슬롯 3".equals(v)) return 2;
        return -1;
    }
    private int parseInt(Spinner s, int fallback) {
        try { return Integer.parseInt(String.valueOf(s.getSelectedItem())); }
        catch (Throwable t) { return fallback; }
    }
    private int selectedChannel(Spinner s) {
        try { return Math.max(0, Math.min(2, Integer.parseInt(String.valueOf(s.getSelectedItem())) - 1)); }
        catch (Throwable t) { return 0; }
    }
    private int arrayIndex(String[] arr, String value) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(value)) return i;
        return 0;
    }
    private int roleIndex(String role) {
        if (ROLE_HOME.equals(role)) return 1;
        if (ROLE_PHONE.equals(role)) return 2;
        return 0;
    }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override protected void onDestroy() {
        if (bridge != null) bridge.close();
        super.onDestroy();
    }
}