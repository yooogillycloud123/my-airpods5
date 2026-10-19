param([Parameter(Mandatory)][string]$AppFolder, [Parameter(Mandatory)][string]$ResultFolder)
$ErrorActionPreference = 'Stop'
$AppFolder = (Resolve-Path $AppFolder).Path
New-Item -ItemType Directory -Force -Path $ResultFolder | Out-Null
$ResultFolder = (Resolve-Path $ResultFolder).Path
$work = Join-Path $ResultFolder 'cold-start-and-conversions'
$inputs = Join-Path $work '한글 경로 시험'
New-Item -ItemType Directory -Force -Path $inputs | Out-Null
Expand-Archive (Join-Path $PSScriptRoot 'fixtures.zip') $inputs -Force
$results = [System.Collections.Generic.List[object]]::new()
$ruleGroup = 'FileeOfflineTest-' + [Guid]::NewGuid().ToString('N')
$oldPath = $env:PATH
$oldData = $env:FILEE_DATA_DIR
$oldDotnet = $env:DOTNET_ROOT
$oldDotnetX64 = $env:DOTNET_ROOT_X64
$before = @{}
Get-ChildItem $inputs -File | ForEach-Object { $before[$_.FullName] = (Get-FileHash $_ -Algorithm SHA256).Hash }

$engines = @{
    rhwp = 'engines/rhwp/rhwp.exe'
    archives = 'engines/7zip/7z.exe'
    ffmpeg = 'engines/ffmpeg/bin/ffmpeg.exe'
    ffprobe = 'engines/ffmpeg/bin/ffprobe.exe'
    pandoc = 'engines/pandoc/pandoc.exe'
    ghostscript = 'engines/ghostscript/gswin64c.exe'
    libreoffice = 'engines/libreoffice/program/soffice.exe'
    java = 'engines/jre/bin/java.exe'
    calibre = 'engines/calibre/ebook-convert.exe'
}
foreach ($entry in $engines.GetEnumerator()) {
    if (-not (Test-Path (Join-Path $AppFolder $entry.Value))) { throw "Bundled engine missing: $($entry.Key)" }
}
foreach ($name in 'Filee.exe', 'filee-cli.exe', 'coreclr.dll', 'hostfxr.dll', 'hostpolicy.dll') {
    if (-not (Test-Path (Join-Path $AppFolder $name))) { throw "Self-contained runtime file missing: $name" }
}

