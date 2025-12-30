@echo off
REM Agent Cloud Optimizer - Agent Run Script (Windows)
REM Runs optimization with selected agent strategy

echo ======================================
echo Agent Cloud Optimizer - Agent Run
echo ======================================
echo.

REM Configuration
if "%AGENT_STRATEGY%"=="" set AGENT_STRATEGY=simple
if "%CONCURRENCY%"=="" set CONCURRENCY=4
if "%DURATION%"=="" set DURATION=10
if "%TARGET_RPS%"=="" set TARGET_RPS=0

echo Configuration:
echo   Agent Strategy: %AGENT_STRATEGY%
echo   Concurrency: %CONCURRENCY%
echo   Duration: %DURATION%s
echo   Target RPS: %TARGET_RPS%
echo.

REM Check if LLM agent is selected
if "%AGENT_STRATEGY%"=="llm" (
    echo Checking Ollama service...
    curl -s http://localhost:11434/api/tags >nul 2>&1
    if errorlevel 1 (
        echo ERROR: Ollama service is not running!
        echo Please start Ollama:
        echo   ollama serve
        echo.
        echo And pull a model:
        echo   ollama pull llama2
        pause
        exit /b 1
    )
    echo √ Ollama service is running
    echo.
)

REM Build if needed
if not exist "target\agent-cloud-optimizer-1.0-SNAPSHOT.jar" (
    echo Building application...
    call mvn clean package -DskipTests
    echo.
)

REM Run optimization
echo Running optimization with %AGENT_STRATEGY% agent...
java -jar target\agent-cloud-optimizer-1.0-SNAPSHOT.jar ^
    -Dagent.strategy=%AGENT_STRATEGY% ^
    -Dbaseline.concurrency=%CONCURRENCY% ^
    -Dload.duration=%DURATION% ^
    -Dtarget.rps=%TARGET_RPS%

echo.
echo ======================================
echo Optimization complete!
echo ======================================
echo.
echo View results:
echo   - Open demo.html in your browser
echo   - Check artifacts\ directory for JSON files
echo   - Review artifacts\reasoning_trace_%AGENT_STRATEGY%.txt
echo.

pause
