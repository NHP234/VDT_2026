param(
    [switch] $Build
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
Push-Location $root

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
    Write-Host "Local infrastructure is starting. Mailpit UI: http://localhost:8025"
}
finally {
    Pop-Location
}
