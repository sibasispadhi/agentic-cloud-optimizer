# Production-Safe Agentic AI: The Observe-Reason-Enforce Pattern for Java Microservices

**Author:** Sibasis Padhi  
**Word Count:** ~3,200 words (excluding code snippets)  
**Target Publication:** InfoQ Java Queue

---

## Key Takeaways

1. The Observe-Reason-Enforce pattern enables safe autonomous operations by separating deterministic observation (SLO breach detection), AI-powered reasoning (LLM advisory), and bounded enforcement (±25% safety constraints), preventing LLM hallucinations from causing production outages.

2. LLMs should be advisors, not executors—Spring Boot orchestrators must maintain deterministic control over production actions while using LLMs to generate bounded recommendations that are validated before execution with automatic rollback on degradation.

3. Tail latency (p99) reveals SLA breaches that median-based monitoring misses, and consecutive window breach detection (N intervals > threshold) prevents false alarms from transient spikes while triggering autonomous optimization for persistent performance degradation.

4. Multi-agent hybrid architectures optimize cost and speed by routing 80% of decisions to fast deterministic rule-based agents (< 1ms, free) and 20% of complex edge cases to LLM agents (~500ms, API cost), with automatic fallback when LLM confidence is low.

5. Closed-loop automation with validation pipelines (execute → measure improvement → rollback if degraded) prevents cascade failures and achieves 30x faster incident resolution (45 min → 90 sec MTTR) compared to manual operations while maintaining zero autonomous-action-related outages through bounded action spaces.

---

## Introduction: The Autonomous Operations Gap

Large language models have transformed code generation, documentation, and development workflows. Yet when organizations attempt to apply the same AI-powered automation to **operations**—auto-scaling, self-healing, performance tuning—they encounter a critical safety gap. The very characteristics that make LLMs powerful for creative tasks (flexibility, adaptability, multi-dimensional reasoning) become liabilities when those models control production infrastructure.

Consider a common scenario: Your payment processing API starts experiencing p99 latency SLA breaches. Traditional approaches require an engineer to analyze metrics (heap usage, GC frequency, thread pool utilization), hypothesize root causes, test parameter changes, and deploy fixes—a process taking 45-60 minutes per incident. An LLM analyzing the same metrics can identify over-provisioning patterns in seconds and recommend specific JVM tuning parameters.

But what happens when you give that LLM direct control?

- **Hallucination Risk:** The LLM recommends `-Xmx-2048m` (negative heap size), crashing your service during peak load.
- **Non-Determinism:** The same metrics produce different recommendations on consecutive runs, making troubleshooting impossible.
- **Unbounded Actions:** The LLM suggests reducing heap by 75%, triggering cascade failures across dependent microservices.

This article presents the **Observe-Reason-Enforce (O-R-E) pattern**, a three-layer architecture that enables safe autonomous operations in production Java microservices. Developed and tested for JVM performance optimization with FinTech-grade SLA requirements (p99 < 100ms), this pattern separates deterministic observation from AI-powered reasoning and bounded enforcement. The result: autonomous systems that achieve 30x faster incident resolution while maintaining zero AI-related outages.

---

## When Autonomous AI Goes Wrong

Before diving into solutions, let's examine three failure modes that emerge when LLMs gain unbounded operational control.

### Failure Mode 1: Hallucinations in Production

LLMs are trained on vast text corpora, including outdated documentation, forum discussions with incorrect advice, and deprecated configuration patterns. When tasked with generating JVM tuning recommendations, an LLM might confidently suggest:

```
-XX:+UseG1GC -XX:MaxGCPauseMillis=50 -Xmx-2048m
```

Three of these flags are valid. The fourth (`-Xmx-2048m`) is syntactically incorrect—Java expects a positive integer. But the LLM, pattern-matching from training data where negative values appear in performance discussions ("reduced latency by -40%"), generates plausible-looking but catastrophic configuration.

**Real Consequence:** If this configuration reaches production, the JVM fails to start. In a Kubernetes environment with rolling deployments, pods restart repeatedly, load balancers mark them unhealthy, traffic shifts to remaining instances (which then overload), and the entire service degrades.

