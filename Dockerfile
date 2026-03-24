# =============================================================================
# Agent Cloud Optimizer - Multi-stage Docker Build
# =============================================================================
# Build: docker build -t aco .
# Run:   docker run -p 8081:8081 aco
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# -----------------------------------------------------------------------------
# Stage 2: Runtime
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 aco && adduser -u 1001 -G aco -D aco

# Copy built JAR
COPY --from=build /app/target/*.jar app.jar

# Create artifacts directory
RUN mkdir -p /app/artifacts && chown -R aco:aco /app

# Switch to non-root user
USER aco

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/v1/work/health || exit 1

# Environment defaults
ENV SERVER_PORT=8081 \
    AGENT_STRATEGY=llm \
    ACO_LLM_PROVIDER=ollama \
    OLLAMA_BASE_URL=http://ollama:11434 \
    JAVA_OPTS="-Xms256m -Xmx512m"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
