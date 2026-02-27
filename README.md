# Agent Cloud Optimizer (ACO)

> **Reference implementation** of agentic AI architecture patterns for Java microservices.  
> Presented at **DevNexus 2026** — *"Agentic AI for Java Microservices: From Traditional Scaling to Self-Optimization"*

Autonomous, **LLM-powered** JVM performance optimizer for Java services — demonstrating how self-optimizing systems work in FinTech-scale environments.  
Runs locally, in Docker, on VMs, or in Kubernetes. No cloud dependency required.

---

## 🎯 DevNexus 2026 Resources

| Resource | Link |
|----------|------|
| 📊 Architecture Patterns | [`docs/ARCHITECTURE_PATTERNS.md`](docs/ARCHITECTURE_PATTERNS.md) |
| 📍 Practical Roadmap | [`docs/PRACTICAL_ROADMAP.md`](docs/PRACTICAL_ROADMAP.md) |
| 🧹 Reset Demo | `./scripts/reset-demo.sh` |

> 🎬 **Slide deck & demo script** will be published after the DevNexus 2026 presentation!

---

## Why ACO?

Tuning JVM-based microservices is still mostly guesswork. You tweak thread pools, heap sizes, and GC settings, redeploy, rerun a load test, and hope the new numbers look better. Repeat that a few dozen times across services and environments and you end up with:

- Fragile configs that nobody remembers the rationale for
- Over-provisioned resources to "be safe"
- Long performance war rooms where people stare at dashboards but don't know what to change

**Agentic Cloud Optimizer (ACO)** removes that guesswork. ACO continuously reads JVM metrics (GC, heap, threads, CPU) while you drive load, then uses an LLM-powered agent (with a safe rule-based fallback) to:

- Diagnose performance bottlenecks and anti-patterns in your current settings
- Propose concrete configuration changes (thread pool sizes, heap limits, GC flags, etc.)
- Explain *why* each change is being suggested, in plain language
- Track every recommendation and outcome on a simple web dashboard

The goal isn't "AI magic." The goal is to give SREs and performance engineers a **repeatable, explainable loop** for JVM tuning that can be run on a laptop, in a lab, or as part of your CI/CD performance checks.

---

## Hero Example: From Over-Provisioning to Right-Sizing

Here's a real scenario using ACO on a Java Spring Boot service under synthetic load.

**Baseline setup**
- Service: Spring Boot REST API with built-in workload simulator
- Workload: Moderate synthetic load (demo mode)
- JVM config:
  - Thread pool: 4 worker threads
  - Heap: `-Xms8g -Xmx8g` (8192 MB)
  - Default GC (G1)

**Key baseline metrics**
- Median latency: **28.99 ms**
- Throughput: **135.30 req/s**
- Heap usage: **0.6%** (53 MB of 8192 MB)
- GC frequency: **0 collections/sec** (idle GC)
- Thread utilization: Low (4 threads handling load easily)

**What ACO diagnosed**

ACO's SimpleAgent analyzed the metrics and identified:
1. **Severe over-provisioning**: Heap usage at 0.6% indicates ~99% waste
2. **Unnecessary concurrency**: 4 threads for this load level is overkill
3. **Cost optimization opportunity**: Can reduce resources without impacting performance

**ACO's recommendation**
```
Reasoning: "Median latency (28.99ms) is well below target (100.00ms). 
Decreasing concurrency from 4 to 3. Low GC frequency (0.00/sec) and 
heap usage (0.6%) indicate over-provisioning. Decreasing heap from 
8192MB to 6144MB to optimize cost."

Recommended Changes:
- Thread pool: 4 → 3 threads (-25%)
- Heap size: 8192 MB → 6144 MB (-25%)
- Confidence: 95%
- Impact: LOW
```

**After applying ACO's recommendations**
- Median latency: **28.99 ms** (unchanged - still meeting SLA)
- Throughput: **102.26 req/s** (sufficient for current load)
- Heap usage: **0.3%** (26 MB of planned 6144 MB)
- GC frequency: **0.10 collections/sec** (healthy)
- **Cost savings: ~25% reduction in heap allocation**

**Key insight**: ACO identified that the service was massively over-provisioned. The original config (8GB heap, 4 threads) was handling a workload that only needed ~50MB of heap and 3 threads. By right-sizing:
- ✅ Maintained target latency (< 100ms)
- ✅ Reduced memory allocation by 25% (2GB saved)
- ✅ Reduced thread overhead by 25%
- ✅ Freed resources for other services
- ✅ Provided full reasoning for every decision

This scenario demonstrates ACO's **cost optimization** capabilities. In production environments with hundreds of services, identifying and fixing over-provisioning like this across the fleet can save significant infrastructure costs.

**The ACO workflow**:
1. Run baseline load test while ACO watches JVM metrics
2. Review agent's analysis and recommendations (with full reasoning)
3. Apply changes via your deployment process (Kubernetes, Helm, etc.)
4. Validate with ACO's side-by-side comparison
5. Export reasoning traces for documentation

ACO is **opt-in and explainable**: it gives you guided tuning decisions with full reasoning, so you stay in control of how your Java services evolve under real-world load.

---

## Quick Start (recommended)

1) Follow **[docs/START_HERE.md](docs/START_HERE.md)**
2) Install Ollama via **[docs/OLLAMA_SETUP.md](docs/OLLAMA_SETUP.md)**
3) Verify setup: `./scripts/verify-ollama.sh`

## Run (CLI mode)

```bash
mvn clean package -DskipTests
./scripts/run-agent.sh
# Windows: scripts\run-agent.bat
```

## Run (Live Web Dashboard)

```bash
mvn clean package -DskipTests
./scripts/run-web-ui.sh
# Windows: scripts\run-web-ui.bat
```

Open: http://localhost:8080/live-dashboard.html

## View results

- Results page (served by the app): http://localhost:8080/results.html
- `artifacts/` directory (baseline/after/report + reasoning traces)

Legacy: `demo.html` now just points you to `/results.html`.

## Features

- ✅ **SLO Breach Detection**: Monitors p99 latency against configurable SLO thresholds — triggers agent automatically
- ✅ **p95/p99 Percentile Tracking**: Tail latency metrics critical for FinTech transaction SLAs
- ✅ **Agentic Closed-Loop**: SLO breach → LLM analyzes → bounded fix → validates improvement
- ✅ **Concurrency Optimization**: Automatically tunes thread pool sizes
- ✅ **Heap Optimization**: Analyzes GC metrics and recommends optimal JVM heap size
- ✅ **LLM-Powered Analysis**: Uses local Ollama for intelligent reasoning (no cloud required)
- ✅ **Rule-Based Fallback**: SimpleAgent provides fast, deterministic decisions
- ✅ **Explainable AI**: Full reasoning traces for every decision
- ✅ **Real-time Dashboard**: WebSocket-powered live monitoring with SLO status

## Optional docs

- What this is / isn't: **[docs/WHAT_THIS_IS.md](docs/WHAT_THIS_IS.md)**
- Windows setup: **[docs/WINDOWS_SETUP.md](docs/WINDOWS_SETUP.md)**
- Example outputs: **[examples/README.md](examples/README.md)**

## License

MIT — see [LICENSE](LICENSE).