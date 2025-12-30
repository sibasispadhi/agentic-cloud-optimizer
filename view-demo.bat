@echo off
REM Script to view demo.html with proper web server (Windows)

echo 🚀 Starting demo viewer...
echo.

REM Check if in right directory
if not exist "demo.html" (
    echo ❌ Error: demo.html not found!
    echo Please run this script from the project root directory
    exit /b 1
)

REM Check if artifacts exist
if not exist "artifacts\report.json" (
    echo ⚠️  Warning: No results found!
    echo Run the agent first: scripts\run-agent.bat
    echo.
    set /p continue="Continue anyway? (y/n): "
    if /i not "%continue%"=="y" exit /b 1
)

echo ✅ Starting web server on http://localhost:8000
echo ✅ Open browser to: http://localhost:8000/demo.html
echo.
echo Press Ctrl+C to stop the server
echo.

REM Start browser
start http://localhost:8000/demo.html

REM Start server
python -m http.server 8000
