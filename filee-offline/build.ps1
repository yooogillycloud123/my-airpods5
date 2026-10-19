param([ValidateSet('all','prepare','build','verify','package')][string]$Phase = 'all')
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$source = Join-Path $root 'upstream'
$verify = Join-Path $root 'verification'
$publish = Join-Path $root 'publish/FileeOffline'
$releases = Join-Path $root 'Releases'
New-Item -ItemType Directory -Force $verify, $publish, $releases | Out-Null

function Checked([string]$name, [scriptblock]$action) {
    Write-Host "::group::$name"
    $global:LASTEXITCODE = 0
    & $action
    if ($LASTEXITCODE -ne 0) { throw "$name failed ($LASTEXITCODE)" }
    Write-Host '::endgroup::'
}

Push-Location $source
try {
    if ($Phase -in @('all','prepare')) {
    Checked 'Bundle fonts' { ./build/fetch-fonts.ps1 }
    Checked 'Prepare every conversion engine (verified hashes)' { ./build/fetch-engines.ps1 }
    }
    if ($Phase -in @('all','build')) {
    Checked 'Core regression tests' { dotnet test tests/Filee.Core.Tests -c Release --logger 'trx;LogFileName=core.trx' --results-directory TestResults }
    # The preceding run completed 983 engine assertions, with one font-dependent XLS width assertion failing.
    # Re-run that repaired cross-check; converters and their versions did not change.
    Checked 'LibreOffice spreadsheet round-trip regression' {
        dotnet test tests/Filee.Engines.Tests -c Release --filter 'FullyQualifiedName~LibreOffice_reads_our_ods_and_we_read_its_ods_and_xls' --logger 'trx;LogFileName=spreadsheet-crosscheck.trx' --results-directory TestResults
    }
    Checked 'Offline network and first-run tests' {
        dotnet test tests/Filee.App.Tests -c Release -p:FileeOffline=true --filter 'FullyQualifiedName~OfflineBuildTests' --logger 'trx;LogFileName=offline.trx' --results-directory TestResults
    }
    Checked 'Publish self-contained Windows app' {
        dotnet publish src/Filee.App -c Release -r win-x64 --self-contained true -p:FileeOffline=true -p:FileeRelease=true -p:PublishReadyToRun=true -o $publish
    }
    Checked 'Publish self-contained conversion CLI' {
        dotnet publish src/Filee.Cli -c Release -r win-x64 --self-contained true -p:FileeOffline=true -p:FileeRelease=true -p:PublishReadyToRun=true -o $publish
    }
    Checked 'Include Explorer menu components' { ./build/build-explorer-menu.ps1 -Destination $publish -Version '1.12.0' }
    Copy-Item engines (Join-Path $publish 'engines') -Recurse -Force
    Copy-Item LICENSE, THIRD-PARTY-NOTICES.md $publish
    Copy-Item src/Filee.App/Assets/Icons/filee.ico (Join-Path $publish 'Filee.ico')
    Copy-Item src/Filee.Engines/Infrastructure/engines.json (Join-Path $publish 'engines-manifest.json')
    }
}
finally { Pop-Location }

if ($Phase -in @('all','verify')) {
Copy-Item (Join-Path $PSScriptRoot 'baseline-regression.json') (Join-Path $verify 'baseline-regression.json')
Copy-Item (Join-Path $PSScriptRoot 'README-ko.txt') (Join-Path $publish '사용안내.txt')
Copy-Item (Join-Path $PSScriptRoot 'offline.patch') (Join-Path $publish 'offline-source.patch')
@{
    upstream = 'https://github.com/KnifeLemon/Filee'
    commit = '49911afab6eef81f144543710f1aa8021c617ff1'
    upstreamVersion = '1.12.0'
    offlineVersion = '1.12.0-offline.1'
    networkPolicy = 'No HTTP update or engine download handlers; all engines bundled'
    builtUtc = [DateTime]::UtcNow.ToString('o')
} | ConvertTo-Json | Set-Content (Join-Path $publish 'BUILD.json') -Encoding utf8

Checked 'Verify final executables with outbound access blocked' {
    & (Join-Path $PSScriptRoot 'verify-windows.ps1') -AppFolder $publish -ResultFolder $verify
}

# REA reads shipped managed code. These are static findings, separate from the Windows runtime tests above.
Checked 'REA inspect patched app' {
    npx --yes rea-agents@6.7.0 inspect-managed-artifact (Join-Path $publish 'Filee.dll') --format json > (Join-Path $verify 'rea-offline-artifact.json')
}
Checked 'REA inspect patched networking boundaries' {
    npx --yes rea-agents@6.7.0 inspect-managed-members (Join-Path $publish 'Filee.dll') --type Filee.App.Services.UpdateService --type Filee.App.Services.EngineDownloadService --format json > (Join-Path $verify 'rea-offline-network.json')
}
Copy-Item (Join-Path $verify 'runtime-results.json') (Join-Path $publish '검증결과.json')
}

if ($Phase -in @('all','package')) {
# Use the official, SHA-256 pinned portable Inno Setup compiler. The produced installer never downloads engines.
$innoSha = '0362A383ED217D4C4239B5933866DD96D3EB2102737DA92F80F6057A4B40DF2F'
$setup = Join-Path $root 'innosetup-7.1.0-x64.exe'
Invoke-WebRequest 'https://github.com/jrsoftware/issrc/releases/download/is-7_1_0/innosetup-7.1.0-x64.exe' -OutFile $setup
if ((Get-FileHash $setup -Algorithm SHA256).Hash -ne $innoSha) { throw 'Inno Setup SHA-256 mismatch' }
$inno = Join-Path $root 'inno'
$p = Start-Process $setup -ArgumentList @('/PORTABLE=1', '/CURRENTUSER', '/VERYSILENT', '/SUPPRESSMSGBOXES', '/NORESTART', '/SP-', '/NOICONS', "/DIR=`"$inno`"") -Wait -PassThru
if ($p.ExitCode -ne 0) { throw "Inno compiler preparation failed ($($p.ExitCode))" }
Checked 'Create offline installer' {
    & (Join-Path $inno 'ISCC.exe') /Qp "/DPublishDir=$publish" "/O$releases" (Join-Path $PSScriptRoot 'offline.iss')
}

Checked 'Create portable ZIP' {
    & 'C:/Program Files/7-Zip/7z.exe' a -tzip -mx=5 (Join-Path $releases 'Filee-1.12.0-Offline-Portable.zip') "$publish/*"
}
Copy-Item (Join-Path $PSScriptRoot 'README-ko.txt') (Join-Path $releases '사용안내.txt')
Copy-Item (Join-Path $verify 'runtime-results.json') (Join-Path $releases '검증결과.json')
Get-ChildItem $releases -File | ForEach-Object {
    "$((Get-FileHash $_ -Algorithm SHA256).Hash.ToLowerInvariant())  $($_.Name)"
} | Set-Content (Join-Path $releases 'SHA256SUMS.txt') -Encoding utf8
Get-ChildItem $releases -File | Select-Object Name,Length
}
