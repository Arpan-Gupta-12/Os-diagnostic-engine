@echo off
REM Starts the packaged app and opens the dashboard in your browser.
REM Add --spring.profiles.active=h2 to run without MySQL.
cd /d "%~dp0\.."
set OPEN_BROWSER=true
java -jar backend\target\os-diagnostic-engine.jar %*
