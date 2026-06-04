<#
.SYNOPSIS
    Hermes AI Agent manager for Windows.
.DESCRIPTION
    Install, uninstall, or check status of Hermes AI Agent (CLI or Desktop).
.EXAMPLE
    .\Install-HermesAgent.ps1                 # interactive menu
    .\Install-HermesAgent.ps1 install         # install CLI
    .\Install-HermesAgent.ps1 install-desktop # install Desktop app
    .\Install-HermesAgent.ps1 uninstall       # uninstall
    .\Install-HermesAgent.ps1 status          # check if installed
#>

param(
    [string]$Action = '',
    [string]$Provider = '',
    [string]$ApiKey = '',
    [string]$Model = '',
    [string]$EmbedModel = '',
    [string]$BaseUrl = ''
)

$ProgressPreference = 'SilentlyContinue'
$HermesDir = Join-Path $env:LOCALAPPDATA 'hermes'
$HermesBin = Join-Path $HermesDir 'hermes.exe'
$DesktopAppDir = "$env:LOCALAPPDATA\Programs\hermes-desktop"
$DesktopAppExe = Join-Path $DesktopAppDir 'hermes-agent.exe'
$GitHubReleases = 'https://api.github.com/repos/fathah/hermes-desktop/releases/latest'

function Find-Hermes {
    $cmd = Get-Command hermes -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $cmd = Get-Command hermes-agent -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    $searchPaths = @(
        (Join-Path $env:USERPROFILE '.local\bin'),
        (Join-Path $env:LOCALAPPDATA 'hermes'),
        (Join-Path $env:APPDATA 'Python\Python311\Scripts'),
        (Join-Path $env:LOCALAPPDATA 'Programs\Python\Python311\Scripts')
    )

    foreach ($dir in $searchPaths) {
        if (Test-Path $dir) {
            $exe = Join-Path $dir 'hermes-agent.exe'
            if (Test-Path $exe) { return $exe }
            $exe = Join-Path $dir 'hermes.exe'
            if (Test-Path $exe) { return $exe }
        }
    }

    return $null
}

function Find-HermesDesktop {
    $regPaths = @('HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*')
    foreach ($rp in $regPaths) {
        $item = Get-ItemProperty $rp -ErrorAction SilentlyContinue | Where-Object { $_.DisplayName -match 'Hermes' }
        if ($item -and $item.UninstallString) {
            $clean = $item.UninstallString -replace '^"|".*$', ''
            if ($clean) {
                $appDir = Split-Path $clean -Parent
                $exe = Join-Path $appDir 'hermes-agent.exe'
                if (Test-Path $exe) { return $exe }
            }
        }
        if ($item -and $item.InstallLocation -and (Test-Path $item.InstallLocation)) {
            $exe = Join-Path $item.InstallLocation 'hermes-agent.exe'
            if (Test-Path $exe) { return $exe }
        }
    }

    # Check Desktop-installed CLI via official install.ps1 (goes to %LOCALAPPDATA%\hermes)
    $wizardExe = "$env:LOCALAPPDATA\hermes\hermes-agent\venv\Scripts\pythonw.exe"
    $wizardScript = "$env:LOCALAPPDATA\hermes\hermes-agent\venv\Scripts\hermes.exe"
    if ((Test-Path $wizardExe) -and (Test-Path $wizardScript)) {
        return $DesktopAppExe
    }

    $knownPaths = @(
        $DesktopAppExe,
        "$env:LOCALAPPDATA\Programs\Hermes Agent\hermes-agent.exe",
        "$env:LOCALAPPDATA\Programs\hermes-agent\hermes-agent.exe",
        "$env:LOCALAPPDATA\hermes-desktop\hermes-agent.exe"
    )
    foreach ($p in $knownPaths) {
        if (Test-Path $p) { return $p }
    }

    return $null
}

function Get-DesktopVersion {
    $exe = Find-HermesDesktop
    if (-not $exe) { return $null }

    try { $ver = (Get-Item $exe).VersionInfo.FileVersion; if ($ver) { return $ver } } catch { }
    try { $ver = (Get-Item $exe).VersionInfo.ProductVersion; if ($ver) { return $ver } } catch { }

    $appDir = Split-Path $exe -Parent
    try {
        $pkg = Join-Path $appDir 'resources\app\package.json'
        if (Test-Path $pkg) {
            $raw = Get-Content $pkg -Raw
            if ($raw -match '"version"\s*:\s*"([^"]+)"') { return $matches[1] }
        }
        $updateYml = Join-Path $appDir 'resources\app-update.yml'
        if (Test-Path $updateYml) {
            $raw = Get-Content $updateYml -Raw
            if ($raw -match 'version[:\s]+(\S+)') { return $matches[1] }
        }
    } catch { }

    try {
        $item = Get-ItemProperty 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*' -ErrorAction SilentlyContinue | Where-Object { $_.DisplayName -match 'Hermes' }
        if ($item -and $item.DisplayVersion) { return $item.DisplayVersion }
    } catch { }

    return $null
}

