<#
.SYNOPSIS
    JiuwenSwarm manager for Windows.
.DESCRIPTION
    Install, uninstall, or check status of JiuwenSwarm.
.EXAMPLE
    .\JiuwenSwarm.ps1              # interactive menu
    .\JiuwenSwarm.ps1 install      # install jiuwenswarm
    .\JiuwenSwarm.ps1 uninstall    # uninstall jiuwenswarm
    .\JiuwenSwarm.ps1 status       # check if installed
#>

param(
    [string]$Action = "",
    [string]$Provider = "",
    [string]$ApiKey = "",
    [string]$Model = "",
    [string]$EmbedModel = "",
    [string]$BaseUrl = ""
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Load defaults from config.json if params are empty
$ModelsFile = Join-Path $ScriptDir 'config.json'
if (Test-Path $ModelsFile) {
    $Models = Get-Content $ModelsFile -Raw -Encoding UTF8 | ConvertFrom-Json
    $agent = $Models.agents.JiuwenSwarm
    $prov = $Models.providers.($agent.provider)
    if (-not $Provider) { $Provider = $agent.provider }
    if (-not $ApiKey) { $ApiKey = $prov.api_key }
    if (-not $BaseUrl) { $BaseUrl = $prov.base_url }
    if (-not $Model) { $Model = $agent.model }
    if (-not $EmbedModel) { $EmbedModel = $agent.embed_model }
}

$LogDir = Join-Path $env:USERPROFILE 'agent-logs'
New-Item -ItemType Directory -Path $LogDir -Force -ErrorAction SilentlyContinue | Out-Null
function Log-Operation { param([string]$Op, [string]$Agent); $ts = Get-Date -Format 'yyyyMMdd-HHmmss'; $f = Join-Path $LogDir "$Agent-$Op-$ts.log"; "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') $Op start" | Out-File $f -Encoding UTF8 }

$Utf8NoBom = New-Object System.Text.UTF8Encoding $false
function Set-ContentNoBom { param([string]$Path, [string]$Value); [System.IO.File]::WriteAllText($Path, $Value, $Utf8NoBom) }

$InstallDir = Join-Path $env:LOCALAPPDATA "jiuwenswarm"
$InstallBin = Join-Path $InstallDir "jiuwenswarm.exe"

function Find-JiuwenSwarm {
    # Fast path: Get-Command is instant
    $cmd = Get-Command jiuwenswarm-app -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $cmd = Get-Command jiuwenswarm-web -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    $searchPaths = @(
        (Join-Path $env:APPDATA "uv\tools\jiuwenswarm\Scripts"),
        (Join-Path $env:USERPROFILE ".local\bin"),
        (Join-Path $env:LOCALAPPDATA "jiuwenswarm")
    )
    
    foreach ($dir in $searchPaths) {
        if (Test-Path $dir) {
            $exe = Join-Path $dir "jiuwenswarm-app.exe"
            if (Test-Path $exe) { return $exe }
            $exe = Join-Path $dir "jiuwenswarm-web.exe"
            if (Test-Path $exe) { return $exe }
        }
    }
    
    return $null
}

function Test-JiuwenSwarmInPath {
    $userPath = [System.Environment]::GetEnvironmentVariable("Path", "User")
    $machinePath = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
    $persistentPath = "$userPath;$machinePath"
    
    $pathDirs = $persistentPath -split ";"
    foreach ($dir in $pathDirs) {
        if (-not $dir) { continue }
        if ($dir -match "^\\\\") { continue }
        if (-not (Test-Path $dir)) { continue }
        $exe = Join-Path $dir "jiuwenswarm-app.exe"
        if (Test-Path $exe) { return $true }
        $exe = Join-Path $dir "jiuwenswarm-web.exe"
        if (Test-Path $exe) { return $true }
    }
    
    return $false
}

function Add-JiuwenSwarmToPath {
    param([string]$path)
    
    if (-not $path) { return $false }
    
    $dir = Split-Path $path -Parent
    $pathDirs = $env:PATH -split ";"
    foreach ($d in $pathDirs) {
        if ($d -eq $dir) { return $false }
    }
    
    $env:PATH = "$dir;$env:PATH"
    Write-Host "Added $dir to PATH" -ForegroundColor Green
    return $true
}

function Get-LatestVersion {
    try {
        $json = Invoke-RestMethod -Uri "https://pypi.org/pypi/jiuwenswarm/json" -TimeoutSec 5 -ErrorAction Stop
        return $json.info.version
    } catch { }
    return $null
}

function Get-Status {
    $path = Find-JiuwenSwarm
    if ($path) {
        $currentVersion = $null
        try {
            $raw = uv tool list 2>&1 | Out-String
            if ($raw -match 'jiuwenswarm\s+v?([^\s]+)') { $currentVersion = $matches[1] }
        } catch { }

        $latestVersion = Get-LatestVersion
        Write-Host "JiuwenSwarm: $path" -ForegroundColor Green
        if ($currentVersion) {
            $status = ""
            if ($currentVersion -eq $latestVersion) { $status = " (latest)" }
            Write-Host "Version    : v$currentVersion$status" -ForegroundColor Cyan
        }
        if ($currentVersion -and $latestVersion -and $currentVersion -ne $latestVersion) {
            Write-Host "Latest     : v$latestVersion" -ForegroundColor Gray
        }
    } else {
Write-Host 'JiuwenSwarm: not installed' -ForegroundColor Red
    }
}

function Do-Install {
    Log-Operation 'install' 'jiuwenswarm'

    $alreadyInstalled = Find-JiuwenSwarm

    if ($alreadyInstalled) {
        Write-Host "`n========== JiuwenSwarm already installed ==========" -ForegroundColor Yellow
        Write-Host 'Starting services in background...' -ForegroundColor Cyan
        $appOk = netstat -ano 2>$null | Select-String "19000.*LISTENING"
        $webOk = netstat -ano 2>$null | Select-String "5173.*LISTENING"
        if (-not $appOk) {
            Start-Process powershell.exe -WindowStyle Hidden -ArgumentList "-NoLogo -Command `$env:PYTHONIOENCODING='utf-8'; jiuwenswarm-app 2>&1 | Out-File `"$env:USERPROFILE\.jiuwenswarm\agent\.logs\app-service.log`""
        }
        if (-not $webOk) {
            Start-Process powershell.exe -WindowStyle Hidden -ArgumentList "-NoLogo -Command `$env:PYTHONIOENCODING='utf-8'; jiuwenswarm-web 2>&1 | Out-File `"$env:USERPROFILE\.jiuwenswarm\agent\.logs\web-service.log`""
        }
        if (-not $webOk) { Write-Host 'Waiting 15s for services...' -ForegroundColor Gray; Start-Sleep 15 }
        Write-Host 'Opening WebUI...' -ForegroundColor Green
        Start-Process "http://localhost:5173"
        return
    }

    Write-Host "`n========== Installing JiuwenSwarm ==========" -ForegroundColor Yellow

    Write-Host "Installing uv..." -ForegroundColor Cyan
    if (Get-Command uv -ErrorAction SilentlyContinue) {
        Write-Host "uv already installed" -ForegroundColor Gray
    } else {
        try {
            $installScript = Invoke-RestMethod -Uri "https://astral.sh/uv/install.ps1"
            Invoke-Expression $installScript
            $env:PATH = "$env:USERPROFILE\.local\bin;$env:PATH"
            Write-Host "[OK] uv installed" -ForegroundColor Green
        } catch {
            Write-Host "[FAIL] Failed to install uv" -ForegroundColor Red
            return
        }
    }

    Write-Host "Checking Python..." -ForegroundColor Cyan
    $pythonCmd = Get-Command python -ErrorAction SilentlyContinue
    if ($pythonCmd) {
        $pyVer = & python --version 2>&1
        if ($pyVer -match "Python (\d+)\.(\d+)") {
            $major = [int]$Matches[1]
            $minor = [int]$Matches[2]
            if ($major -gt 3 -or ($major -eq 3 -and $minor -ge 11)) {
                Write-Host "Python $major.$minor already installed" -ForegroundColor Gray
            } else {
                Write-Host "Python $major.$minor found, but 3.11+ required. Installing..." -ForegroundColor Cyan
                try { & uv python install 3.11 2>&1 | Out-Null } catch { Write-Host "[FAIL] Failed" -ForegroundColor Red; return }
            }
        }
    } else {
        Write-Host "Installing Python 3.11..." -ForegroundColor Cyan
        try { & uv python install 3.11 2>&1 | Out-Null } catch { Write-Host "[FAIL] Failed" -ForegroundColor Red; return }
    }

    Write-Host "Installing jiuwenswarm (this may take a few minutes)..." -ForegroundColor Cyan
    $latestStable = Get-LatestVersion
    $versionPin = if ($latestStable) { "@$latestStable" } else { "" }
    & uv tool install "jiuwenswarm$versionPin" --with openjiuwen --prerelease=allow 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[FAIL] Failed to install jiuwenswarm" -ForegroundColor Red
        return
    }
    Write-Host "[OK] jiuwenswarm installed" -ForegroundColor Green

    # Ensure uv Scripts directory is in PATH (both session and persistent)
    $jiuwenBin = Join-Path $env:APPDATA "uv\tools\jiuwenswarm\Scripts"
    $uvBinDir = Join-Path $env:USERPROFILE ".local\bin"
    $userPath = [System.Environment]::GetEnvironmentVariable("Path", "User")
    if ($userPath -notlike "*$jiuwenBin*") {
        [System.Environment]::SetEnvironmentVariable("Path", "$jiuwenBin;$uvBinDir;$userPath", "User")
        Write-Host "Added $jiuwenBin to user PATH" -ForegroundColor Gray
    }
    if ($env:PATH -notlike "*$jiuwenBin*") {
        $env:PATH = "$jiuwenBin;$uvBinDir;$env:PATH"
    }

    # Auto-configure UTF-8 encoding in PowerShell profile
    $utf8Line = 'if (-not (Test-Path env:PYTHONIOENCODING)) { $env:PYTHONIOENCODING = ''utf-8'' }'
    if (-not (Test-Path $PROFILE)) {
        New-Item -Path $PROFILE -ItemType File -Force | Out-Null
    }
    $existingProfile = Get-Content $PROFILE -Raw -ErrorAction SilentlyContinue
    if ($existingProfile -notmatch 'PYTHONIOENCODING') {
        Add-Content -Path $PROFILE -Value "`n# Auto-configured by JiuwenSwarm installer`n$utf8Line`n"
        Write-Host "Configured UTF-8 encoding in PowerShell profile" -ForegroundColor Gray
    }

    # Always re-init workspace (fresh config, fresh frontend assets)
    Write-Host "`n========== Initializing Workspace ==========" -ForegroundColor Yellow
    Write-Host "Reinitializing workspace..." -ForegroundColor Cyan
    $env:PYTHONIOENCODING = "utf-8"
    "yes`nyes" | & jiuwenswarm-init -f 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Workspace initialized" -ForegroundColor Green
    } else {
        Write-Host "[WARN] Workspace init had warnings (may already exist)" -ForegroundColor Yellow
    }

    # Write .env AFTER init (init regenerates template .env with placeholders)
    Write-Host "`n========== Writing Configuration ==========" -ForegroundColor Yellow
    Write-Host "Writing .env with real credentials..." -ForegroundColor Cyan
    
    $envFile = "$env:USERPROFILE\.jiuwenswarm\config\.env"
    $envDir = Split-Path $envFile -Parent
    New-Item -ItemType Directory -Path $envDir -Force | Out-Null
    
    $envContent = @"
