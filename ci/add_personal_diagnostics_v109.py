#!/usr/bin/env python3
"""AirPods personal v1.0.9 DIAGNOSTICS FIRST; no speculative audio fixes.

Collect Java fatal stack traces before the process dies and persist bounded
lifecycle breadcrumbs. On each launch an Android-native dialog can copy/share
the report. Offline/on-device; no network, upload, root or additional app.
Does not change the Bluetooth/AACP/ATT/noise-control engine.
"""
from pathlib import Path
import sys

root=Path(sys.argv[1])
base=root/"android/app/src/main/java/me/kavishdevar/librepods"
def swap(rel,old,new,label):
    f=base/rel
    s=f.read_text(encoding="utf-8")
    n=s.count(old)
    if n!=1:
        raise RuntimeError(f"{label} SOURCE MISMATCH n={n}")
    f.write_text(s.replace(old,new,1), encoding="utf-8")
    print("DIAG_PATCH_PASS",label)

new_file=base/"AirPodsCrashDiary.kt"
assert not new_file.exists()
new_file.write_text("""package me.kavishdevar.librepods

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.widget.Toast
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Personal local-only diagnostic mode: never uploads user logs.
 * The original uncaught handler still receives every fatal exception.
 * System/native crashes without a Java stack still require adb logcat.
 */
object AirPodsCrashDiary {
    private val installed = AtomicBoolean(false)
    private val fatalLock = Any()
    private const val LIMIT = 24_000

    private fun stamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date())

    fun mark(context: Context, stage: String) {
        try {
            synchronized(fatalLock) {
                val f = File(context.filesDir, "airpods_local_events.txt")
                if (f.exists() && f.length() > LIMIT) {
                    val previous = f.readText()
                    f.writeText(previous.takeLast(LIMIT / 2))
                }
                f.appendText(stamp() + " pid=" + Process.myPid() +
                    " thread=" + Thread.currentThread().name + " " +
                    stage.take(350) + "\\n")
            }
        } catch (_: Throwable) { /* diagnostics must never crash the app */ }
    }

    fun install(app: Application) {
        if (!installed.compareAndSet(false, true)) return
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val sw = StringWriter()
                error.printStackTrace(PrintWriter(sw))
                val text = "AIRPODS_JAVA_FATAL=" + stamp() +
                    "\\nprocess=" + Process.myPid() +
                    "\\nthread=" + thread.name +
                    "\\n" + sw.toString().take(48_000)
                File(app.filesDir, "airpods_last_fatal.txt").writeText(text)
                mark(app, "FATAL " + error.javaClass.name + ": " + error.message)
            } catch (_: Throwable) { }
            if (originalHandler != null) {
                originalHandler.uncaughtException(thread, error)
            } else {
                Process.killProcess(Process.myPid())
            }
        }
        mark(app, "Application.onCreate crash handler installed")
    }

    fun report(context: Context): String {
        val fatal = try {
            File(context.filesDir,"airpods_last_fatal.txt")
                .takeIf { it.isFile }?.readText()?.takeLast(48_000)
                ?: "NO_JAVA_FATAL_CAPTURED - collect Android system bugreport/adb logcat if Home exit continues"
        } catch (e: Exception) { "FATAL_LOG_READ_ERROR: " + e.message }
        val history = try {
            File(context.filesDir,"airpods_local_events.txt")
                .takeIf { it.isFile }?.readText()?.takeLast(LIMIT) ?: "NO_EVENTS"
        } catch (e: Exception) { "EVENT_LOG_READ_ERROR: " + e.message }
        return ("My AirPods 5 1.0.9 local diagnostics\\n" +
            "generated=" + stamp() +
            "\\ndevice=" + Build.MANUFACTURER + " " + Build.MODEL +
            " sdk=" + Build.VERSION.SDK_INT +
            "\\nappPackage=" + context.packageName +
            "\\n\\n--- JAVA FATAL ---\\n" + fatal +
            "\\n\\n--- LIFECYCLE EVENTS ---\\n" + history).takeLast(68_000)
    }

    fun show(activity: Activity) {
        try {
            val hasFatal = File(activity.filesDir,"airpods_last_fatal.txt").isFile
            val title = if (hasFatal) "에어팟 오류 기록 발견" else "에어팟 진단 모드"
            val explanation =
                "1. [앱 실행]으로 들어가 문제를 재현하세요.\\n" +
                "2. 다시 앱을 열고 [로그 복사]를 눌러 채팅에 붙여넣으세요.\\n" +
                "3. 앱이 열리지 않으면 PC ADB 시스템 로그가 필요합니다.\\n" +
                "이 기록은 휴대폰 내부에만 저장되고 자동 전송되지 않습니다." +
                if (hasFatal) "\\n\\n이전 실행에서 Java 오류 기록이 저장됐습니다." else ""
            AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(explanation)
                .setCancelable(false)
                .setPositiveButton("앱 실행") { dialog, _ -> dialog.dismiss() }
                .setNeutralButton("로그 복사") { dialog, _ ->
                    val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE)
                        as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText("AirPods diagnostic report",report(activity))
                    )
                    Toast.makeText(activity,"로그 복사됨 · 채팅에 붙여넣기",Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                }
                .setNegativeButton("로그 공유") { dialog, _ ->
                    try {
                        val i=Intent(Intent.ACTION_SEND).apply {
                            type="text/plain"
                            putExtra(Intent.EXTRA_TEXT,report(activity))
                        }
                        activity.startActivity(Intent.createChooser(i,"에어팟 오류 기록 보내기"))
                    } catch (_: Exception) {
                        Toast.makeText(activity,"로그 공유 실패 · 로그 복사를 사용하세요",Toast.LENGTH_LONG).show()
                    }
                    dialog.dismiss()
                }
                .show()
        } catch (e: Exception) {
            mark(activity,"CRASH_DIALOG_ERROR " + e.toString())
        }
    }
}
""",encoding="utf-8")
print("DIAG_NEW_KOTLIN_CLASS=PASS",new_file)

