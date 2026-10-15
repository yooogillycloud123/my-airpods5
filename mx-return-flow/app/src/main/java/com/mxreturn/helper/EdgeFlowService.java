package com.mxreturn.helper;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class EdgeFlowService extends Service {
    public static final String ACTION_REFRESH = "com.mxreturn.helper.action.FLOW_REFRESH";
    private static final String CHANNEL_ID = "mx_return_flow";
    private static final int NOTI_ID = 1815;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<View> edgeViews = new ArrayList<>();
    private WindowManager wm;
    private HidHostBridge bridge;
    private boolean busy = false;
    private long lastTriggerAt = 0L;

    @Override public void onCreate() {
        super.onCreate();
        startAsForeground();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        openBridge();
        rebuildEdges();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isEnabled()) {
            stopSelf();
            return START_NOT_STICKY;
        }
        rebuildEdges();
        return START_STICKY;
    }

    private boolean isEnabled() {
        return getSharedPreferences("cfg", MODE_PRIVATE).getBoolean("flow_enabled", false);
    }

    private void openBridge() {
        try {
            if (bridge != null) bridge.close();
            bridge = new HidHostBridge(this);
            bridge.open((ok, msg) -> { });
        } catch (Throwable ignored) { }
    }

    private void rebuildEdges() {
        clearEdges();
        if (!isEnabled()) return;
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) return;

        SharedPreferences p = getSharedPreferences("cfg", MODE_PRIVATE);
        addEdge(Gravity.TOP, p.getInt("flow_top_slot", 0));
        addEdge(Gravity.BOTTOM, p.getInt("flow_bottom_slot", -1));
        addEdge(Gravity.LEFT, p.getInt("flow_left_slot", -1));
        addEdge(Gravity.RIGHT, p.getInt("flow_right_slot", -1));
    }

    private void addEdge(int gravity, int logicalSlot) {
        if (logicalSlot < 0 || logicalSlot > 2 || wm == null) return;

        int dp = getSharedPreferences("cfg", MODE_PRIVATE).getInt("flow_edge_dp", 2);
        int thickness = Math.max(1, dp(dp));

        View v = new View(this);
        v.setBackgroundColor(Color.TRANSPARENT);
        v.setOnHoverListener(new EdgeHoverListener(logicalSlot));

        int width = (gravity == Gravity.LEFT || gravity == Gravity.RIGHT)
                ? thickness : WindowManager.LayoutParams.MATCH_PARENT;
        int height = (gravity == Gravity.TOP || gravity == Gravity.BOTTOM)
                ? thickness : WindowManager.LayoutParams.MATCH_PARENT;

        int type;
        if (Build.VERSION.SDK_INT >= 26) type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        else type = WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                width, height, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = gravity;
        lp.setTitle("MX Return Flow Edge");
        try {
            wm.addView(v, lp);
            edgeViews.add(v);
        } catch (Throwable ignored) { }
    }

    private class EdgeHoverListener implements View.OnHoverListener {
        final int logicalSlot;
        Runnable pending;
        boolean inside;

        EdgeHoverListener(int logicalSlot) { this.logicalSlot = logicalSlot; }

        @Override public boolean onHover(View v, android.view.MotionEvent e) {
            if ((e.getSource() & android.view.InputDevice.SOURCE_MOUSE)
                    != android.view.InputDevice.SOURCE_MOUSE) return false;

            int a = e.getActionMasked();
            if (a == android.view.MotionEvent.ACTION_HOVER_ENTER ||
                    a == android.view.MotionEvent.ACTION_HOVER_MOVE) {
                if (!inside) {
                    inside = true;
                    arm();
                }
                return true;
            }
            if (a == android.view.MotionEvent.ACTION_HOVER_EXIT) {
                inside = false;
                cancel();
                return true;
            }
            return false;
        }

        void arm() {
            cancel();
            int dwell = getSharedPreferences("cfg", MODE_PRIVATE)
                    .getInt("flow_dwell_ms", 180);
            pending = () -> {
                if (inside) triggerSlot(logicalSlot);
            };
            handler.postDelayed(pending, Math.max(60, dwell));
        }

        void cancel() {
            if (pending != null) handler.removeCallbacks(pending);
            pending = null;
        }
    }

    private void triggerSlot(final int logicalSlot) {
        long now = SystemClock.uptimeMillis();
        int cooldown = getSharedPreferences("cfg", MODE_PRIVATE)
                .getInt("flow_cooldown_ms", 1200);
        if (busy || now - lastTriggerAt < cooldown) return;
        lastTriggerAt = now;
        busy = true;

        SharedPreferences p = getSharedPreferences("cfg", MODE_PRIVATE);
        final int kbTarget = clamp(p.getInt("oho_kb_" + logicalSlot,
                new int[]{2,1,0}[logicalSlot]));
        final int mouseTarget = clamp(p.getInt("oho_mouse_" + logicalSlot,
                new int[]{0,1,2}[logicalSlot]));

        if (bridge != null && bridge.isReady()) {
            sendBoth(logicalSlot, kbTarget, mouseTarget);
            return;
        }

        try {
            if (bridge != null) bridge.close();
            bridge = new HidHostBridge(this);
            bridge.open((ok, msg) -> handler.post(() -> {
                if (!ok || bridge == null || !bridge.isReady()) {
                    busy = false;
                    Toast.makeText(this, "MX Flow: HID Host 준비 실패", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendBoth(logicalSlot, kbTarget, mouseTarget);
            }));
        } catch (Throwable t) {
            busy = false;
        }
    }

    private void sendBoth(final int logicalSlot, final int kbTarget, final int mouseTarget) {
        boolean keyboardOk = true;
        try {
            HidHostBridge.SwitchResult kr = bridge.switchKeyboardToHost(kbTarget);
            keyboardOk = kr != null && kr.ok;
        } catch (Throwable t) { keyboardOk = false; }

        final boolean finalKeyboardOk = keyboardOk;
        handler.postDelayed(() -> {
            boolean mouseOk = true;
            try {
                HidHostBridge.SwitchResult mr = bridge.switchMouseToHost(mouseTarget);
                mouseOk = mr != null && mr.ok;
            } catch (Throwable t) { mouseOk = false; }

            if (!finalKeyboardOk || !mouseOk) {
                Toast.makeText(this,
                        "MX Flow 슬롯 " + (logicalSlot + 1) + ": 일부 전환 실패",
                        Toast.LENGTH_SHORT).show();
            }
            handler.postDelayed(() -> busy = false, 800);
        }, 110);
    }

    private int clamp(int v) { return v < 0 || v > 2 ? 0 : v; }

    private void clearEdges() {
        if (wm == null) return;
        for (View v : edgeViews) {
            try { wm.removeViewImmediate(v); } catch (Throwable ignored) { }
        }
        edgeViews.clear();
    }

    private void startAsForeground() {
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "MX Return Flow", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("화면 가장자리에서 MX 키보드와 마우스를 자동 전환합니다.");
            nm.createNotificationChannel(ch);
        }
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        startForeground(NOTI_ID, b
                .setSmallIcon(android.R.drawable.ic_menu_directions)
                .setContentTitle("MX Return Flow 켜짐")
                .setContentText("화면 끝으로 밀면 키보드 + 마우스가 함께 이동합니다.")
                .setContentIntent(pi)
                .setOngoing(true)
                .build());
    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override public void onDestroy() {
        clearEdges();
        handler.removeCallbacksAndMessages(null);
        try { if (bridge != null) bridge.close(); } catch (Throwable ignored) { }
        bridge = null;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
