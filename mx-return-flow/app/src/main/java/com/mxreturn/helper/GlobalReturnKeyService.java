package com.mxreturn.helper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

public class GlobalReturnKeyService extends AccessibilityService {
    private static final long SIGNAL_GUARD_MS = 180L;
    private static final long RETURN_GUARD_MS = 700L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private HidHostBridge bridge;
    private boolean busy = false;
    private long lastSignalAt = 0L;
    private long lastReturnAt = 0L;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        try {
            AccessibilityServiceInfo info = getServiceInfo();
            if (info == null) info = new AccessibilityServiceInfo();
            // OHO+ SAFE mode: never claim SOURCE_MOUSE MotionEvents.
            // Android does not forward claimed MotionEvents to the rest of the system.
            // Keep only the optional keyboard Ctrl+Alt+P shortcut.
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            info.setMotionEventSources(0);
            setServiceInfo(info);
        } catch (Throwable ignored) { }
        openBridge();
    }

    private void openBridge() {
        try {
            if (bridge != null) bridge.close();
            bridge = new HidHostBridge(this);
            bridge.open((ok, msg) -> { });
        } catch (Throwable ignored) { }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() { }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event == null) return false;
        if (getSharedPreferences("cfg", MODE_PRIVATE).getBoolean("global_return_hotkey", true)
                && event.getKeyCode() == KeyEvent.KEYCODE_P
                && event.isCtrlPressed() && event.isAltPressed()
                && !event.isShiftPressed() && !event.isMetaPressed()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                triggerReturn(autoRole(), "Ctrl+Alt+P");
            }
            return true;
        }

        // Never consume mouse BACK/FORWARD or other mouse-generated key events.
        return false;
    }

    private String autoRole() {
        return getSharedPreferences("cfg", MODE_PRIVATE)
                .getString("auto_follow_role", "회사 PC");
    }

    private int slotForRole(boolean mouse, String role) {
        String prefix = mouse ? "mouse_role_" : "kb_role_";
        for (int i = 0; i < 3; i++)
            if (role.equals(getSharedPreferences("cfg", MODE_PRIVATE)
                    .getString(prefix + i, ""))) return i;
        return -1;
    }

    private void triggerReturn(final String role, final String source) {
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastReturnAt < RETURN_GUARD_MS || busy) return;
        lastReturnAt = now;
        busy = true;

        final int mouseTarget = slotForRole(true, role);
        final int keyboardTarget = slotForRole(false, role);
        if (mouseTarget < 0 && keyboardTarget < 0) {
            busy = false;
            Toast.makeText(this, "MX Return: 선택한 역할의 슬롯 설정이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bridge != null && bridge.isReady()) {
            sendBoth(mouseTarget, keyboardTarget, source, role);
            return;
        }

        try {
            if (bridge != null) bridge.close();
            bridge = new HidHostBridge(this);
            bridge.open((ok, msg) -> handler.post(() -> {
                if (!ok) {
                    busy = false;
                    Toast.makeText(this, "MX Return: HID Host 준비 실패", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendBoth(mouseTarget, keyboardTarget, source, role);
            }));
        } catch (Throwable t) {
            busy = false;
            Toast.makeText(this, "MX Return: 복귀 준비 실패", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendBoth(final int mouseTarget, final int keyboardTarget,
                          final String source, final String role) {
        try {
            if (mouseTarget >= 0) bridge.switchMouseToHost(mouseTarget);
        } catch (Throwable ignored) { }

        handler.postDelayed(() -> {
            try {
                HidHostBridge.SwitchResult kr = keyboardTarget >= 0
                        ? bridge.switchKeyboardToHost(keyboardTarget)
                        : new HidHostBridge.SwitchResult(true, "keyboard target 없음");
                if (!kr.ok)
                    Toast.makeText(this, "MX Return: " + source + " / " + role + " 일부 전환 실패", Toast.LENGTH_SHORT).show();
            } catch (Throwable t) {
                Toast.makeText(this, "MX Return: 키보드 복귀 명령 실패", Toast.LENGTH_SHORT).show();
            } finally {
                handler.postDelayed(() -> busy = false, 900);
            }
        }, 220);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        try { if (bridge != null) bridge.close(); } catch (Throwable ignored) { }
        bridge = null;
        super.onDestroy();
    }
}