function Test-HermesInPath {
    $userPath = [System.Environment]::GetEnvironmentVariable('Path', 'User')
    $machinePath = [System.Environment]::GetEnvironmentVariable('Path', 'Machine')
    $persistentPath = "$userPath;$machinePath"

    $pathDirs = $persistentPath -split ';'
    foreach ($dir in $pathDirs) {
        if (-not $dir) { continue }
        if ($dir -match '^\\\\') { continue }
        if (-not (Test-Path $dir)) { continue }
        $exe = Join-Path $dir 'hermes-agent.exe'
        if (Test-Path $exe) { return $true }
        $exe = Join-Path $dir 'hermes.exe'
        if (Test-Path $exe) { return $true }
    }

    return $false
}

function Add-HermesToPath {
    param([string]$hermesPath)
    if (-not $hermesPath) { return $false }
    $hermesDir = Split-Path $hermesPath -Parent
    $pathDirs = $env:PATH -split ';'
    foreach ($dir in $pathDirs) {
        if ($dir -eq $hermesDir) { return $false }
    }
    $env:PATH = "$hermesDir;$env:PATH"
    Write-Host "Added $hermesDir to PATH" -ForegroundColor Green
    return $true
}

function Get-LatestVersion {
    try {
        $json = Invoke-RestMethod -Uri 'https://pypi.org/pypi/hermes-agent/json' -TimeoutSec 5 -ErrorAction Stop
        return $json.info.version
    } catch { }
    return $null
}

function Get-LatestDesktopVersion {
    try {
        $json = Invoke-RestMethod -Uri $GitHubReleases -TimeoutSec 10 -ErrorAction Stop
        return $json.tag_name -replace '^v', ''
    } catch { }
    return $null
}

function Get-Status {
    $cliPath = Find-Hermes
    $desktopPath = Find-HermesDesktop

    if (-not $cliPath -and -not $desktopPath) {
        Write-Host 'NOT INSTALLED' -ForegroundColor Red
        return $false
    }

    if ($cliPath) {
        $currentVersion = $null
        try {
            $raw = uv tool list 2>&1 | Out-String
            if ($raw -match 'hermes-agent\s+v?([^\s]+)') { $currentVersion = $matches[1] }
        } catch { }

        $latestVersion = Get-LatestVersion
        Write-Host 'INSTALLED (CLI)' -ForegroundColor Green -NoNewline
        if ($currentVersion) { Write-Host " v$currentVersion" -NoNewline -ForegroundColor Cyan }
        Write-Host " - $cliPath"

        if ($currentVersion -and $latestVersion) {
            if ($latestVersion -eq $currentVersion) {
                Write-Host "  Latest version (v$latestVersion), up to date" -ForegroundColor Green
            } else {
                Write-Host "  Latest: v$latestVersion (you have v$currentVersion)" -ForegroundColor Gray
            }
        } elseif ($currentVersion -and -not $latestVersion) {
            Write-Host '  Could not check for updates (offline?)' -ForegroundColor Gray
        }
    }

    if ($desktopPath) {
        $desktopVersion = Get-DesktopVersion
        $desktopLatest = Get-LatestDesktopVersion
        Write-Host 'INSTALLED (Desktop)' -ForegroundColor Green -NoNewline
        if ($desktopVersion) { Write-Host " v$desktopVersion" -NoNewline -ForegroundColor Cyan }
        Write-Host " - $desktopPath"

        if ($desktopVersion -and $desktopLatest) {
            if ($desktopLatest -eq $desktopVersion) {
                Write-Host "  Latest version (v$desktopLatest), up to date" -ForegroundColor Green
            } else {
                Write-Host "  Latest: v$desktopLatest (you have v$desktopVersion)" -ForegroundColor Gray
            }
        } elseif ($desktopVersion -and -not $desktopLatest) {
            Write-Host '  Could not check for updates (offline?)' -ForegroundColor Gray
        }
    }

    return $true
}

