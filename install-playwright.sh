#!/usr/bin/env bash
set -euo pipefail

echo "Installing Playwright Chromium (one-time, idempotent)..."
mvn -q exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
echo "Playwright Chromium ready."
