#!/usr/bin/env bash
# Starts the packaged app and opens the dashboard in your browser.
# Add --spring.profiles.active=h2 to run without MySQL.
cd "$(dirname "$0")/.."
OPEN_BROWSER=true java -jar backend/target/os-diagnostic-engine.jar "$@"