The hallucination problem isn't solvable through better prompting alone. LLMs fundamentally lack the ability to verify their outputs against runtime constraints.

### Failure Mode 2: Non-Deterministic Chaos

Production operations require **reproducibility**. When an incident occurs at 2 AM, the on-call engineer needs to understand exactly what the automation did and why. Non-deterministic systems break this requirement.

Consider an LLM analyzing these metrics:

```
p99 Latency: 145ms (SLA: 100ms)
Heap Usage: 0.6% (53MB of 8GB)
GC Frequency: 0.3/sec
```

Run 1: "Reduce heap to 6GB"  
Run 2: "Reduce heap to 5GB"  
Run 3: "Reduce heap to 7GB"

All three recommendations are plausible. But if your automation applies different changes each time the same conditions occur:

- A/B testing becomes invalid (the change itself varies)
- Rollback logic can't determine which parameters to revert
- Post-incident reviews can't establish causality

Production systems tolerate failures—they don't tolerate **unexplainable** failures.

### Failure Mode 3: Unbounded Actions

LLMs lack intrinsic safety constraints. Asked to "optimize performance," an LLM might recommend:

```
Recommendation: Reduce heap from 8GB to 2GB (-75%)
Reasoning: Current usage is only 53MB
```

The reasoning is directionally correct—over-provisioned heap does increase GC pause times. But a 75% reduction ignores:

- **Traffic variability:** Current 53MB reflects off-peak load. Peak might be 1.5GB.
- **Memory fragmentation:** Heap must accommodate allocation patterns during request spikes.
- **Dependency effects:** Other services might depend on this service's buffer capacity during failures.

Without bounded action spaces, LLMs optimize for narrow metrics while ignoring systemic constraints they cannot perceive.

---

## The Observe-Reason-Enforce Pattern

The O-R-E pattern addresses these failure modes through architectural separation of concerns:

```
┌──────────────────────────────────────────────┐
│        OBSERVE LAYER (Deterministic)         │
│  - SLO breach detection (consecutive windows)│
│  - JVM metrics collection (p99, heap, GC)    │
│  - Percentile calculation (p99/p95/median)   │
└────────────────┬─────────────────────────────┘
                 │ Structured Metrics
                 ↓
┌──────────────────────────────────────────────┐
│       REASON LAYER (AI-Powered Advisory)     │
│  - Multi-dimensional pattern recognition     │
│  - Root cause hypothesis generation          │
│  - Bounded recommendation proposal           │
└────────────────┬─────────────────────────────┘
                 │ Recommendation + Confidence
                 ↓
┌──────────────────────────────────────────────┐
│      ENFORCE LAYER (Bounded Execution)       │
│  - ±25% parameter change limits              │
│  - Forbidden operation filtering             │
│  - Pre-execution validation                  │
│  - Post-execution measurement & rollback     │
└──────────────────────────────────────────────┘
```

**Figure 1:** The O-R-E architecture separates deterministic observation, AI-powered reasoning, and bounded enforcement. Each layer is independently testable and replaceable.

### Layer 1: Observe (Deterministic)

The observation layer collects metrics and detects SLO breaches using **deterministic logic**. This ensures triggers are reproducible and auditable.

**Key Design Decision:** Focus on **tail latency (p99)** rather than median. Services can have "healthy" median latency (30ms) while simultaneously breaching p99 SLAs (150ms). Traditional monitoring misses these breaches.

**Consecutive Window Breach Detection:**

```java
public boolean isBreached(SloPolicy policy) {
    int requiredWindow = policy.getBreachWindowIntervals(); // e.g., 3
    
    if (recentMeasurements.size() < requiredWindow) {
        return false;
    }
    
    // Check if ALL of last N measurements exceed threshold
    for (RunResult result : getLastNMeasurements(requiredWindow)) {
        if (!policy.isP99Breached(result.getP99LatencyMs())) {
            return false; // One good measurement breaks the window
        }
    }
    
    return true; // Sustained breach detected
}
```

