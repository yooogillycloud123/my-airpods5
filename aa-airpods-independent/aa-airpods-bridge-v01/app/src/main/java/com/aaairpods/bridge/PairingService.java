package com.aaairpods.bridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.Bundle;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.github.muntashirakon.adb.AbsAdbConnectionManager;
import io.github.muntashirakon.adb.AdbStream;
import io.github.muntashirakon.adb.android.AdbMdns;

public class PairingService extends Service {
    public static final String ACTION_WAIT = "com.aaairpods.bridge.PAIR_WAIT";
    public static final String ACTION_PAIR = "com.aaairpods.bridge.PAIR_NOW";
    private static final String KEY_CODE = "pair_code";
    private static final String CHANNEL_ID = "aa_airpods_pairing";
    private static final int NOTIF_ID = 4601;

    public static volatile String statusText = "";
    public static volatile boolean running = false;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_WAIT : intent.getAction();
        if (action == null) action = ACTION_WAIT;

        startAsForeground(buildWaitingNotification());
        running = true;

        if (ACTION_PAIR.equals(action)) {
            Bundle results = RemoteInput.getResultsFromIntent(intent);
            CharSequence cs = results == null ? null : results.getCharSequence(KEY_CODE);
            String code = cs == null ? "" : cs.toString().trim();

            if (!code.matches("\\d{6}")) {
                statusText = "6자리 페어링 코드가 필요합니다.";
                notifyNow(buildWaitingNotification("6자리 숫자를 다시 입력하세요."));
                return START_NOT_STICKY;
            }

            statusText = "페어링 코드 수신 완료: " + maskCode(code) +
                    "\n무선 디버깅 페어링 포트 자동 탐색 중...";
            notifyNow(buildProgressNotification("페어링 포트 찾는 중..."));
            final String finalCode = code;
            executor.submit(() -> doPair(finalCode));
        } else {
            statusText =
                    "페어링 입력 대기 중\n" +
                    "설정의 '페어링 코드로 기기 페어링' 창은 그대로 열어두고,\n" +
                    "상단 알림창을 내려 '6자리 코드 입력'에 코드를 넣으세요.";
        }

