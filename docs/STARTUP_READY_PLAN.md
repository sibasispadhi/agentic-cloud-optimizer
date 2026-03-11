# ACO Startup-Ready Plan: From Demo to Production

**Created:** 2026-03-11  
**Goal:** Make ACO immediately usable by startups with zero friction  
**Timeline:** 3 weeks to v1.0-startup  
**Status:** ACTIVE

---

## 🎯 Executive Summary

ACO is currently a **working JVM performance optimizer** with:
- ✅ SLO breach detection (p99 tracking)
- ✅ LLM-powered analysis (Ollama)
- ✅ Rule-based fallback (SimpleAgent)
- ✅ Real-time dashboard (WebSocket)
- ✅ Explainable reasoning traces
- ✅ Before/after metrics comparison

**What's Missing for Startups:**
- ❌ One-command install (Docker)
- ❌ Kubernetes deployment (Helm)
- ❌ Community infrastructure (Discord, CONTRIBUTING.md)
- ❌ Example integrations (Spring Boot starter app)

**This plan delivers all of the above in 4 weeks.**

---

## 📊 Current State Assessment

### What Works Today

| Feature | Status | Evidence |
|---------|--------|----------|
| JVM metrics collection | ✅ Working | GcMetricsCollector.java |
| p95/p99 latency tracking | ✅ Working | LoadRunner.java |
| SLO breach detection | ✅ Working | SloBreachDetector.java |
| LLM analysis (Ollama) | ✅ Working | SpringAiLlmAgent.java |
| Rule-based agent | ✅ Working | SimpleAgent.java |
| Safety bounds (±25%) | ✅ Working | Bounded Action Space |
| Real-time dashboard | ✅ Working | live-dashboard.html |
| Results comparison | ✅ Working | results.html |
| Reasoning traces | ✅ Working | artifacts/*.txt |

### What Startups Need

| Need | Priority | Current State |
|------|----------|---------------|
| 5-minute install | 🔴 CRITICAL | ❌ Requires Java + Maven + Ollama |
| Works without Ollama | 🔴 CRITICAL | ❌ LLM agent fails without Ollama |
| Kubernetes deployment | 🟡 HIGH | ❌ Manual deployment only |
| Example Spring Boot app | 🟡 HIGH | ❌ Uses built-in simulator |
| Community support | 🟢 MEDIUM | ❌ No Discord/Slack |
| Production hardening | 🟢 MEDIUM | ⚠️ Demo-quality |

---

## 🚀 3-Week Startup-Ready Plan

### Week 1: Zero-Friction Install (Days 1-7)

**Goal:** Anyone can run ACO in 5 minutes with one command.

#### Day 1-2: Docker Foundation

**Deliverables:**
- [ ] `Dockerfile` - Multi-stage Java build
- [ ] `docker-compose.yml` - ACO + Ollama + sample app
- [ ] `docker-compose.simple.yml` - ACO only (SimpleAgent mode)

**docker-compose.yml:**
```yaml
version: '3.8'
services:
  aco:
    build: .
    ports:
      - "8081:8081"
    environment:
      - SPRING_AI_OLLAMA_BASE_URL=http://ollama:11434
      - ACO_AGENT_MODE=llm  # or 'simple' for no LLM
    depends_on:
      - ollama
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/v1/work/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    # Auto-pull llama3.2 on startup
    entrypoint: ["/bin/sh", "-c", "ollama serve & sleep 5 && ollama pull llama3.2 && wait"]

volumes:
  ollama_data:
```

**Dockerfile:**
```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Day 3-4: SimpleAgent-Only Mode

**Problem:** Startups don't want to run Ollama (resource heavy).

**Solution:** Make SimpleAgent work standalone without any LLM.

**Deliverables:**
- [ ] Add `ACO_AGENT_MODE` environment variable
- [ ] `simple` mode = SimpleAgent only (fast, deterministic)
- [ ] `llm` mode = LLM with SimpleAgent fallback
- [ ] `docker-compose.simple.yml` for no-Ollama deployment

```yaml
# docker-compose.simple.yml - No LLM, just rule-based agent
version: '3.8'
services:
  aco:
    build: .
    ports:
      - "8081:8081"
    environment:
      - ACO_AGENT_MODE=simple  # No LLM required!
```

**Startup command:**
```bash
# Full mode (with LLM)
docker compose up

# Simple mode (no LLM, fast)
docker compose -f docker-compose.simple.yml up
```

#### Day 5-6: Quick Start Guide

**Deliverables:**
- [ ] Update `README.md` with Docker quick start
- [ ] Add GIF/screenshot of dashboard
- [ ] `QUICKSTART.md` - 5-minute tutorial

**README.md Quick Start:**
```markdown
## 🚀 Quick Start (5 minutes)

### Option A: With LLM (recommended)
```bash
git clone https://github.com/sibasispadhi/agentic-cloud-optimizer
cd agentic-cloud-optimizer
docker compose up
# Open http://localhost:8081/live-dashboard.html
```

### Option B: Without LLM (faster, simpler)
```bash
docker compose -f docker-compose.simple.yml up
```

That's it! ACO is now analyzing your JVM performance.
```

#### Day 7: Test & Polish

**Deliverables:**
- [ ] Test Docker build on clean machine
- [ ] Test both compose files
- [ ] Add healthchecks
- [ ] Document troubleshooting

---

### Week 2: Kubernetes Deployment (Days 8-14)

**Goal:** Deploy ACO to any Kubernetes cluster with Helm.

#### Day 15-17: Helm Chart

**Deliverables:**
- [ ] `charts/aco/Chart.yaml`
- [ ] `charts/aco/values.yaml`
- [ ] `charts/aco/templates/deployment.yaml`
- [ ] `charts/aco/templates/service.yaml`
- [ ] `charts/aco/templates/configmap.yaml`

**values.yaml:**
```yaml
replicaCount: 1

image:
  repository: ghcr.io/sibasispadhi/agentic-cloud-optimizer
  tag: latest
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 8081

config:
  agentMode: simple  # simple or llm
  llmProvider: ollama  # ollama, openai, anthropic
  
secrets:
  openaiApiKey: ""  # Set via --set or external secret

resources:
  limits:
    cpu: 1000m
    memory: 1Gi
  requests:
    cpu: 500m
    memory: 512Mi

# Optional: Deploy Ollama sidecar
ollama:
  enabled: false
  image: ollama/ollama:latest
```

**Install command:**
```bash
# Simple mode (no LLM)
helm install aco ./charts/aco

# With OpenAI
helm install aco ./charts/aco \
  --set config.agentMode=llm \
  --set config.llmProvider=openai \
  --set secrets.openaiApiKey=$OPENAI_API_KEY
```

#### Day 18-19: GitHub Container Registry

**Deliverables:**
- [ ] Update `.github/workflows/release.yml` to push to GHCR
- [ ] Auto-build and push on tag
- [ ] Multi-arch support (amd64 + arm64)

```yaml
# .github/workflows/release.yml
- name: Build and push Docker image
  uses: docker/build-push-action@v5
  with:
    push: true
    tags: |
      ghcr.io/sibasispadhi/agentic-cloud-optimizer:latest
      ghcr.io/sibasispadhi/agentic-cloud-optimizer:${{ github.ref_name }}
    platforms: linux/amd64,linux/arm64
```

#### Day 20-21: Kubernetes Documentation

**Deliverables:**
- [ ] `docs/KUBERNETES.md` - Full K8s deployment guide
- [ ] Example for GKE, EKS, AKS
- [ ] Monitoring integration (Prometheus metrics endpoint)

---

### Week 3: Community & Polish (Days 15-21)

**Goal:** Make ACO welcoming for contributors and production-ready.

#### Day 15-16: Community Files

**Deliverables:**
- [ ] `CONTRIBUTING.md` - How to contribute
- [ ] `CODE_OF_CONDUCT.md` - Contributor Covenant
- [ ] `SECURITY.md` - Security policy
- [ ] `.github/FUNDING.yml` - Sponsorship

**CONTRIBUTING.md outline:**
```markdown
# Contributing to ACO

## Quick Start for Contributors
1. Fork the repo
2. `docker compose up` (starts dev environment)
3. Make changes
4. `mvn test` (run tests)
5. Open PR

## Architecture Overview
- `SimpleAgent` - Rule-based, fast, deterministic
- `SpringAiLlmAgent` - LLM-powered analysis
- `SloBreachDetector` - Consecutive window detection
- `OptimizationOrchestrator` - Closed-loop controller

## Good First Issues
Look for issues labeled `good-first-issue`
```

#### Day 17-18: Example Application

**Deliverables:**
- [ ] `examples/spring-boot-app/` - Sample app to optimize
- [ ] `examples/spring-boot-app/docker-compose.yml` - ACO + sample app
- [ ] Tutorial: "Optimize your first Spring Boot app with ACO"

**Example app:**
```java
@RestController
public class DemoController {
    
    @GetMapping("/api/process")
    public String process() {
        // Simulate work that can be tuned
        simulateMemoryPressure();
        return "OK";
    }
    
    private void simulateMemoryPressure() {
        // Allocate some memory to trigger GC
        byte[] data = new byte[1024 * 1024]; // 1MB
        // ... process ...
    }
}
```

#### Day 19-20: Production Hardening

**Deliverables:**
- [ ] Add Prometheus metrics endpoint (`/actuator/prometheus`)
- [ ] Add structured logging (JSON format option)
- [ ] Add graceful shutdown
- [ ] Add rate limiting for optimization actions
- [ ] Add circuit breaker for LLM calls

**Metrics to expose:**
```
aco_slo_breaches_total
aco_optimizations_total
aco_optimizations_success_total
aco_optimizations_rollback_total
aco_agent_decision_duration_seconds
aco_p99_latency_ms
```

#### Day 21: Final Release

**Deliverables:**
- [ ] Tag `v1.0.0-startup`
- [ ] Update README with badges
- [ ] Create GitHub Release with changelog
- [ ] Announce on Twitter/LinkedIn

**README badges:**
```markdown
[![Docker](https://img.shields.io/docker/v/sibasispadhi/aco?label=Docker)](https://ghcr.io/sibasispadhi/agentic-cloud-optimizer)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/sibasispadhi/agentic-cloud-optimizer)](https://github.com/sibasispadhi/agentic-cloud-optimizer/stargazers)
```

---

## 📋 Complete Deliverables Checklist

### Week 1: Zero-Friction Install
- [x] `Dockerfile`
- [x] `docker-compose.yml`
- [x] `docker-compose.simple.yml`
- [x] `AGENT_STRATEGY` environment variable
- [x] Updated README.md
- [ ] `docs/QUICKSTART.md`

### Week 2: Kubernetes Deployment
- [ ] `charts/aco/` Helm chart
- [ ] GitHub Container Registry publishing
- [ ] Multi-arch Docker images
- [ ] `docs/KUBERNETES.md`

### Week 3: Community & Polish
- [ ] `CONTRIBUTING.md`
- [ ] `CODE_OF_CONDUCT.md`
- [ ] `SECURITY.md`
- [ ] `examples/spring-boot-app/`
- [ ] Prometheus metrics
- [ ] v1.0.0-startup release

---

## 🎯 Success Metrics

### By End of Week 4:

| Metric | Target |
|--------|--------|
| Time to first run | < 5 minutes |
| Docker pull + run | One command |
| Helm install | One command |
| LLM | Ollama (100% local) |
| Documentation pages | 10+ |
| Example apps | 1 |
| GitHub stars | 100+ (stretch goal) |

### Startup User Journey:

```
1. Find ACO on GitHub (README sells it)
2. git clone + docker compose up (5 min)
3. See dashboard working (wow moment)
4. Point at their own app (10 min)
5. See optimization recommendation (value delivered)
6. Star the repo, share on Twitter
```

---

## 🚧 Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Docker build complexity | Multi-stage build, minimize layers |
| Ollama slow to start | Offer SimpleAgent-only mode |
| Helm chart complexity | Start simple, add features later |
| Breaking changes | Semantic versioning, changelog |

---

## 📅 Daily Schedule

| Day | Task | Deliverable |
|-----|------|-------------|
| 1 | Dockerfile | Working Docker build |
| 2 | docker-compose.yml | Full stack compose |
| 3 | SimpleAgent mode | AGENT_STRATEGY |
| 4 | Simple compose | docker-compose.simple.yml |
| 5 | README update | Quick start section |
| 6 | QUICKSTART.md | Tutorial doc |
| 7 | Testing | Verified on clean machine |
| 8 | Helm Chart.yaml | Chart structure |
| 9 | Helm values | values.yaml |
| 10 | Helm templates | deployment, service |
| 11 | GHCR workflow | Auto-publish images |
| 12 | Multi-arch | amd64 + arm64 |
| 13 | K8s docs | KUBERNETES.md |
| 14 | K8s testing | Verified on minikube |
| 15 | CONTRIBUTING.md | Contributor guide |
| 16 | CODE_OF_CONDUCT | Community standards |
| 17 | Example app | spring-boot-app |
| 18 | Example compose | App + ACO together |
| 19 | Prometheus | Metrics endpoint |
| 20 | Hardening | Logging, shutdown |
| 21 | Release | v1.0.0-startup |

---

## 🔗 Related Documents

- [GOVERNANCE_FIRST_VISION.md](./GOVERNANCE_FIRST_VISION.md) - 6-milestone long-term plan
- [PRACTICAL_ROADMAP.md](./PRACTICAL_ROADMAP.md) - Implementation patterns
- [M1_PLAN.md](./MILESTONES/M1_PLAN.md) - OptimizationPlan artifact (future)
- [ARCHITECTURE_PATTERNS.md](./ARCHITECTURE_PATTERNS.md) - Technical patterns

---

## 🐶 Quick Commands Reference

```bash
# Development
mvn clean package -DskipTests
./scripts/run-web-ui.sh

# Docker (full with Ollama)
docker compose up --build

# Docker (simple, no LLM)
docker compose -f docker-compose.simple.yml up --build

# Kubernetes
helm install aco ./charts/aco
```

---

**Let's make ACO the easiest way for startups to optimize their JVM performance!** 🚀