**Why This Matters:** A single p99 spike might be transient (GC pause, network blip). Requiring **3 consecutive breaches** ensures we detect sustained degradation, not noise.

**Percentile Calculation:**

```java
public double getP99LatencyMs() {
    Collections.sort(latencySamples);
    int p99Index = (int) Math.ceil(latencySamples.size() * 0.99) - 1;
    return latencySamples.get(p99Index);
}
```

For 100 requests, p99 returns the 99th slowest. This reveals tail latency that averages hide.

### Layer 2: Reason (AI-Powered Advisory)

When the observation layer confirms an SLO breach, the reasoning layer activates. The LLM analyzes metrics and proposes remediation—but critically, **does not execute**.

**LLM as Advisory Layer:**

```java
public Recommendation analyze(RunResult baseline, WorkloadConfig current) {
    String prompt = buildStructuredPrompt(baseline, current);
    
    ChatResponse response = chatClient.prompt()
        .user(prompt)
        .call()
        .chatResponse();
    
    Recommendation parsed = parseRecommendation(response.getContent());
    
    // CRITICAL: Validate before returning
    if (!enforcer.isValid(parsed, current)) {
        return Recommendation.noAction("Failed safety constraints");
    }
    
    return parsed; // Advisory only - not executed yet
}
```

**Key Pattern:** The method returns a `Recommendation` object, not a `void` execution result. The LLM proposes; the enforce layer decides.

**Structured Prompting:**

The prompt embeds safety constraints:

```text
CONSTRAINTS:
- Changes must be within ±25% of current values
- Heap: MIN 4GB, MAX 12GB
- Threads: ±2 maximum change
- Respond: HEAP_MB=<value> THREADS=<value> REASONING=<explanation>
```

