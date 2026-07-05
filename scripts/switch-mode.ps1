param (
    [ValidateSet("simulator", "real")]
    [string]$Mode = "simulator"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
Push-Location $root

try {
    if ($Mode -eq "real") {
        if (-not (Test-Path -LiteralPath ".env.real")) {
            throw "File .env.real does not exist. Please create it first."
        }
        Copy-Item -Path ".env.real" -Destination ".env" -Force
        Write-Host ">>> Switched configuration to REAL mode." -ForegroundColor Green
    } else {
        if (-not (Test-Path -LiteralPath ".env.simulator")) {
            throw "File .env.simulator does not exist. Please create it first."
        }
        Copy-Item -Path ".env.simulator" -Destination ".env" -Force
        Write-Host ">>> Switched configuration to SIMULATOR mode." -ForegroundColor Cyan
    }

    # Automatically force recreate containers to apply the new environment variables
    Write-Host ">>> Rebuilding/recreating containers with new env..." -ForegroundColor Yellow
    docker compose up -d --force-recreate
    Write-Host ">>> Done! System is ready in $Mode mode." -ForegroundColor Green
}
finally {
    Pop-Location
}
