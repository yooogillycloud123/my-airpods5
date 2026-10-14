# My AirPods 5 - extract SYSTEM crash evidence (read-only) with ADB.
# Use only if in-app Java crash recorder does not capture the Home exit.
# Requires Android platform-tools adb and developer USB debugging approved on phone.
$ErrorActionPreference = "Stop"
$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$out = Join-Path (Get-Location) "MyAirPods5_SystemCrash_$stamp.txt"
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
  Write-Host "ADB not found. Install Android Platform Tools, then rerun this script."
  exit 2
}
$devices = & adb devices
if (-not ($devices -match "device$")) {
  Write-Host "Connect your Galaxy to USB, approve USB debugging, and retry."
  exit 3
}
@"
MY AIRPODS 5 SYSTEM CRASH REPORT
Date: $(Get-Date)
Package: com.myairpods5.personal
NOTE: logs can contain private names/device IDs; review before sharing.
"@ | Set-Content -LiteralPath $out -Encoding UTF8
"--- CRASH BUFFER ---" | Add-Content $out
& adb logcat -b crash -d -v threadtime 2>&1 | Out-String | Add-Content $out
"--- MAIN/SYSTEM RELEVANT CONTEXT ---" | Add-Content $out
$all = & adb logcat -b main -b system -d -t 12000 -v threadtime 2>&1
$all | Select-String -Pattern 'com\.myairpods5\.personal|librepods|AirPodsService|AndroidRuntime|FATAL EXCEPTION|ActivityTaskManager|ActivityManager|Fatal signal|am_crash|DeadObjectException' -Context 5,12 | Out-String | Add-Content $out
Write-Host "Created $out"
Write-Host "Review before uploading; share it in ChatGPT for precise crash diagnosis."
