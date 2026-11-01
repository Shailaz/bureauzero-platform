$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$Python = Get-Command python -ErrorAction SilentlyContinue
if (-not $Python) {
    $Python = Get-Command py -ErrorAction SilentlyContinue
}
if (-not $Python) {
    throw "Python 3.10 or newer is required. Install Python and reopen PowerShell."
}

& $Python.Source -m venv .venv
$VenvPython = Join-Path $PSScriptRoot ".venv\Scripts\python.exe"
$env:PLAYWRIGHT_BROWSERS_PATH = Join-Path $PSScriptRoot ".playwright-browsers"
& $VenvPython -m pip install -r requirements.txt
if ($LASTEXITCODE -ne 0) { throw "Python dependency installation failed." }
& $VenvPython -m playwright install chromium
if ($LASTEXITCODE -ne 0) { throw "Playwright Chromium installation failed." }

Write-Host "Python dependencies and Chromium are ready. Configure GEMINI_API_KEY in the repository .env file if needed."