function Run-Case([string]$id, [string]$source, [string]$target, [string]$extension = $target) {
    $out = Join-Path $work $id
    New-Item -ItemType Directory -Force -Path $out | Out-Null
    $logs = Join-Path $work 'process-logs'
    New-Item -ItemType Directory -Force -Path $logs | Out-Null
    $stdout = Join-Path $logs "$id.stdout.json"
    $stderr = Join-Path $logs "$id.stderr.txt"
    $arguments = @('convert', "`"$(Join-Path $inputs $source)`"", '--to', $target, '--json', '-o', "`"$out`"")
    $clock = [Diagnostics.Stopwatch]::StartNew()
    $p = Start-Process (Join-Path $AppFolder 'filee-cli.exe') -ArgumentList $arguments -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru -WorkingDirectory $AppFolder
    if (-not $p.WaitForExit(180000)) {
        & taskkill.exe /PID $p.Id /T /F 2>$null | Out-Null
        throw "$id timed out"
    }
    $p.Refresh()
    $files = @(Get-ChildItem $out -File -Filter "*.$extension")
    if ($p.ExitCode -ne 0 -or $files.Count -eq 0) {
        Get-Content $stdout, $stderr -ErrorAction SilentlyContinue | Write-Host
        throw "$id failed: exit=$($p.ExitCode), output files=$($files.Count)"
    }
    foreach ($file in $files) {
        if ($file.Length -eq 0) { throw "$id produced an empty file" }
        $stream = [IO.File]::OpenRead($file.FullName)
        try { $bytes = New-Object byte[] 8; $read = $stream.Read($bytes,0,8) } finally { $stream.Dispose() }
        if ($extension -eq 'pdf' -and [Text.Encoding]::ASCII.GetString($bytes,0,5) -ne '%PDF-') { throw "$id invalid PDF" }
        if ($extension -in @('docx','xlsx','hwpx','epub','odt') -and ($bytes[0] -ne 80 -or $bytes[1] -ne 75)) { throw "$id invalid package" }
        if ($extension -eq 'png' -and ($bytes[0] -ne 137 -or $bytes[1] -ne 80)) { throw "$id invalid PNG" }
    }
    $results.Add(@{ id=$id; source=$source; target=$target; status='PASS'; seconds=[Math]::Round($clock.Elapsed.TotalSeconds,2); outputs=@($files | ForEach-Object { @{ name=$_.Name; bytes=$_.Length; sha256=(Get-FileHash $_ -Algorithm SHA256).Hash.ToLowerInvariant() } }) })
    Write-Host "$id PASS ($($clock.Elapsed.TotalSeconds.ToString('0.0'))s)"
    return $files[0].FullName
}

try {
    $profiles = @(Get-NetFirewallProfile)
    if (@($profiles | Where-Object { -not $_.Enabled }).Count -gt 0) { throw 'Windows firewall profiles must be enabled for this verification' }
    $programs = @(Get-ChildItem $AppFolder -Recurse -File | Where-Object { $_.Extension -in @('.exe','.bin') })
    foreach ($program in $programs) {
        New-NetFirewallRule -DisplayName "$ruleGroup $($program.Name)" -Group $ruleGroup -Direction Outbound -Action Block -Profile Any -Enabled True -Program $program.FullName | Out-Null
    }
    $rules = @(Get-NetFirewallRule -Group $ruleGroup)
    if ($rules.Count -ne $programs.Count) { throw 'Outbound block rule count mismatch' }
    Write-Host "Blocked outbound traffic for $($rules.Count) packaged executables."

    # No installed .NET or engine on PATH can satisfy the tests. Filee must use its own runtime and engines.
    $env:PATH = "$env:SystemRoot/System32;$env:SystemRoot"
    $env:DOTNET_ROOT = Join-Path $work 'no-installed-dotnet'
    $env:DOTNET_ROOT_X64 = $env:DOTNET_ROOT
    $env:DOTNET_MULTILEVEL_LOOKUP = '0'
    $env:FILEE_DATA_DIR = Join-Path $work 'first-launch-user-data'

    $gui = Start-Process (Join-Path $AppFolder 'Filee.exe') -PassThru -WorkingDirectory $AppFolder
    try {
        # Startup includes the 10-second local LibreOffice warm-up. No settings or first-run downloads exist yet.
        for ($i=0; $i -lt 12; $i++) {
            Start-Sleep -Seconds 1
            $gui.Refresh()
            if ($gui.HasExited) { throw "GUI cold start exited unexpectedly ($($gui.ExitCode))" }
        }
        $ui = @{ processAlive=$true; windowHandleObserved=($gui.MainWindowHandle -ne 0); seconds=12; manualDragTest=$false }
    }
    finally { if (-not $gui.HasExited) { Stop-Process -Id $gui.Id -Force } }

    $cli = Join-Path $AppFolder 'filee-cli.exe'
    $version = (& $cli version | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $version -notmatch 'offline') { throw "Unexpected packaged CLI: $version" }

    $null = Run-Case '01-image-jpg' '사진.png' 'jpg'
    $null = Run-Case '02-image-webp' '사진.png' 'webp'
    $pdf = Run-Case '03-image-pdf' '사진.png' 'pdf'
    Copy-Item $pdf (Join-Path $inputs '그림.pdf')
    $null = Run-Case '04-pdf-png' '그림.pdf' 'png'
    $hwpx = Run-Case '05-text-hwpx' '안내문.txt' 'hwpx'
    Copy-Item $hwpx (Join-Path $inputs '안내문.hwpx')
    $pdf = Run-Case '06-hwpx-pdf' '안내문.hwpx' 'pdf'
    Copy-Item $pdf (Join-Path $inputs '안내문.pdf')
    $null = Run-Case '07-pdf-text' '안내문.pdf' 'txt'
    $null = Run-Case '08-hwpx-docx' '안내문.hwpx' 'docx'
    $hwp = Run-Case '09-hwpx-hwp' '안내문.hwpx' 'hwp'
    Copy-Item $hwp (Join-Path $inputs '안내문.hwp')
    $null = Run-Case '10-hwp-pdf' '안내문.hwp' 'pdf'
    $null = Run-Case '11-hwp-text' '안내문.hwp' 'txt'
    $null = Run-Case '12-word-pdf' '보고서.docx' 'pdf'
    $null = Run-Case '13-excel-pdf' '업무명부.xlsx' 'pdf'
    $null = Run-Case '14-excel-csv' '업무명부.xlsx' 'csv'
    $null = Run-Case '15-powerpoint-pdf' '업무안내.pptx' 'pdf'
    $null = Run-Case '16-wave-mp3' '녹음.wav' 'mp3'
    $null = Run-Case '17-wave-flac' '녹음.wav' 'flac'
    $null = Run-Case '18-zip-7z' '묶음.zip' '7z'
    $null = Run-Case '19-epub-pdf' '안내책.epub' 'pdf'
    $null = Run-Case '20-eps-pdf' '도형.eps' 'pdf'
    $null = Run-Case '21-rtf-docx' '간단문서.rtf' 'docx'
    $null = Run-Case '22-word-odt' '보고서.docx' 'odt'

    $media = Join-Path $inputs '시험영상.mp4'
    & (Join-Path $AppFolder 'engines/ffmpeg/bin/ffmpeg.exe') -hide_banner -loglevel error -f lavfi -i 'testsrc=size=96x64:rate=10' -f lavfi -i 'sine=frequency=440:sample_rate=44100' -t 1.2 -c:v libx264 -pix_fmt yuv420p -c:a aac -y $media
    if ($LASTEXITCODE -ne 0) { throw 'Offline FFmpeg fixture generation failed' }
    $null = Run-Case '23-video-webm' '시험영상.mp4' 'webm'
    $null = Run-Case '24-video-audio' '시험영상.mp4' 'mp3'

    foreach ($file in $before.Keys) {
        if ((Get-FileHash $file -Algorithm SHA256).Hash -ne $before[$file]) { throw "Original fixture was modified: $file" }
    }
    @{
        status='PASS'; testedUtc=[DateTime]::UtcNow.ToString('o'); operatingSystem=[Environment]::OSVersion.VersionString
        version=$version; sourceCommit='49911afab6eef81f144543710f1aa8021c617ff1'
        outboundFirewallBlocked=$true; blockedProgramCount=$rules.Count; inheritedEnginePathRemoved=$true
        installedDotnetPathRemoved=$true; coldStart=$ui; originalFilesPreserved=$true
        totalConversions=$results.Count; conversions=$results
        limitations=@('Windows hosted runner; not the actual company PC', 'GUI liveness observed; manual Explorer Ctrl-drag not exercised', 'Synthetic representative documents; not a guarantee for every document layout')
    } | ConvertTo-Json -Depth 8 | Set-Content (Join-Path $ResultFolder 'runtime-results.json') -Encoding utf8
}
finally {
    $env:PATH=$oldPath
    $env:FILEE_DATA_DIR=$oldData
    $env:DOTNET_ROOT=$oldDotnet
    $env:DOTNET_ROOT_X64=$oldDotnetX64
    Get-NetFirewallRule -Group $ruleGroup -ErrorAction SilentlyContinue | Remove-NetFirewallRule
}
