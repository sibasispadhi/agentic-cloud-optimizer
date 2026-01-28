
**[← Back to START_HERE](START_HERE.md)** | **[← Back to README](../README.md)**

---

# What ACO Is (and Is Not)

## What ACO Is

Agentic Cloud Optimizer (ACO) is a **guided JVM tuning assistant** for Java microservices. It is designed to help SREs, performance engineers, and JVM developers:

- Understand how their services behave under load (threads, heap, GC, CPU).
- Get **concrete, explainable recommendations** for tuning JVM and concurrency settings.
- Run repeatable performance experiments with a clear before/after story.

At a high level, ACO:

1. Collects JVM metrics while you run a load test or replay traffic.
2. Analyzes those metrics using:
   - A rule-based agent (deterministic heuristics), and
   - An LLM-powered agent (running locally via Ollama by default).
3. Generates **human-readable tuning suggestions**:
   - Thread pool sizes / concurrency
   - Heap sizing
   - GC-related flags and thresholds
4. Logs every recommendation, its reasoning, and the associated metrics, so you can review and compare runs.

The goal is to give teams a **repeatable, explainable loop** for JVM performance tuning, not to hide decisions behind a black box.

---

## What ACO Is Not

ACO is intentionally **not** an autonomous system that makes live production changes on its own. Specifically:

- ❌ **ACO does not directly modify your production environment.**
  - It does *not* call Kubernetes APIs to resize deployments.
  - It does *not* patch JVM settings in running pods.
  - It does *not* bypass your change management or CI/CD process.

- ❌ **ACO is not a replacement for your observability stack.**
  - It does not try to be Prometheus, OpenTelemetry, or your APM.
  - It consumes metrics and logs; it does not store or index them long-term.

- ❌ **ACO is not a magic "fix my latency" button.**
  - It will not override bad architecture, missing indexes, or broken business logic.
  - It focuses on JVM- and concurrency-level tuning where metrics are available.

---

## How ACO Fits Into Your Workflow

Think of ACO as a **recommendation engine** that plugs into your existing engineering practices. Typical use:

1. You run ACO alongside a JVM-based service under load.
2. ACO generates suggested changes such as:
   - JVM flags
   - Thread pool sizes
   - Container resource hints
3. You review those suggestions and then apply them manually (or script the application) via:
   - CI/CD pipelines
   - Kubernetes manifests / Helm charts
   - JVM startup flags or config files
4. You re-run the test and compare metrics using:
   - ACO's results pages (`/results.html`)
   - Your observability stack (Prometheus, Grafana, etc.)

In other words:

> **ACO proposes. You decide and apply.**

---

## Governance, Safety, and Auditability

ACO is built with **governance and auditability** in mind:

- Every tuning recommendation is:
  - Derived from measurable metrics,
  - Backed by a textual explanation from the agent(s), and
  - Logged for later review.

- You can:
  - Store ACO run artifacts (metrics snapshots + reasoning text) as part of your performance test evidence.
  - Link a specific configuration change in Git/CI to a specific ACO run and explanation.
  - Use ACO outputs as input to change advisory boards (CABs) or internal risk reviews.

This makes it easier to answer questions like:
- *"Why did we change these JVM flags?"*
- *"What data did we look at before scaling this service?"*
- *"Who or what suggested this tuning, and when?"*

ACO is not meant to remove humans from the loop. It is meant to give them **better, auditable, data-driven suggestions** for JVM and concurrency tuning in Java microservices.

---

## What Does ACO Optimize?

- **Concurrency**: Thread pool sizes for the selected workload simulator
- **JVM Heap Size**: Based on GC metrics (frequency, pause time, heap usage)

### Workload Simulators

- `demo` (default): Built-in demo workload
- `http`: External HTTP REST endpoint (v0.2.0)

See **[README.md](../README.md)** for examples.

---

## Where Do Outputs Go?

After a run, look in:

- `artifacts/` (baseline.json, after.json, report.json, reasoning traces)

See **[examples/README.md](../examples/README.md)** for how to interpret them.
