#!/bin/bash

# Script to view demo.html with proper web server

echo "🚀 Starting demo viewer..."
echo ""

# Check if in right directory
if [ ! -f "demo.html" ]; then
    echo "❌ Error: demo.html not found!"
    echo "Please run this script from the project root directory:"
    echo "  cd /Users/s0p0bes/Documents/my-code/agent-cloud-optimizer"
    echo "  ./view-demo.sh"
    exit 1
fi

# Check if artifacts exist
if [ ! -f "artifacts/report.json" ]; then
    echo "⚠️  Warning: No results found!"
    echo "Run the agent first: ./scripts/run-agent.sh"
    echo ""
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Find available port
PORT=8000
while lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1 ; do
    echo "⚠️  Port $PORT is busy, trying $((PORT+1))..."
    PORT=$((PORT+1))
done

echo "✅ Starting web server on http://localhost:$PORT"
echo "✅ Opening demo.html in browser..."
echo ""
echo "📍 URL: http://localhost:$PORT/demo.html"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

# Open browser (works on Mac, will fail gracefully on Linux/Windows)
sleep 1 && open http://localhost:$PORT/demo.html 2>/dev/null &

# Start server
python3 -m http.server $PORT
