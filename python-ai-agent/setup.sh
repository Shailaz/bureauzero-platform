#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
export PLAYWRIGHT_BROWSERS_PATH="$PWD/.playwright-browsers"
python3 -m venv .venv
.venv/bin/python -m pip install -r requirements.txt
.venv/bin/python -m playwright install chromium

echo "Python dependencies and Chromium are ready."
echo "Set GEMINI_API_KEY to enable document extraction and legacy date correction."
