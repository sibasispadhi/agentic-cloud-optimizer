#!/bin/bash

# Agent Cloud Optimizer - Baseline Run Script
# Runs baseline load test without optimization

set -e

echo "======================================"
echo "Agent Cloud Optimizer - Baseline Run"
echo "======================================"
echo ""

# Configuration
CONCURRENCY=${CONCURRENCY:-4}
DURATION=${DURATION:-10}
TARGET_RPS=${TARGET_RPS:-0}

echo "Configuration:"
echo "  Concurrency: $CONCURRENCY"
echo "  Duration: ${DURATION}s"
echo "  Target RPS: $TARGET_RPS"
echo ""

# Build if needed
if [ ! -f "target/agent-cloud-optimizer-1.0-SNAPSHOT.jar" ]; then
    echo "Building application..."
    mvn clean package -DskipTests
    echo ""
fi

# Run baseline
echo "Running baseline load test..."
java -jar target/agent-cloud-optimizer-1.0-SNAPSHOT.jar \
    -Dagent.strategy=simple \
    -Dbaseline.concurrency=$CONCURRENCY \
    -Dload.duration=$DURATION \
    -Dtarget.rps=$TARGET_RPS

echo ""
echo "======================================"
echo "Baseline run complete!"
echo "Check artifacts/ directory for results"
echo "======================================"
