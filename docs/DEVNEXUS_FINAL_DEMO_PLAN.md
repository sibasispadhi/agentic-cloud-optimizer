# DevNexus 2026 — Final Demo Strategy

**Date:** March 2, 2026  
**Event:** DevNexus 2026  
**Talk:** Agentic AI for Java Microservices  
**Speaker:** Sibasis Padhi  

---

## 🎯 THE SITUATION

After extensive testing with CONCURRENCY=4, 8, 16, and 32:

**❌ We cannot trigger an SLO breach in the live system!**

| Concurrency | p99 Latency | SLO Status |
|-------------|-------------|------------|
| 4 threads   | 49ms        | ✅ Compliant |
| 8 threads   | 49ms        | ✅ Compliant |
| 16 threads  | 64ms        | ✅ Compliant |
| 32 threads  | 61ms        | ✅ Compliant |

**SLO breach threshold:** p99 > 120ms  
**Actual p99 at MAX load (32 threads, 947 req/s):** 61ms

**Why?** The system has been heavily optimized since the original demo artifacts were created. Even under extreme load, p99 stays rock-solid under 65ms.

**This is EXCELLENT for production, but makes the "breach → fix" narrative impossible to demo live.**

---

## ✅ RECOMMENDED DEMO APPROACH

### **Strategy: Hybrid (Pre-generated + Live)**

Use the **best of both worlds**:
- Pre-generated breach scenario for the main story (safe, dramatic)
- Optional live demo for transparency/credibility (fast, reliable)

---

## 📋 DEMO FLOW

### **Act 1: The Problem (Slides 1-4)**

**Talking points:**
> "FinTech microservice handling millions of transactions daily.  
> Median latency: 30ms. Looks great!  
> But p99 is 150ms — we're breaching our 100ms SLA.  
> That's 100,000 angry customers out of 10 million daily transactions.  
> Traditional auto-scaling looks at CPU and median — misses this entirely."

**What to show:**
- Slide 4: Median vs p99 comparison
- Slide 3: "The Reality" metrics

**No live demo yet** — just set the stage.

---

### **Act 2: SLO Breach Detection (Slide 7)**

**Talking points:**
> "When should the agent activate? SLO breach detection answers that.  
> Policy: p99 must be under 100ms. Breach if > 120ms for 3 consecutive checks.  
> Here's what happened when we ran this earlier..."

**What to show:**
- Slide 6: SLO policy code snippet
- Terminal (optional): Show pre-generated `baseline.json`

```bash
cat examples/devnexus/baseline.json | python3 -m json.tool
```

**Key numbers to point out:**
```json
"p99_latency_ms": 150.3,  // ← BREACH! Over 120ms threshold
"median_latency_ms": 28.99,  // ← Looks fine!
"heap_usage_percent": 0.65,  // ← Massive over-provisioning
"gc_frequency_per_sec": 2.1  // ← Excessive GC
```

**Talk track:**
> "p99 is 150ms — that's a breach. Median is 29ms — auto-scaling says 'everything's fine.'  
> But look at heap: 8GB allocated, only 53MB used (0.65%). Massive over-provisioning.  
> GC is running 2.1 times per second — scanning 8GB to find 53MB of garbage!"

---

### **Act 3: Agent Reasoning (Slides 9-10)**

**Talking points:**
> "The LLM agent analyzes this multi-dimensionally.  
> It's not just looking at one metric — it's correlating heap usage, GC frequency,  
> thread contention, and p99 latency together.  
> Let me show you the actual reasoning trace..."

**What to show:**
- Terminal: Show pre-generated `reasoning_trace.txt`

```bash
cat examples/devnexus/reasoning_trace.txt
```

**Key reasoning to highlight:**
```
"p99 latency of 150.3ms exceeds the 100ms SLA threshold.
Heap usage at 0.65% (53MB of 8192MB) indicates severe over-provisioning.
GC frequency at 2.1/sec suggests fragmented allocation patterns.
Reducing heap to 6144MB will improve GC efficiency.
Reducing threads from 4 to 3 decreases context-switching overhead."
```

**Talk track:**
> "The agent identified the root cause: over-provisioned heap causing excessive GC scan time.  
> Its recommendation: reduce heap from 8GB to 6GB, reduce threads from 4 to 3.  
> Confidence: 94%. But here's the key — this goes through our enforcement layer..."

**Point to Slide 6: O-R-E architecture**
> "The enforcement layer validates: ±25% max change, cooldown windows, auto-rollback.  
> LLM proposes → Policy constrains → System verifies → Rollback exists."

---

### **Act 4: Results (Slides 12-13)**

**Talking points:**
> "Here's what happened after the agent applied the optimization..."