        return START_NOT_STICKY;
    }

    private void doPair(String code) {
        AdbMdns mdns = null;
        try {
            AtomicReference<String> host = new AtomicReference<>();
            AtomicInteger port = new AtomicInteger(-1);
            CountDownLatch latch = new CountDownLatch(1);

            mdns = new AdbMdns(
                    getApplicationContext(),
                    AdbMdns.SERVICE_TYPE_TLS_PAIRING,
                    (InetAddress address, int foundPort) -> {
                        if (address != null && foundPort > 0) {
                            host.set(address.getHostAddress());
                            port.set(foundPort);
                            latch.countDown();
                        }
                    }
            );
            mdns.start();

            if (!latch.await(30, TimeUnit.SECONDS)) {
                fail("페어링 포트 자동 탐색 실패\n페어링 코드 창을 열어 둔 채 다시 알림에서 코드를 입력하세요.");
                return;
            }

            statusText = "페어링 포트 발견\n포트 " + port.get() +
                    "\n이 기기 자체(127.0.0.1)로 페어링 중...";
            notifyNow(buildProgressNotification("코드 확인 / 로컬 페어링 중..."));

            AbsAdbConnectionManager manager =
                    LocalAdbConnectionManager.getInstance(getApplicationContext());

            ExecutorService pairExecutor = Executors.newSingleThreadExecutor();
            boolean paired;
            try {
                Future<Boolean> future = pairExecutor.submit(
                        () -> manager.pair("127.0.0.1", port.get(), code));
                paired = future.get(15, TimeUnit.SECONDS);
            } finally {
                pairExecutor.shutdownNow();
            }

            if (!paired) {
                fail("ADB 페어링 실패");
                return;
            }

            statusText = "페어링 성공\nADB 연결 포트 탐색 중...";
            notifyNow(buildProgressNotification("페어링 성공 / ADB 연결 중..."));

            AtomicInteger connectPort = new AtomicInteger(-1);
            CountDownLatch connectLatch = new CountDownLatch(1);
            AdbMdns connectMdns = new AdbMdns(
                    getApplicationContext(),
                    AdbMdns.SERVICE_TYPE_TLS_CONNECT,
                    (InetAddress address, int foundPort) -> {
                        if (foundPort > 0) {
                            connectPort.set(foundPort);
                            connectLatch.countDown();
                        }
                    }
            );
            connectMdns.start();

            boolean connected = false;
            try {
                if (connectLatch.await(12, TimeUnit.SECONDS) && connectPort.get() > 0) {
                    connected = manager.isConnected() ||
                            manager.connect("127.0.0.1", connectPort.get());
                }
            } finally {
                connectMdns.stop();
            }

            if (!connected && !manager.isConnected()) {
                try {
                    connected = manager.autoConnect(getApplicationContext(), 8000);
                } catch (Throwable ignored) {
                }
            }

            if (!connected && !manager.isConnected()) {
                fail("페어링은 성공했지만 ADB 연결 포트를 찾지 못했습니다.\n무선 디버깅을 ON 상태로 유지한 채 앱으로 돌아와 다시 시도하세요.");
                return;
            }

            String id = runShell(manager, "id").trim();
            try { manager.disconnect(); } catch (Throwable ignored) {}
            statusText =
                    "APK 내부 ADB 페어링 성공 / 새 연결 준비\n" + id +
                    "\n\n페어링/연결 완료. 앱으로 돌아가 2번 R25.1.9c FIX 시작을 누르세요.";

            notifyNow(buildDoneNotification(
                    "ADB 연결 완료",
                    "앱으로 돌아가 2번 R25.1.9c FIX 시작을 누르세요."
            ));

            running = false;
            stopForeground(STOP_FOREGROUND_DETACH);
            stopSelf();
        } catch (Throwable t) {
            fail("APK 내부 ADB 페어링/연결 오류\n" + rootMessage(t));
        } finally {
            if (mdns != null) {
                try { mdns.stop(); } catch (Throwable ignored) {}
            }
        }
    }

    private String runShell(AbsAdbConnectionManager manager, String command) throws Exception {
        AdbStream stream = null;
        InputStream is = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            stream = manager.openStream("shell:" + command);
            is = stream.openInputStream();
            byte[] buffer = new byte[4096];
            while (true) {
                int n;
                try {
                    n = is.read(buffer);
                } catch (java.io.IOException ioe) {
                    String msg = String.valueOf(ioe.getMessage()).toLowerCase();
                    if (msg.contains("stream closed")) break;
                    throw ioe;
                }
                if (n < 0) break;
                if (n > 0) bos.write(buffer, 0, n);
            }
            return bos.toString(StandardCharsets.UTF_8.name());
        } finally {
            try { if (is != null) is.close(); } catch (Throwable ignored) {}
            try { if (stream != null) stream.close(); } catch (Throwable ignored) {}
        }
    }

    private Notification buildWaitingNotification() {
        return buildWaitingNotification("설정의 페어링 창을 유지한 채 여기서 입력");
    }

    private Notification buildWaitingNotification(String text) {
        RemoteInput remoteInput = new RemoteInput.Builder(KEY_CODE)
                .setLabel("6자리 코드 입력")
                .build();

        Intent pairIntent = new Intent(this, PairingService.class);
        pairIntent.setAction(ACTION_PAIR);
        PendingIntent pairPending = PendingIntent.getService(
                this,
                4602,
                pairIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );

        Notification.Action action = new Notification.Action.Builder(
                android.R.drawable.ic_menu_send,
                "6자리 코드 입력",
                pairPending
        )
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(false)
                .build();

        Intent openApp = new Intent(this, OneAppLabActivity.class);
        PendingIntent content = PendingIntent.getActivity(
                this,
                4603,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return baseBuilder()
                .setContentTitle("AA AirPods 통합본 · 페어링 대기")
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(
                        "삼성 '페어링 코드로 기기 페어링' 창을 닫지 마세요. " +
                        "상단 알림창을 내려 이 알림의 '6자리 코드 입력'을 눌러 입력하면 됩니다."
                ))
                .setContentIntent(content)
                .addAction(action)
                .setOngoing(true)
                .build();
    }

    private Notification buildProgressNotification(String text) {
        return baseBuilder()
                .setContentTitle("AA R25 Fix · 내부 ADB")
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    private Notification buildDoneNotification(String title, String text) {
        Intent openApp = new Intent(this, R25FixActivity.class);
        openApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(
                this,
                4604,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return baseBuilder()
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(content)
                .setAutoCancel(true)
                .setOngoing(false)
                .build();
    }

    private Notification.Builder baseBuilder() {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setShowWhen(true);
    }

    private void startAsForeground(Notification n) {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                    NOTIF_ID,
                    n,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            );
        } else {
            startForeground(NOTIF_ID, n);
        }
    }

    private void notifyNow(Notification n) {
        getSystemService(NotificationManager.class).notify(NOTIF_ID, n);
    }

    private void fail(String message) {
        statusText = message;
        running = false;
        notifyNow(buildDoneNotification("페어링 실패", "알림을 눌러 앱에서 상태를 확인하세요."));
        try { stopForeground(STOP_FOREGROUND_DETACH); } catch (Throwable ignored) {}
        stopSelf();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(
                    CHANNEL_ID,
                    "AA AirPods 페어링",
                    NotificationManager.IMPORTANCE_HIGH
            );
            c.setDescription("무선 디버깅 페어링 코드를 설정 화면을 닫지 않고 입력");
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    private String maskCode(String code) {
        if (code == null || code.length() != 6) return "******";
        return code.substring(0, 2) + "****";
    }

    private String rootMessage(Throwable t) {
        Throwable x = t;
        while (x.getCause() != null) x = x.getCause();
        String m = x.getMessage();
        return x.getClass().getSimpleName() + (m == null ? "" : ": " + m);
    }

    @Override
    public void onDestroy() {
        running = false;
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
