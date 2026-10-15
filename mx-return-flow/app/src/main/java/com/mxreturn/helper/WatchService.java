package com.mxreturn.helper;

import android.app.*;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.os.*;
import android.util.Log;

public class WatchService extends Service {
    private static final String TAG = "MXReturn-Watch";
    private static final String CHANNEL_ID = "mx_return_watch";
    private static final int NOTI_ID = 1814;
    private static final long HOST_POLL_MS = 1200L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private HandlerThread workerThread;
    private Handler worker;
    private HidHostBridge bridge;
    private boolean armed = false;
    private boolean receiverRegistered = false;
    private volatile boolean destroyed = false;
    private int lastKeyboardHost = -1;
    private int lastMouseHost = -1;
    private String lastAppliedRole = "";

    private final Runnable hostMonitor = new Runnable() {
        @Override public void run() {
            if (destroyed || !isAutoEnabled() || bridge == null || !bridge.isReady()) return;
            if (worker != null) worker.post(WatchService.this::monitorHosts);
            mainHandler.postDelayed(this, HOST_POLL_MS);
        }
    };

    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            String a = i.getAction();
            BluetoothDevice d = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String n = safeName(d);
            Log.i(TAG, "BT event=" + a + " device=" + n);

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(a) && (isKeyboard(n) || isMouse(n))) {
                mainHandler.postDelayed(WatchService.this::refreshArmState, 350);
                return;
            }

            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(a) && (isKeyboard(n) || isMouse(n))) {
                final boolean wasArmed = armed;
                mainHandler.postDelayed(() -> {
                    try {
                        if (!wasArmed || !isAutoEnabled() || bridge == null || !bridge.isReady()) return;
                        if (!bridge.isKeyboardConnected() && bridge.isMouseConnected()) {
                            followRole(autoRole());
                        } else if (!bridge.isMouseConnected() && bridge.isKeyboardConnected()) {
                            followRole(autoRole());
                        }
                        refreshArmState();
                    } catch (Throwable t) { Log.e(TAG, "disconnect fallback", t); }
                }, 420);
            }
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        startAsForeground();
        workerThread = new HandlerThread("MXReturn-SlotMonitor");
        workerThread.start();
        worker = new Handler(workerThread.getLooper());

        IntentFilter f = new IntentFilter();
        f.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        f.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        try { registerReceiver(btReceiver, f); receiverRegistered = true; }
        catch (Throwable t) { Log.e(TAG, "registerReceiver", t); }

        bridge = new HidHostBridge(this);
        bridge.open((ok, msg) -> {
            Log.i(TAG, "bridge ready=" + ok + " " + msg);
            mainHandler.postDelayed(this::refreshArmState, 300);
        });
    }

    private boolean isAutoEnabled() {
        return getSharedPreferences("cfg", MODE_PRIVATE).getBoolean("auto", false);
    }

    private void refreshArmState() {
        try {
            if (bridge != null && bridge.isReady()) {
                armed = bridge.isKeyboardConnected() && bridge.isMouseConnected();
                if (armed) {
                    if (worker != null) worker.post(this::primeKnownHosts);
                    mainHandler.removeCallbacks(hostMonitor);
                    mainHandler.post(hostMonitor);
                } else {
                    mainHandler.removeCallbacks(hostMonitor);
                    lastKeyboardHost = -1;
                    lastMouseHost = -1;
                }
                Log.i(TAG, "arm=" + armed + " kbHost=" + lastKeyboardHost + " mouseHost=" + lastMouseHost);
            }
        } catch (Throwable t) { Log.e(TAG, "arm", t); }
    }

    private void primeKnownHosts() {
        try {
            if (bridge == null || !bridge.isReady()) return;
            BluetoothDevice kb = bridge.findKeyboard();
            BluetoothDevice mouse = bridge.findMouse();
            int kh = bridge.getActualHostSlot(kb);
            int mh = bridge.getActualHostSlot(mouse);
            if (kh >= 0) lastKeyboardHost = kh;
            if (mh >= 0) lastMouseHost = mh;
            Log.i(TAG, "prime hosts kb=" + lastKeyboardHost + " mouse=" + lastMouseHost);
        } catch (Throwable t) { Log.e(TAG, "primeKnownHosts", t); }
    }

    private void monitorHosts() {
        try {
            if (destroyed || !isAutoEnabled() || !armed || bridge == null || !bridge.isReady()) return;
            monitorOne(false);
            monitorOne(true);
        } catch (Throwable t) { Log.e(TAG, "monitorHosts", t); }
    }

    private void monitorOne(boolean mouse) {
        BluetoothDevice d = mouse ? bridge.findMouse() : bridge.findKeyboard();
        if (d == null) return;
        int current = bridge.getActualHostSlot(d);
        if (current < 0) return;

        int previous = mouse ? lastMouseHost : lastKeyboardHost;
        if (mouse) lastMouseHost = current; else lastKeyboardHost = current;
        if (previous < 0 || previous == current) return;

        String role = roleAt(mouse, current);
        if (role.length() == 0 || "사용안함".equals(role)) return;
        Log.i(TAG, (mouse ? "mouse" : "keyboard") + " host changed " +
                (previous + 1) + "->" + (current + 1) + " role=" + role);

        if (role.equals(lastAppliedRole)) {
            lastAppliedRole = "";
            return;
        }
        followRole(role);
    }

    private void followRole(String role) {
        if (bridge == null || !bridge.isReady()) return;
        int mouseTarget = slotForRole(true, role);
        int keyboardTarget = slotForRole(false, role);
        if (mouseTarget < 0 && keyboardTarget < 0) return;

        lastAppliedRole = role;
        if (mouseTarget >= 0) {
            HidHostBridge.SwitchResult r = bridge.switchMouseToHost(mouseTarget);
            Log.i(TAG, "follow mouse " + role + " -> " + r.ok + " " + r.message);
        }
        if (keyboardTarget >= 0) {
            mainHandler.postDelayed(() -> {
                try {
                    HidHostBridge.SwitchResult r = bridge.switchKeyboardToHost(keyboardTarget);
                    Log.i(TAG, "follow keyboard " + role + " -> " + r.ok + " " + r.message);
                } catch (Throwable t) { Log.e(TAG, "follow keyboard", t); }
            }, 220);
        }
    }

    private int slotForRole(boolean mouse, String role) {
        SharedPreferences p = getSharedPreferences("cfg", MODE_PRIVATE);
        String prefix = mouse ? "mouse_role_" : "kb_role_";
        for (int i = 0; i < 3; i++) if (role.equals(p.getString(prefix + i, ""))) return i;
        return -1;
    }

    private String roleAt(boolean mouse, int slot) {
        if (slot < 0 || slot > 2) return "";
        SharedPreferences p = getSharedPreferences("cfg", MODE_PRIVATE);
        return p.getString((mouse ? "mouse_role_" : "kb_role_") + slot, "");
    }

    private String autoRole() {
        return getSharedPreferences("cfg", MODE_PRIVATE).getString("auto_follow_role", "회사 PC");
    }

    private String safeName(BluetoothDevice d) {
        try { return d == null || d.getName() == null ? "" : d.getName().toLowerCase(); }
        catch (Throwable t) { return ""; }
    }
    private boolean isKeyboard(String n) { return n != null && n.contains("mx keys s"); }
    private boolean isMouse(String n) { return n != null && n.contains("mx master 3s"); }

    private void startAsForeground() {
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "MX Return 자동 복귀", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Bluetooth host 변화가 확인될 때만 대응하는 슬롯 자동추종");
            nm.createNotificationChannel(ch);
        }
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        startForeground(NOTI_ID, b.setSmallIcon(android.R.drawable.ic_menu_manage)
                .setContentTitle("MX Return 슬롯 자동추종 켜짐")
                .setContentText("0x1814 host 확인 + 장치 간 역할 동기화")
                .setContentIntent(pi).setOngoing(true).build());
    }

    @Override public int onStartCommand(Intent i, int f, int s) {
        if (!isAutoEnabled()) { stopSelf(); return START_NOT_STICKY; }
        mainHandler.postDelayed(this::refreshArmState, 250);
        return START_STICKY;
    }

    @Override public void onDestroy() {
        destroyed = true;
        mainHandler.removeCallbacksAndMessages(null);
        if (worker != null) worker.removeCallbacksAndMessages(null);
        if (receiverRegistered) { try { unregisterReceiver(btReceiver); } catch (Throwable ignored) {} receiverRegistered = false; }
        try { if (bridge != null) bridge.close(); } catch (Throwable ignored) { }
        if (workerThread != null) { try { workerThread.quitSafely(); } catch (Throwable ignored) {} workerThread = null; }
        worker = null;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent i) { return null; }
}