function Do-Install {
    if (Find-Hermes) {
        Write-Host '[WARN] Hermes CLI is already installed.' -ForegroundColor Yellow
        return
    }
    Write-Host "`n========== Installing Hermes CLI ==========" -ForegroundColor Yellow

    & uv tool install hermes-agent --with websockets 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host '[FAIL] uv tool install failed' -ForegroundColor Red
        return
    }
    Write-Host '[OK] hermes-agent installed via uv' -ForegroundColor Green

    $cfgDir = Join-Path $env:LOCALAPPDATA 'hermes'
    if (-not (Test-Path $cfgDir)) {
        New-Item -ItemType Directory -Path $cfgDir -Force | Out-Null
    }

    $configYaml = Join-Path $cfgDir 'config.yaml'
    if (-not (Test-Path $configYaml)) {
        $lines = @()
        if ($Model)      { $lines += "model: $Model" }
        if ($Provider)   { $lines += "provider: $Provider" }
        if ($ApiKey)     { $lines += "api_key: $ApiKey" }
        if ($EmbedModel) { $lines += "embed_model: $EmbedModel" }
        if ($lines) {
            Set-Content -Path $configYaml -Value ($lines -join "`n")
            Write-Host "[OK] Created $configYaml" -ForegroundColor Gray
        }
    }

    Write-Host '[OK] Hermes CLI installed' -ForegroundColor Green
}

function Do-InstallAll {
    Do-InstallDesktop
}

