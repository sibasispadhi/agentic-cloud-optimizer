# Agent Cloud Optimizer (ACO)

> Governance-first, local-first optimization for JVM microservices.
> ACO combines metric-driven diagnosis, bounded recommendations, audit artifacts,
> confidence gates, rollback modeling, and benchmark scenarios in one Java codebase.

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![Ollama](https://img.shields.io/badge/ollama-local%20LLM-purple.svg)](https://ollama.ai/)

---

## What ACO Is

ACO is an open-source Java application for experimenting with **governed optimization** of JVM-backed services.
It watches runtime signals such as latency, heap, GC, CPU, and thread behavior; produces structured
optimization plans; evaluates those plans against policy and actuation budgets; and validates whether
changes helped or hurt.

This is **not** "YOLO let the LLM touch prod." That would be extremely innovative in the worst possible way.

ACO currently supports:
- **Local LLM analysis** through Ollama
- **Deterministic fallback analysis** through `SimpleAgent`
- **OptimizationPlan artifacts** for auditability
- **Policy evaluation** before actuation
- **Actuation budgets** for bounded change
- **Confidence gates and autonomy modes**
- **Validation and rollback modeling**
- **Deterministic benchmark scenarios** for amplification testing

---

## Why This Project Exists

JVM tuning is still too often a guessing game:
- thread pools get bumped because latency is bad
- heap gets inflated "just to be safe"
- GC settings drift without a recorded reason
- optimization decisions are made in war rooms and forgotten a week later

ACO turns that into a more disciplined loop:

1. **Observe** runtime behavior
2. **Reason** about likely bottlenecks
3. **Assemble** a structured optimization plan
4. **Govern** that plan with policy, budgets, and autonomy checks
5. **Validate** the outcome
6. **Preserve** evidence for review and rollback

The point is not raw automation. The point is **bounded, explainable optimization**.

---

## Quick Start

### Option 1: Docker with local LLM
```bash
git clone https://github.com/sibasispadhi/agentic-cloud-optimizer.git
cd agentic-cloud-optimizer
docker compose up --build
```

Open:
- Live dashboard: http://localhost:8081/live-dashboard.html
- Results page: http://localhost:8081/results.html

First run downloads the Ollama model, so yeah, give it a minute.

### Option 2: SimpleAgent only (no LLM download)
```bash
git clone https://github.com/sibasispadhi/agentic-cloud-optimizer.git
cd agentic-cloud-optimizer
docker compose -f docker-compose.simple.yml up --build
```

Open:
- Live dashboard: http://localhost:8081/live-dashboard.html
- Results page: http://localhost:8081/results.html

### Option 3: Local development
```bash
# Terminal 1
ollama serve
ollama pull llama3.2:3b

# Terminal 2
mvn spring-boot:run
```

Then open:
- http://localhost:8081/live-dashboard.html
- http://localhost:8081/results.html

For a more guided local setup, use **[docs/START_HERE.md](docs/START_HERE.md)**.

---

## Core Workflow

When you run an optimization cycle, ACO performs a governance-first pipeline:

1. **Baseline run**
   - execute workload
   - collect latency, throughput, heap, CPU, GC, and thread signals

2. **SLO check**
   - determine whether configured thresholds are breached

3. **Agent reasoning**
   - use `SpringAiLlmAgent` or `SimpleAgent` to generate recommendations

4. **Plan assembly**
   - package changes, evidence, metadata, validation, and rollback into an `OptimizationPlan`

5. **Policy evaluation**
   - evaluate proposed changes through `PolicyEngine`

6. **Budget check**
   - consume from `ActuationBudgetLedger` before permitting change

7. **Autonomy gate**
   - decide advisory vs actuation behavior based on confidence and mode

8. **Validation + rollback modeling**
   - define validation criteria and recovery path

9. **Artifact + report generation**
   - persist outputs for audit and comparison

---

## Implemented Phases

### Phase 0 — Baseline optimizer stabilization
- local LLM-backed reasoning with Ollama
- deterministic fallback agent
- externalized thresholds in configuration
- Docker startup flows

### Phase 1 — OptimizationPlan artifact
Implemented in `src/main/java/com/cloudoptimizer/agent/artifact/`

Key outputs:
- `OptimizationPlan`
- `PlanChange`
- `PlanEvidence`
- `ValidationRecipe`
- `RollbackRecipe`

### Phase 2 — Policy engine
Implemented in `src/main/java/com/cloudoptimizer/agent/policy/`

Key outputs:
- `PolicyEngine`
- `DefaultPolicyEngine`
- `ActuationPolicy`
- `PolicyDecision`

### Phase 3 — Actuation budgets
Implemented in `src/main/java/com/cloudoptimizer/agent/budget/`

Key outputs:
- `ActuationBudget`
- `ActuationBudgetLedger`
- `BudgetConsumption`

### Phase 4 — Confidence gates and progressive autonomy
Implemented in `src/main/java/com/cloudoptimizer/agent/autonomy/`

Key outputs:
- `AutonomyGate`
- `AutonomyMode`
- `AutonomyGateResult`

### Phase 5 — Validation and rollback modeling
Implemented in the artifact and service layers

Key outputs:
- `ValidationExecutor`
- `RollbackExecutor`
- `ValidationResult`
- `RollbackResult`

### Phase 6 — Benchmark scenarios
Implemented in `src/main/java/com/cloudoptimizer/agent/benchmark/`

Included scenarios:
- retry storm
- thread saturation
- CPU throttling
- heap overprovisioning
- burst traffic

---

## Benchmark / Amplification Evidence

ACO includes deterministic benchmark scenarios so governance claims can be tested without needing
production data.

Example outcomes from the benchmark layer:
- **Retry storm**: 3× amplification factor
- **Naive latency-reactive agent**: p99 worsened by **58%**
- **Governed agent**: p99 recovered by **75%**

That is the whole point of the project: useful automation should dampen instability, not cosplay as a chaos monkey.

---

## Generated Outputs

ACO writes artifacts and reports under `artifacts/`.

Typical outputs include:
- `baseline.json`
- `after.json`
- `report.json`
- optimization-plan artifacts
- reasoning traces
- validation and rollback records

You can also inspect example outputs in **[examples/README.md](examples/README.md)**.

---

## Run Commands

### Build
```bash
mvn clean package -DskipTests
```

### CLI mode
```bash
./scripts/run-agent.sh
# Windows: scripts\run-agent.bat
```

### Live web UI
```bash
./scripts/run-web-ui.sh
# Windows: scripts\run-web-ui.bat
```

### Verify Ollama
```bash
./scripts/verify-ollama.sh
```

---

## Tech Stack

- **Java 21**
- **Spring Boot 3.2**
- **Spring AI**
- **Ollama** for local LLM inference
- **Jackson** for artifact serialization
- **JUnit** for test coverage
- **Docker / Docker Compose** for local execution

---

## Public Docs

- **[docs/START_HERE.md](docs/START_HERE.md)** — setup and first run
- **[docs/INDEX.md](docs/INDEX.md)** — current documentation map
- **[docs/WHAT_THIS_IS.md](docs/WHAT_THIS_IS.md)** — scope and boundaries
- **[docs/OLLAMA_SETUP.md](docs/OLLAMA_SETUP.md)** — Ollama install help
- **[docs/WINDOWS_SETUP.md](docs/WINDOWS_SETUP.md)** — Windows notes
- **[docs/ARCHITECTURE_PATTERNS.md](docs/ARCHITECTURE_PATTERNS.md)** — design patterns
- **[docs/STARTUP_READY_PLAN.md](docs/STARTUP_READY_PLAN.md)** — rollout/readiness plan

---

## What Is Not Implemented Yet

These are roadmap items, not shipped capabilities:
- GitOps PR generation
- OpenSLO ingestion
- external telemetry adapters beyond the current local flow
- broader multi-service / multi-region rollout controls

If it is not in code and tested, it does not get to sit in the README pretending it exists.

---

## License

MIT — see [LICENSE](LICENSE).