This increases (but doesn't guarantee) valid recommendations. The enforcer validates regardless.

### Layer 3: Enforce (Bounded Execution)

The enforcement layer validates recommendations, executes changes, measures results, and rolls back if performance degrades.

**Bounded Action Space Validation:**

```java
public boolean isValid(Recommendation rec, WorkloadConfig current) {
    // Absolute bounds
    if (rec.getHeapSizeMb() < MIN_HEAP_MB || rec.getHeapSizeMb() > MAX_HEAP_MB) {
        return false;
    }
    
    // ±25% change limit
    double changePercent = Math.abs(
        (rec.getHeapSizeMb() - current.getHeapSizeMb()) / 
        (double) current.getHeapSizeMb()
    );
    
    if (changePercent > 0.25) {
        return false;
    }
    
    return true;
}
```

**Example:** If current heap is 8GB:
- ✅ Valid: 6GB to 10GB (±25%)
- ❌ Invalid: 4GB (-50%), 11GB (+37.5%)

**Rollback Detection:**

```java
public boolean shouldRollback(RunResult before, RunResult after) {
    double degradation = (after.getP99LatencyMs() - before.getP99LatencyMs()) /
                        before.getP99LatencyMs();
    
    return degradation > 0.10; // Rollback if p99 worsens by >10%
}
```

---

## Production Implementation

The O-R-E pattern coordinates through an orchestrator managing the closed-loop workflow:

**O-R-E Orchestration:**

```java
public OptimizationResult optimize(WorkloadConfig config, SloPolicy policy) {
    // PHASE 1: OBSERVE
    RunResult baseline = runner.execute(config);
    if (!detector.isBreached(policy)) {
        return OptimizationResult.noActionNeeded();
    }
    
    // PHASE 2: REASON
    OptimizationAgent agent = selectAgent(baseline, config);
    Recommendation recommendation = agent.analyze(baseline, config);
    
    // PHASE 3: ENFORCE
    WorkloadConfig optimized = recommendation.apply(config);
    RunResult afterOptimization = runner.execute(optimized);
    
    // PHASE 4: VALIDATE & ROLLBACK IF NEEDED
    if (enforcer.shouldRollback(baseline, afterOptimization)) {
        return OptimizationResult.rolledBack();
    }
    
    return OptimizationResult.success(baseline, afterOptimization);
}
```

Four phases: **Observe** (detect breach) → **Reason** (get recommendation) → **Enforce** (apply change) → **Validate** (measure and rollback if degraded).

### Multi-Agent Hybrid Architecture

The orchestrator routes between deterministic and LLM-based agents:

**SimpleAgent (Deterministic):**
- Handles obvious cases (heap usage < 10% → reduce heap)
- Executes in < 1ms, zero API cost
- Handles ~80% of scenarios

**SpringAiLlmAgent (AI-Powered):**
- Handles complex multi-dimensional patterns
- Executes in ~500ms, API cost per call
- Handles ~20% of edge cases

**Routing Logic:**

```java
private OptimizationAgent selectAgent(RunResult baseline, WorkloadConfig config) {
    double heapUsagePercent = baseline.getHeapUsedMb() / 
                             (double) config.getHeapSizeMb();
    
    if (heapUsagePercent < 0.10) {
        return simpleAgent; // Fast, deterministic
    }
    
    return llmAgent; // Complex scenario
}
```

This hybrid optimizes for **speed** (most cases instant) and **capability** (complex cases get intelligent analysis).

---

## Case Study: Autonomous JVM Tuning

We tested the O-R-E pattern with a simulated payment processing API (p99 < 100ms SLA requirement).

### Test Scenario: Over-Provisioned Heap

**Initial Configuration:**
- Heap: 8GB
- Threads: 4
- Workload: Simulated transactions (10-50ms CPU-bound operations)

**Problem:** p99 latency 150ms—breaching 100ms SLA despite "adequate" resources.

**Root Cause:** Over-provisioned heap (8GB) with minimal usage (53MB, 0.6%). Large heap triggered infrequent but lengthy GC pauses.

### Autonomous Optimization Workflow

**1. Detection (Observe Layer):**

```
Interval 1: p99 = 148.3ms [BREACH]
Interval 2: p99 = 150.1ms [BREACH]
Interval 3: p99 = 149.7ms [BREACH]
→ Sustained breach detected
```

**2. Analysis (Reason Layer):**

```
LLM Recommendation:
HEAP_MB=6144 THREADS=3
REASONING=Heap usage 0.6% indicates severe over-provisioning.
Reducing to 6GB (25% reduction) decreases GC pause times.
```

**3. Validation (Enforce Layer):**

```
✓ Heap 6GB: Within [4GB, 12GB] bounds
✓ Change 8GB→6GB: 25% (exactly at limit)
✓ Threads 4→3: Delta 1 (< 2 limit)
→ Approved for execution
```

**4. Results:**

```
Before: p99 = 150.3ms, heap = 53MB/8GB
After:  p99 = 62.4ms, heap = 51MB/6GB
Improvement: 58.5%
```

GC pauses reduced from ~45ms to ~15ms, restoring SLA compliance.

### Reproducibility Validation

| Run | Baseline p99 | Optimized p99 | Improvement |
|-----|--------------|---------------|--------------|
| 1   | 150.3ms      | 62.4ms        | 58.5%        |
| 2   | 148.7ms      | 64.1ms        | 56.9%        |
| 3   | 151.2ms      | 61.8ms        | 59.1%        |

**Observation:** Expected variability (±2ms) but consistent improvement. Structured prompting + validation increased LLM determinism.

### Impact Metrics

**Manual Tuning:**
- Analyze + hypothesize + test + deploy: **45-60 min MTTR**

**Autonomous O-R-E:**
- Detect + analyze + validate + execute: **~90 sec MTTR**
- **30x faster incident resolution**

**Infrastructure Cost:**
- Baseline: 8GB heap
- Optimized: 6GB heap
- **24% resource reduction** (compounds across 20+ service instances)

### Rollback Validation

We tested rollback by forcing a bad recommendation:

```
Test: Force heap to 4GB (minimum)
Result: p99 degraded 62ms → 78ms (25%)
Action: Automatic rollback triggered
Recovery: 90 seconds
```

Closed-loop validation prevents persisting bad changes.

---

## When to Use This Pattern

### ✅ Ideal Use Cases

**Auto-Scaling Triggers:**  
LLMs analyze multi-dimensional signals (latency + error rate + queue depth) to recommend scaling. Enforce layer ensures budget/availability constraints.

**Self-Healing Systems:**  
LLMs correlate symptoms across logs/metrics/traces to recommend remediation (restart, clear cache, reroute traffic). Bounded actions prevent worse failures.

**Performance Optimization:**  
JVM tuning, database pools, cache policies—scenarios where multiple parameters interact non-obviously. LLMs excel at multi-dimensional optimization.

### ❌ Anti-Patterns

**Safety-Critical Without Rollback:**  
If you can't rollback (database migrations, financial transactions), limit LLMs to advisory-only with human approval.

**High-Frequency Decisions:**  
LLM inference (~500ms) is too slow for sub-second decisions. Use deterministic rules; reserve LLMs for infrequent complex scenarios.

**Regulatory Compliance:**  
Some industries require human-in-the-loop. Add approval gates before enforcement.

### Design Checklist

- [ ] Can you rollback changes if performance degrades?
- [ ] Can you measure improvement deterministically?
- [ ] Can you define safe parameter ranges?
- [ ] Is 500ms-1sec analysis time tolerable?
- [ ] Do benefits outweigh LLM API costs?

If any answer is "no," use observe + reason layers for insights but keep enforcement manual.

---

## Conclusion

Autonomous operations powered by LLMs represent a significant evolution in cloud infrastructure management. But the flexibility that makes LLMs powerful for code generation becomes a liability when those models control production systems. Hallucinations cause outages. Non-determinism breaks debugging. Unbounded actions trigger cascade failures.

The Observe-Reason-Enforce pattern addresses these challenges through architectural separation:

1. **Deterministic observation** ensures triggers are reproducible and auditable
2. **AI-powered reasoning** leverages LLM multi-dimensional analysis
3. **Bounded enforcement** validates recommendations and rolls back failures

This architecture transforms LLMs from unpredictable executors into valuable advisors. Our testing with FinTech-grade SLA requirements demonstrated:

- **58% improvement** in p99 latency (150ms → 62ms)
- **24% reduction** in infrastructure costs through right-sizing
- **30x faster incident resolution** (45 min → 90 sec MTTR)
- **100% rollback success** rate when optimizations degraded performance

The patterns presented—consecutive window breach detection, bounded action spaces, multi-agent hybrid routing, closed-loop validation—are reusable beyond JVM tuning. Apply them to auto-scaling, self-healing, database optimization, or any scenario requiring AI-powered automation without sacrificing production safety.

The full implementation, including Spring Boot 3.x integration with Spring AI, is available at: **https://github.com/sibasispadhi/agentic-cloud-optimizer**

The repository includes Docker Compose setup for local reproduction, comprehensive test suite, and WebSocket-based real-time dashboard.

As LLMs become more powerful, the temptation to give them direct operational control will grow. Resist that temptation. Use the O-R-E pattern to harness their analytical capabilities while maintaining the deterministic safety that production systems require.

---

## Author Bio

Sibasis Padhi is a Staff Software Engineer at Walmart Global Tech specializing in Agentic AI for cloud-native microservices and cloud performance optimization for high-volume FinTech transaction systems. With 18+ years in enterprise software engineering, he focuses on measurable reliability (SLOs, p95/p99 latency), distributed observability (logs/metrics/traces), and governance guardrails that keep autonomous or AI-assisted operations safe, auditable, and resilient under real production load. His work includes microservices architecture, performance engineering, and security-aware operational controls to improve throughput, cost efficiency, and failure recovery in large-scale distributed systems.

---

**END OF ARTICLE**

**Metadata:**
- Word Count: ~3,200 words (excluding code snippets)
- Code Examples: 6 focused snippets (10-20 lines each)
- Diagrams: 1 (O-R-E architecture)
- Tables: 1 (Reproducibility validation)
- Full implementation: https://github.com/sibasispadhi/agentic-cloud-optimizer
