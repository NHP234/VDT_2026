param(
    [switch] $Build
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
Push-Location $root

function Get-EnvFileValue {
    param(
        [string] $Path,
        [string] $Name,
        [string] $DefaultValue
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return $DefaultValue
    }

    $line = Get-Content -LiteralPath $Path |
        Where-Object { $_ -match "^$([regex]::Escape($Name))=" } |
        Select-Object -First 1

    if (-not $line) {
        return $DefaultValue
    }

    return ($line -replace "^$([regex]::Escape($Name))=", "")
}

try {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "Docker CLI was not found. Install Docker Desktop or make docker available on PATH."
    }

    $dockerConfig = Join-Path $root ".docker-cli"
    if (-not (Test-Path -LiteralPath $dockerConfig)) {
        New-Item -ItemType Directory -Path $dockerConfig | Out-Null
    }
    $env:DOCKER_CONFIG = $dockerConfig

    if (-not (Test-Path -LiteralPath ".env")) {
        Copy-Item -LiteralPath ".env.example" -Destination ".env"
        Write-Host "Created local .env from .env.example. Review it before adding real credentials."
    }

    $composeArgs = @("compose", "--env-file", ".env", "up", "-d")
    if ($Build) {
        $composeArgs += "--build"
    }

    & docker @composeArgs

    $frontendPort = Get-EnvFileValue -Path ".env" -Name "FRONTEND_PORT" -DefaultValue "5173"
    $inboxPort = Get-EnvFileValue -Path ".env" -Name "INBOX_SERVICE_PORT" -DefaultValue "8080"
    $channelPort = Get-EnvFileValue -Path ".env" -Name "CHANNEL_SERVICE_PORT" -DefaultValue "8081"
    $mailpitPort = Get-EnvFileValue -Path ".env" -Name "MAILPIT_WEB_PORT" -DefaultValue "8025"

    Write-Host "Local stack is starting."
    Write-Host "Frontend: http://localhost:$frontendPort"
    Write-Host "Inbox service: http://localhost:$inboxPort"
    Write-Host "Channel service: http://localhost:$channelPort"
    Write-Host "Mailpit UI: http://localhost:$mailpitPort"
}
finally {
    Pop-Location
}
