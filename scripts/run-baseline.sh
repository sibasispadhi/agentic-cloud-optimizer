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

# Run baseline
echo "Running baseline load test..."
java \
    -Dagent.strategy=simple \
    -Dbaseline.concurrency=$CONCURRENCY \
    -Dload.duration=$DURATION \
    -Dtarget.rps=$TARGET_RPS \
    -jar "$JAR_PATH"

echo ""
echo "======================================"
echo "Baseline run complete!"
echo "Check artifacts/ directory for results"
echo "======================================"
