<#
MX Return FLOW - Windows edge agent
- No EXE, no admin, no network.
- Watches configured monitor edges.
- Sends MX Keys / MX Master to a configured Easy-Switch target via HID++ ChangeHost.
- When the mouse later returns to this PC, optionally warps the cursor to a configured ingress edge.
#>

[CmdletBinding()]
param(
    [string]$Config = (Join-Path $PSScriptRoot 'flow-config.json'),
    [switch]$ListMonitors,
    [switch]$CreateConfig,
    [switch]$VerboseLog
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0

$MxSwitch = Join-Path $PSScriptRoot 'mxswitch.ps1'
if (-not (Test-Path -LiteralPath $MxSwitch -PathType Leaf)) {
    throw "mxswitch.ps1 not found next to MXReturnFlow.ps1"
}

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

function Write-FlowLog {
    param([string]$Message)
    if ($VerboseLog) {
        $ts = Get-Date -Format 'HH:mm:ss.fff'
        Write-Host "[$ts] $Message"
    }
}

function Get-FlowScreens {
    return @([System.Windows.Forms.Screen]::AllScreens)
}

function Show-FlowScreens {
    foreach ($s in Get-FlowScreens) {
        $b = $s.Bounds
        "{0}  primary={1}  x={2} y={3} w={4} h={5}" -f $s.DeviceName, $s.Primary, $b.X, $b.Y, $b.Width, $b.Height
    }
}

function New-DefaultFlowConfig {
    [pscustomobject]@{
        pollMs = 25
        edgeThicknessPx = 2
        dwellMs = 220
        cooldownMs = 1200
        targets = [pscustomobject]@{
            phone = [pscustomobject]@{
                keyboardName = 'MX Keys S'
                keyboardChannel = 2
                mouseName = 'MX Master 3S'
                mouseChannel = 2
            }
        }
        routes = @(
            [pscustomobject]@{
                enabled = $true
                monitor = '*'
                edge = 'bottom'
                segmentStart = 0.0
                segmentEnd = 1.0
                target = 'phone'
            }
        )
        returnWarp = [pscustomobject]@{
            enabled = $true
            monitor = 'PRIMARY'
            edge = 'bottom'
            position = 0.5
            insetPx = 8
            detectAfterMs = 700
        }
    }
}

function Save-FlowConfig {
    param($Object, [string]$Path)
    $dir = Split-Path -Parent $Path
    if ($dir -and -not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
    $Object | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $Path -Encoding UTF8
}

if ($ListMonitors) {
    Show-FlowScreens
    exit 0
}

if ($CreateConfig -or -not (Test-Path -LiteralPath $Config -PathType Leaf)) {
    Save-FlowConfig (New-DefaultFlowConfig) $Config
    Write-Host "Created: $Config"
    Write-Host "Detected monitors:"
    Show-FlowScreens
    if ($CreateConfig) { exit 0 }
}

$cfg = Get-Content -LiteralPath $Config -Raw | ConvertFrom-Json

function Get-IntValue {
    param($Object, [string]$Name, [int]$Default)
    try {
        if ($null -ne $Object.PSObject.Properties[$Name]) { return [int]$Object.$Name }
    } catch { }
    return $Default
}

function Get-DoubleValue {
    param($Object, [string]$Name, [double]$Default)
    try {
        if ($null -ne $Object.PSObject.Properties[$Name]) { return [double]$Object.$Name }
    } catch { }
    return $Default
}

function Get-Target {
    param([string]$Name)
    $p = $cfg.targets.PSObject.Properties[$Name]
    if ($null -eq $p) { return $null }
    return $p.Value
}

function Test-MonitorMatch {
    param($Screen, [string]$Selector)
    if (-not $Selector -or $Selector -eq '*') { return $true }
    if ($Selector.ToUpperInvariant() -eq 'PRIMARY') { return [bool]$Screen.Primary }
    return $Screen.DeviceName -ieq $Selector
}

function Test-TowardEdge {
    param([string]$Edge, $Previous, $Current)
    if ($null -eq $Previous) { return $false }
    switch ($Edge.ToLowerInvariant()) {
        'bottom' { return $Current.Y -gt $Previous.Y }
        'top'    { return $Current.Y -lt $Previous.Y }
        'left'   { return $Current.X -lt $Previous.X }
        'right'  { return $Current.X -gt $Previous.X }
        default  { return $false }
    }
}

function Test-RouteHit {
    param($Route, $Screen, $Point)

    if (-not [bool]$Route.enabled) { return $false }
    if (-not (Test-MonitorMatch $Screen ([string]$Route.monitor))) { return $false }

    $b = $Screen.Bounds
    $edge = ([string]$Route.edge).ToLowerInvariant()
    $thickness = [Math]::Max(1, (Get-IntValue $cfg 'edgeThicknessPx' 2))

    $start = Get-DoubleValue $Route 'segmentStart' 0.0
    $end = Get-DoubleValue $Route 'segmentEnd' 1.0
    $start = [Math]::Max(0.0, [Math]::Min(1.0, $start))
    $end = [Math]::Max($start, [Math]::Min(1.0, $end))

    if ($edge -in @('bottom','top')) {
        $fraction = if ($b.Width -le 1) { 0.5 } else { ($Point.X - $b.Left) / [double]$b.Width }
        if ($fraction -lt $start -or $fraction -gt $end) { return $false }
        if ($edge -eq 'bottom') { return $Point.Y -ge ($b.Bottom - $thickness) }
        return $Point.Y -lt ($b.Top + $thickness)
    }

    if ($edge -in @('left','right')) {
        $fraction = if ($b.Height -le 1) { 0.5 } else { ($Point.Y - $b.Top) / [double]$b.Height }
        if ($fraction -lt $start -or $fraction -gt $end) { return $false }
        if ($edge -eq 'right') { return $Point.X -ge ($b.Right - $thickness) }
        return $Point.X -lt ($b.Left + $thickness)
    }

    return $false
}

function Invoke-MxOne {
    param([string]$Name, [int]$Channel)
    if ($Channel -notin @(1,2,3)) { return 2 }
    Write-FlowLog "Switch $Name -> channel $Channel"
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $MxSwitch -Name $Name -Channel $Channel *> $null
    return [int]$LASTEXITCODE
}

function Invoke-FlowTarget {
    param([string]$TargetName)

    $t = Get-Target $TargetName
    if ($null -eq $t) {
        Write-FlowLog "Unknown target: $TargetName"
        return $false
    }

    $kc = [int]$t.keyboardChannel
    $mc = [int]$t.mouseChannel
    $kn = [string]$t.keyboardName
    $mn = [string]$t.mouseName

    $kExit = Invoke-MxOne $kn $kc
    Start-Sleep -Milliseconds 90
    $mExit = Invoke-MxOne $mn $mc

    Write-FlowLog "Target $TargetName results: keyboard=$kExit mouse=$mExit"
    return ($mExit -eq 0)
}

function Get-IngressScreen {
    param([string]$Selector)
    $screens = Get-FlowScreens
    if (-not $Selector -or $Selector.ToUpperInvariant() -eq 'PRIMARY') {
        return @($screens | Where-Object { $_.Primary })[0]
    }
    $m = @($screens | Where-Object { $_.DeviceName -ieq $Selector })
    if ($m.Count) { return $m[0] }
    return @($screens | Where-Object { $_.Primary })[0]
}

function Move-CursorToIngress {
    if ($null -eq $cfg.returnWarp -or -not [bool]$cfg.returnWarp.enabled) { return }

    $s = Get-IngressScreen ([string]$cfg.returnWarp.monitor)
    if ($null -eq $s) { return }

    $b = $s.Bounds
    $edge = ([string]$cfg.returnWarp.edge).ToLowerInvariant()
    $pos = Get-DoubleValue $cfg.returnWarp 'position' 0.5
    $pos = [Math]::Max(0.0, [Math]::Min(1.0, $pos))
    $inset = [Math]::Max(1, (Get-IntValue $cfg.returnWarp 'insetPx' 8))

    $x = [int]($b.Left + ($b.Width * $pos))
    $y = [int]($b.Top + ($b.Height * $pos))
    switch ($edge) {
        'bottom' { $y = $b.Bottom - $inset }
        'top'    { $y = $b.Top + $inset }
        'left'   { $x = $b.Left + $inset }
        'right'  { $x = $b.Right - $inset }
        default  { $y = $b.Bottom - $inset }
    }

    [System.Windows.Forms.Cursor]::Position = New-Object System.Drawing.Point($x, $y)
    Write-FlowLog "Return warp -> $($s.DeviceName) $edge ($x,$y)"
}

$pollMs = [Math]::Max(10, (Get-IntValue $cfg 'pollMs' 25))
$dwellMs = [Math]::Max(60, (Get-IntValue $cfg 'dwellMs' 220))
$cooldownMs = [Math]::Max(300, (Get-IntValue $cfg 'cooldownMs' 1200))

$previous = $null
$candidateKey = $null
$candidateSince = [DateTime]::MinValue
$lastTrigger = [DateTime]::MinValue
$away = $false
$awaySince = [DateTime]::MinValue
$frozen = $null

Write-Host "MX Return FLOW running. Ctrl+C to stop."
Write-Host "Config: $Config"
if ($VerboseLog) { Show-FlowScreens }

while ($true) {
    $point = [System.Windows.Forms.Cursor]::Position
    $now = Get-Date

    if ($away) {
        $detectAfter = if ($null -ne $cfg.returnWarp) { Get-IntValue $cfg.returnWarp 'detectAfterMs' 700 } else { 700 }

        if (($now - $awaySince).TotalMilliseconds -ge $detectAfter -and $null -ne $frozen) {
            $moved = ([Math]::Abs($point.X - $frozen.X) -gt 2) -or ([Math]::Abs($point.Y - $frozen.Y) -gt 2)
            if ($moved) {
                Write-FlowLog "Mouse input returned to PC"
                Move-CursorToIngress
                $point = [System.Windows.Forms.Cursor]::Position
                $away = $false
                $candidateKey = $null
                $lastTrigger = $now
            }
        }

        $previous = $point
        Start-Sleep -Milliseconds $pollMs
        continue
    }

    $screen = [System.Windows.Forms.Screen]::FromPoint($point)
    $hit = $null
    $hitIndex = -1
    $routes = @($cfg.routes)

    for ($i = 0; $i -lt $routes.Count; $i++) {
        $r = $routes[$i]
        if (Test-RouteHit $r $screen $point) {
            $hit = $r
            $hitIndex = $i
            break
        }
    }

    if ($null -eq $hit) {
        $candidateKey = $null
        $candidateSince = [DateTime]::MinValue
    } else {
        $key = "$hitIndex|$($screen.DeviceName)|$($hit.edge)|$($hit.target)"
        if ($candidateKey -ne $key) {
            if (Test-TowardEdge ([string]$hit.edge) $previous $point) {
                $candidateKey = $key
                $candidateSince = $now
                Write-FlowLog "Armed edge: $key"
            } else {
                $candidateKey = $null
            }
        } elseif (($now - $candidateSince).TotalMilliseconds -ge $dwellMs -and ($now - $lastTrigger).TotalMilliseconds -ge $cooldownMs) {
            $target = [string]$hit.target
            Write-FlowLog "Trigger edge: $key"
            $frozen = New-Object System.Drawing.Point($point.X, $point.Y)
            if (Invoke-FlowTarget $target) {
                $away = $true
                $awaySince = Get-Date
                $lastTrigger = $awaySince
            } else {
                Write-FlowLog "Mouse switch failed; staying on PC"
                $lastTrigger = Get-Date
            }
            $candidateKey = $null
        }
    }

    $previous = $point
    Start-Sleep -Milliseconds $pollMs
}
