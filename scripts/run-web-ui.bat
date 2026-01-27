@echo off
REM Agent Cloud Optimizer - Web UI Mode
REM Launches the live optimization dashboard

echo ================================================================
echo    Agent Cloud Optimizer - Live Web Dashboard
echo ================================================================
echo.
echo Starting server on http://localhost:8080
echo Open your browser to: http://localhost:8080/live-dashboard.html
echo.
echo Press Ctrl+C to stop the server
echo.

REM Build the project first
if not exist "target\*.jar" (
    echo Building project...
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
REM Run in web mode
java -jar %JAR_PATH% ^
    --run.mode=web ^
    --logging.level.com.cloudoptimizer=INFO

pause