API_BASE="${BaseUrl}"
API_KEY="${ApiKey}"
MODEL_NAME="${Model}"
MODEL_PROVIDER=OpenAI
CUSTOM_HEADERS=

EMBED_API_BASE="${BaseUrl}"
EMBED_API_KEY="${ApiKey}"
EMBED_MODEL="${EmbedModel}"

VIDEO_API_BASE="${BaseUrl}"
VIDEO_API_KEY="${ApiKey}"
VIDEO_MODEL_NAME="${Model}"
VIDEO_PROVIDER=OpenAI

AUDIO_API_BASE="${BaseUrl}"
AUDIO_API_KEY="${ApiKey}"
AUDIO_MODEL_NAME="${Model}"
AUDIO_PROVIDER=OpenAI

VISION_API_BASE="${BaseUrl}"
VISION_API_KEY="${ApiKey}"
VISION_MODEL_NAME="${Model}"
VISION_PROVIDER=OpenAI

JINA_API_KEY=
SERPER_API_KEY=
PERPLEXITY_API_KEY=
GITHUB_TOKEN=
FREE_SEARCH_PROXY_URL=
FREE_SEARCH_SSL_VERIFY=false
FREE_SEARCH_DDG_URL=https://html.duckduckgo.com/html/

BROWSER_RUNTIME_MCP_ENABLED=1
BROWSER_RUNTIME_MCP_CLIENT_TYPE=streamable-http
BROWSER_RUNTIME_MCP_SERVER_ID=playwright_runtime_wrapper
BROWSER_RUNTIME_MCP_SERVER_NAME=playwright-runtime-wrapper
BROWSER_RUNTIME_MCP_SERVER_PATH=http://127.0.0.1:8940/mcp
BROWSER_RUNTIME_MCP_TIMEOUT_S=300
BROWSER_RUNTIME_MCP_HOST=127.0.0.1
BROWSER_RUNTIME_MCP_PORT=8940
BROWSER_RUNTIME_MCP_PATH=/mcp
BROWSER_RUNTIME_MCP_COMMAND=
BROWSER_RUNTIME_MCP_ARGS=
BROWSER_RUNTIME_MCP_AUTO_SSE_FALLBACK=1

