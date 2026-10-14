package me.kavishdevar.librepods

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

/** In-app UPDATE (user initiated). Never bypass Android package signature checks. */
object PersonalUpdateManager {
    private const val CHANNEL =
        "https://raw.githubusercontent.com/yooogillycloud123/my-airpods5/main/update-channel.json"
    private const val PREFIX =
        "https://github.com/yooogillycloud123/my-airpods5/releases/download/"
    private val main = Handler(Looper.getMainLooper())
    private const val MAX_APK_BYTES = 140L * 1024L * 1024L

    private fun ui(activity: Activity, action: () -> Unit) {
        main.post {
            if (!activity.isFinishing && !activity.isDestroyed) action()
        }
    }

    private fun alert(activity: Activity, title: String, body: String) {
        ui(activity) {
            AlertDialog.Builder(activity).setTitle(title).setMessage(body)
                .setPositiveButton("확인", null).show()
        }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun signerForInstalled(activity: Activity): String? {
        return try {
            val info = if (Build.VERSION.SDK_INT >= 33) {
                activity.packageManager.getPackageInfo(
                    activity.packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                activity.packageManager.getPackageInfo(
                    activity.packageName, PackageManager.GET_SIGNING_CERTIFICATES
                )
            }
            info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()?.let(::sha256)
        } catch (e: Exception) { null }
    }

    private fun signerForApk(activity: Activity, apk: File): String? {
        return try {
            @Suppress("DEPRECATION")
            val info = activity.packageManager.getPackageArchiveInfo(
                apk.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES
            )
            info?.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()?.let(::sha256)
        } catch (e: Exception) { null }
    }

    fun check(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        ui(activity) { android.widget.Toast.makeText(activity, "업데이트 확인 중…", android.widget.Toast.LENGTH_SHORT).show() }
        Thread({
            try {
                val connection = URL(CHANNEL).openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 15000
                connection.instanceFollowRedirects = true
                connection.useCaches = false
                val jsonText = try {
                    if (connection.responseCode != 200) {
                        throw IllegalStateException("버전 정보 서버 응답 ${connection.responseCode}")
                    }
                    connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText().take(12000) }
                } finally { connection.disconnect() }
                val info = JSONObject(jsonText)
                val targetPackage = info.getString("packageName")
                val targetCode = info.getLong("versionCode")
                val targetName = info.getString("versionName")
                val targetUrl = info.getString("apkUrl")
                val targetHash = info.getString("sha256").lowercase(Locale.US)
                val targetCert = info.getString("signerSha256").lowercase(Locale.US)
                if (targetPackage != activity.packageName) {
                    throw IllegalArgumentException("다른 앱의 업데이트 정보")
                }
                if (!targetHash.matches(Regex("[0-9a-f]{64}")) ||
                    !targetCert.matches(Regex("[0-9a-f]{64}")) ||
                    !targetUrl.startsWith(PREFIX)) {
                    throw IllegalArgumentException("서명/배포 검증 정보가 올바르지 않음")
                }
                if (targetCode <= BuildConfig.VERSION_CODE.toLong()) {
                    alert(activity, "최신 버전", "현재 버전 ${BuildConfig.VERSION_NAME} — 업데이트가 필요하지 않습니다.")
                    return@Thread
                }
                val installedCert = signerForInstalled(activity)
                if (installedCert == null || installedCert != targetCert) {
                    alert(activity, "안전한 업데이트 불가",
                        "새 버전(${targetName})의 APK 서명키가 기존 설치본과 다릅니다. " +
                        "기존 데이터를 보호하기 위해 자동 설치를 중단했습니다. " +
                        "원본 앱이나 현재 정상 앱을 삭제하지 마세요.")
                    return@Thread
                }
                ui(activity) {
                    AlertDialog.Builder(activity)
                        .setTitle("업데이트 발견")
                        .setMessage("${BuildConfig.VERSION_NAME} → ${targetName}\n" +
                            "GitHub에서 APK를 내려받고 파일·서명·패키지를 모두 검증한 뒤 설치 화면을 엽니다.")
                        .setNegativeButton("취소", null)
                        .setPositiveButton("확인 및 설치") { _, _ ->
                            download(activity, targetCode, targetUrl, targetHash, targetCert)
                        }
                        .show()
                }
            } catch (e: Exception) {
                alert(activity, "업데이트 확인 실패", e.message ?: "인터넷 연결을 확인하세요.")
            }
        }, "airpods-update-check").start()
    }

    private fun download(
        activity: Activity, targetCode: Long, url: String, expectedHash: String, cert: String
    ) {
        Thread({
            val folder = File(activity.cacheDir, "updates")
            val candidate = File(folder, "my-airpods5-update.apk")
            try {
                folder.mkdirs()
                candidate.delete()
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 15000
                c.readTimeout = 30000
                c.instanceFollowRedirects = true
                if (c.responseCode != 200) {
                    throw IllegalStateException("APK 다운로드 실패 (HTTP ${c.responseCode})")
                }
                val digest = MessageDigest.getInstance("SHA-256")
                var total = 0L
                try {
                    c.inputStream.use { input ->
                        candidate.outputStream().use { output ->
                            val chunk = ByteArray(32768)
                            while (true) {
                                val n = input.read(chunk)
                                if (n == -1) break
                                total += n
                                if (total > MAX_APK_BYTES) throw IllegalStateException("APK 최대 크기 초과")
                                digest.update(chunk, 0, n)
                                output.write(chunk, 0, n)
                            }
                        }
                    }
                } finally { c.disconnect() }
                val hash = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
                if (hash != expectedHash) throw SecurityException("APK SHA-256 무결성 불일치")
                val parsed = activity.packageManager.getPackageArchiveInfo(
                    candidate.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES
                ) ?: throw SecurityException("APK 패키지 분석 불가")
                if (parsed.packageName != activity.packageName ||
                    parsed.longVersionCode != targetCode ||
                    signerForApk(activity, candidate) != cert) {
                    throw SecurityException("패키지·버전·서명 중 하나라도 불일치")
                }
                ui(activity) {
                    if (Build.VERSION.SDK_INT >= 26 && !activity.packageManager.canRequestPackageInstalls()) {
                        AlertDialog.Builder(activity).setTitle("설치 권한 필요")
                            .setMessage("앱 업데이트를 설치하려면 이 앱의 '알 수 없는 앱 설치'를 허용해야 합니다. " +
                                "설정 후 업데이트 확인을 다시 눌러주세요.")
                            .setNegativeButton("취소", null)
                            .setPositiveButton("설정") { _, _ ->
                                activity.startActivity(Intent(
                                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:${activity.packageName}")
                                ))
                            }.show()
                        return@ui
                    }
                    val contentUri = FileProvider.getUriForFile(
                        activity, "${activity.packageName}.provider", candidate
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(contentUri, "application/vnd.android.package-archive")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    activity.startActivity(intent)
                }
            } catch (e: Exception) {
                candidate.delete()
                alert(activity, "안전 검증 실패", e.message ?: "다운로드된 파일을 사용할 수 없습니다.")
            }
        }, "airpods-update-download").start()
    }
}
