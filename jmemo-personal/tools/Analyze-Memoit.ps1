# Read-only REA audit of user-provided original Memoit installer.
# Does NOT install or execute the target, does NOT copy installer to GitHub.
param(
  [Parameter(Mandatory=$true)][string]$Installer,
  [string]$OutDir = ".\\memoit-rea-evidence"
)
$ErrorActionPreference = "Stop"
$source = (Resolve-Path -LiteralPath $Installer).Path
if (!(Test-Path $source -PathType Leaf)) { throw "Installer missing" }
if ([IO.Path]::GetExtension($source).ToLowerInvariant() -ne ".exe") { throw "Expected EXE" }
$expected = "af3c6eea56afa8a2523d1ef703c3204a3596851adec9b6da1f9e03d198bea05e"
$hash = (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash.ToLowerInvariant()
if ($hash -ne $expected) { throw "SHA256 mismatch. Stop: wrong installer." }
New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
$meta = @{
  target = [IO.Path]::GetFileName($source)
  target_sha256 = $hash
  evidence_kind = "static_installer_only"
  executed_target = $false
  note = "NSIS setup is not the installed main app. No behavior parity asserted."
}
$meta | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $OutDir "target.json")
if (!(Get-Command rea -ErrorAction SilentlyContinue)) {
  Write-Host "REA is not installed. On an Internet-connected PC: npm install -g rea-agents"
  throw "REA CLI missing"
}
rea --version | Out-File -Encoding UTF8 (Join-Path $OutDir "rea-version.txt")
rea doctor --json | Out-File -Encoding UTF8 (Join-Path $OutDir "doctor.json")
rea inspect-pe-imports $source --json | Out-File -Encoding UTF8 (Join-Path $OutDir "pe-imports.json")
rea inspect-pe-certificates $source --json | Out-File -Encoding UTF8 (Join-Path $OutDir "pe-certificates.json")
# Native Ghidra decompile REQUIRES installed Ghidra 12.1.x + JDK21 on Windows.
# Don't turn this into installer execution, and don't pretend importer success means main-app analysis.
if ($env:GHIDRA_INSTALL_DIR) {
  rea providers --json | Out-File -Encoding UTF8 (Join-Path $OutDir "providers.json")
  rea analyze $source --provider ghidra --json | Out-File -Encoding UTF8 (Join-Path $OutDir "native-overview.json")
}
Write-Host "Read-only REA evidence at: $OutDir"
Write-Host "Next: analyze extracted/installed Memoit application binary, NOT the setup stub."