PLAYWRIGHT_MCP_COMMAND=npx
PLAYWRIGHT_MCP_ARGS=-y @playwright/mcp@latest
PLAYWRIGHT_CDP_URL=http://127.0.0.1:9222
PLAYWRIGHT_TOOL_TIMEOUT_S=300
BROWSER_TIMEOUT_S=300
BROWSER_ALLOW_SHORT_TIMEOUT_OVERRIDE=0

BROWSER_DRIVER=managed
BROWSER_PROFILE_NAME=Default

EMAIL_ADDRESS=""
EMAIL_TOKEN=""

EVOLUTION_AUTO_SCAN="false"

SKILLNET_DOWNLOAD_TIMEOUT=60
SKILLNET_MAX_RETRIES=3

TEAM_SKILLS_HUB_BASE_URL=
TEAM_SKILLS_HUB_USER_TOKEN=
TEAM_SKILLS_HUB_SYSTEM_TOKEN=
TEAM_SKILLS_HUB_TIMEOUT=60
TEAM_SKILLS_HUB_ALLOWED_DOWNLOAD_HOSTS=

NO_PROXY=localhost,127.0.0.1,api.openai.rnd.huawei.com

MEMORY_MODE=

EXTENSION_DIRS=""

JIUWENSWARM_ENABLE_ORIGIN_CHECK=0
JIUWENSWARM_WS_ALLOWED_ORIGIN_HOSTS=127.0.0.1,localhost
"@
    $envContent | Out-File -FilePath $envFile -Encoding UTF8
    Write-Host "[OK] .env written: $envFile" -ForegroundColor Green

    # Set system environment variables (config.yaml placeholders resolve from process env)
    Write-Host "Setting environment variables..." -ForegroundColor Gray
    [System.Environment]::SetEnvironmentVariable("API_BASE", $BaseUrl, "User")
    [System.Environment]::SetEnvironmentVariable("API_KEY", $ApiKey, "User")
    [System.Environment]::SetEnvironmentVariable("MODEL_NAME", $Model, "User")
    [System.Environment]::SetEnvironmentVariable("MODEL_PROVIDER", "OpenAI", "User")
    [System.Environment]::SetEnvironmentVariable("VIDEO_API_BASE", $BaseUrl, "User")
    [System.Environment]::SetEnvironmentVariable("VIDEO_API_KEY", $ApiKey, "User")
    [System.Environment]::SetEnvironmentVariable("VIDEO_MODEL_NAME", $Model, "User")
    [System.Environment]::SetEnvironmentVariable("VIDEO_PROVIDER", "OpenAI", "User")
    [System.Environment]::SetEnvironmentVariable("AUDIO_API_BASE", $BaseUrl, "User")
    [System.Environment]::SetEnvironmentVariable("AUDIO_API_KEY", $ApiKey, "User")
    [System.Environment]::SetEnvironmentVariable("AUDIO_MODEL_NAME", $Model, "User")
    [System.Environment]::SetEnvironmentVariable("AUDIO_PROVIDER", "OpenAI", "User")
    [System.Environment]::SetEnvironmentVariable("VISION_API_BASE", $BaseUrl, "User")
    [System.Environment]::SetEnvironmentVariable("VISION_API_KEY", $ApiKey, "User")
    [System.Environment]::SetEnvironmentVariable("VISION_MODEL_NAME", $Model, "User")
    [System.Environment]::SetEnvironmentVariable("VISION_PROVIDER", "OpenAI", "User")
    [System.Environment]::SetEnvironmentVariable("EMBED_API_BASE", $BaseUrl, "User")
    [System.Environment]::SetEnvironmentVariable("EMBED_API_KEY", $ApiKey, "User")
    [System.Environment]::SetEnvironmentVariable("EMBED_MODEL", $EmbedModel, "User")
    
    $env:API_BASE = $BaseUrl
    $env:API_KEY = $ApiKey
    $env:MODEL_NAME = $Model
    $env:MODEL_PROVIDER = "OpenAI"
    $env:VIDEO_API_BASE = $BaseUrl
    $env:VIDEO_API_KEY = $ApiKey
    $env:VIDEO_MODEL_NAME = $Model
    $env:VIDEO_PROVIDER = "OpenAI"
    $env:AUDIO_API_BASE = $BaseUrl
    $env:AUDIO_API_KEY = $ApiKey
    $env:AUDIO_MODEL_NAME = $Model
    $env:AUDIO_PROVIDER = "OpenAI"
    $env:VISION_API_BASE = $BaseUrl
    $env:VISION_API_KEY = $ApiKey
    $env:VISION_MODEL_NAME = $Model
    $env:VISION_PROVIDER = "OpenAI"
    $env:EMBED_API_BASE = $BaseUrl
    $env:EMBED_API_KEY = $ApiKey
    $env:EMBED_MODEL = $EmbedModel

    # Replace placeholders in config.yaml with actual values
    $configYaml = "$env:USERPROFILE\.jiuwenswarm\config\config.yaml"
    if (Test-Path $configYaml) {
        Write-Host "Injecting values into config.yaml..." -ForegroundColor Gray
        $yamlContent = Get-Content $configYaml -Raw -Encoding UTF8
        $yamlContent = $yamlContent -replace '\$\{API_BASE\}', $BaseUrl
        $yamlContent = $yamlContent -replace '\$\{API_KEY\}', $ApiKey
        $yamlContent = $yamlContent -replace '\$\{MODEL_NAME\}', $Model
        $yamlContent = $yamlContent -replace '\$\{MODEL_PROVIDER\}', 'OpenAI'
        $yamlContent = $yamlContent -replace '\$\{EMBED_API_BASE\}', $BaseUrl
        $yamlContent = $yamlContent -replace '\$\{EMBED_API_KEY\}', $ApiKey
        $yamlContent = $yamlContent -replace '\$\{EMBED_MODEL\}', $EmbedModel
        $yamlContent = $yamlContent -replace '\$\{VIDEO_API_BASE\}', $BaseUrl
        $yamlContent = $yamlContent -replace '\$\{VIDEO_API_KEY\}', $ApiKey
        $yamlContent = $yamlContent -replace '\$\{VIDEO_MODEL_NAME\}', $Model
        $yamlContent = $yamlContent -replace '\$\{VIDEO_PROVIDER\}', 'OpenAI'
        $yamlContent = $yamlContent -replace '\$\{AUDIO_API_BASE\}', $BaseUrl
        $yamlContent = $yamlContent -replace '\$\{AUDIO_API_KEY\}', $ApiKey
        $yamlContent = $yamlContent -replace '\$\{AUDIO_MODEL_NAME\}', $Model
        $yamlContent = $yamlContent -replace '\$\{AUDIO_PROVIDER\}', 'OpenAI'
        $yamlContent = $yamlContent -replace '\$\{VISION_API_BASE\}', $BaseUrl
        $yamlContent = $yamlContent -replace '\$\{VISION_API_KEY\}', $ApiKey
        $yamlContent = $yamlContent -replace '\$\{VISION_MODEL_NAME\}', $Model
        $yamlContent = $yamlContent -replace '\$\{VISION_PROVIDER\}', 'OpenAI'
        Set-ContentNoBom -Path $configYaml -Value $yamlContent
        Write-Host "config.yaml updated with real values" -ForegroundColor Green
    }

    if (Find-JiuwenSwarm) {
        Write-Host "[OK] JiuwenSwarm installed successfully" -ForegroundColor Green
        Get-Status | Out-Null
    } else {
        Write-Host "[WARN] jiuwenswarm not found on PATH after install (unexpected)" -ForegroundColor Yellow
    }

    # Kill any leftover services, then start fresh
    Write-Host ""
    Write-Host "Stopping any leftover services..." -ForegroundColor Gray
    $pids = netstat -ano 2>$null | Select-String "19000.*LISTENING","5173.*LISTENING" | ForEach-Object { ($_ -split '\s+')[-1] } | Sort-Object -Unique
    foreach ($p in $pids) { taskkill /F /T /PID $p 2>$null }
    Start-Sleep 2

    Write-Host " Starting services (detached background)..." -ForegroundColor Cyan
    $procApp = Start-Process powershell.exe -WindowStyle Hidden -PassThru `
        -ArgumentList "-NoLogo -Command `$env:PYTHONIOENCODING='utf-8'; `$env:API_BASE='$BaseUrl'; `$env:API_KEY='$ApiKey'; `$env:MODEL_NAME='$Model'; `$env:MODEL_PROVIDER='OpenAI'; jiuwenswarm-app 2>&1 | Out-File -Append -FilePath `"$env:USERPROFILE\.jiuwenswarm\agent\.logs\app-service.log`""
    $procWeb = Start-Process powershell.exe -WindowStyle Hidden -PassThru `
        -ArgumentList "-NoLogo -Command `$env:PYTHONIOENCODING='utf-8'; `$env:API_BASE='$BaseUrl'; `$env:API_KEY='$ApiKey'; `$env:MODEL_NAME='$Model'; `$env:MODEL_PROVIDER='OpenAI'; jiuwenswarm-web 2>&1 | Out-File -Append -FilePath `"$env:USERPROFILE\.jiuwenswarm\agent\.logs\web-service.log`""
    Write-Host "App PID: $($procApp.Id), Web PID: $($procWeb.Id)" -ForegroundColor Gray

    # Wait for services with retry (app can take 20-30s on first boot)
    $maxWait = 60
    $waited = 0
    $appOk = $false
    $webOk = $false
    Write-Host "Waiting for services (up to ${maxWait}s)..." -ForegroundColor Gray
    while ($waited -lt $maxWait -and (-not $appOk -or -not $webOk)) {
        Start-Sleep 5
        $waited += 5
        $appOk = netstat -ano 2>$null | Select-String "19000.*LISTENING"
        $webOk = netstat -ano 2>$null | Select-String "5173.*LISTENING"
        # Break early if process died
        if ($procApp.HasExited) { Write-Host "App process exited!" -ForegroundColor Red; break }
    }

    if ($appOk) { Write-Host "App :19000 - OK (after ${waited}s)" -ForegroundColor Green }
    else {
        Write-Host "App :19000 - not responding after ${waited}s" -ForegroundColor Red
        $logPath = "$env:USERPROFILE\.jiuwenswarm\agent\.logs\app-service.log"
        Write-Host "Last 5 lines of app log:" -ForegroundColor Yellow
        if (Test-Path $logPath) {
            Get-Content $logPath -Tail 5 | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
        } else {
            Write-Host "  (no log file found)" -ForegroundColor Gray
        }
    }
    if ($webOk) { Write-Host "Web :5173 - OK (after ${waited}s)" -ForegroundColor Green }
    else {
        Write-Host "Web :5173 - not responding after ${waited}s" -ForegroundColor Red
        $logPath = "$env:USERPROFILE\.jiuwenswarm\agent\.logs\web-service.log"
        Write-Host "Last 5 lines of web log:" -ForegroundColor Yellow
        if (Test-Path $logPath) {
            Get-Content $logPath -Tail 5 | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
        } else {
            Write-Host "  (no log file found)" -ForegroundColor Gray
        }
    }

    if ($webOk) {
        Write-Host "Opening web UI..." -ForegroundColor Cyan
        Start-Process "http://localhost:5173"
        Write-Host "NOTE: If you see an old interface, press Ctrl+Shift+R to clear browser cache" -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host "========== JiuwenSwarm Usage Guide ==========" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "WEB UI (what just opened in your browser):" -ForegroundColor White
    Write-Host "  URL: http://localhost:5173" -ForegroundColor Cyan
    Write-Host "  The main way to use JiuwenSwarm: chat, file operations," -ForegroundColor Gray
    Write-Host "  agent configuration, session history, settings." -ForegroundColor Gray
    Write-Host ""
    Write-Host "HOW IT WORKS:" -ForegroundColor White
    Write-Host "  Two processes run in background (hidden windows):" -ForegroundColor Gray
    Write-Host "  - jiuwenswarm-app  : backend (AgentServer + Gateway, port 19000)" -ForegroundColor Cyan
    Write-Host "  - jiuwenswarm-web  : frontend UI (serves web page, port 5173)" -ForegroundColor Cyan
    Write-Host "  The web frontend talks to the backend via http/ws proxy." -ForegroundColor Gray
    Write-Host "  Both are needed. Without app, web has nothing to connect to." -ForegroundColor Gray
    Write-Host ""
    Write-Host "START/STOP SERVICES:" -ForegroundColor White
    Write-Host "  jiuwenswarm-start all   # start app + web together" -ForegroundColor Cyan
    Write-Host "  jiuwenswarm-app         # backend only (backend on :19000)" -ForegroundColor Cyan
    Write-Host "  jiuwenswarm-web         # frontend only (ui on :5173)" -ForegroundColor Cyan
    Write-Host "  Close the hidden PowerShell windows to stop them." -ForegroundColor Gray
    Write-Host ""
    Write-Host "CLI TOOLS:" -ForegroundColor White
    Write-Host "  jiuwenswarm-tui acp      # ACP stdio interface (protocol-level CLI)" -ForegroundColor Cyan
    Write-Host "  jiuwenswarm-acp-chat     # Test the acp_agents profile (one-shot)" -ForegroundColor Cyan
    Write-Host "    Usage: jiuwenswarm-acp-chat codex 'hello'" -ForegroundColor Gray
    Write-Host "    (codex is the profile key under acp_agents in config.yaml)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "CONFIG:" -ForegroundColor White
    Write-Host "  .env:  $env:USERPROFILE\.jiuwenswarm\config\.env" -ForegroundColor Cyan
    Write-Host "  yaml:  $env:USERPROFILE\.jiuwenswarm\config\config.yaml" -ForegroundColor Cyan
    Write-Host "  logs:  $env:USERPROFILE\.jiuwenswarm\agent\.logs\" -ForegroundColor Cyan
}

