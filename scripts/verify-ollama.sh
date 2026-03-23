#!/bin/bash

# Ollama Verification Script for Agent Cloud Optimizer
# Checks if Ollama is properly configured for local ACO usage

echo "🔍 Verifying Ollama Setup for Agent Cloud Optimizer"
echo "=================================================="
echo ""

# Check 1: Ollama installed
echo "✓ Checking Ollama installation..."
if command -v ollama &> /dev/null; then
    VERSION=$(ollama --version)
    echo "  ✅ Ollama is installed: $VERSION"
else
    echo "  ❌ Ollama is NOT installed"
    echo "     Install: brew install ollama (macOS) or curl -fsSL https://ollama.com/install.sh | sh (Linux)"
    exit 1
fi
echo ""

# Check 2: Ollama service running
echo "✓ Checking Ollama service..."
if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "  ✅ Ollama service is running on http://localhost:11434"
else
    echo "  ❌ Ollama service is NOT running"
    echo "     Start: ollama serve"
    exit 1
fi
echo ""

# Check 3: Models available
echo "✓ Checking available models..."
MODELS=$(ollama list 2>/dev/null)
if echo "$MODELS" | grep -q "llama3.2:1b"; then
    echo "  ✅ llama3.2:1b is available (Recommended for demos)"
elif echo "$MODELS" | grep -q "llama3.2:3b"; then
    echo "  ✅ llama3.2:3b is available (Good for production)"
elif echo "$MODELS" | grep -q "llama"; then
    echo "  ⚠️  A llama model is available but not llama3.2"
    echo "     Recommended: ollama pull llama3.2:1b"
else
    echo "  ❌ No llama models found"
    echo "     Install: ollama pull llama3.2:1b"
    exit 1
fi
echo ""

# Check 4: Test model response
echo "✓ Testing model response..."
RESPONSE=$(curl -s http://localhost:11434/api/generate -d '{"model":"llama3.2:1b","prompt":"Say OK","stream":false}' 2>/dev/null)
if echo "$RESPONSE" | grep -q "response"; then
    echo "  ✅ Model responds correctly"
else
    echo "  ❌ Model test failed"
    echo "     Try: ollama run llama3.2:1b 'Hello'"
    exit 1
fi
echo ""

# Check 5: Configuration
echo "✓ Checking application configuration..."
if [ -f "src/main/resources/application-dev.yml" ]; then
    MODEL_CONFIG=$(grep "model:" src/main/resources/application-dev.yml | head -1)
    echo "  ✅ Configuration found: $MODEL_CONFIG"
else
    echo "  ⚠️  Configuration file not found (using defaults)"
fi
echo ""

# Summary
echo "=================================================="
echo "✅ All checks passed! Ready for local ACO usage."
echo ""
echo "📊 Your Setup:"
ollama list
echo ""
echo "🚀 To start the application:"
echo "   java -jar target/agent-cloud-optimizer-0.2.0.jar --run.mode=web"
echo ""
echo "🌐 Dashboard: http://localhost:8081/live-dashboard.html"
echo ""
