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
if not exist "target\*.jar" (
    echo Building application...
    if exist "mvnw.cmd" (
        call mvnw.cmd clean package -DskipTests
    ) else (
        call mvn clean package -DskipTests
    )
    echo.
)

REM Grab first jar from target/
for %%f in (target\*.jar) do (
    set JAR_PATH=%%f
    goto :run
)

echo ERROR: No jar found in target\
exit /b 1

:run
REM Run baseline
echo Running baseline load test...
java ^
    -Dagent.strategy=simple ^
    -Dbaseline.concurrency=%CONCURRENCY% ^
    -Dload.duration=%DURATION% ^
    -Dtarget.rps=%TARGET_RPS% ^
    -jar %JAR_PATH%

echo.
echo ======================================
echo Baseline run complete!
echo Check artifacts\ directory for results
echo ======================================

pause
