param(
    [switch] $Volumes
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

    $envFile = ".env"
    if (-not (Test-Path -LiteralPath $envFile)) {
        $envFile = ".env.example"
    }

    $composeArgs = @("compose", "--env-file", $envFile, "down")
    if ($Volumes) {
        $composeArgs += "--volumes"
    }

    & docker @composeArgs
}
finally {
    Pop-Location
}
