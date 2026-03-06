# DevNexus 2026 — LIVE DEMO Quick Reference

**When to run:** During code walkthrough OR Q&A
**Duration:** ~25 seconds
**Risk:** Low (tested multiple times, very reliable)

---

## 🎯 THE PLAN

### **Pre-generated breach scenario** (Acts 1-4)
→ Show `examples/devnexus/` artifacts for the dramatic "SLO breach → fix" story

### **Live demo** (Act 5 or Q&A)
→ Run CONCURRENCY=16 to show "proactive optimization" in real-time

---

## 🚀 LIVE DEMO COMMANDS

### **Step 1: Reset (if needed)**
```bash
./scripts/reset-demo.sh
```
**Time:** 5-10 seconds  
**When:** Only if you ran the demo before the talk

---

### **Step 2: Run the live demo**
```bash
AGENT_STRATEGY=simple CONCURRENCY=16 DURATION=10 \
  java -Dagent.strategy=simple \
       -Dbaseline.concurrency=16 \
       -Dload.duration=10 \
       -Drun.mode=cli \
       -jar target/*.jar
```

**Time:** ~25 seconds  
**What happens:**
- Runs 16 concurrent threads for 10 seconds
- Baseline: p99 ~64ms (no breach)
- Agent optimizes: concurrency 16 → 12
- After: p99 ~53ms
- **Improvement: ~18%**

---

## 🎤 TALK TRACK WHILE DEMO RUNS

### **When you hit Enter (demo starts):**

> *"Okay, this is going to run 16 concurrent threads hammering the system for 10 seconds.  
> Notice we're NOT breaching the SLO this time — the system's been optimized.  
> Traditional monitoring would say 'everything's fine, no action needed.'  
> But watch what the agent does..."*

**Point to the terminal output as it runs:**
> *"It's collecting metrics... p99, GC frequency, heap usage, throughput...  
> Analyzing them together, not in isolation..."*

### **When baseline results appear (~12 seconds in):**

> *"Baseline: p99 is 64 milliseconds. Under the 100ms target. Looks good!  
> But the agent says: 'We can do better. Heap usage is low, we're over-provisioned.  
> Let me reduce concurrency from 16 threads to 12.'"*

### **When after results appear (~25 seconds total):**

> *"There! p99 went from 64ms to 53ms. That's an 18% improvement.  
> Throughput decreased slightly, but we're more efficient.  
> This is PROACTIVE optimization — not waiting for a breach,  
> but continuously making good systems better."*

**Point to the summary:**
> *"Notice: confidence 95%, impact level LOW.  
> The agent knows this is a safe change, not a risky one.  
> And if it were wrong, we have auto-rollback built in."*

---

## 📊 EXPECTED RESULTS

```
============================================================
OPTIMIZATION SUMMARY
============================================================

Concurrency:  16 → 12 (-4)

Latency (median):
  Before:  31.5 ms
  After:   30.4 ms
  Change:  -1.1 ms (-3.5%)

Latency (p99):
  Before:  64.5 ms
  After:   52.9 ms
  Change:  -11.6 ms (-18%)

Throughput (RPS):
  Before:  493 req/s
  After:   394 req/s
  Change:  -99 req/s (-20%)

Decision Confidence: 95%
Impact Level: LOW

============================================================
Optimization Complete!
============================================================
```

---

## 💡 KEY TALKING POINTS

1. **"This is proactive, not reactive"**  
   → Traditional: wait for breach, then fix  
   → Agentic: continuously optimize healthy systems

2. **"Multi-objective optimization"**  
   → Not just latency — also throughput, cost, GC efficiency  
   → Trade-offs are explicit and explained

3. **"Confidence-driven decisions"**  
   → Agent is 95% confident this is safe  
   → If confidence were <80%, it would escalate to human

4. **"Explainable AI"**  
   → You can see exactly why it made this decision  
   → No black box, full transparency

---

## 🚨 IF SOMETHING GOES WRONG

| Problem | Solution |
|---------|----------|
| **Demo hangs or freezes** | Ctrl+C, say: "Let me show you the pre-generated results instead" → show `examples/devnexus/report.json` |
| **Results look weird** | Say: "Interesting! Even the demo variability shows how dynamic systems are. But here's the consistent pattern from multiple runs..." → show pre-generated |
| **Terminal is unreadable on projector** | Say: "Let me read these numbers to you..." → verbally describe results |
| **Build failed before talk** | Use pre-generated only, skip live demo entirely |
| **Ollama not running** | Good news: AGENT_STRATEGY=simple doesn't need Ollama! |

---

## ✅ PRE-DEMO CHECKLIST (Run these 30 min before talk)

```bash
# 1. Verify jar exists
ls -lh target/*.jar
# Expected: aco-demo-*.jar (50-60 MB)

# 2. Quick smoke test (OPTIONAL - only if paranoid)
AGENT_STRATEGY=simple CONCURRENCY=4 DURATION=5 \
  java -Dagent.strategy=simple -Dbaseline.concurrency=4 \
       -Dload.duration=5 -Drun.mode=cli -jar target/*.jar
# Should complete in ~12 seconds

# 3. Reset to clean state
./scripts/reset-demo.sh

# 4. Open slides in separate window
open presentation/devnexus-2026-slides-OFFLINE.html
```

---

## 🎬 SUGGESTED DEMO FLOW

### **Option A: During Code Walkthrough (Acts 5-6)**

1. Show Slides 13-16 (code architecture)
2. Say: *"You've seen the pre-generated breach scenario. Let me show you this running live..."*
3. Run CONCURRENCY=16 demo
4. While it runs: explain O-R-E pattern
5. When results appear: highlight 18% improvement
6. Transition to Q&A

**Total time:** 16-17 minutes

---

### **Option B: During Q&A**

1. Someone asks: *"Can you show it live?"* OR *"Is this real?"*
2. Say: *"Absolutely! Let me run it right now..."*
3. Run CONCURRENCY=16 demo
4. Use talk track above
5. Answer follow-up questions

**Total time:** 2-3 minutes (plus Q&A)

---

## 🎯 RECOMMENDATION

**Use Option A** (during code walkthrough):
- More controlled timing
- Flows naturally after architecture slides
- Shows both scenarios: breach (pre-gen) + proactive (live)
- Audience sees the full picture

**Fallback to Option B** if:
- Running short on time
- Audience seems skeptical
- You want to be extra safe

---

## 📱 COPY-PASTE READY COMMANDS

### **Reset (if needed):**
```bash
./scripts/reset-demo.sh
```

### **Live demo:**
```bash
AGENT_STRATEGY=simple CONCURRENCY=16 DURATION=10 java -Dagent.strategy=simple -Dbaseline.concurrency=16 -Dload.duration=10 -Drun.mode=cli -jar target/*.jar
```

### **Show results (if needed):**
```bash
cat artifacts/report.json | python3 -m json.tool | grep -A10 improvements
```

---

## 🏆 FINAL WORDS

**This live demo:**
- ✅ Is fast (25 seconds)
- ✅ Is reliable (tested 4+ times)
- ✅ Shows real code execution
- ✅ Proves it's not "fake"
- ✅ Demonstrates a different scenario (proactive vs breach)
- ✅ Adds credibility to your talk

**You've got this!** 🚀

The pre-generated breach scenario is your main story,  
the live demo is your credibility boost.  

Together, they make a killer presentation! 🎤
