#!/bin/bash

# Agent Cloud Optimizer - Agent Run Script
# Runs optimization with selected agent strategy

set -e

echo "======================================"
echo "Agent Cloud Optimizer - Agent Run"
echo "======================================"
echo ""

# Configuration
AGENT_STRATEGY=${AGENT_STRATEGY:-simple}
CONCURRENCY=${CONCURRENCY:-4}
DURATION=${DURATION:-10}
TARGET_RPS=${TARGET_RPS:-0}

echo "Configuration:"
echo "  Agent Strategy: $AGENT_STRATEGY"
echo "  Concurrency: $CONCURRENCY"
echo "  Duration: ${DURATION}s"
echo "  Target RPS: $TARGET_RPS"
echo ""

# Check if LLM agent is selected
if [ "$AGENT_STRATEGY" = "llm" ]; then
    echo "Checking Ollama service..."
    if ! curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
        echo "ERROR: Ollama service is not running!"
        echo "Please start Ollama:"
        echo "  ollama serve"
        echo ""
        echo "And pull a model:"
        echo "  ollama pull llama2"
        exit 1
    fi
    echo "✓ Ollama service is running"
    echo ""
fi

# Build if needed
if [ ! -f "target/agent-cloud-optimizer-1.0-SNAPSHOT.jar" ]; then
    echo "Building application..."
    mvn clean package -DskipTests
    echo ""
fi

# Run optimization
echo "Running optimization with $AGENT_STRATEGY agent..."
java -jar target/agent-cloud-optimizer-1.0-SNAPSHOT.jar \
    -Dagent.strategy=$AGENT_STRATEGY \
    -Dbaseline.concurrency=$CONCURRENCY \
    -Dload.duration=$DURATION \
    -Dtarget.rps=$TARGET_RPS

echo ""
echo "======================================"
echo "Optimization complete!"
echo "======================================"
echo ""
echo "View results:"
echo "  - Open demo.html in your browser"
echo "  - Check artifacts/ directory for JSON files"
echo "  - Review artifacts/reasoning_trace_${AGENT_STRATEGY}.txt"
echo ""
