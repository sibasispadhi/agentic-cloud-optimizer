#!/bin/bash

# Agent Cloud Optimizer - LLM-Powered Optimization
# Runs AI-powered autonomous optimization (default: LLM mode)

set -e

echo "======================================"
echo "Agent Cloud Optimizer - Agent Run"
echo "======================================"
echo ""

# Configuration
AGENT_STRATEGY=${AGENT_STRATEGY:-llm}  # DEFAULT: LLM-powered (use 'simple' only for testing)
CONCURRENCY=${CONCURRENCY:-4}
DURATION=${DURATION:-10}
TARGET_RPS=${TARGET_RPS:-0}

echo "Configuration:"
echo "  Agent Strategy: $AGENT_STRATEGY (LLM is the primary mode)"
echo "  Concurrency: $CONCURRENCY"
echo "  Duration: ${DURATION}s"
echo "  Target RPS: $TARGET_RPS"
echo ""

# Check if LLM agent is selected (default mode)
if [ "$AGENT_STRATEGY" = "llm" ]; then
    echo "Running in LLM mode (AI-powered optimization)..."
    echo "Checking Ollama service..."
    if ! curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
        echo "ERROR: Ollama service is not running!"
        echo ""
        echo "ACO is designed for LLM-powered optimization."
        echo "Please install and start Ollama:"
        echo "  https://ollama.ai/download"
        echo ""
        echo "Then start the service:"
        echo "  ollama serve"
        echo ""
        echo "And pull a model:"
        echo "  ollama pull llama3.2:1b"
        echo ""
        echo "Or run in fallback mode (NOT RECOMMENDED):"
        echo "  AGENT_STRATEGY=simple ./scripts/run-agent.sh"
        exit 1
    fi
    echo "✓ Ollama service is running"
    echo "✓ Using AI-powered optimization"
    echo ""
elif [ "$AGENT_STRATEGY" = "simple" ]; then
    echo "WARNING: Running in simple mode (rule-based fallback)"
    echo "This is NOT the intended use case for ACO!"
    echo "For full value, use LLM mode. See: docs/OLLAMA_SETUP.md"
    echo ""
fi

# Build if needed
if ! ls target/*.jar >/dev/null 2>&1; then
    echo "Building application..."
    if [ -x "./mvnw" ]; then
        ./mvnw clean package -DskipTests
    else
        mvn clean package -DskipTests
    fi
    echo ""
fi

JAR_PATH=$(ls -1 target/*.jar | head -n 1)

# Run optimization
echo "Running optimization with $AGENT_STRATEGY agent..."
java \
    -Dagent.strategy=$AGENT_STRATEGY \
    -Dbaseline.concurrency=$CONCURRENCY \
    -Dload.duration=$DURATION \
    -Dtarget.rps=$TARGET_RPS \
    -jar "$JAR_PATH"

echo ""
echo "======================================"
echo "Optimization complete!"
echo "======================================"
echo ""
echo "View results:"
echo "  - Open demo.html in your browser"
echo "  - Check artifacts/ directory for JSON files"
if [ "$AGENT_STRATEGY" = "llm" ]; then
    echo "  - Review artifacts/reasoning_trace_llm.txt (AI reasoning)"
else
    echo "  - Review artifacts/reasoning_trace_rule.txt (basic rules)"
fi
echo ""
