<#
.SYNOPSIS
    Master controller for AI agent installers.
.DESCRIPTION
    Controls Hermes, JiuwenSwarm, and OpenClaw installers.
    Configuration is loaded from config.json.
.EXAMPLE
    .\Agent-Manager.ps1
#>

# ======================== Load configuration ========================
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ConfigFile = Join-Path $ScriptDir 'config.json'

if (Test-Path $ConfigFile) {
    $Config = Get-Content $ConfigFile -Raw -Encoding UTF8 | ConvertFrom-Json
} else {
    Write-Host "[Ã--] Config file not found: $ConfigFile" -ForegroundColor Red
    Write-Host "Create config.json with the following structure:" -ForegroundColor Yellow
    @'
{
    "Hermes": { "Provider": "", "ApiKey": "", "Model": "" },
    "JiuwenSwarm": { "Provider": "", "ApiKey": "", "Model": "", "EmbedModel": "" },
    "OpenClaw": { "Provider": "", "ApiKey": "", "Model": "", "Version": "" }
}
'@
    exit 1
}

# ======================== Script paths ========================
$HermesScript = Join-Path $ScriptDir 'Install-HermesAgent.ps1'
$JiuwenSwarmScript = Join-Path $ScriptDir 'Install-JiuwenSwarm.ps1'
$OpenClawScript = Join-Path $ScriptDir 'Install-OpenClaw.ps1'

# ======================== Helper functions ========================
function Invoke-ToolAction {
    param([string]$Script, [string]$Action, [string]$Label)
    
    if (-not (Test-Path $Script)) {
        Write-Host "[FAIL] $Label script not found: $Script" -ForegroundColor Red
        return
    }
    
    $provider = $Config.$Label.Provider
    $apiKey = $Config.$Label.ApiKey
    $model = $Config.$Label.Model
    $embedModel = $Config.$Label.EmbedModel
    $version = $Config.$Label.Version
    $baseUrl = $Config.$Label.BaseUrl

    # Validate required config for install/update actions
    if ($Action -in @('install', 'update')) {
        $missing = @()
        if (-not $apiKey)  { $missing += 'ApiKey' }
        if (-not $baseUrl) { $missing += 'BaseUrl' }
        if (-not $model)   { $missing += 'Model' }
        if ($missing) {
            Write-Host "[WARN] Config missing for $Label`: $($missing -join ', ')" -ForegroundColor Yellow
            Write-Host "Edit config.json and add the missing fields." -ForegroundColor Gray
            return
        }
    }
    
    $args = @(
        '-Action', $Action,
        '-Provider', $provider,
        '-ApiKey', $apiKey,
        '-Model', $model,
        '-BaseUrl', $baseUrl
    )
    if ($embedModel) { $args += @('-EmbedModel', $embedModel) }
    if ($version) { $args += @('-Version', $version) }
    
    & powershell -NoProfile -ExecutionPolicy Bypass -File $Script @args
}

function Open-HermesUI {
    $desktopExe = "$env:LOCALAPPDATA\Programs\hermes-desktop\hermes-agent.exe"
    if (Test-Path $desktopExe) {
        if (Get-Process -Name 'hermes-agent' -ErrorAction SilentlyContinue) {
            Write-Host "Hermes Desktop already running" -ForegroundColor Gray
        } else {
            Start-Process $desktopExe
            Write-Host "Hermes Desktop launched" -ForegroundColor Green
        }
    } else {
        Write-Host "Hermes Desktop not installed. Use [H1] to install." -ForegroundColor Yellow
    }
}

function Stop-Hermes {
    $p = Get-Process -Name 'hermes-agent' -ErrorAction SilentlyContinue
    if ($p) {
        taskkill /F /T /IM hermes-agent.exe 2>$null
        $hermesProcs = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object {
            $_.Name -match 'python' -and $_.CommandLine -match 'hermes'
        }
        foreach ($proc in $hermesProcs) { taskkill /F /T /PID $proc.ProcessId 2>$null }
        Write-Host 'Hermes Desktop stopped' -ForegroundColor Green
    }
    else { Write-Host 'Hermes Desktop not running' -ForegroundColor Gray }
}

