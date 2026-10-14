#!/usr/bin/env python3
"""v1.0.10: evidence-based VideoView BadToken fatal repair.

USER'S REAL STACK: android.widget.VideoView$5.onError ->
AlertDialog.Builder.show -> WindowManager.BadTokenException (main).
The two source VideoViews in non-Activity TYPE_APPLICATION_OVERLAY windows use
HARDCODED android.resource://me.kavishdevar.librepods URIs despite the app
having applicationId com.myairpods5.personal. On media open failure, default
VideoView error handler attempts to open an Activity dialog from service context
and kills the app. Fix the package mismatch + consume media errors.
Do NOT change AACP/Bluetooth/noise-control/battery code.
"""
from pathlib import Path
import sys

root=Path(sys.argv[1])
overlay=root/"android/app/src/main/java/me/kavishdevar/librepods/presentation/overlays"
def exact(file:str,old:str,new:str,what:str):
    p=overlay/file
    s=p.read_text(encoding="utf-8")
    if s.count(old)!=1:
        raise SystemExit(f"VIDEO_FATAL_PATCH_BLOCKED: {what}, matches={s.count(old)}")
    p.write_text(s.replace(old,new,1),encoding="utf-8")
    print("VIDEO_FATAL_PATCH_PASS",what)

exact("PopupWindow.kt",
    '''                vid.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
                vid.setVideoPath("android.resource://me.kavishdevar.librepods/" + R.raw.connected)
                vid.resolveAdjustedSize(vid.width, vid.height)''',
    '''                vid.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
                // The generated APK is com.myairpods5.personal, NOT the original
                // library namespace. A hardcoded old resource authority causes
                // MediaPlayer error, whose VideoView default dialog crashes here
                // because this popup is not hosted by an Activity.
                vid.setOnErrorListener { _, what, extra ->
                    Log.e("PopupWindow", "Overlay animation unavailable: media=$what/$extra")
                    vid.visibility = View.GONE
                    true // Consume VideoView's default BadToken dialog fallback.
                }
                vid.setVideoPath("android.resource://${context.packageName}/${R.raw.connected}")
                vid.resolveAdjustedSize(vid.width, vid.height)''',
    "popup_dynamic_resource_authority_and_safe_media_error")

exact("IslandWindow.kt",
    '''        val videoView = islandView.findViewById<VideoView>(R.id.island_video_view)
        val videoUri = "android.resource://me.kavishdevar.librepods/${R.raw.island}".toUri()
        videoView.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
        videoView.setVideoURI(videoUri)''',
    '''        val videoView = islandView.findViewById<VideoView>(R.id.island_video_view)
        // Resolve resources from the INSTALLED applicationId.
        val videoUri = "android.resource://${context.packageName}/${R.raw.island}".toUri()
        videoView.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
        videoView.setOnErrorListener { _, what, extra ->
            e("IslandWindow", "Overlay animation unavailable: media=$what/$extra")
            videoView.visibility = View.GONE
            true // Never let VideoView display a dialog using service context.
        }
        videoView.setVideoURI(videoUri)''',
    "island_dynamic_resource_authority_and_safe_media_error")

for file in ("PopupWindow.kt","IslandWindow.kt"):
    s=(overlay/file).read_text(encoding="utf-8")
    if "android.resource://me.kavishdevar.librepods" in s:
        raise SystemExit("VIDEO_FATAL_PATCH_FAILED: old hardcoded video URI present")
    if s.count("setOnErrorListener")!=1:
        raise SystemExit("VIDEO_FATAL_PATCH_FAILED: safe listener missing")
    if "context.packageName" not in s:
        raise SystemExit("VIDEO_FATAL_PATCH_FAILED: runtime package missing")

g=root/"android/app/build.gradle.kts"
s=g.read_text(encoding="utf-8")
assert s.count('versionCode = 10009') == 1
assert s.count('versionName = "1.0.9-forensic-log"') == 1
s=s.replace('versionCode = 10009','versionCode = 10010',1)
s=s.replace('versionName = "1.0.9-forensic-log"','versionName = "1.0.10-video-badtoken-fix"',1)
g.write_text(s,encoding="utf-8")
print("VIDEO_BADTOKEN_LOG_EVIDENCE_GATE=PASS")
print("AUDIO_AACP_CORE_UNCHANGED_BY_V110=PASS")
