package com.mxreturn.helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

public class FlowBootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!context.getSharedPreferences("cfg", Context.MODE_PRIVATE)
                .getBoolean("flow_enabled", false)) return;
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(context)) return;
        Intent s = new Intent(context, EdgeFlowService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(s);
            else context.startService(s);
        } catch (Throwable ignored) { }
    }
}
