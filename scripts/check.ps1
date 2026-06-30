param(
    [switch] $SkipDocker
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
Push-Location $root

function Invoke-IfExists {
    param(
        [string] $Path,
        [scriptblock] $Action,
        [string] $SkipMessage
    )

    if (Test-Path -LiteralPath $Path) {
        & $Action
    }
    else {
        Write-Host $SkipMessage
    }
}

function Ensure-JavaHome {
    if ($env:JAVA_HOME) {
        return
    }

    $defaultJavaHome = "C:\Program Files\Java\jdk-21"
    if (Test-Path -LiteralPath (Join-Path $defaultJavaHome "bin\java.exe")) {
        $env:JAVA_HOME = $defaultJavaHome
        Write-Host "JAVA_HOME was not set. Using $defaultJavaHome for this check run."
    }
}

function Invoke-NativeChecked {
    param(
        [scriptblock] $Command,
        [string] $Description
    )

    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw "$Description failed with exit code $LASTEXITCODE."
    }
}

try {
    if (-not $SkipDocker) {
        if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
            throw "Docker CLI was not found. Re-run with -SkipDocker to skip Compose validation."
        }

        $dockerConfig = Join-Path $root ".docker-cli"
        if (-not (Test-Path -LiteralPath $dockerConfig)) {
            New-Item -ItemType Directory -Path $dockerConfig | Out-Null
        }
        $env:DOCKER_CONFIG = $dockerConfig

        Invoke-NativeChecked -Description "Docker Compose configuration validation" -Command {
            & docker compose --env-file ".env.example" config | Out-Null
        }
        Write-Host "Docker Compose configuration is valid."
    }

    Ensure-JavaHome

    Invoke-IfExists `
        -Path "backend/inbox-service/mvnw.cmd" `
        -Action {
            Push-Location "backend/inbox-service"
            try {
                Invoke-NativeChecked -Description "inbox-service Maven tests" -Command {
                    .\mvnw.cmd -B test
                }
            }
            finally {
                Pop-Location
            }
        } `
        -SkipMessage "Skipping inbox-service tests: Maven project has not been scaffolded yet."

    Invoke-IfExists `
        -Path "backend/channel-service/mvnw.cmd" `
        -Action {
            Push-Location "backend/channel-service"
            try {
                Invoke-NativeChecked -Description "channel-service Maven tests" -Command {
                    .\mvnw.cmd -B test
                }
            }
            finally {
                Pop-Location
            }
        } `
        -SkipMessage "Skipping channel-service tests: Maven project has not been scaffolded yet."

    Invoke-IfExists `
        -Path "frontend/package.json" `
        -Action {
            Push-Location "frontend"
            try {
                Invoke-NativeChecked -Description "frontend npm ci" -Command {
                    npm ci
                }
                Invoke-NativeChecked -Description "frontend lint" -Command {
                    npm run lint --if-present
                }
                Invoke-NativeChecked -Description "frontend tests" -Command {
                    npm run test --if-present -- --run
                }
                Invoke-NativeChecked -Description "frontend build" -Command {
                    npm run build --if-present
                }
            }
            finally {
                Pop-Location
            }
        } `
        -SkipMessage "Skipping frontend checks: React project has not been scaffolded yet."
}
finally {
    Pop-Location
}
