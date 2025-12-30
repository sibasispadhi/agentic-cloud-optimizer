@echo off
REM Agent Cloud Optimizer - Baseline Run Script (Windows)
REM Runs baseline load test without optimization

echo ======================================
echo Agent Cloud Optimizer - Baseline Run
echo ======================================
echo.

REM Configuration
if "%CONCURRENCY%"=="" set CONCURRENCY=4
if "%DURATION%"=="" set DURATION=10
if "%TARGET_RPS%"=="" set TARGET_RPS=0

echo Configuration:
echo   Concurrency: %CONCURRENCY%
echo   Duration: %DURATION%s
echo   Target RPS: %TARGET_RPS%
echo.

REM Build if needed
if not exist "target\agent-cloud-optimizer-1.0-SNAPSHOT.jar" (
    echo Building application...
    call mvn clean package -DskipTests
    echo.
)

REM Run baseline
echo Running baseline load test...
java -jar target\agent-cloud-optimizer-1.0-SNAPSHOT.jar ^
    -Dagent.strategy=simple ^
    -Dbaseline.concurrency=%CONCURRENCY% ^
    -Dload.duration=%DURATION% ^
    -Dtarget.rps=%TARGET_RPS%

echo.
echo ======================================
echo Baseline run complete!
echo Check artifacts\ directory for results
echo ======================================

pause