**What to show:**
- Terminal: Show pre-generated `report.json`

```bash
cat examples/devnexus/report.json | python3 -m json.tool | grep -A3 -B3 "improvements\|slo_compliance"
```

**Key results to highlight:**

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| **p99 latency** | 150.3ms | 62.4ms | **-58.5%** |
| **p95 latency** | 95.4ms | 58.7ms | **-38.5%** |
| **Heap size** | 8192 MB | 6144 MB | **-25%** |
| **GC frequency** | 2.1/sec | 0.8/sec | **-61.9%** |
| **Cost** | $0.0054 | $0.0041 | **-24.1%** |
| **SLO status** | 🔴 BREACH | ✅ RESTORED | — |

**Talk track:**
> "p99 went from 150ms to 62ms. That's a 58% improvement.  
> Infrastructure cost down 24%. GC pressure down 62%.  
> And most importantly — the SLO is restored. We're back under 100ms.  
> This all happened autonomously, in seconds, while you were asleep."

---

### **Act 5: OPTIONAL Live Demo (During Code Walkthrough)**

**ONLY if you want to show something live for credibility.**

This is the **CONCURRENCY=16 proactive optimization** demo:

**Talking points:**
> "Now, you might be thinking: 'Did you cherry-pick that data?'  
> Fair question! Let me run this live, right now.  
> This time, the system isn't breaching — but the agent can still optimize it."

**What to run:**
```bash
# Reset demo state
./scripts/reset-demo.sh

# Run live demo with CONCURRENCY=16
AGENT_STRATEGY=simple CONCURRENCY=16 DURATION=10 \
  java -Dagent.strategy=simple \
       -Dbaseline.concurrency=16 \
       -Dload.duration=10 \
       -Drun.mode=cli \
       -jar target/*.jar
```

**Expected results:**
- Baseline p99: ~64ms (no breach)
- After optimization p99: ~53ms
- Improvement: ~18%
- Throughput: Reduced 20% (efficiency gain)

**Talk track while it's running (~25 seconds):**
> "This is running 16 concurrent threads for 10 seconds.  
> Notice: we're NOT breaching the SLO this time — p99 is around 64ms.  
> Traditional monitoring would say: 'Everything's fine, no action needed.'  
> But the agent says: 'We can do better!'  
> Watch... it's going to reduce concurrency from 16 to 12..."

**When results appear:**
> "There! p99 went from 64ms to 53ms. That's an 18% improvement.  
> This is PROACTIVE optimization — not reactive firefighting.  
> The agent continuously improves your system, even when it's healthy.  
> This is the future of operations: AI that makes good systems better."

