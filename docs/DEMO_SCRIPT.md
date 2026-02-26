# DevNexus 2026 — Live Demo Script

**Title:** Agentic AI for Java Microservices  
**Duration:** ~15 minutes (within the 35-min talk)  
**Speaker:** Sibasis Padhi  
**Setup:** Local machine, no cloud dependency, no WiFi required  

---

## ⚠️ Pre-Demo Checklist (Do 30 min before)

```bash
# 1. Reset demo state
./scripts/reset-demo.sh

# 2. Verify Ollama is running
curl -s http://localhost:11434/api/tags | head -5
# Expected: JSON with model list including llama3.2:1b

# 3. Build the project (if not already)
mvn clean package -DskipTests

# 4. Open the live dashboard in browser (keep it in background)
open http://localhost:8080/live-dashboard.html

# 5. Open slide deck in another tab
open presentation/devnexus-2026-slides.html
```

---

## Demo Flow

### Act 1: The False Comfort of Median (Slide 3 → Live)

**Talking points:**
> "Let me show you what this looks like in practice. We have a Java
> microservice simulating FinTech transaction load."

```bash
# Run baseline with SimpleAgent first (fast, no LLM wait)
AGENT_STRATEGY=simple CONCURRENCY=4 DURATION=10 \
  java -Dagent.strategy=simple \
       -Dbaseline.concurrency=4 \
       -Dload.duration=10 \
       -Drun.mode=cli \
       -jar target/*.jar
```

**Point to the dashboard:**
> "Median latency: 28ms. Looks fine, right? CPU is happy.
> Traditional monitoring says: 'No action needed.'"

**Reveal:**
> "But look at the p99... 150ms. We're breaching our 100ms SLA.
> This affects the worst 1% — that's 100K angry customers
> out of 10 million daily transactions."

---

### Act 2: SLO Breach Detection Fires (Slide 6)

**Talking points:**
> "With SLO breach detection wired into our orchestrator,
> the system detects this automatically."

**Point to WebSocket logs or terminal output:**
```
🚨 SLO BREACH DETECTED! p99=150ms exceeds 120ms threshold
→ Triggering autonomous agent for root cause analysis
```

> "No pager. No 2 AM wake-up call. The agent activates itself."

---

### Act 3: LLM Reasoning (Slide 8)

```bash
# Now run with LLM agent
AGENT_STRATEGY=llm CONCURRENCY=4 DURATION=10 \
  java -Dagent.strategy=llm \
       -Dbaseline.concurrency=4 \
       -Dload.duration=10 \
       -Drun.mode=cli \
       -jar target/*.jar
```

**Point to reasoning trace:**
> "The LLM is analyzing heap usage, GC pauses, thread contention,
> and p99 latency together. It's doing multi-dimensional analysis
> that would take an engineer 20-30 minutes."

**Point to the recommendation:**
> "It proposes: reduce heap from 8GB to 6GB, adjust threads.
> But here's the key — this recommendation is validated against
> our ±25% safety bounds before it's applied."

---

### Act 4: Before/After (Slide 11)

**Show the report.json or dashboard:**
```bash
cat artifacts/report.json | python3 -m json.tool | head -40
```

> "p99 went from 150ms to 62ms. That's 58% improvement.
> Infrastructure cost down 25%. GC pressure down 62%.
> And the SLO is restored — we're back under 100ms."

**Point to slo_compliance section:**
> "The report even tracks whether the SLO was restored
> after optimization. Full accountability."

---

### Act 5: Architecture Pattern Recap (Slide 5)

> "What you just saw is the Observe-Reason-Enforce pattern.
> The observation layer is deterministic — no AI hallucinations.
> The reasoning layer uses the LLM as an advisor, not a dictator.
> The enforcement layer constrains actions to safe bounds.
> Each layer is independently testable. And this works anywhere —
> local, Docker, VMs, Kubernetes. No platform lock-in."

---

## 🚨 If Things Go Wrong

| Problem | Fix |
|---------|-----|
| Ollama not responding | Use `AGENT_STRATEGY=simple` (rule-based fallback) |
| Dashboard won't load | Show pre-generated `artifacts/report.json` |
| Build fails | Use pre-built jar: `ls target/*.jar` |
| Port 8080 in use | `SERVER_PORT=9090 java -Dserver.port=9090 -jar target/*.jar` |
| Demo gives weird numbers | Run `./scripts/reset-demo.sh` and retry |

## 🎬 Backup: Pre-Generated Artifacts

If live demo fails completely, show these from `examples/devnexus/`:
- `baseline.json` — baseline metrics
- `after.json` — post-optimization metrics
- `report.json` — full comparison report
- `reasoning_trace.txt` — LLM reasoning output

> "I ran this earlier and here are the actual results..."

---

## Timing Guide

| Segment | Time | Cumulative |
|---------|------|------------|
| Act 1: Baseline + median lie | 2 min | 2 min |
| Act 2: SLO breach fires | 1 min | 3 min |
| Act 3: LLM reasoning | 4 min | 7 min |
| Act 4: Before/after results | 3 min | 10 min |
| Act 5: Pattern recap | 2 min | 12 min |
| Code walkthrough (slides) | 3 min | 15 min |

**Total demo time: ~15 minutes**
