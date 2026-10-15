package com.mxreturn.helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.concurrent.atomic.AtomicBoolean;

public class SlotCommandReceiver extends BroadcastReceiver {
    public static final String ACTION_SWITCH_SLOT = "com.mxreturn.helper.action.SWITCH_SLOT";
    public static final String EXTRA_LOGICAL_SLOT = "logical_slot";

    private static final int[][] DEFAULTS = {
            {2, 0}, // 논리 슬롯 1: 키보드 3 + 마우스 1
            {1, 1}, // 논리 슬롯 2: 키보드 2 + 마우스 2
            {0, 2}  // 논리 슬롯 3: 키보드 1 + 마우스 3
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_SWITCH_SLOT.equals(intent.getAction())) return;

        final Context app = context.getApplicationContext();
        final int logical = intent.getIntExtra(EXTRA_LOGICAL_SLOT, -1);
        if (logical < 0 || logical > 2) return;

        if (Build.VERSION.SDK_INT >= 31 &&
                app.checkSelfPermission("android.permission.BLUETOOTH_CONNECT")
                        != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(app, "MX Return을 한 번 열어 Bluetooth 권한을 허용해 주세요.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        final PendingResult pending = goAsync();
        final Handler handler = new Handler(Looper.getMainLooper());
        final AtomicBoolean done = new AtomicBoolean(false);
        final HidHostBridge[] bridgeHolder = new HidHostBridge[1];

        SharedPreferences p = app.getSharedPreferences("cfg", Context.MODE_PRIVATE);
        final int kbTarget = sanitize(p.getInt("oho_kb_" + logical, DEFAULTS[logical][0]));
        final int mouseTarget = sanitize(p.getInt("oho_mouse_" + logical, DEFAULTS[logical][1]));

        final Runnable finish = () -> {
            if (!done.compareAndSet(false, true)) return;
            handler.removeCallbacksAndMessages(null);
            try {
                if (bridgeHolder[0] != null) bridgeHolder[0].close();
            } catch (Throwable ignored) { }
            bridgeHolder[0] = null;
            try { pending.finish(); } catch (Throwable ignored) { }
        };

        handler.postDelayed(() -> {
            if (!done.get()) {
                Toast.makeText(app, "MX Return 슬롯 " + (logical + 1) + ": 장치 준비 시간 초과",
                        Toast.LENGTH_SHORT).show();
                finish.run();
            }
        }, 4500);

        try {
            HidHostBridge bridge = new HidHostBridge(app);
            bridgeHolder[0] = bridge;
            bridge.open((ok, msg) -> handler.post(() -> {
                if (done.get()) return;
                if (!ok || bridgeHolder[0] == null || !bridgeHolder[0].isReady()) {
                    Toast.makeText(app, "MX Return 슬롯 " + (logical + 1) + ": HID Host 준비 실패",
                            Toast.LENGTH_SHORT).show();
                    finish.run();
                    return;
                }

                boolean mouseOk = true;
                try {
                    HidHostBridge.SwitchResult mr = bridgeHolder[0].switchMouseToHost(mouseTarget);
                    mouseOk = mr != null && mr.ok;
                } catch (Throwable t) {
                    mouseOk = false;
                }

                final boolean finalMouseOk = mouseOk;
                handler.postDelayed(() -> {
                    if (done.get()) return;
                    boolean keyboardOk = true;
                    try {
                        HidHostBridge.SwitchResult kr =
                                bridgeHolder[0].switchKeyboardToHost(kbTarget);
                        keyboardOk = kr != null && kr.ok;
                    } catch (Throwable t) {
                        keyboardOk = false;
                    }

                    if (!finalMouseOk || !keyboardOk) {
                        Toast.makeText(app,
                                "MX Return 슬롯 " + (logical + 1) + ": 일부 전환 실패",
                                Toast.LENGTH_SHORT).show();
                    }
                    finish.run();
                }, 220);
            }));
        } catch (Throwable t) {
            Toast.makeText(app, "MX Return 슬롯 " + (logical + 1) + ": 전환 준비 실패",
                    Toast.LENGTH_SHORT).show();
            finish.run();
        }
    }

    private static int sanitize(int slot) {
        return slot < 0 || slot > 2 ? 0 : slot;
    }
}