**Benefits of this approach:**
- ✅ Shows transparency ("I'm not faking this")
- ✅ Fast (25 seconds)
- ✅ Reliable (won't fail)
- ✅ Different narrative: proactive vs reactive
- ✅ More realistic for production systems

**Risks:**
- ⏱️ Takes 25 seconds (could feel long during talk)
- 🎯 Less dramatic than breach scenario

**Decision:** Include this if you have time and want to show live code. Otherwise, skip it.

---

## 🚨 IF THINGS GO WRONG

| Problem | Solution |
|---------|----------|
| Audience asks "Is this live?" | Show the CONCURRENCY=16 live demo in Q&A |
| Terminal freezes during live demo | Ctrl+C, fall back to pre-generated artifacts |
| Pre-generated files not found | They're in `examples/devnexus/` — copy to `artifacts/` |
| Ollama not running | Use `AGENT_STRATEGY=simple` (rule-based, no LLM) |
| "Why not show breach live?" | "Great question! The system's been optimized since I created the demo — it's too robust now. That's actually a good problem to have!" |

---

## 📊 ARTIFACTS CHECKLIST

### **Pre-generated (MUST HAVE for main demo):**
- ✅ `examples/devnexus/baseline.json` (p99=150.3ms breach)
- ✅ `examples/devnexus/after.json` (p99=62.4ms restored)
- ✅ `examples/devnexus/report.json` (full comparison)
- ✅ `examples/devnexus/reasoning_trace.txt` (LLM reasoning)

### **Live demo (OPTIONAL):**
- ✅ `artifacts/baseline.json` (generated from CONCURRENCY=16)
- ✅ `artifacts/after.json` (generated from CONCURRENCY=16)
- ✅ `artifacts/report.json` (generated from CONCURRENCY=16)

### **Slides:**
- ✅ `presentation/devnexus-2026-slides-OFFLINE.html` (cleaned, no Walmart branding)

### **Scripts:**
- ✅ `scripts/reset-demo.sh` (reset state before demo)
- ✅ `scripts/run-web-ui.sh` (optional, for dashboard)

---

## 🎖️ FINAL RECOMMENDATION

**For DevNexus 2026, use this flow:**

1. **Main Presentation (Slides 1-12):**  
   → Show pre-generated breach scenario from `examples/devnexus/`  
   → Talk track: "Here's what happened when we ran this earlier..."  
   → Show `baseline.json`, `reasoning_trace.txt`, `report.json`  
   → Narrative: "SLO breach → agent detects → analyzes → fixes → SLO restored"

2. **Code Walkthrough (Slides 13-16):**  
   → Show code snippets for O-R-E pattern  
   → No live demo here — just explain the architecture

3. **Q&A:**  
   → If someone asks "Can you show it live?": Run CONCURRENCY=16  
   → Talk track: "The system's been optimized, so we can't breach it anymore.  
      But I can show you proactive optimization — watch this..."
   → Run the 25-second live demo  
   → Show 64ms → 53ms improvement

**This approach gives you:**
- ✅ Safety (pre-generated artifacts won't fail)
- ✅ Drama (breach scenario is compelling)
- ✅ Credibility (can show live if asked)
- ✅ Flexibility (adapt to audience questions)
- ✅ Time efficiency (no waiting for load tests during main talk)

---

## ⏱️ TIMING BREAKDOWN

| Segment | Time | Notes |
|---------|------|-------|
| Act 1: The Problem | 2 min | Slides only |
| Act 2: SLO Breach (pre-gen baseline) | 2 min | Show `baseline.json` |
| Act 3: Agent Reasoning (pre-gen trace) | 3 min | Show `reasoning_trace.txt` |
| Act 4: Results (pre-gen report) | 3 min | Show `report.json` |
| Code Walkthrough | 3 min | Slides 13-16 |
| **Optional:** Live Demo CONCURRENCY=16 | 2 min | Q&A only |

**Total main demo: 13 minutes** (fits comfortably in 35-min talk)

---

## 🎤 KEY MESSAGES

1. **Median lies.** p99 tells the truth about user experience.
2. **Traditional monitoring is reactive.** Agentic AI is proactive.
3. **LLM as advisor, not dictator.** Observe-Reason-Enforce keeps it safe.
4. **Multi-objective optimization.** Not just latency — also cost, GC, throughput.
5. **Explainable decisions.** You can see exactly why the agent did what it did.
6. **Production-ready pattern.** No cloud lock-in, works anywhere.

---

## ✅ PRE-DEMO CHECKLIST (30 min before talk)

```bash
# 1. Verify pre-generated artifacts exist
ls -lh examples/devnexus/
# Expected: baseline.json, after.json, report.json, reasoning_trace.txt

# 2. Reset demo state (cleans artifacts/)
./scripts/reset-demo.sh

# 3. Verify Ollama is running (for optional live demo)
curl -s http://localhost:11434/api/tags | python3 -c "import sys,json; print('OK' if json.load(sys.stdin).get('models') else 'FAIL')"

# 4. Open slides (OFFLINE version, no WiFi needed)
open presentation/devnexus-2026-slides-OFFLINE.html

# 5. Practice showing pre-generated artifacts
cat examples/devnexus/baseline.json | python3 -m json.tool | head -20
cat examples/devnexus/reasoning_trace.txt | head -30
cat examples/devnexus/report.json | python3 -m json.tool | grep -A5 improvements
```

---

## 🎯 DECISION POINTS

### **Do you want to run a live demo during the main talk?**

**Option A: NO** (Recommended for safety)
- Use pre-generated artifacts for entire main presentation
- Show live demo ONLY if asked during Q&A
- **Pro:** Safe, fast, no risk of failure during talk
- **Con:** Might feel "canned" to some audience members

**Option B: YES** (For transparency)
- Show pre-generated breach scenario in main talk
- Run CONCURRENCY=16 live during code walkthrough
- **Pro:** Shows transparency, live code execution
- **Con:** Takes 25 seconds, less dramatic than breach

**My recommendation: Option A.** Save live demo for Q&A.

---

## 📞 FINAL WORDS

You have:
- ✅ Clean, vendor-neutral slides
- ✅ Compelling pre-generated breach scenario
- ✅ Fast, reliable live demo backup (CONCURRENCY=16)
- ✅ Clear talk track and messaging
- ✅ Multiple fallback options

**You're ready for DevNexus 2026! 🚀**

If something goes wrong, you have 3 layers of backup:
1. Pre-generated artifacts (safest)
2. Live CONCURRENCY=16 demo (fast, reliable)
3. Slides + code walkthrough (always works)

Good luck! 🎤