function Do-InstallDesktop {
    Write-Host "`n========== Hermes Desktop ==========" -ForegroundColor Yellow

    # If already installed, just open it
    $pythonw = "$env:LOCALAPPDATA\hermes\hermes-agent\venv\Scripts\pythonw.exe"
    $hermesExe = "$env:LOCALAPPDATA\hermes\hermes-agent\venv\Scripts\hermes.exe"
    if ((Test-Path $pythonw) -and (Test-Path $hermesExe) -and (Test-Path $DesktopAppExe)) {
        Write-Host '  Already installed — opening Desktop...' -ForegroundColor Gray
        if (-not (Get-Process -Name 'hermes-agent' -ErrorAction SilentlyContinue)) {
            Start-Process $DesktopAppExe
        }
        Write-Host '[OK] Hermes Desktop running' -ForegroundColor Green
        return
    }

    # Kill running Desktop (it locks files in venv)
    Write-Host '  Stopping existing processes...' -ForegroundColor Gray
    Get-Process -Name 'hermes-agent' -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
    Get-Process -Name 'python' -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep 3

    # Desktop uses %LOCALAPPDATA%\hermes on Windows (not ~/.hermes)
    $cfgDir = Join-Path $env:LOCALAPPDATA 'hermes'
    New-Item -ItemType Directory -Path $cfgDir -Force | Out-Null

    # Write .env with provider-specific keys from config.json
    $envFile = Join-Path $cfgDir '.env'
    @"
CUSTOM_API_KEY=${ApiKey}
CUSTOM_BASE_URL=${BaseUrl}
CUSTOM_DEFAULT_MODEL=${Model}
API_SERVER_KEY=$([guid]::NewGuid().ToString())
"@ | Set-Content -Path $envFile -Encoding UTF8 -Force
    Write-Host "[OK] Written $envFile" -ForegroundColor Gray

    # Write config.yaml with direct values from config.json
    $configYaml = Join-Path $cfgDir 'config.yaml'
    $lines = @(
        'model:',
        "  default: `"$Model`"",
        "  provider: `"$Provider`""
    )
    $lines += ''
    $lines += 'providers:'
    $lines += "  $Provider`:"
    $lines += "    api_key: `"$ApiKey`""
    if ($BaseUrl) { $lines += "    base_url: `"$BaseUrl`"" }
    ($lines -join "`n") | Set-Content -Path $configYaml -Encoding UTF8 -Force
    Write-Host "[OK] Written $configYaml (provider: $Provider)" -ForegroundColor Gray

    # Mark setup as complete so Desktop skips first-run wizard
    @{ locale = "en"; setupComplete = $true } | ConvertTo-Json | Set-Content -Path (Join-Path $cfgDir 'desktop.json') -Encoding UTF8 -Force
    Write-Host "[OK] Written $cfgDir\desktop.json" -ForegroundColor Gray

    # Also mirror to ~/.hermes (Desktop may fall back here on NSIS launch)
    $homeHermes = Join-Path $env:USERPROFILE '.hermes'
    New-Item -ItemType Directory -Path $homeHermes -Force | Out-Null
    Copy-Item "$cfgDir\.env" "$homeHermes\.env" -Force
    Copy-Item "$cfgDir\config.yaml" "$homeHermes\config.yaml" -Force
    Copy-Item "$cfgDir\desktop.json" "$homeHermes\desktop.json" -Force
    Write-Host "[OK] Mirrored config to $homeHermes" -ForegroundColor Gray

    # Run official install.ps1 to install Hermes CLI with proper venv layout
    Write-Host 'Downloading wizard install script...' -ForegroundColor Gray
    $installScript = Join-Path $cfgDir 'install.ps1'
    try {
        Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/NousResearch/hermes-agent/main/scripts/install.ps1' -OutFile $installScript -UseBasicParsing
    } catch {
        Write-Host '[FAIL] Could not download install.ps1' -ForegroundColor Red
        return
    }

    Write-Host 'Running wizard install script...' -ForegroundColor Gray
    $p = Start-Process -FilePath 'powershell' -ArgumentList @(
        '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $installScript,
        '-SkipSetup', '-HermesHome', $cfgDir, '-InstallDir', "$cfgDir\hermes-agent"
    ) -Wait -NoNewWindow -PassThru
    if ($p.ExitCode -ne 0) {
        Write-Host '[WARN] Wizard install script returned exit code ' + $p.ExitCode -ForegroundColor Yellow
    } else {
        Write-Host '[OK] Wizard install script completed' -ForegroundColor Green
    }

    # Re-write config (install.ps1 may have overwritten it with templates)
    $envFile = Join-Path $cfgDir '.env'
    @"
CUSTOM_API_KEY=${ApiKey}
CUSTOM_BASE_URL=${BaseUrl}
CUSTOM_DEFAULT_MODEL=${Model}
API_SERVER_KEY=$([guid]::NewGuid().ToString())
"@ | Set-Content -Path $envFile -Encoding UTF8 -Force
    $configYaml = Join-Path $cfgDir 'config.yaml'
    $lines = @(
        'model:',
        "  default: `"$Model`"",
        "  provider: `"$Provider`""
    )
    $lines += ''
    $lines += 'providers:'
    $lines += "  $Provider`:"
    $lines += "    api_key: `"$ApiKey`""
    if ($BaseUrl) { $lines += "    base_url: `"$BaseUrl`"" }
    ($lines -join "`n") | Set-Content -Path $configYaml -Encoding UTF8 -Force
    # Mirror to ~/.hermes
    $homeHermes = Join-Path $env:USERPROFILE '.hermes'
    New-Item -ItemType Directory -Path $homeHermes -Force | Out-Null
    Copy-Item "$cfgDir\.env" "$homeHermes\.env" -Force
    Copy-Item "$cfgDir\config.yaml" "$homeHermes\config.yaml" -Force
    Copy-Item "$cfgDir\desktop.json" "$homeHermes\desktop.json" -Force
    Write-Host '  Config files re-written after install' -ForegroundColor Gray

    # Download and install Desktop app
    Write-Host 'Looking up latest Desktop release...' -ForegroundColor Gray
    try {
        $rel = Invoke-WebRequest -Uri $GitHubReleases -UseBasicParsing | ConvertFrom-Json
    } catch {
        Write-Host '[FAIL] Cannot reach GitHub' -ForegroundColor Red
        return
    }

    $ver = $rel.tag_name -replace '^v', ''
    $asset = $rel.assets | Where-Object { $_.name -match '-setup\.exe$' } | Select-Object -First 1
    if (-not $asset) { $asset = $rel.assets | Where-Object { $_.name -match '\.exe$' -and $_.name -notmatch 'blockmap' } | Select-Object -First 1 }

    Write-Host "  v$ver ($([math]::Round($asset.size/1MB, 1)) MB)" -ForegroundColor Gray
    Write-Host "  Downloading..." -ForegroundColor Gray
    $setupExe = Join-Path $env:TEMP $asset.name
    $ProgressPreference = 'SilentlyContinue'
    try {
        Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $setupExe -UseBasicParsing
    } catch {
        Write-Host '[FAIL] Download failed' -ForegroundColor Red
        return
    }

    Write-Host 'Installing Desktop app (silent)...' -ForegroundColor Gray
    $p = Start-Process -FilePath $setupExe -ArgumentList '/S' -Wait -NoNewWindow -PassThru
    Remove-Item $setupExe -Force -ErrorAction SilentlyContinue

    Write-Host '[OK] Hermes Desktop installed' -ForegroundColor Green

    # NSIS one-click auto-launches Desktop. Wait for it, restart if crashed.
    Write-Host '  Waiting for Desktop...' -ForegroundColor Gray
    $appeared = $false
    for ($i = 0; $i -lt 10; $i++) {
        Start-Sleep 3
        $p = Get-Process -Name 'hermes-agent' -ErrorAction SilentlyContinue
        if ($p) { $appeared = $true; Write-Host "  Desktop running (PID $($p.Id))" -ForegroundColor Green; break }
    }
    if (-not $appeared) {
        Write-Host '  NSIS launch may have failed - starting manually' -ForegroundColor Yellow
        Start-Process $DesktopAppExe
    }
}

