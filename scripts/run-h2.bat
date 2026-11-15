@echo off
REM Starts the app using embedded H2 database (no MySQL required) and opens your browser.
cd /d "%~dp0\.."
set OPEN_BROWSER=true
java -jar backend\target\os-diagnostic-engine.jar --spring.profiles.active=h2 %*