# ======================== Update ========================
function Do-Update {
    if (-not (Find-JiuwenSwarm)) {
        Write-Host "JiuwenSwarm is not installed." -ForegroundColor Yellow
        return
    }
    Write-Host "`n========== Checking for Updates ==========" -ForegroundColor Yellow

    $latestVersion = Get-LatestVersion
    if (-not $latestVersion) {
        Write-Host "[WARN] Could not check latest version (network issue?)" -ForegroundColor Yellow
        return
    }

    $currentVersion = $null
    try {
        $raw = uv tool list 2>&1 | Out-String
        if ($raw -match 'jiuwenswarm\s+v?([^\s]+)') { $currentVersion = $matches[1] }
    } catch { }

    if ($currentVersion -and ($currentVersion -eq $latestVersion)) {
        Write-Host "[OK] Already on latest version v$currentVersion" -ForegroundColor Green
        return
    }

    # Compare: stable always wins over same-base beta
    $curBase = $currentVersion -replace '[a-z].*', ''
    $latBase = $latestVersion -replace '[a-z].*', ''
    if ($curBase -eq $latBase) {
        if ($latestVersion -notmatch '[a-z]' -and $currentVersion -match '[a-z]') {
            # Same base, current is beta, latest is stable -> upgrade
        } elseif ($currentVersion -gt $latestVersion) {
            Write-Host "[OK] You have v$currentVersion (latest stable is v$latestVersion)" -ForegroundColor Green
            return
        }
    } elseif ($curBase -gt $latBase) {
        Write-Host "[OK] You have v$currentVersion (latest stable is v$latestVersion)" -ForegroundColor Green
        return
    }

    Write-Host "Updating JiuwenSwarm from v$currentVersion to v$latestVersion..." -ForegroundColor Cyan
    $env:PYTHONIOENCODING = "utf-8"

    # Kill running services (they lock files uv needs to replace)
    Write-Host "Stopping services..." -ForegroundColor Gray
    $pids = netstat -ano 2>$null | Select-String "19000.*LISTENING","5173.*LISTENING" | ForEach-Object { ($_ -split '\s+')[-1] } | Sort-Object -Unique
    foreach ($p in $pids) { taskkill /F /T /PID $p 2>$null }
    Start-Sleep 2

    uv tool install jiuwenswarm@$latestVersion --with openjiuwen --prerelease=allow --reinstall 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] JiuwenSwarm updated to v$latestVersion" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Update failed (kill running services and try again)" -ForegroundColor Red
    }
}

