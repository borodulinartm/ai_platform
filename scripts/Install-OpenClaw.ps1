<#
.SYNOPSIS
    OpenClaw manager for Windows.
.DESCRIPTION
    Install, uninstall, or check status of OpenClaw.
    One-click Node.js 24.x+ environment check/install, Git detect-only, and OpenClaw 2026.3.13 install/update.
.EXAMPLE
    .\Install-OpenClaw.ps1              # interactive menu
    .\Install-OpenClaw.ps1 install      # install (full setup)
    .\Install-OpenClaw.ps1 uninstall    # uninstall
    .\Install-OpenClaw.ps1 status       # check if installed
#>

param(
    [string]$Action = '',
    [string]$Provider = '',
    [string]$ApiKey = '',
    [string]$Model = '',
    [string]$Version = '',
    [string]$BaseUrl = ''
)

# ======================== Core Encoding Configuration ========================
chcp 65001 > $null
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# ======================== Core Configuration ========================
$targetNodeVersion = "24.0.0"
$defaultOpenClawVersion = if ($Version) { $Version } else { "" }
$tempDir = "$env:TEMP\OpenClawInstaller"
$gitFallbackUrl = "https://github.com/git-for-windows/git/releases/download/v2.45.1.windows.1/Git-2.45.1-64-bit.exe"

# Use params if provided, otherwise hardcoded defaults
$customBaseUrl    = if ($BaseUrl)   { $BaseUrl }   else { "https://api.modelarts-maas.com/v2" }
$customApiKey     = if ($ApiKey)    { $ApiKey }    else { "sU_Uh4ERpHcmTWdGMt1RNzKdAUtqy-SHmbdbTsPqbtr5dqpGz8X9FVrduvtQnKAqAdlNTbb9d65FvHazIl5EYw" }
$customModelId    = if ($Model)     { $Model }     else { "glm-5" }
$customProviderId = if ($Provider)  { $Provider }  else { "huawei" }

$targetOpenClawVersion = $defaultOpenClawVersion
$versionSuffix = if ($targetOpenClawVersion) { "@$targetOpenClawVersion" } else { "" }
$LogDir = Join-Path $env:USERPROFILE 'agent-logs'
New-Item -ItemType Directory -Path $LogDir -Force -ErrorAction SilentlyContinue | Out-Null
function Log-Operation { param([string]$Op, [string]$Agent); $ts = Get-Date -Format 'yyyyMMdd-HHmmss'; $f = Join-Path $LogDir "$Agent-$Op-$ts.log"; "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') $Op start" | Out-File $f -Encoding UTF8 }

$Utf8NoBom = New-Object System.Text.UTF8Encoding $false
function Set-ContentNoBom { param([string]$Path, [string]$Value); [System.IO.File]::WriteAllText($Path, $Value, $Utf8NoBom) }

$openClawConfigPath = "$env:USERPROFILE\.openclaw\openclaw.json"

# ======================== Management functions ========================
function Find-OpenClaw {
    # Fast path: Get-Command is instant and doesn't walk directories
    $cmd = Get-Command openclaw -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    # Slow path: scan PATH directories (skip network/UNC to avoid hangs)
    $pathDirs = $env:PATH -split ';'
    foreach ($dir in $pathDirs) {
        if (-not $dir) { continue }
        if ($dir -match '^\\\\') { continue }  # skip UNC/network paths
        if (-not (Test-Path $dir)) { continue }
        $cmd = Join-Path $dir 'openclaw.cmd'
        if (Test-Path $cmd) { return $cmd }
        $exe = Join-Path $dir 'openclaw'
        if (Test-Path $exe) { return $exe }
    }
    return $null
}

function Get-LatestVersion {
    try {
        $result = npm view openclaw version 2>$null
        if ($LASTEXITCODE -eq 0 -and $result) { return $result.Trim() }
    } catch { }
    return $null
}