function Open-JiuwenSwarmUI {
    $appPort = netstat -ano 2>$null | Select-String "19000.*LISTENING"
    $webPort = netstat -ano 2>$null | Select-String "5173.*LISTENING"
    
    if (-not $appPort) {
        Write-Host "Starting jiuwenswarm-app in background..." -ForegroundColor Cyan
        Start-Process powershell.exe -WindowStyle Hidden -ArgumentList "-NoLogo -Command `$env:PYTHONIOENCODING='utf-8'; jiuwenswarm-app 2>&1 | Out-File `"$env:USERPROFILE\.jiuwenswarm\agent\.logs\app-service.log`""
    }
    if (-not $webPort) {
        Write-Host "Starting jiuwenswarm-web in background..." -ForegroundColor Cyan
        Start-Process powershell.exe -WindowStyle Hidden -ArgumentList "-NoLogo -Command `$env:PYTHONIOENCODING='utf-8'; jiuwenswarm-web 2>&1 | Out-File `"$env:USERPROFILE\.jiuwenswarm\agent\.logs\web-service.log`""
    }
    
    if (-not $appPort -or -not $webPort) {
        Write-Host "Waiting for services (will try to open after 15s)..." -ForegroundColor Gray
        $waited = 0
        while ($waited -lt 15) {
            Start-Sleep 3; $waited += 3
            $webPort = netstat -ano 2>$null | Select-String "5173.*LISTENING"
            if ($webPort) { break }
        }
    }
    
    Write-Host "Opening http://localhost:5173" -ForegroundColor Green
    Start-Process "http://localhost:5173"
}

function Stop-JiuwenSwarm {
    $pids = netstat -ano 2>$null | Select-String "19000.*LISTENING","5173.*LISTENING" | ForEach-Object { ($_ -split '\s+')[-1] } | Sort-Object -Unique
    if ($pids) {
        foreach ($pid in $pids) { taskkill /F /T /PID $pid 2>$null }
        Write-Host 'JiuwenSwarm services stopped' -ForegroundColor Green
    }
    else { Write-Host 'No JiuwenSwarm services running' -ForegroundColor Gray }
}

function Open-OpenClawUI {
    $port = netstat -ano 2>$null | Select-String "18789.*LISTENING"
    if (-not $port) {
        Write-Host "Starting openclaw gateway in background..." -ForegroundColor Cyan
        Start-Process powershell.exe -WindowStyle Hidden -ArgumentList "-NoLogo -NoProfile -Command openclaw gateway --force 2>`$null"
        Write-Host "Waiting (10s)..." -ForegroundColor Gray
        Start-Sleep 10
    }
    Write-Host "Opening http://127.0.0.1:18789" -ForegroundColor Green
    Start-Process "http://127.0.0.1:18789"
}

function Stop-OpenClaw {
    $pids = netstat -ano 2>$null | Select-String "18789.*LISTENING" | ForEach-Object { ($_ -split '\s+')[-1] } | Sort-Object -Unique
    if ($pids) {
        foreach ($pid in $pids) { taskkill /F /T /PID $pid 2>$null }
        Write-Host 'OpenClaw stopped' -ForegroundColor Green
    }
    else { Write-Host 'OpenClaw not running' -ForegroundColor Gray }
}

function View-HermesLog { View-AgentLogs }
function View-JiuwenSwarmLog { View-AgentLogs }
function View-OpenClawLog { View-AgentLogs }

function View-AgentLogs {
    $path = Join-Path $env:USERPROFILE 'agent-logs'
    if (Test-Path $path) {
        Write-Host "Opening $path" -ForegroundColor Green
        Start-Process explorer.exe -ArgumentList $path
    } else {
        Write-Host "No logs yet" -ForegroundColor Yellow
    }
}