function Do-Update {
    if (-not (Find-Hermes)) {
        Write-Host 'Hermes CLI is not installed.' -ForegroundColor Yellow
        return
    }
    Write-Host "`n========== Checking for Updates ==========" -ForegroundColor Yellow

    $latestVersion = Get-LatestVersion
    if (-not $latestVersion) {
        Write-Host '[WARN] Could not check latest version (network issue?)' -ForegroundColor Yellow
        return
    }

    $currentVersion = $null
    try {
        $raw = uv tool list 2>&1 | Out-String
        if ($raw -match 'hermes-agent\s+v?([^\s]+)') { $currentVersion = $matches[1] }
    } catch { }

    if ($currentVersion -and ($currentVersion -eq $latestVersion)) {
        Write-Host "[OK] Already on latest version v$currentVersion" -ForegroundColor Green
        return
    }

    Write-Host "Updating hermes-agent from v$currentVersion to v$latestVersion..." -ForegroundColor Cyan
    uv tool upgrade hermes-agent 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] hermes-agent updated to v$latestVersion" -ForegroundColor Green
    } else {
        Write-Host '[WARN] uv upgrade failed, trying reinstall...' -ForegroundColor Yellow
        uv tool install hermes-agent --with websockets --reinstall 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Host '[OK] hermes-agent reinstalled' -ForegroundColor Green
        } else {
            Write-Host '[FAIL] Update failed' -ForegroundColor Red
        }
    }
}

function Do-UninstallCli {
    if (-not (Find-Hermes)) {
        Write-Host 'Hermes CLI is not installed.' -ForegroundColor Yellow
        return
    }
    Write-Host "`n========== Uninstalling Hermes CLI ==========" -ForegroundColor Yellow

    & uv tool uninstall hermes-agent 2>&1 | Out-Null
    Write-Host 'Removed hermes-agent via uv' -ForegroundColor Gray

    if (Test-Path $HermesDir) { Remove-Item $HermesDir -Recurse -Force -ErrorAction SilentlyContinue }
    Get-Item "$env:USERPROFILE\.local\bin\hermes*" -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue

    $hermesConfigDir = "$env:USERPROFILE\.hermes"
    if (Test-Path $hermesConfigDir) {
        Remove-Item $hermesConfigDir -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "Removed $hermesConfigDir" -ForegroundColor Gray
    }

    $path = [System.Environment]::GetEnvironmentVariable('Path', 'User')
    $cleaned = ($path -split ';' | Where-Object { $_ -notlike '*hermes*' }) -join ';'
    [System.Environment]::SetEnvironmentVariable('Path', $cleaned, 'User')
    $env:PATH = "$([System.Environment]::GetEnvironmentVariable('Path', 'User'));$([System.Environment]::GetEnvironmentVariable('Path', 'Machine'))"

    Write-Host '[OK] Hermes CLI uninstalled' -ForegroundColor Green
}