swap("LibrePodsApplication.kt",
    "    override fun onCreate() {\n",
    "    override fun onCreate() {\n        AirPodsCrashDiary.install(this)\n",
    "early_global_java_uncaught_exception_capture")

swap("MainActivity.kt",
    '''        super.onCreate(savedInstanceState)
        enableEdgeToEdge()''',
    '''        super.onCreate(savedInstanceState)
        AirPodsCrashDiary.mark(this, "Activity.onCreate begin")
        AirPodsCrashDiary.show(this)
        enableEdgeToEdge()''',
    "show_copyable_diagnostic_dialog_before_risky_compose")

swap("MainActivity.kt",
    '''            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as AirPodsService.LocalBinder''',
    '''            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                AirPodsCrashDiary.mark(context, "ServiceConnection.onServiceConnected begin")
                val binder = service as AirPodsService.LocalBinder''',
    "record_service_binder_connect")

swap("MainActivity.kt",
    '''                        airPodsViewModel.init(
                            service = service,''',
    '''                        AirPodsCrashDiary.mark(context, "AirPodsViewModel.init START")
                        airPodsViewModel.init(
                            service = service,''',
    "record_viewmodel_init_enter")

swap("MainActivity.kt",
    '''                            appContext = context.applicationContext
                        )
                    } catch (e: Exception) {''',
    '''                            appContext = context.applicationContext
                        )
                        AirPodsCrashDiary.mark(context, "AirPodsViewModel.init COMPLETE")
                    } catch (e: Exception) {''',
    "record_viewmodel_init_complete")

swap("MainActivity.kt",
    '''            Log.e("MainActivity", "Could not bind AirPodsService", e)
        }
    }''',
    '''            Log.e("MainActivity", "Could not bind AirPodsService", e)
            AirPodsCrashDiary.mark(context, "BIND_EXCEPTION " + e.toString())
        }
    }''',
    "record_binding_exception")

swap("MainActivity.kt",
    '''    override fun onDestroy() {
        try {''',
    '''    override fun onDestroy() {
        AirPodsCrashDiary.mark(this, "Activity.onDestroy")
        try {''',
    "record_activity_destroy")

gradle=root/"android/app/build.gradle.kts"
g=gradle.read_text(encoding="utf-8")
assert g.count('versionCode = 10008')==1
assert g.count('versionName = "1.0.8-single-ear-safe"')==1
g=g.replace('versionCode = 10008','versionCode = 10009',1)
g=g.replace('versionName = "1.0.8-single-ear-safe"','versionName = "1.0.9-forensic-log"',1)
gradle.write_text(g,encoding="utf-8")
print("DIAG_V109_VERSION=PASS")
assert (base/"bluetooth").is_dir()
print("NO_AUDIO_PROTOCOL_CHANGES_IN_V109=PASS")
