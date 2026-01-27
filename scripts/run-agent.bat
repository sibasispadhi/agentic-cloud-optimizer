@echo off
REM Agent Cloud Optimizer - LLM-Powered Optimization (Windows)
REM Runs AI-powered autonomous optimization (default: LLM mode)

echo ======================================
echo Agent Cloud Optimizer - Agent Run
echo ======================================
echo.

REM Configuration
if "%AGENT_STRATEGY%"=="" set AGENT_STRATEGY=llm
if "%CONCURRENCY%"=="" set CONCURRENCY=4
if "%DURATION%"=="" set DURATION=10
if "%TARGET_RPS%"=="" set TARGET_RPS=0

echo Configuration:
echo   Agent Strategy: %AGENT_STRATEGY% (LLM is the primary mode)
echo   Concurrency: %CONCURRENCY%
echo   Duration: %DURATION%s
echo   Target RPS: %TARGET_RPS%
echo.

REM Check if LLM agent is selected (default mode)
if "%AGENT_STRATEGY%"=="llm" (
    echo Running in LLM mode (AI-powered optimization)...
    echo Checking Ollama service...
    curl -s http://localhost:11434/api/tags >nul 2>&1
    if errorlevel 1 (
        echo ERROR: Ollama service is not running!
        echo.
        echo ACO is designed for LLM-powered optimization.
        echo Please install and start Ollama:
        echo   https://ollama.ai/download
        echo.
        echo Then start the service:
        echo   ollama serve
        echo.
        echo And pull a model:
        echo   ollama pull llama3.2:1b
        echo.
        echo Or run in fallback mode (NOT RECOMMENDED):
        echo   set AGENT_STRATEGY=simple
        echo   scripts\run-agent.bat
        pause
        exit /b 1
    )
    echo √ Ollama service is running
    echo √ Using AI-powered optimization
    echo.
)

if "%AGENT_STRATEGY%"=="simple" (
    echo WARNING: Running in simple mode (rule-based fallback)
    echo This is NOT the intended use case for ACO!
    echo For full value, use LLM mode. See: docs\OLLAMA_SETUP.md
    echo.
)

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
REM Run optimization
echo Running optimization with %AGENT_STRATEGY% agent...
java ^
    -Dagent.strategy=%AGENT_STRATEGY% ^
    -Dbaseline.concurrency=%CONCURRENCY% ^
    -Dload.duration=%DURATION% ^
    -Dtarget.rps=%TARGET_RPS% ^
    -jar %JAR_PATH%

echo.
echo ======================================
echo Optimization complete!
echo ======================================
echo.
echo View results:
echo   - Open demo.html in your browser
echo   - Check artifacts\ directory for JSON files
if "%AGENT_STRATEGY%"=="llm" (
    echo   - Review artifacts\reasoning_trace_llm.txt (AI reasoning)
) else (
    echo   - Review artifacts\reasoning_trace_rule.txt (basic rules)
)
echo.

pause