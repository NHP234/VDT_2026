Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$exports = Join-Path $root "exports"
if (-not (Test-Path -LiteralPath $exports)) {
    New-Item -ItemType Directory -Path $exports | Out-Null
}

Push-Location $root
try {
    Write-Host ">>> Compiling Beamer Slides (Pass 1)..." -ForegroundColor Yellow
    xelatex -interaction=nonstopmode -halt-on-error -output-directory $exports slides.tex
    
    Write-Host ">>> Compiling Beamer Slides (Pass 2 for page numbers and links)..." -ForegroundColor Yellow
    xelatex -interaction=nonstopmode -halt-on-error -output-directory $exports slides.tex
    
    Write-Host ">>> Compilation successful! Output PDF located at: exports/slides.pdf" -ForegroundColor Green
}
catch {
    Write-Host ">>> Compilation failed! Check logs." -ForegroundColor Red
    throw
}
finally {
    Pop-Location
}
