package com.mxreturn.helper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import org.lsposed.hiddenapibypass.LSPass;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class HidHostBridge {
    private static final String TAG = "MXReturn-HID";
    private static final int HID_HOST = 4;
    private static final int FEATURE_ROOT = 0x0000;
    private static final int FEATURE_CHANGE_HOST = 0x1814;
    private static final int DEFAULT_CHANGE_HOST_INDEX = 0x0A;
    private static final int SOFTWARE_ID = 0x0B;
    private static final String MOUSE_NAME = "MX Master 3S";
    private static final String KEYBOARD_NAME = "MX Keys S";
    private static final String ACTION_REPORT = "android.bluetooth.input.profile.action.REPORT";

    private final Context context;
    private BluetoothAdapter adapter;
    private BluetoothProfile hidHost;
    private HandlerThread reportThread;
    private Handler reportHandler;
    private boolean reportReceiverRegistered = false;
    private final ConcurrentHashMap<String, Integer> featureIndexCache = new ConcurrentHashMap<>();
    private final Object reportLock = new Object();
    private PendingReport pendingReport;

    private final BroadcastReceiver reportReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent intent) {
            try {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                byte[] report = intent.getByteArrayExtra("android.bluetooth.input.extra.REPORT");
                if (report == null) report = intent.getByteArrayExtra("android.bluetooth.input.extra.REPORT_DATA");
                if (device == null || report == null) return;
                PendingReport pending;
                synchronized (reportLock) { pending = pendingReport; }
                if (pending != null && safeSameDevice(device, pending.device)) {
                    pending.report.set(report);
                    pending.latch.countDown();
                }
            } catch (Throwable t) { Log.e(TAG, "reportReceiver", t); }
        }
    };

    public interface ReadyCallback { void onReady(boolean ok, String message); }
    public interface HostProbeCallback { void onComplete(String text); }
    public HidHostBridge(Context context) { this.context = context.getApplicationContext(); }

    public void open(ReadyCallback cb) {
        try {
            adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) { cb.onReady(false, "BluetoothAdapter 없음"); return; }
            startReportReceiver();
            boolean requested = adapter.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
                @Override public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    hidHost = proxy; cb.onReady(true, "HID Host 연결됨: " + proxy.getClass().getName());
                }
                @Override public void onServiceDisconnected(int profile) { hidHost = null; }
            }, HID_HOST);
            if (!requested) cb.onReady(false, "HID Host profile 요청 실패");
        } catch (Throwable t) { cb.onReady(false, "HID Host 열기 오류: " + t); }
    }

    private void startReportReceiver() {
        if (reportReceiverRegistered) return;
        try {
            reportThread = new HandlerThread("MXReturn-HID-Reports"); reportThread.start();
            reportHandler = new Handler(reportThread.getLooper());
            IntentFilter f = new IntentFilter(ACTION_REPORT);
            context.registerReceiver(reportReceiver, f, null, reportHandler);
            reportReceiverRegistered = true;
        } catch (Throwable t) { Log.e(TAG, "register REPORT receiver failed", t); }
    }

    public boolean isReady() { return hidHost != null; }
    public List<BluetoothDevice> connectedDevices() {
        try { return hidHost == null ? new ArrayList<>() : hidHost.getConnectedDevices(); }
        catch (Throwable t) { return new ArrayList<>(); }
    }
    public BluetoothDevice findMouse() { return findByName(MOUSE_NAME); }
    public BluetoothDevice findKeyboard() { return findByName(KEYBOARD_NAME); }
    public boolean isMouseConnected() { return findMouse() != null; }
    public boolean isKeyboardConnected() { return findKeyboard() != null; }

    private BluetoothDevice findByName(String needle) {
        for (BluetoothDevice d : connectedDevices()) {
            try {
                String n = d.getName();
                if (n != null && n.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT))) return d;
            } catch (SecurityException ignored) { }
        }
        return null;
    }

    public String diagnostics() {
        StringBuilder sb = new StringBuilder();
        sb.append("HID Host ready: ").append(hidHost != null).append('\n');
        if (hidHost != null) {
            sb.append("Proxy: ").append(hidHost.getClass().getName()).append('\n');
            List<BluetoothDevice> list = connectedDevices();
            sb.append("Connected HID: ").append(list.size()).append('\n');
            for (BluetoothDevice d : list) {
                try { sb.append(" • ").append(d.getName()).append(" / ").append(d.getAddress()).append('\n'); }
                catch (Throwable t) { sb.append(" • <권한 필요>\n"); }
            }
            try {
                Method found = null;
                for (Method m : LSPass.getDeclaredMethods(hidHost.getClass())) {
                    if ("sendData".equals(m.getName())) { found = m; break; }
                }
                if (found != null) sb.append("sendData hidden-API bypass: OK / ").append(found).append('\n');
                else sb.append("sendData hidden-API bypass: NOT FOUND\n");
            } catch (Throwable t) {
                sb.append("sendData hidden-API bypass: FAIL / ").append(t.getClass().getSimpleName()).append(": ").append(t.getMessage()).append('\n');
            }
        }
        return sb.toString();
    }

    public int getActualHostSlot(BluetoothDevice device) {
        HostInfo info = queryCurrentHost(device, 320);
        return info == null ? -1 : info.currentHost;
    }

    public void probeCurrentHosts(HostProbeCallback cb) {
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append(diagnostics()).append('\n');
            probeOne(findKeyboard(), "키보드 MX Keys S", "kb_role_", sb);
            probeOne(findMouse(), "마우스 MX Master 3S", "mouse_role_", sb);
            cb.onComplete(sb.toString());
        }, "MXReturn-HostProbe").start();
    }

    private void probeOne(BluetoothDevice device, String label, String rolePrefix, StringBuilder sb) {
        if (device == null) { sb.append(label).append(": 현재 폰에 연결 안 됨\n"); return; }
        HostInfo info = queryCurrentHost(device, 700);
        if (info == null) {
            sb.append(label).append(": 0x1814 실제 host 확인 실패");
            Integer cached = featureIndexCache.get(safeAddress(device));
            if (cached != null) sb.append(" (feature index 0x").append(hex(cached)).append(")");
            sb.append('\n'); return;
        }
        String role = roleForSlot(rolePrefix, info.currentHost);
        sb.append(label).append(": 0x1814 index=0x").append(hex(info.featureIndex))
                .append(" / host=").append(info.currentHost + 1).append("번")
                .append(" / 역할=").append(role).append(" / host 수=").append(info.hostCount)
                .append(" / capabilities=0x").append(hex(info.capabilities)).append('\n');
    }

    private String roleForSlot(String prefix, int slot) {
        try { return context.getSharedPreferences("cfg", Context.MODE_PRIVATE).getString(prefix + slot, "설정없음"); }
        catch (Throwable t) { return "설정없음"; }
    }

    public SwitchResult switchMouseToHost(int targetChannel) {
        BluetoothDevice mouse = findMouse();
        if (mouse == null) return new SwitchResult(false, "MX Master 3S가 현재 폰 HID에 연결되어 있지 않음");
        return switchDeviceToHost(mouse, "MX Master 3S", targetChannel);
    }
    public SwitchResult switchKeyboardToHost(int targetChannel) {
        BluetoothDevice keyboard = findKeyboard();
        if (keyboard == null) return new SwitchResult(false, "MX Keys S가 현재 폰 HID에 연결되어 있지 않음");
        return switchDeviceToHost(keyboard, "MX Keys S", targetChannel);
    }

    private SwitchResult switchDeviceToHost(BluetoothDevice device, String label, int targetChannel) {
        if (targetChannel < 0 || targetChannel > 2) return new SwitchResult(false, "채널은 1~3만 가능");
        int featureIndex = resolveFeatureIndex(device, FEATURE_CHANGE_HOST);
        boolean usedFallbackIndex = false;
        if (featureIndex < 0) { featureIndex = DEFAULT_CHANGE_HOST_INDEX; usedFallbackIndex = true; }

        String preferred = buildChangeHostReport(0xFF, targetChannel, 0x1B, featureIndex);
        InvocationResult a = invokeSend(device, preferred);
        if (a.accepted) return new SwitchResult(true, label + " → 슬롯 " + (targetChannel + 1) + " 전송 요청됨 (0x1814 index=0x" + hex(featureIndex) + (usedFallbackIndex ? ", fallback" : ", runtime-resolved") + ")");

        String report00 = buildChangeHostReport(0x00, targetChannel, 0x1E, featureIndex);
        InvocationResult b = invokeSend(device, report00);
        if (b.accepted) return new SwitchResult(true, label + " → 슬롯 " + (targetChannel + 1) + " 전송 요청됨 (compat 00): " + report00);

        String reportFF = buildChangeHostReport(0xFF, targetChannel, 0x1D, featureIndex);
        InvocationResult c = invokeSend(device, reportFF);
        if (c.accepted) return new SwitchResult(true, label + " → 슬롯 " + (targetChannel + 1) + " fallback 전송 요청됨 (compat FF): " + reportFF);

        return new SwitchResult(false, label + " HID++ 전송 실패\n1차: " + a.message + "\n2차: " + b.message + "\n3차: " + c.message);
    }

    private int resolveFeatureIndex(BluetoothDevice device, int featureId) {
        String address = safeAddress(device);
        Integer cached = featureIndexCache.get(address);
        if (cached != null) return cached;
        String report = buildRootGetFeatureReport(device, featureId);
        InvocationResult sent = invokeSend(device, report);
        if (!sent.accepted) return -1;
        byte[] response = awaitReport(device, 320);
        if (response == null || response.length < 5) return -1;
        int candidate = u8(response[4]);
        if (candidate <= 0 || candidate > 0x7F) return -1;
        featureIndexCache.put(address, candidate);
        return candidate;
    }

    private HostInfo queryCurrentHost(BluetoothDevice device, long timeoutMs) {
        int featureIndex = resolveFeatureIndex(device, FEATURE_CHANGE_HOST);
        if (featureIndex < 0) featureIndex = DEFAULT_CHANGE_HOST_INDEX;
        String report = buildChangeHostInfoReport(featureIndex);
        InvocationResult sent = invokeSend(device, report);
        if (!sent.accepted) return null;
        byte[] response = awaitReport(device, 850);
        if (response == null || response.length < 7) return null;
        int hostCount = u8(response[4]);
        int currentHost = u8(response[5]);
        int capabilities = u8(response[6]);
        if (hostCount < 1 || hostCount > 8 || currentHost >= hostCount) return null;
        return new HostInfo(hostCount, currentHost, capabilities, featureIndex);
    }

    private String buildRootGetFeatureReport(BluetoothDevice device, int featureId) {
        return buildReport(0xFF, 0x00, 0x00, featureId >> 8, featureId & 0xFF);
    }
    private String buildChangeHostInfoReport(int featureIndex) {
        return buildReport(0xFF, featureIndex, SOFTWARE_ID, 0, 0);
    }
    private String buildChangeHostReport(int deviceIndex, int targetChannel, int fnSoftware, int featureIndex) {
        int[] bytes = new int[20];
        bytes[0] = 0x11; bytes[1] = deviceIndex; bytes[2] = featureIndex; bytes[3] = fnSoftware; bytes[4] = targetChannel;
        StringBuilder sb = new StringBuilder(40);
        for (int b : bytes) sb.append(String.format(Locale.US, "%02X", b & 0xFF));
        return sb.toString();
    }
    private String buildReport(int deviceIndex, int featureIndex, int fnSoftware, int p0, int p1) {
        int[] bytes = new int[20];
        bytes[0] = 0x11; bytes[1] = deviceIndex; bytes[2] = featureIndex; bytes[3] = fnSoftware; bytes[4] = p0; bytes[5] = p1;
        StringBuilder sb = new StringBuilder(40);
        for (int b : bytes) sb.append(String.format(Locale.US, "%02X", b & 0xFF));
        return sb.toString();
    }

    private InvocationResult invokeSend(BluetoothDevice device, String hexReport) {
        if (hidHost == null) return new InvocationResult(false, "HID Host proxy 없음");
        try {
            Object r = LSPass.invoke(hidHost.getClass(), hidHost, "sendData", device, hexReport);
            boolean accepted = (r instanceof Boolean) && (Boolean) r;
            if (accepted) return new InvocationResult(true, "sendData=true (LSPass)");
        } catch (Throwable t) { Log.e(TAG, "sendData LSPass failed", t); }
        try {
            Object r = LSPass.invoke(hidHost.getClass(), hidHost, "setReport", device, (byte) 2, hexReport);
            boolean accepted = (r instanceof Boolean) && (Boolean) r;
            if (accepted) return new InvocationResult(true, "setReport=true (LSPass)");
            return new InvocationResult(false, "setReport returned false (LSPass)");
        } catch (Throwable t) { return new InvocationResult(false, "LSPass " + t.getClass().getSimpleName() + ": " + t.getMessage()); }
    }

    private byte[] awaitReport(BluetoothDevice device, long timeoutMs) {
        PendingReport pending = new PendingReport(device);
        synchronized (reportLock) { pendingReport = pending; }
        try {
            pending.latch.await(timeoutMs, TimeUnit.MILLISECONDS);
            return pending.report.get();
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }
        finally { synchronized (reportLock) { if (pendingReport == pending) pendingReport = null; } }
    }

    private boolean safeSameDevice(BluetoothDevice a, BluetoothDevice b) {
        try { return a.getAddress().equalsIgnoreCase(b.getAddress()); } catch (Throwable t) { return a == b; }
    }
    private String safeAddress(BluetoothDevice d) { try { return d.getAddress(); } catch (Throwable t) { return "unknown"; } }
    private int u8(byte b) { return b & 0xFF; }
    private String hex(int v) { return String.format(Locale.US, "%02X", v & 0xFF); }
    public void clearFeatureCache() { featureIndexCache.clear(); }

    public void close() {
        try { if (reportReceiverRegistered) context.unregisterReceiver(reportReceiver); } catch (Throwable ignored) { }
        reportReceiverRegistered = false;
        if (reportThread != null) { try { reportThread.quitSafely(); } catch (Throwable ignored) { } }
        reportThread = null; reportHandler = null;
        synchronized (reportLock) { pendingReport = null; }
        try { if (adapter != null && hidHost != null) adapter.closeProfileProxy(HID_HOST, hidHost); } catch (Throwable ignored) { }
        hidHost = null; featureIndexCache.clear();
    }

    private static final class PendingReport {
        final BluetoothDevice device;
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<byte[]> report = new AtomicReference<>();
        PendingReport(BluetoothDevice device) { this.device = device; }
    }
    private static final class HostInfo {
        final int hostCount, currentHost, capabilities, featureIndex;
        HostInfo(int hostCount, int currentHost, int capabilities, int featureIndex) { this.hostCount = hostCount; this.currentHost = currentHost; this.capabilities = capabilities; this.featureIndex = featureIndex; }
    }
    private static final class InvocationResult {
        final boolean accepted; final String message;
        InvocationResult(boolean accepted, String message) { this.accepted = accepted; this.message = message; }
    }
    public static final class SwitchResult {
        public final boolean ok; public final String message;
        SwitchResult(boolean ok, String message) { this.ok = ok; this.message = message; }
    }
}