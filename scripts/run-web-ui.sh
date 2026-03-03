#!/bin/bash

# Agent Cloud Optimizer - Web UI Mode
# Launches the live optimization dashboard

set -e

echo "="
echo "=" Agent Cloud Optimizer - Live Web Dashboard
echo "="
echo ""
echo "Starting server on http://localhost:8081"
echo "Open your browser to: http://localhost:8081/live-dashboard.html"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

# Build the project first
if ! ls target/*.jar >/dev/null 2>&1; then
    echo "Building project..."
    if [ -x "./mvnw" ]; then
        ./mvnw clean package -DskipTests
    else
        mvn clean package -DskipTests
    fi
    echo ""
fi

JAR_PATH=$(ls -1 target/*.jar | head -n 1)

# Run in web mode
java -jar "$JAR_PATH" \
    --run.mode=web \
    --logging.level.com.cloudoptimizer=INFO