function Do-Uninstall {
    Log-Operation 'uninstall' 'jiuwenswarm'

    Write-Host "`n========== Uninstalling JiuwenSwarm ==========" -ForegroundColor Yellow

    # Kill running services first (they hold file locks)
    Write-Host "Stopping running services..." -ForegroundColor Cyan
    $pids = netstat -ano 2>$null | Select-String "19000.*LISTENING","5173.*LISTENING" | ForEach-Object { ($_ -split '\s+')[-1] } | Sort-Object -Unique
    foreach ($p in $pids) { taskkill /F /T /PID $p 2>$null }
    Start-Sleep 2

    # 1. Remove old jiuwenclaw (leftover from previous version)
    & uv tool uninstall jiuwenclaw 2>&1 | Out-Null
    $oldClawDir = Join-Path $env:APPDATA "uv\tools\jiuwenclaw"
    if (Test-Path $oldClawDir) {
        Remove-Item $oldClawDir -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "Removed uv\tools\jiuwenclaw" -ForegroundColor Gray
    }
    $oldWorkspace = "$env:USERPROFILE\.jiuwenclaw"
    if (Test-Path $oldWorkspace) {
        Remove-Item $oldWorkspace -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "Removed ~\.jiuwenclaw" -ForegroundColor Gray
    }

    # 2. Remove jiuwenswarm
    if (Find-JiuwenSwarm) {
        & uv tool uninstall jiuwenswarm 2>&1 | Out-Null
        Write-Host "Removed via uv" -ForegroundColor Gray
    }

    # Remove known install locations
    $pathsToClean = @(
        (Join-Path $env:LOCALAPPDATA "jiuwenswarm"),
        (Join-Path $env:USERPROFILE ".local\bin\jiuwenswarm*.exe"),
        (Join-Path $env:USERPROFILE ".local\bin\jiuwenbox*.exe"),
        (Join-Path $env:APPDATA "uv\tools\jiuwenswarm")
    )
    foreach ($p in $pathsToClean) {
        if ($p -like "*.exe") {
            Get-Item $p -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
        } else {
            if (Test-Path $p) {
                Remove-Item $p -Recurse -Force -ErrorAction SilentlyContinue
                Write-Host "Removed $p" -ForegroundColor Gray
            }
        }
    }

    # Remove workspace
    $workspaceDir = "$env:USERPROFILE\.jiuwenswarm"
    if (Test-Path $workspaceDir) {
        Remove-Item $workspaceDir -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "Removed $workspaceDir" -ForegroundColor Gray
    }

    # Clean PATH (both registry and session)
    $path = [System.Environment]::GetEnvironmentVariable("Path", "User")
    $cleaned = ($path -split ";" | Where-Object { $_ -notlike "*jiuwen*" -and $_ -notlike "*uv\\tools\\jiuwen*" }) -join ";"
    [System.Environment]::SetEnvironmentVariable("Path", $cleaned, "User")
    
    $userPath = [System.Environment]::GetEnvironmentVariable("Path", "User")
    $machinePath = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
    $env:PATH = "$userPath;$machinePath"

    # Final verification
    if (Find-JiuwenSwarm) {
        Write-Host "[WARN] Some remnants may remain. Restart your terminal." -ForegroundColor Yellow
    } else {
        Write-Host "[OK] JiuwenSwarm uninstalled" -ForegroundColor Green
    }
}

function Show-Menu {
    Write-Host ""
    Write-Host "JiuwenSwarm Manager" -ForegroundColor Cyan
    Write-Host "----------------" -ForegroundColor DarkGray
    Write-Host "[1] Install"   -ForegroundColor White
    Write-Host "[2] Uninstall" -ForegroundColor White
    Write-Host "[3] Status"    -ForegroundColor White
    Write-Host "[4] Update"    -ForegroundColor White
    Write-Host "[Q] Quit"      -ForegroundColor DarkGray
    Write-Host ""

    $choice = Read-Host "Choose"
    switch ($choice) {
        "1" { Do-Install }
        "2" { Do-Uninstall }
        "3" { Get-Status | Out-Null }
        "4" { Do-Update }
        default { return }
    }
}

switch ($Action) {
    "install"   { Do-Install }
    "uninstall" { Do-Uninstall }
    "status"    { Get-Status | Out-Null }
    "update"    { Do-Update }
    default     { Show-Menu }
}



