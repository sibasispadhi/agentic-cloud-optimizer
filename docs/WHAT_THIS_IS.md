**[← Back to START_HERE](START_HERE.md)** | **[← Back to README](../README.md)**

---

# What ACO Is (and Is Not)

## What ACO Is

Agent Cloud Optimizer (ACO) is a **governance-first optimization system** for JVM-based services.
It helps engineers run controlled optimization loops using measurable runtime evidence instead of
hand-wavy tuning folklore.

ACO currently does five core things:

1. **Observes runtime behavior**
   - latency
   - throughput
   - heap usage
   - GC activity
   - CPU pressure
   - thread behavior

2. **Generates recommendations**
   - using either a local LLM-backed agent (`SpringAiLlmAgent`) or a deterministic fallback
     (`SimpleAgent`)

3. **Packages decisions into a structured artifact**
   - `OptimizationPlan`
   - `PlanChange`
   - `PlanEvidence`
   - `ValidationRecipe`
   - `RollbackRecipe`

4. **Applies governance checks before actuation**
   - policy evaluation
   - actuation budget checks
   - autonomy / confidence gating

5. **Models validation and rollback**
   - how to confirm a change helped
   - how to recover when it did not

The goal is not "AI makes tuning magical." The goal is to make optimization **bounded,
explainable, and reviewable**.

---

## What ACO Is Not

ACO is intentionally **not** a free-roaming production mutation engine.

### ACO does not currently:

- ❌ patch live Kubernetes deployments by itself
- ❌ edit Helm charts or open GitOps pull requests automatically
- ❌ ingest OpenSLO specifications
- ❌ act as a replacement for Prometheus, OpenTelemetry, or an APM platform
- ❌ solve application-level architectural bugs, bad SQL, or broken business logic
- ❌ provide multi-region production rollout control

If you are looking for "let the model auto-fix prod and hope for the best," this repo is not that.
On purpose. Because that would be dumb.

---

## How ACO Works Today

A typical ACO run follows this path:

1. **Run a baseline workload**
2. **Check SLO-related signals**
3. **Generate a recommendation** from the LLM-backed or deterministic agent
4. **Assemble an `OptimizationPlan`** as the single source of truth for the run
5. **Evaluate the plan through policy** using the policy engine
6. **Check actuation budget** using `ActuationBudgetLedger`
7. **Evaluate autonomy mode and confidence** using `AutonomyGate`
8. **Produce validation and rollback steps**
9. **Persist artifacts and reports** for review

This means ACO is not just a recommendation engine. It is a **governed recommendation pipeline**.

---

## Governance Components That Exist in Code

These are not conceptual buzzwords floating around in a slide deck. They are implemented.

### 1. Optimization artifact
Located under `src/main/java/com/cloudoptimizer/agent/artifact/`

Key pieces:
- `OptimizationPlan`
- `PlanMetadata`
- `PlanIntent`
- `PlanChange`
- `PlanEvidence`
- `PlanWriter`
- `ValidationRecipe`
- `RollbackRecipe`
- `ValidationResult`
- `RollbackResult`

Why it matters:
- every run has a traceable artifact
- evidence and intent are preserved
- validation and rollback are first-class parts of the run

### 2. Policy engine
Located under `src/main/java/com/cloudoptimizer/agent/policy/`

Key pieces:
- `PolicyEngine`
- `DefaultPolicyEngine`
- `ActuationPolicy`
- `PolicyContext`
- `PolicyDecision`

Why it matters:
- recommendation and permission are separated
- a recommendation can be generated and still be blocked
- governance is explicit instead of implied

### 3. Actuation budgets
Located under `src/main/java/com/cloudoptimizer/agent/budget/`

Key pieces:
- `ActuationBudget`
- `ActuationBudgetLedger`
- `BudgetConsumption`
- `ProposedChangeDelta`

Why it matters:
- repeated changes are bounded
- operational churn is treated as a risk surface
- the system can say "enough changes for now"

### 4. Confidence gates and autonomy modes
Located under `src/main/java/com/cloudoptimizer/agent/autonomy/`

Key pieces:
- `AutonomyGate`
- `AutonomyConfig`
- `AutonomyMode`
- `AutonomyGateResult`

Why it matters:
- advisory-first operation is supported
- low-risk automatic behavior can be constrained
- confidence and impact can block actuation

### 5. Validation and rollback modeling
Implemented across service and artifact layers

Key pieces:
- `ValidationExecutor`
- `RollbackExecutor`
- `ValidationResult`
- `RollbackResult`

Why it matters:
- every approved change should have a way to be checked
- rollback is modeled as part of the workflow, not an afterthought

### 6. Benchmark scenarios
Located under `src/main/java/com/cloudoptimizer/agent/benchmark/`

Key pieces:
- `ScenarioRunner`
- `ScenarioDecisionEngine`
- `RetryStormScenario`
- `ThreadSaturationScenario`
- `CpuThrottlingScenario`
- `HeapOverprovisionedScenario`
- `BurstTrafficScenario`

Why it matters:
- governance behavior can be tested without production data
- amplification can be measured instead of merely described

---

## How ACO Fits Into an Engineering Workflow

ACO fits best as a **local or controlled-environment optimization assistant**.

Typical workflow:

1. Run ACO against a JVM service or simulator
2. Inspect the generated plan and reasoning
3. Review policy warnings / violations
4. Decide whether to apply the change through your normal delivery process
5. Re-run and compare before/after outcomes
6. Keep the artifact as evidence for future review

In other words:

> **ACO proposes, governs, and documents. You decide how far execution goes.**

That is a lot healthier than pretending autonomy is a personality trait.

---

## What ACO Optimizes Right Now

Current optimization focus areas:
- **Concurrency / thread settings**
- **Heap sizing**
- **JVM tuning decisions derived from runtime metrics**

Current workload modes:
- `demo` workload simulator
- `http` workload simulator

Current benchmark scenarios:
- retry storm
- thread saturation
- CPU throttling
- heap overprovisioning
- burst traffic

---

## Where Outputs Go

After a run, inspect:

- `artifacts/`
  - `baseline.json`
  - `after.json`
  - `report.json`
  - optimization artifacts
  - reasoning traces
  - validation / rollback records

See **[examples/README.md](../examples/README.md)** for example output structure.

---

## Current Boundaries and Limitations

ACO is useful, but not magic. Current boundaries include:

- it is strongest in **controlled and testable environments**
- it focuses on **JVM and concurrency-level tuning**, not whole-system redesign
- it does not yet implement **GitOps output generation**
- it does not yet implement **OpenSLO ingestion**
- it does not yet provide **broad production rollout orchestration**

Those are roadmap items, not present-tense promises.

---

## Bottom Line

ACO is a JVM optimization system with real governance machinery in the loop:
- structured artifacts
- policy checks
- actuation budgets
- autonomy gates
- validation
- rollback modeling
- benchmark scenarios

That is the real point of the project.
Not bigger buzzwords. Not fake autonomy. Not decorative AI.