# ======================== Menu ========================
function Show-ManagerMenu {
    Write-Host ''
    Write-Host 'AI Agent Manager' -ForegroundColor Cyan
    Write-Host '---------------' -ForegroundColor DarkGray
    Write-Host ''
    Write-Host 'Hermes AI Agent' -ForegroundColor Yellow
    Write-Host '[H1] Install'    -ForegroundColor White
    Write-Host '[H2] Uninstall'  -ForegroundColor White
    Write-Host '[H3] Status'     -ForegroundColor White
    Write-Host '[H4] Update'     -ForegroundColor White
    Write-Host '[HW] Open'       -ForegroundColor White
    Write-Host '[HS] Stop'       -ForegroundColor White
    Write-Host '[HL] View Logs'  -ForegroundColor White
    Write-Host ''
    Write-Host 'JiuwenSwarm' -ForegroundColor Yellow
    Write-Host '[J1] Install'   -ForegroundColor White
    Write-Host '[J2] Uninstall' -ForegroundColor White
    Write-Host '[J3] Status'    -ForegroundColor White
    Write-Host '[J4] Update'    -ForegroundColor White
    Write-Host '[JW] Open'      -ForegroundColor White
    Write-Host '[JS] Stop'      -ForegroundColor White
    Write-Host '[JL] View Logs' -ForegroundColor White
    Write-Host ''
    Write-Host 'OpenClaw' -ForegroundColor Yellow
    Write-Host '[O1] Install'   -ForegroundColor White
    Write-Host '[O2] Uninstall' -ForegroundColor White
    Write-Host '[O3] Status'    -ForegroundColor White
    Write-Host '[O4] Update'    -ForegroundColor White
    Write-Host '[OW] Open'      -ForegroundColor White
    Write-Host '[OS] Stop'      -ForegroundColor White
    Write-Host '[OL] View Logs' -ForegroundColor White
    Write-Host ''
    Write-Host '[A] Show all statuses' -ForegroundColor White
    Write-Host '[Q] Quit'              -ForegroundColor DarkGray
    Write-Host ''

    $choice = Read-Host 'Choose'
    switch ($choice.ToUpper()) {
        'H1' { Invoke-ToolAction $HermesScript 'install' 'Hermes' }
        'H2' { Invoke-ToolAction $HermesScript 'uninstall' 'Hermes' }
        'H3' { Invoke-ToolAction $HermesScript 'status' 'Hermes' }
        'H4' { Invoke-ToolAction $HermesScript 'update' 'Hermes' }
        'HW' { Open-HermesUI }
        'HS' { Stop-Hermes }
        'HL' { View-HermesLog }
        'J1' { Invoke-ToolAction $JiuwenSwarmScript 'install' 'JiuwenSwarm' }
        'J2' { Invoke-ToolAction $JiuwenSwarmScript 'uninstall' 'JiuwenSwarm' }
        'J3' { Invoke-ToolAction $JiuwenSwarmScript 'status' 'JiuwenSwarm' }
        'J4' { Invoke-ToolAction $JiuwenSwarmScript 'update' 'JiuwenSwarm' }
        'JW' { Open-JiuwenSwarmUI }
        'JS' { Stop-JiuwenSwarm }
        'JL' { View-JiuwenSwarmLog }
        'O1' { Invoke-ToolAction $OpenClawScript 'install' 'OpenClaw' }
        'O2' { Invoke-ToolAction $OpenClawScript 'uninstall' 'OpenClaw' }
        'O3' { Invoke-ToolAction $OpenClawScript 'status' 'OpenClaw' }
        'O4' { Invoke-ToolAction $OpenClawScript 'update' 'OpenClaw' }
        'OW' { Open-OpenClawUI }
        'OS' { Stop-OpenClaw }
        'OL' { View-OpenClawLog }
        'A'  {
            Write-Host ''
            Write-Host "`n========== All Agent Status ==========" -ForegroundColor Yellow
            Write-Host "Hermes: " -NoNewline -ForegroundColor White
            Invoke-ToolAction $HermesScript 'status' 'Hermes'
            Write-Host "JiuwenSwarm: " -NoNewline -ForegroundColor White
            Invoke-ToolAction $JiuwenSwarmScript 'status' 'JiuwenSwarm'
            Write-Host "OpenClaw: " -NoNewline -ForegroundColor White
            Invoke-ToolAction $OpenClawScript 'status' 'OpenClaw'
        }
        'Q' { exit 0 }
        default { }
    }
}
while ($true) {
    Show-ManagerMenu
}


