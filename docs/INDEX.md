# Documentation Index

**Last Updated:** 2026-03-23  
**Status:** Phases 0-6 complete  

Agent Cloud Optimizer (ACO) is now beyond the planning stage. This index points to the
**docs that actually exist**, the **capabilities that are actually implemented**, and the
**next areas of work** that are still future-facing.

---

## Start Here

If you are new to the project, read in this order:

1. **[README.md](../README.md)**
   - Project overview
   - Quick start
   - Core capabilities
   - Local run options

2. **[START_HERE.md](START_HERE.md)**
   - Local setup steps
   - Build and run commands
   - Where outputs go

3. **[WHAT_THIS_IS.md](WHAT_THIS_IS.md)**
   - What ACO does
   - What ACO does not do
   - How it fits into an engineering workflow

4. **[ARCHITECTURE_PATTERNS.md](ARCHITECTURE_PATTERNS.md)**
   - Design patterns used in the implementation
   - Separation of concerns across orchestrator, policy, autonomy, and artifacts

5. **[STARTUP_READY_PLAN.md](STARTUP_READY_PLAN.md)**
   - Packaging and rollout considerations
   - Deployment-readiness plan for broader adoption

---

## What Is Implemented

ACO currently ships a governance-first optimization pipeline for JVM tuning.

### Phase 0 — Baseline optimizer stabilization
- Local LLM-backed optimization via Ollama
- Rule-based fallback agent
- Externalized configuration thresholds
- Docker-based local startup flows

### Phase 1 — OptimizationPlan artifact
Implemented under `src/main/java/com/cloudoptimizer/agent/artifact/`.

Key classes:
- `OptimizationPlan`
- `PlanChange`
- `PlanEvidence`
- `PlanMetadata`
- `PlanWriter`
- `ValidationRecipe`
- `RollbackRecipe`

What it gives you:
- A structured, serializable optimization artifact
- Evidence, intent, validation, and rollback captured in one model
- A single source of truth for each optimization run

### Phase 2 — Policy engine
Implemented under `src/main/java/com/cloudoptimizer/agent/policy/`.

Key classes:
- `PolicyEngine`
- `DefaultPolicyEngine`
- `ActuationPolicy`
- `PolicyContext`
- `PolicyDecision`

What it gives you:
- Explicit permission checks before changes are proposed or applied
- Governance separated from diagnosis
- Auditable warnings and violations

### Phase 3 — Actuation budgets
Implemented under `src/main/java/com/cloudoptimizer/agent/budget/`.

Key classes:
- `ActuationBudget`
- `ActuationBudgetLedger`
- `BudgetConsumption`
- `ProposedChangeDelta`

What it gives you:
- Bounded operational change, not just bounded traffic
- Token-style accounting for change frequency and magnitude
- Protection against repeated high-impact automated perturbations

### Phase 4 — Confidence gates and progressive autonomy
Implemented under `src/main/java/com/cloudoptimizer/agent/autonomy/`.

Key classes:
- `AutonomyGate`
- `AutonomyGateResult`
- `AutonomyMode`
- `AutonomyConfig`

What it gives you:
- Advisory-first behavior
- Explicit autonomy modes
- Confidence-based gating before execution

### Phase 5 — Validation and rollback modeling
Implemented in service and artifact layers.

Key classes:
- `ValidationExecutor`
- `RollbackExecutor`
- `ValidationResult`
- `RollbackResult`

What it gives you:
- Validation steps attached to recommendations
- Rollback-aware execution planning
- Safer experimentation and recovery paths

### Phase 6 — Benchmark scenarios
Implemented under `src/main/java/com/cloudoptimizer/agent/benchmark/`.

Key classes:
- `ScenarioRunner`
- `ScenarioDecisionEngine`
- `RetryStormScenario`
- `ThreadSaturationScenario`
- `CpuThrottlingScenario`
- `HeapOverprovisionedScenario`
- `BurstTrafficScenario`
- `AmplificationMetrics`

What it gives you:
- Deterministic incident-style replay scenarios
- Quantified amplification analysis
- A way to compare naive vs governed optimization behavior

---

## Source Layout

Main application code lives under:

```text
src/main/java/com/cloudoptimizer/agent/
├── artifact/     # OptimizationPlan and related run artifacts
├── autonomy/     # confidence gates and autonomy modes
├── benchmark/    # benchmark scenarios and amplification metrics
├── budget/       # actuation budget models and ledger
├── config/       # Spring configuration
├── controller/   # HTTP and WebSocket controllers
├── model/        # domain models and metrics rows
├── policy/       # governance and permission checks
├── service/      # orchestration, agents, reporting, validation, rollback
└── simulator/    # demo and HTTP workload simulators
```

Tests live under:

```text
src/test/java/com/cloudoptimizer/agent/
```

---

## Existing Public Docs

- **[README.md](../README.md)** — overview, quick start, feature summary
- **[START_HERE.md](START_HERE.md)** — setup and run instructions
- **[OLLAMA_SETUP.md](OLLAMA_SETUP.md)** — local Ollama installation and model setup
- **[WHAT_THIS_IS.md](WHAT_THIS_IS.md)** — product boundaries and workflow fit
- **[WINDOWS_SETUP.md](WINDOWS_SETUP.md)** — Windows-specific setup help
- **[ARCHITECTURE_PATTERNS.md](ARCHITECTURE_PATTERNS.md)** — implementation patterns
- **[STARTUP_READY_PLAN.md](STARTUP_READY_PLAN.md)** — readiness and rollout planning

---

## Planned / Not Yet Implemented

These are still roadmap items, not shipped capabilities:

- GitOps PR generation / config patch output
- OpenSLO ingestion
- External telemetry adapters beyond the current local flow
- Broader multi-service and multi-region blast-radius controls
- Deeper production integration patterns

Do not describe these as implemented until code and tests exist. Yes, that is me being
annoyingly honest on purpose.

---

## Recommended Reading by Audience

### For users evaluating ACO
1. [README.md](../README.md)
2. [WHAT_THIS_IS.md](WHAT_THIS_IS.md)
3. [START_HERE.md](START_HERE.md)

### For engineers extending ACO
1. [ARCHITECTURE_PATTERNS.md](ARCHITECTURE_PATTERNS.md)
2. This index
3. Source packages under `artifact/`, `policy/`, `autonomy/`, `budget/`, and `benchmark/`

### For governance / reliability readers
1. [WHAT_THIS_IS.md](WHAT_THIS_IS.md)
2. [ARCHITECTURE_PATTERNS.md](ARCHITECTURE_PATTERNS.md)
3. Benchmark and policy code paths

---

## Next Documentation Fixes

The next public docs that should be aligned with the shipped phases are:

1. `README.md`
2. `docs/WHAT_THIS_IS.md`
3. `docs/START_HERE.md`
4. `docs/STARTUP_READY_PLAN.md` (if stale after review)

---

Keep docs truthful. Fancy lies rot faster than code.
