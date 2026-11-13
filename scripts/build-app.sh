#!/usr/bin/env bash
# Builds the React frontend into the Spring Boot app and packages one runnable JAR.
set -e
cd "$(dirname "$0")/.."
(cd frontend && npm install && npm run build:app)
(cd backend && mvn -q clean package)
echo "Done: backend/target/os-diagnostic-engine.jar"
