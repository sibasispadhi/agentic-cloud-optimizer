**[← Back to START_HERE](START_HERE.md)** | **[← Back to README](../README.md)**

---

# What This Project Is (and isn’t)

Agent Cloud Optimizer (ACO) is a **production-ready implementation** demonstrating how an autonomous agent can:

1. run a baseline load test,
2. analyze performance metrics (latency, throughput, GC metrics),
3. recommend optimizations (concurrency + heap size),
4. validate the change with a second load test,
5. produce artifacts + reasoning traces.

It’s built to be **LLM-first** (local Ollama), with a **simple rule-based fallback**.

---

## ✅ This project IS

- A fully functional optimizer you can run locally in minutes.
- A reference architecture for "observe → reason → decide → act → validate".
- A practical tool for JVM performance tuning with full explainability and reasoning traces.
- A foundation you can extend (WorkloadSimulator interface, HTTP simulator, etc.).

---

## ❌ This project is NOT (yet)

- A production-ready autoscaler.
- A drop-in agent that can safely reconfigure your infra without work.
- A complete “optimize everything” solution.

If you’re looking for future plans/ideas, check `docs/archive/` (kept for reference).

---

## What does it optimize today?

- **Concurrency** for the selected workload simulator
- **JVM Heap Size** based on GC metrics (frequency, pause time, heap usage)

### Workload simulators

- `demo` (default): built-in demo workload
- `http`: external HTTP REST endpoint (v0.2.0)

See **[README.md](../README.md)** for examples.

---

## Where do outputs go?

After a run, look in:

- `artifacts/` (baseline.json, after.json, report.json, reasoning traces)

See **[examples/README.md](../examples/README.md)** for how to interpret them.