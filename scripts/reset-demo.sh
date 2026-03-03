#!/bin/bash
# Reset demo state for a clean DevNexus presentation run
set -e

echo "=================================="
echo "  ACO Demo Reset"
echo "=================================="
echo ""

# 1. Clean artifacts
if [ -d "artifacts" ]; then
    echo "✂️  Cleaning artifacts/..."
    rm -rf artifacts/*.json artifacts/*.txt
else
    mkdir -p artifacts
fi

# 2. Clean metrics data
if [ -d "data/metrics" ]; then
    echo "✂️  Cleaning data/metrics/..."
    rm -rf data/metrics/*.csv data/metrics/*.json
fi

# 3. Rebuild jar
echo "🔨 Building project..."
mvn clean package -DskipTests -q
echo "✅ Build complete"

# 4. Verify Ollama
echo ""
echo "🦙 Checking Ollama..."
if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "✅ Ollama is running"
    MODEL=$(curl -s http://localhost:11434/api/tags | python3 -c "import sys,json; tags=json.load(sys.stdin); print(tags.get('models',[{}])[0].get('name','unknown'))" 2>/dev/null || echo "unknown")
    echo "   Model: $MODEL"
else
    echo "⚠️  Ollama is NOT running. Start it with: ollama serve"
    echo "   Fallback available: AGENT_STRATEGY=simple"
fi

echo ""
echo "=================================="
echo "  ✅ Demo state reset!"
echo "=================================="
echo ""
echo "Next steps:"
echo "  1. Start the server:  ./scripts/run-web-ui.sh &"
echo "     (wait ~10s for Spring Boot to start)"
echo "  2. Open dashboard:    open http://localhost:8081/live-dashboard.html"
echo "     Verify: WebSocket dot shows green 'Connected'"
echo "  3. Open slides:       open presentation/devnexus-2026-slides-OFFLINE.html"
echo "  4. Run demo:          follow docs/DEMO_SCRIPT.md"
echo "  5. Fallback artifacts: examples/devnexus/"
echo ""
