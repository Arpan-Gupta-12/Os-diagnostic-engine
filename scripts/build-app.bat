@echo off
REM Builds the React frontend into the Spring Boot app and packages one runnable JAR.
cd /d "%~dp0\.."
pushd frontend
call npm install || exit /b 1
call npm run build:app || exit /b 1
popd
pushd backend
call mvn -q clean package || exit /b 1
popd
echo Done: backend\target\os-diagnostic-engine.jar