function Do-UninstallDesktop {
    Write-Host "`n========== Uninstalling Hermes Desktop ==========" -ForegroundColor Yellow

    Get-Process -Name 'hermes-agent' -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep 2

    $found = Find-HermesDesktop
    if (-not $found) {
        Write-Host 'Hermes Desktop is not installed.' -ForegroundColor Yellow
        return
    }

    $installDir = Split-Path $found -Parent
    if ($installDir -like '*\hermes-agent' -or $installDir -like '*\hermes-desktop' -or $installDir -like '*\Hermes Agent') {
        if (Test-Path $installDir) {
            Remove-Item $installDir -Recurse -Force -ErrorAction SilentlyContinue
            Write-Host "Removed $installDir" -ForegroundColor Gray
        }
    }

    $hermesConfigDir = "$env:USERPROFILE\.hermes"
    if (Test-Path $hermesConfigDir) {
        Remove-Item $hermesConfigDir -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "Removed $hermesConfigDir" -ForegroundColor Gray
    }

    $startMenu = [Environment]::GetFolderPath('StartMenu')
    $shortcutDirs = @(
        (Join-Path $startMenu 'Programs\Hermes Agent'),
        (Join-Path $startMenu 'Programs\Hermes Desktop')
    )
    foreach ($sd in $shortcutDirs) {
        if (Test-Path $sd) { Remove-Item $sd -Recurse -Force -ErrorAction SilentlyContinue }
    }

    Write-Host '[OK] Hermes Desktop uninstalled' -ForegroundColor Green
}

function Do-UninstallAll {
    Write-Host "`n========== Uninstalling ALL Hermes ==========" -ForegroundColor Yellow

    # Kill running Desktop (locks files in venv)
    Write-Host '  Stopping Desktop...' -ForegroundColor Gray
    Get-Process -Name 'hermes-agent' -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep 2

    if (Find-Hermes) {
        Write-Host 'Removing CLI...' -ForegroundColor Gray
        & uv tool uninstall hermes-agent 2>&1 | Out-Null
        Write-Host 'Removed hermes-agent via uv' -ForegroundColor Gray
    }

    if (Test-Path $HermesDir) { Remove-Item $HermesDir -Recurse -Force -ErrorAction SilentlyContinue }
    Get-Item "$env:USERPROFILE\.local\bin\hermes*" -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue

    $hermesConfigDir = "$env:USERPROFILE\.hermes"
    if (Test-Path $hermesConfigDir) {
        Remove-Item $hermesConfigDir -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "Removed $hermesConfigDir" -ForegroundColor Gray
    }

    $desktopDirs = @(
        $DesktopAppDir,
        "$env:LOCALAPPDATA\hermes-desktop",
        "$env:APPDATA\hermes-desktop"
    )
    foreach ($dd in $desktopDirs) {
        if (Test-Path $dd) {
            Remove-Item $dd -Recurse -Force -ErrorAction SilentlyContinue
            Write-Host "Removed $dd" -ForegroundColor Gray
        }
    }

    $startMenu = [Environment]::GetFolderPath('StartMenu')
    $shortcutDirs = @(
        (Join-Path $startMenu 'Programs\Hermes Agent'),
        (Join-Path $startMenu 'Programs\Hermes Desktop')
    )
    foreach ($sd in $shortcutDirs) {
        if (Test-Path $sd) { Remove-Item $sd -Recurse -Force -ErrorAction SilentlyContinue }
    }

    $path = [System.Environment]::GetEnvironmentVariable('Path', 'User')
    $cleaned = ($path -split ';' | Where-Object { $_ -notlike '*hermes*' }) -join ';'
    [System.Environment]::SetEnvironmentVariable('Path', $cleaned, 'User')
    $env:PATH = "$([System.Environment]::GetEnvironmentVariable('Path', 'User'));$([System.Environment]::GetEnvironmentVariable('Path', 'Machine'))"

    Write-Host '[OK] ALL Hermes components uninstalled' -ForegroundColor Green
}

function Show-Menu {
    Write-Host ''
    Write-Host '  Hermes Agent Manager' -ForegroundColor Cyan
    Write-Host '  -------------------' -ForegroundColor DarkGray
    Write-Host '  [1] Install'   -ForegroundColor White
    Write-Host '  [2] Uninstall' -ForegroundColor White
    Write-Host '  [3] Status'    -ForegroundColor White
    Write-Host '  [4] Update'    -ForegroundColor White
    Write-Host '  [Q] Quit'      -ForegroundColor DarkGray
    Write-Host ''

    $choice = Read-Host 'Choose'
    switch ($choice) {
        '1' { Do-InstallDesktop }
        '2' { Do-UninstallAll }
        '3' { Get-Status | Out-Null }
        '4' { Do-Update }
        default { return }
    }
}

switch ($Action) {
    'install'    { Do-InstallDesktop }
    'uninstall'  { Do-UninstallAll }
    'status'     { Get-Status | Out-Null }
    'update'     { Do-Update }
    default      { Show-Menu }
}

