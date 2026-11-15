@echo off
REM Builds the React frontend into the Spring Boot app and packages one runnable JAR.
cd /d "%~dp0\.."

where mvn >nul 2>nul
if errorlevel 1 (
  if exist "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6-bin\439sdfsg2nbdob9ciift5h5nse\apache-maven-3.9.6\bin" (
    set "PATH=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6-bin\439sdfsg2nbdob9ciift5h5nse\apache-maven-3.9.6\bin;%PATH%"
  ) else if exist "%ProgramFiles%\JetBrains\IntelliJ IDEA 2025.1.3\plugins\maven\lib\maven3\bin" (
    set "PATH=%ProgramFiles%\JetBrains\IntelliJ IDEA 2025.1.3\plugins\maven\lib\maven3\bin;%PATH%"
  )
)

pushd frontend
call npm install || exit /b 1
call npm run build:app || exit /b 1
popd
pushd backend
call mvn -q clean package || exit /b 1
popd
echo Done: backend\target\os-diagnostic-engine.jar