function Get-Status {
    $path = Find-OpenClaw
    Write-Host ''
    if ($path) {
        $currentVersion = $null
        try { $versionString = & openclaw -v 2>&1; if ($versionString -match 'OpenClaw\s+(\d+\.\d+\.\d+)') { $currentVersion = $matches[1] } elseif ($versionString -match '(\d+\.\d+\.\d+)') { $currentVersion = $matches[1] } } catch { }

        $latestVersion = Get-LatestVersion
        Write-Host "OpenClaw: $path" -ForegroundColor Green
        if ($currentVersion) {
            $status = ""
            if ($currentVersion -eq $latestVersion) { $status = " (latest)" }
            Write-Host "Version : v$currentVersion$status" -ForegroundColor Cyan
        }
        if ($currentVersion -and $latestVersion -and $currentVersion -ne $latestVersion) {
            Write-Host "Latest  : v$latestVersion" -ForegroundColor Gray
        }
    } else {
        Write-Host 'OpenClaw: not installed' -ForegroundColor Red
    }
    Write-Host ''
}

# ======================== Utility Functions ========================
function Test-Admin {
    $currentUser = [Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()
    return $currentUser.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Compare-Version {
    param(
        [string]$Version1,
        [string]$Version2
    )
    try {
        $cleanV1 = ($Version1 -replace '[^\d\.]', '').Trim('.')
        $cleanV2 = ($Version2 -replace '[^\d\.]', '').Trim('.')
        
        if (-not $cleanV1.Contains('.')) { $cleanV1 += ".0.0" }
        if (-not $cleanV2.Contains('.')) { $cleanV2 += ".0.0" }

        $v1 = [version]$cleanV1
        $v2 = [version]$cleanV2
        return $v1 -ge $v2
    }
    catch {
        Write-Host "Version parse failed: Version1='$Version1' -> cleaned='$cleanV1', Version2='$Version2' -> cleaned='$cleanV2'" -ForegroundColor Red
        Write-Host "Error details: $_" -ForegroundColor DarkRed
        return $false
    }
}

function Refresh-EnvironmentVariables {
    Write-Host "Refreshing system environment variables..." -ForegroundColor Cyan
    $machinePath = [System.Environment]::GetEnvironmentVariable("PATH", "Machine")
    $userPath = [System.Environment]::GetEnvironmentVariable("PATH", "User")
    $env:PATH = "$machinePath;$userPath"
    Write-Host "Environment variables refreshed" -ForegroundColor Green
}

function Invoke-DownloadFile {
    param(
        [string]$Url,
        [string]$OutputPath
    )
    try {
        if (-not [Uri]::IsWellFormedUriString($Url, [UriKind]::Absolute)) {
            throw "Invalid URL format: $Url"
        }

        Write-Host "Downloading: $Url" -ForegroundColor Cyan
        Invoke-WebRequest -Uri $Url -OutFile $OutputPath -UseBasicParsing -ErrorAction Stop
        Write-Host "Download complete: $OutputPath" -ForegroundColor Green
    }
    catch {
        Write-Host "Download failed: $_" -ForegroundColor Red
        exit 1
    }
}

function Get-OpenClawVersion {
    [CmdletBinding()]
    param (
        [Parameter(Mandatory = $true, ValueFromPipeline = $true)]
        [string]$VersionString
    )

    process {
        $regexPattern = 'OpenClaw\s+(\d+\.\d+\.\d+)'
        
        if ($VersionString -match $regexPattern) {
            return $matches[1]
        }
        else {
            Write-Warning "Cannot parse OpenClaw version from string '$VersionString'"
            return $null
        }
    }
}

# ======================== Node.js Installation ========================
function Install-NodeJs {
    Write-Host "`n========== Checking Node.js Environment ==========" -ForegroundColor Yellow

    $nodeInstalled = $false
    $nodeVersion = $null
    try {
        $nodeVersionOutput = node -v 2>&1
        if (-not $LASTEXITCODE) {
            $nodeVersion = $nodeVersionOutput -replace 'v', ''
            $nodeInstalled = $true
            Write-Host "Node.js detected, version: $nodeVersion" -ForegroundColor Cyan
        }
    }
    catch {
        Write-Host "Node.js not detected" -ForegroundColor Cyan
    }

    if ($nodeInstalled -and (Compare-Version -Version1 $nodeVersion -Version2 $targetNodeVersion)) {
        Write-Host "Node.js version meets requirements (>=24.x), no install needed" -ForegroundColor Green
        return
    }

    Write-Host "Need to install/update Node.js to 24.x LTS" -ForegroundColor Cyan
    if (-not (Test-Path $tempDir)) { New-Item -ItemType Directory -Path $tempDir | Out-Null }

    $nodeMsiUrl = "https://nodejs.org/dist/latest-v24.x/node-v24.14.0-x64.msi"
    $nodeMsiPath = "$tempDir\node-v24.14.0-x64.msi"

    Invoke-DownloadFile -Url $nodeMsiUrl -OutputPath $nodeMsiPath
    Write-Host "Installing Node.js..." -ForegroundColor Cyan
    Start-Process msiexec.exe -ArgumentList "/i `"$nodeMsiPath`" /qn /norestart" -Wait -NoNewWindow

    Refresh-EnvironmentVariables
    try {
        $newNodeVersion = node -v 2>&1
        if (-not $LASTEXITCODE) {
            Write-Host "Node.js installed successfully, version: $newNodeVersion" -ForegroundColor Green
        }
        else {
            throw "Node.js post-install verification failed"
        }
    }
    catch {
        Write-Host "Node.js installation failed: $_" -ForegroundColor Red
        exit 1
    }
}

# ======================== Git Installation (detect only) ========================
function Install-Git {
    Write-Host "`n========== Checking Git Environment ==========" -ForegroundColor Yellow

    $gitInstalled = $false
    try {
        git --version 2>&1
        if (-not $LASTEXITCODE) {
            $gitInstalled = $true
            Write-Host "Git detected (version not checked), no action needed" -ForegroundColor Green
        }
    }
    catch {
        Write-Host "Git not detected, need to install" -ForegroundColor Cyan
    }

    if (-not $gitInstalled) {
        if (-not (Test-Path $tempDir)) { New-Item -ItemType Directory -Path $tempDir | Out-Null }

        $gitExeUrl = $gitFallbackUrl
        $gitExePath = "$tempDir\$(Split-Path $gitExeUrl -Leaf)"

        Invoke-DownloadFile -Url $gitExeUrl -OutputPath $gitExePath
        Write-Host "Installing Git..." -ForegroundColor Cyan
        Start-Process $gitExePath -ArgumentList "/VERYSILENT /NORESTART /NOCANCEL /SP- /SUPPRESSMSGBOXES" -Wait -NoNewWindow

        Refresh-EnvironmentVariables
        try {
            git --version 2>&1
            if (-not $LASTEXITCODE) {
                Write-Host "Git installed successfully (version not checked)" -ForegroundColor Green
            }
            else {
                throw "Git post-install verification failed"
            }
        }
        catch {
            Write-Host "Git installation failed: $_" -ForegroundColor Red
            exit 1
        }
    }
}

# ======================== OpenClaw Installation/Update ========================
function Install-OpenClawApp {
    Write-Host "`n========== Checking OpenClaw Environment ==========" -ForegroundColor Yellow

    $openClawInstalled = $false
    $openClawVersion = $null

    if (-not $openClawInstalled) {
        try {
            $openClawVersionOutput = openclaw -v 2>&1
            if (-not $LASTEXITCODE) {
                $openClawVersion = Get-OpenClawVersion -VersionString $openClawVersionOutput
                $openClawInstalled = $true
                Write-Host "OpenClaw detected via CLI, version: $openClawVersion" -ForegroundColor Cyan
            }
        }
        catch {
            Write-Host "OpenClaw not detected" -ForegroundColor Cyan
        }
    }

    if ($openClawInstalled) {
        if (Compare-Version -Version1 $openClawVersion -Version2 $targetOpenClawVersion) {
            Write-Host "`n[âˆš] Current OpenClaw version $openClawVersion is up to date (>= $targetOpenClawVersion), no action needed" -ForegroundColor Green
            return
        }
        else {
            Write-Host "Need to update OpenClaw from $openClawVersion to $targetOpenClawVersion" -ForegroundColor Cyan
            Write-Host "Running: npm install -g openclaw$versionSuffix" -ForegroundColor Cyan
            $env:NPM_CONFIG_FUND = "false"
            $env:NPM_CONFIG_AUDIT = "false"
            $env:NPM_CONFIG_LOGLEVEL = "error"
            npm install -g openclaw$versionSuffix 2>&1 | Where-Object { $_ -notmatch "npm warn" }
            if ($LASTEXITCODE -eq 0) {
                Write-Host "`n[âˆš] OpenClaw installed successfully!" -ForegroundColor Green
            }
            else {
                Write-Host "`n[Ã--] OpenClaw installation failed, check network or permissions" -ForegroundColor Red
                exit 1
            }
        }
    }
    else {
        Write-Host "Need to install OpenClaw$versionSuffix" -ForegroundColor Cyan
        Write-Host "Running: npm install -g openclaw$versionSuffix" -ForegroundColor Cyan
        $env:NPM_CONFIG_FUND = "false"
        $env:NPM_CONFIG_AUDIT = "false"
        $env:NPM_CONFIG_LOGLEVEL = "error"
        npm install -g openclaw$versionSuffix 2>&1 | Where-Object { $_ -notmatch "npm warn" }
        if ($LASTEXITCODE -eq 0) {
            Write-Host "`n[âˆš] OpenClaw installed successfully!" -ForegroundColor Green
        }
        else {
            Write-Host "`n[Ã--] OpenClaw installation failed, check network or permissions" -ForegroundColor Red
            exit 1
        }
    }

    try {
        $finalVersion = openclaw -v 2>&1
        Write-Host "`nFinal verification: OpenClaw current version is $finalVersion" -ForegroundColor Cyan
    }
    catch {
        Write-Host "`n[!] OpenClaw version verification failed, but install/update completed" -ForegroundColor Yellow
    }
}

# ======================== OpenClaw Init + Config ========================
function Initialize-OpenClaw {
    Write-Host "`n========== Initializing OpenClaw ==========" -ForegroundColor Yellow

    Write-Host "Running OpenClaw init command..." -ForegroundColor Cyan
    $initCommand = "openclaw onboard --non-interactive --accept-risk --custom-base-url $customBaseUrl --custom-api-key $customApiKey --custom-model-id $customModelId --custom-provider-id $customProviderId --skip-health --skip-channels --skip-skills"
    Write-Host "Running: openclaw onboard" -ForegroundColor Gray
    Invoke-Expression $initCommand 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[âˆš] OpenClaw initialized successfully!" -ForegroundColor Green
    }
    else {
        Write-Host "[Ã--] OpenClaw initialization failed, check parameters" -ForegroundColor Red
        exit 1
    }

    Write-Host "Updating OpenClaw config file..." -ForegroundColor Cyan
    $configPath = "$env:USERPROFILE\.openclaw\openclaw.json"
    if (Test-Path $configPath) {
        $configContent = Get-Content $configPath -Raw -Encoding UTF8
        $newConfigContent = $configContent -replace '"coding"', '"full"'
        Set-ContentNoBom -Path $configPath -Value $newConfigContent
    }
    else {
        Write-Host "[!] Config file not found: $configPath" -ForegroundColor Yellow
        Write-Host "Please manually create the file and set `"tools`": { `"default`": `"full`" }" -ForegroundColor Yellow
    }
}

# ======================== OpenClaw Startup ========================
function Start-OpenClaw {
    Write-Host "`n========== Starting OpenClaw Services ==========" -ForegroundColor Yellow

    Write-Host "Starting OpenClaw Gateway service..." -ForegroundColor Cyan
    Start-Process powershell.exe -WindowStyle Hidden -ArgumentList "-NoLogo -NoProfile -ExecutionPolicy Bypass -Command `"openclaw gateway stop 2>`$null; openclaw gateway --force`""
    Write-Host "[âˆš] OpenClaw Gateway service started (detached background)" -ForegroundColor Green

    Write-Host "Waiting for service initialization (10 seconds)..." -ForegroundColor Cyan
    Start-Sleep -Seconds 10

    Write-Host "Opening OpenClaw Dashboard UI..." -ForegroundColor Cyan
    Start-Process powershell.exe -ArgumentList "-ExecutionPolicy Bypass -Command `"openclaw dashboard`""
    Write-Host "[âˆš] OpenClaw Dashboard UI opened" -ForegroundColor Green

    Write-Host "`n========== OpenClaw Startup Complete ==========" -ForegroundColor Yellow

    Write-Host "`n========== Usage Tips ==========" -ForegroundColor Yellow
    Write-Host "1. Start OpenClaw service:" -ForegroundColor White
    Write-Host " openclaw gateway --force" -ForegroundColor Cyan
    Write-Host "2. Current workspace (generated files location):" -ForegroundColor White
    Write-Host " $env:USERPROFILE\.openclaw\workspace" -ForegroundColor Cyan
    Write-Host "3. OpenClaw Web UI URL:" -ForegroundColor White
    Write-Host " http://127.0.0.1:18789" -ForegroundColor Cyan
    Write-Host "4. Open OpenClaw Web UI:" -ForegroundColor White
    Write-Host " openclaw dashboard" -ForegroundColor Cyan
    Write-Host "5. OpenClaw local terminal:" -ForegroundColor White
    Write-Host " openclaw tui" -ForegroundColor Cyan
    Write-Host "6. Update OpenClaw to latest version (if needed):" -ForegroundColor White
    Write-Host " openclaw update" -ForegroundColor Cyan
}

# ======================== Install (Full Setup Flow) ========================
function Do-Install {
    Log-Operation 'install' 'openclaw'

    if (Find-OpenClaw) {
        Write-Host "`n========== OpenClaw already installed ==========" -ForegroundColor Yellow
        Get-Status | Out-Null
        if ($Action -eq '') {
            $answer = Read-Host 'Reinstall? [y/N]'
            if ($answer -notmatch '^[Yy]') { return }
        } else {
            # Called from manager - just open WebUI
            $port = netstat -ano 2>$null | Select-String "18789.*LISTENING"
            if (-not $port) {
                Write-Host 'Starting gateway in background...' -ForegroundColor Cyan
                Start-Process powershell.exe -WindowStyle Hidden -ArgumentList "-NoLogo -NoProfile -Command openclaw gateway --force 2>`$null"
                Write-Host 'Waiting 10s...' -ForegroundColor Gray
                Start-Sleep 10
            }
            Write-Host 'Opening WebUI...' -ForegroundColor Green
            Start-Process "http://127.0.0.1:18789"
            return
        }
    }

    if ($targetOpenClawVersion) {
        Write-Host "Using OpenClaw version: $targetOpenClawVersion" -ForegroundColor Cyan
    } else {
        Write-Host "Using latest OpenClaw version" -ForegroundColor Cyan
    }

    # Enable strict error handling for setup operations
    $ErrorActionPreference = "Stop"

    try {
        if (-not (Test-Admin)) {
            Write-Host "Administrator privileges required, requesting elevation..." -ForegroundColor Yellow
            $elevationArgs = "-ExecutionPolicy Bypass -File `"$PSCommandPath`" -Action install"
            if ($Provider)  { $elevationArgs += " -Provider `"$Provider`"" }
            if ($ApiKey)    { $elevationArgs += " -ApiKey `"$ApiKey`"" }
            if ($Model)     { $elevationArgs += " -Model `"$Model`"" }
            if ($Version)   { $elevationArgs += " -Version `"$Version`"" }
            if ($BaseUrl)   { $elevationArgs += " -BaseUrl `"$BaseUrl`"" }
            Start-Process powershell.exe -ArgumentList $elevationArgs -Verb RunAs
            exit 0
        }

        if (-not (Test-Path $tempDir)) { New-Item -ItemType Directory -Path $tempDir | Out-Null }

        Install-NodeJs
        Install-Git
        Install-OpenClawApp

        if (Test-Path $openClawConfigPath) {
            Write-Host "`nOpenClaw config file already exists: $openClawConfigPath" -ForegroundColor Green
            Write-Host "Skipping init, starting services directly..." -ForegroundColor Cyan
        }
        else {
            Initialize-OpenClaw
        }

        Start-OpenClaw

        Write-Host "`nCleaning up temp files..." -ForegroundColor Cyan
        Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue

        Write-Host "`n========== All Operations Complete ==========" -ForegroundColor Yellow
        Write-Host "[âˆš] OpenClaw $targetOpenClawVersion installation and startup complete!" -ForegroundColor Green
    }
    catch {
        Write-Host "`n[Ã--] Script execution error: $_" -ForegroundColor Red
        Write-Host "`n[?] Try manually deleting $env:USERPROFILE\.openclaw (if it exists) and re-run the script" -ForegroundColor Yellow
        if (Test-Path $tempDir) { Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue }
        exit 1
    }
}

# ======================== Update ========================
function Do-Update {
    if (-not (Find-OpenClaw)) {
        Write-Host "OpenClaw is not installed." -ForegroundColor Yellow
        return
    }
    Write-Host "`n========== Checking for Updates ==========" -ForegroundColor Yellow

    $latestVersion = Get-LatestVersion
    if (-not $latestVersion) {
        Write-Host "[!] Could not check latest version (network issue?)" -ForegroundColor Yellow
        return
    }

    $currentVerOutput = & openclaw -v 2>&1
    $currentVersion = $null
    if ($currentVerOutput -match '(\d+\.\d+\.\d+)') { $currentVersion = $matches[1] }

    if ($currentVersion -and (Compare-Version -Version1 $currentVersion -Version2 $latestVersion)) {
        Write-Host "[âˆš] Already on latest version v$currentVersion" -ForegroundColor Green
        return
    }

    Write-Host "Updating OpenClaw from v$currentVersion to v$latestVersion..." -ForegroundColor Cyan
    Write-Host "Running: npm install -g openclaw@$latestVersion" -ForegroundColor Cyan
    $env:NPM_CONFIG_FUND = "false"
    $env:NPM_CONFIG_AUDIT = "false"
    $env:NPM_CONFIG_LOGLEVEL = "error"
    npm install -g openclaw@$latestVersion 2>&1 | Where-Object { $_ -notmatch "npm warn" }
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[âˆš] OpenClaw updated to v$latestVersion" -ForegroundColor Green
        Write-Host "Restart running services: openclaw gateway stop; openclaw gateway --force" -ForegroundColor Gray
    } else {
        Write-Host "[Ã--] Update failed" -ForegroundColor Red
    }
}

# ======================== Uninstall ========================
function Do-Uninstall {
    Log-Operation 'uninstall' 'openclaw'

    if (-not (Find-OpenClaw)) {
        Write-Host "OpenClaw is not installed." -ForegroundColor Yellow
        return
    }
    Write-Host "
========== Uninstalling OpenClaw ==========" -ForegroundColor Yellow
    
    if (Get-Command npm -ErrorAction SilentlyContinue) {
        try {
            $null = & npm uninstall -g openclaw 2>&1
            Write-Host "Removed via npm" -ForegroundColor Gray
        } catch {
            Write-Host "npm uninstall failed: $_" -ForegroundColor Gray
        }
    } else {
        Write-Host "npm not found on PATH, skipping npm uninstall" -ForegroundColor Gray
    }
    
    $openClawDir = "$env:USERPROFILE\.openclaw"
    if (Test-Path $openClawDir) {
        Remove-Item $openClawDir -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "Removed $openClawDir" -ForegroundColor Gray
    }
    
    Write-Host "[âˆš] OpenClaw uninstalled" -ForegroundColor Green
}

# ======================== Menu ========================
function Show-Menu {
    Write-Host ''
    Write-Host 'OpenClaw Manager' -ForegroundColor Cyan
    Write-Host '---------------' -ForegroundColor DarkGray
    Write-Host '[1] Install'   -ForegroundColor White
    Write-Host '[2] Uninstall' -ForegroundColor White
    Write-Host '[3] Status'    -ForegroundColor White
    Write-Host '[4] Update'    -ForegroundColor White
    Write-Host '[Q] Quit'      -ForegroundColor DarkGray
    Write-Host ''

    $choice = Read-Host 'Choose'
    switch ($choice) {
        '1' { Do-Install }
        '2' { Do-Uninstall }
        '3' { Get-Status | Out-Null }
        '4' { Do-Update }
        default { return }
    }
}

# ======================== Entry Point ========================
switch ($Action) {
    'install'   { Do-Install }
    'uninstall' { Do-Uninstall }
    'status'    { Get-Status | Out-Null }
    'update'    { Do-Update }
    default     { Show-Menu }
}


