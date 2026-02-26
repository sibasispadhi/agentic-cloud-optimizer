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
echo "  1. Open dashboard:  open http://localhost:8080/live-dashboard.html"
echo "  2. Open slides:     open presentation/devnexus-2026-slides.html"
echo "  3. Run demo:        follow docs/DEMO_SCRIPT.md"
echo ""
