# Agentic AI Architecture Patterns for Java Microservices

**Author:** Sibasis Padhi  
**Context:** Architecture patterns distilled from building ACO and related JVM optimization workflows  
**Last Updated:** February 18, 2026  

---

## 🎯 Purpose

This document presents **reusable architecture patterns** for integrating agentic AI into Java microservices. These patterns are distilled from production experience building self-optimizing systems for FinTech applications processing millions of transactions per day.

**Target Audience:** Java developers, architects, and SREs building autonomous systems  
**Reference Implementation:** [Agentic Cloud Optimizer (ACO)](https://github.com/sibasispadhi/agentic-cloud-optimizer)  
**Applicability:** Any Java microservice requiring autonomous optimization, self-healing, or intelligent automation

---

## 📋 Pattern Catalog

1. **[Observe-Reason-Enforce (O-R-E) Pattern](#1-observe-reason-enforce-o-r-e-pattern)** - Core agentic architecture
2. **[Bounded Action Space Pattern](#2-bounded-action-space-pattern)** - Safety constraints for autonomous agents
3. **[LLM as Advisor Pattern](#3-llm-as-advisor-pattern)** - Using LLMs for reasoning, not decision-making
4. **[Closed-Loop Automation Pattern](#4-closed-loop-automation-pattern)** - Self-healing feedback loops
5. **[Multi-Agent Reasoning Pattern](#5-multi-agent-reasoning-pattern)** - Hybrid deterministic + LLM agents

---

## 1. Observe-Reason-Enforce (O-R-E) Pattern

### Problem

Traditional monitoring systems detect problems but require human intervention to resolve them. In high-scale FinTech systems processing millions of transactions, manual intervention is too slow and error-prone.

**Pain Points:**
- Alerts fire → Engineer gets paged → Manual analysis → Manual fix
- Mean time to recovery (MTTR) measured in minutes/hours
- Human errors during high-pressure incidents
- Scalability bottleneck: humans can't respond at machine speed

### Solution

Implement a three-layer architecture separating **observation** (deterministic), **reasoning** (AI-powered), and **enforcement** (bounded actions).

```
┌─────────────────────────────────────────────────┐
│  LAYER 1: OBSERVE (Deterministic)               │
│  • Collect metrics (p95/p99 latency, heap, GC)  │
│  • Detect SLO breaches (threshold-based)        │
│  • Trigger optimization when needed             │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│  LAYER 2: REASON (AI-Powered)                   │
│  • Analyze metrics with LLM                     │
│  • Generate bounded recommendations             │
│  • Explain reasoning for auditability           │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│  LAYER 3: ENFORCE (Bounded Execution)           │
│  • Apply safety constraints (±25% max delta)    │
│  • Execute approved changes                     │
│  • Validate results, rollback if needed         │
└─────────────────────────────────────────────────┘
```

### Java Implementation

**Layer 1: Observe (Deterministic)**
```java
@Service
public class SloBreachDetector {
    private final LinkedList<RunResult> recentMeasurements = new LinkedList<>();
    
    public boolean isBreached(SloPolicy policy) {
        int requiredWindow = policy.getBreachWindowIntervals();
        
        if (recentMeasurements.size() < requiredWindow) {
            return false; // Insufficient data
        }
        
        // Check last N consecutive measurements
        List<RunResult> window = getLastNMeasurements(requiredWindow);
        
        // All intervals must breach to trigger (prevents false alarms)
        for (RunResult result : window) {
            if (!policy.isP99Breached(result.getP99LatencyMs())) {
                return false; // Window broken
            }
        }
        
        return true; // Consecutive breach detected!
    }
}
```

**Layer 2: Reason (AI-Powered)**
```java
@Service
public class SpringAiLlmAgent {
    private final ChatClient chatClient;
    
    public AgentDecision analyzeAndRecommend(RunResult baseline, SloPolicy policy) {
        // Build context-rich prompt
        String prompt = LlmPromptBuilder.buildPrompt(
            baseline,
            policy,
            "Analyze metrics and recommend JVM tuning to restore SLA"
        );
        
        // LLM generates recommendation with reasoning
        String response = chatClient.call(prompt);
        
        // Parse structured response
        AgentDecision decision = LlmResponseParser.parse(response);
        
        // LLM provides recommendation, NOT direct execution
        return decision;
    }
}
```

**Layer 3: Enforce (Bounded Execution)**
```java
@Service
public class OptimizationOrchestrator {
    private final SloBreachDetector detector;
    private final SpringAiLlmAgent agent;
    private final LoadRunner loadRunner;
    
    public void optimizeIfNeeded() {
        RunResult baseline = loadRunner.runLoad(10, 4, 0);
        
        // OBSERVE: Check SLO breach
        if (detector.isBreached(sloPolicy)) {
            log.warn("SLO BREACH: {}", detector.getBreachReason());
            
            // REASON: Ask agent for recommendation
            AgentDecision decision = agent.analyzeAndRecommend(baseline, sloPolicy);
            
            // ENFORCE: Apply with safety constraints
            if (isSafeToApply(decision, baseline)) {
                RunResult after = applyRecommendation(decision);
                
                // Validate improvement
                if (after.getP99LatencyMs() > baseline.getP99LatencyMs()) {
                    rollback(); // Safety net!
                }
            }
        }
    }
    
    private boolean isSafeToApply(AgentDecision decision, RunResult baseline) {
        // Bounded action space: max ±25% parameter changes
        int recommendedConcurrency = parseConcurrency(decision.getRecommendation());
        int delta = Math.abs(recommendedConcurrency - baseline.getConcurrency());
        double deltaPct = (delta / (double) baseline.getConcurrency()) * 100;
        
        return deltaPct <= 25.0; // Safety constraint
    }
}
```

### Benefits

✅ **Separation of Concerns:**
- Observation logic is deterministic (testable, predictable)
- Reasoning is AI-powered (adaptable, context-aware)
- Enforcement has safety rails (prevents catastrophic failures)

✅ **Auditability:**
- Each layer logs its decisions
- LLM reasoning is captured and explainable
- Rollback capability ensures safety

✅ **Scalability:**
- Agents respond at machine speed (seconds, not minutes)
- No human bottleneck for routine optimizations
- Humans focus on policy-setting, not execution

### When to Use

- ✅ Self-optimizing microservices (JVM tuning, scaling, caching)
- ✅ Self-healing systems (auto-restart, circuit breaker tuning)
- ✅ Anomaly response (traffic spikes, performance degradation)
- ❌ Safety-critical systems without rollback capability
- ❌ Systems requiring perfect accuracy (use deterministic rules)

---

## 2. Bounded Action Space Pattern

### Problem

LLMs can generate recommendations that are:
- **Too aggressive:** -75% heap size → OutOfMemoryError
- **Too conservative:** +2% concurrency → no meaningful impact
- **Nonsensical:** "Set threads to -5" (invalid configuration)

**Real Example from Production:**
```
LLM Recommendation: "Reduce heap from 8GB to 2GB" (-75%)
Result: Immediate OutOfMemoryError, service crash
Impact: 15-minute outage during peak trading hours
```

### Solution

Define explicit bounds for all agent actions BEFORE giving the agent autonomy. Treat the LLM as an advisor, but enforce constraints deterministically.

### Java Implementation

**Define Action Bounds:**
```java
public class ActionBounds {
    // JVM parameter constraints
    public static final double MAX_HEAP_DELTA_PCT = 25.0;     // ±25% max
    public static final int MIN_HEAP_MB = 512;                // Absolute floor
    public static final int MAX_HEAP_MB = 16384;              // Absolute ceiling
    
    // Concurrency constraints
    public static final double MAX_CONCURRENCY_DELTA_PCT = 25.0;
    public static final int MIN_CONCURRENCY = 1;
    public static final int MAX_CONCURRENCY = 32;
    
    // Timing constraints
    public static final long COOLDOWN_MS = 600_000;           // 10 minutes between changes
    
    // Forbidden parameters (never touch)
    public static final Set<String> FORBIDDEN_PARAMS = Set.of(
        "XX:+UseG1GC",      // Don't change GC algorithm
        "Xss",              // Don't change stack size
        "XX:MaxPermSize"    // Deprecated, dangerous
    );
}
```

**Enforcement Logic:**
```java
@Service
public class SafetyConstraintEnforcer {
    private Instant lastChangeTime = Instant.EPOCH;
    
    public ValidationResult validate(AgentDecision decision, RunResult baseline) {
        List<String> violations = new ArrayList<>();
        
        // 1. Cooldown check
        if (Duration.between(lastChangeTime, Instant.now()).toMillis() < ActionBounds.COOLDOWN_MS) {
            violations.add("Cooldown period not elapsed (10 min required)");
        }
        
        // 2. Heap delta check
        if (decision.getRecommendedHeapSizeMb() != null) {
            int currentHeap = baseline.getHeapMetrics().getHeapSizeMb();
            int recommendedHeap = decision.getRecommendedHeapSizeMb();
            double deltaPct = Math.abs(((recommendedHeap - currentHeap) / (double) currentHeap) * 100);
            
            if (deltaPct > ActionBounds.MAX_HEAP_DELTA_PCT) {
                violations.add(String.format(
                    "Heap delta %.1f%% exceeds max %.1f%%", 
                    deltaPct, ActionBounds.MAX_HEAP_DELTA_PCT
                ));
            }
            
            if (recommendedHeap < ActionBounds.MIN_HEAP_MB || 
                recommendedHeap > ActionBounds.MAX_HEAP_MB) {
                violations.add(String.format(
                    "Heap %dMB outside bounds [%d, %d]",
                    recommendedHeap, ActionBounds.MIN_HEAP_MB, ActionBounds.MAX_HEAP_MB
                ));
            }
        }
        
        // 3. Forbidden parameter check
        for (String param : ActionBounds.FORBIDDEN_PARAMS) {
            if (decision.getRecommendation().contains(param)) {
                violations.add("Recommendation touches forbidden parameter: " + param);
            }
        }
        
        return new ValidationResult(violations.isEmpty(), violations);
    }
    
    public void recordChange() {
        lastChangeTime = Instant.now();
    }
}
```

**Integration with Agent:**
```java
public AgentDecision generateSafeDecision(RunResult baseline) {
    // LLM generates initial recommendation
    AgentDecision rawDecision = llmAgent.analyzeAndRecommend(baseline);
    
    // Enforce safety constraints
    ValidationResult validation = safetyEnforcer.validate(rawDecision, baseline);
    
    if (!validation.isValid()) {
        log.warn("Agent recommendation REJECTED: {}", validation.getViolations());
        
        // Fallback to conservative rule-based decision
        return simpleAgent.makeDecision(baseline);
    }
    
    log.info("Agent recommendation APPROVED: {}", rawDecision.getRecommendation());
    return rawDecision;
}
```

### Benefits

✅ **Prevents Catastrophic Failures:**
- LLM can't crash your service with extreme recommendations
- Absolute bounds prevent invalid configurations
- Cooldown prevents thrashing

✅ **Gradual Improvement:**
- Small, incremental changes (±25%) are safer
- Multiple iterations can achieve large improvements
- Easy to rollback small changes

✅ **Production-Ready:**
- Deterministic safety checks (no AI unpredictability)
- Clear audit trail of rejected recommendations
- Forbidden parameters protect critical settings

### When to Use

- ✅ **ALWAYS** when using LLMs for autonomous actions
- ✅ Production systems with strict SLAs
- ✅ Systems where downtime is expensive
- ❌ Experimental/sandbox environments (constraints can be relaxed)

---

## 3. LLM as Advisor Pattern

### Problem

LLMs are powerful but unpredictable. Giving them direct control over production systems is dangerous:

**Failure Modes:**
- Hallucinations: LLM invents metrics that don't exist
- Non-determinism: Same input → different outputs
- Context drift: Forgets constraints midway through reasoning
- Prompt injection: Malicious input manipulates LLM behavior

### Solution

**LLMs should ADVISE, not DECIDE.**

Structure your system so LLMs generate recommendations, but deterministic code makes final decisions and executes actions.

```
┌─────────────────────────────────────────────────┐
│  DETERMINISTIC ORCHESTRATOR (Decision Maker)    │
│  • Collects metrics                             │
│  • Asks LLM for advice                          │
│  • Validates advice                             │
│  • Makes final decision                         │
│  • Executes action                              │
└─────────────────────────────────────────────────┘
                     ↓ (advice only)
┌─────────────────────────────────────────────────┐
│  LLM AGENT (Advisor)                            │
│  • Analyzes metrics                             │
│  • Generates recommendation                     │
│  • Explains reasoning                           │
│  • Returns structured response                  │
└─────────────────────────────────────────────────┘
```

### Java Implementation

**LLM as Advisor (Advisory Only):**
```java
@Service
public class SpringAiLlmAgent {
    private final ChatClient chatClient;
    
    /**
     * LLM generates ADVICE, not actions.
     * Returns structured recommendation for orchestrator to evaluate.
     */
    public AgentDecision advise(RunResult baseline, SloPolicy sloPolicy, String context) {
        String prompt = String.format("""
            You are a JVM performance advisor for a FinTech transaction processing system.
            
            CURRENT METRICS:
            - p99 latency: %.2fms (target: %.2fms)
            - Heap usage: %dMB / %dMB (%.1f%%)
            - GC frequency: %.2f pauses/sec
            - Concurrency: %d threads
            
            CONSTRAINTS:
            - Max parameter change: ±25%%
            - Target p99: < %.2fms
            - Forbidden: Do not change GC algorithm
            
            TASK: Analyze the metrics and recommend ONE bounded change to improve p99 latency.
            
            Respond in this EXACT JSON format:
            {
              "recommendation": "<specific action>",
              "reasoning": "<why this will help>",
              "confidence": <0.0-1.0>,
              "estimated_improvement_pct": <number>
            }
            """,
            baseline.getP99LatencyMs(),
            sloPolicy.getTargetP99Ms(),
            baseline.getHeapMetrics().getHeapUsedMb(),
            baseline.getHeapMetrics().getHeapSizeMb(),
            baseline.getHeapMetrics().getHeapUsagePercent(),
            baseline.getHeapMetrics().getGcFrequencyPerSec(),
            baseline.getConcurrency(),
            sloPolicy.getTargetP99Ms()
        );
        
        // LLM generates ADVICE
        String response = chatClient.call(prompt);
        
        // Parse structured response
        AgentDecision advice = LlmResponseParser.parse(response);
        
        log.info("LLM ADVICE: {} (confidence: {})", 
                advice.getRecommendation(), advice.getConfidenceScore());
        
        return advice; // Returns ADVICE, not executed action
    }
}
```

**Orchestrator as Decision Maker:**
```java
@Service
public class OptimizationOrchestrator {
    private final SpringAiLlmAgent llmAdvisor;
    private final SimpleAgent ruleBasedAgent;
    private final SafetyConstraintEnforcer safetyEnforcer;
    
    public AgentDecision makeDecision(RunResult baseline, SloPolicy sloPolicy) {
        // Step 1: Ask LLM for ADVICE
        AgentDecision llmAdvice = llmAdvisor.advise(baseline, sloPolicy, "optimize");
        
        // Step 2: ORCHESTRATOR validates advice
        ValidationResult validation = safetyEnforcer.validate(llmAdvice, baseline);
        
        if (!validation.isValid()) {
            log.warn("LLM advice rejected: {}", validation.getViolations());
            
            // Step 3: Fallback to deterministic agent
            return ruleBasedAgent.makeDecision(baseline, sloPolicy);
        }
        
        // Step 4: ORCHESTRATOR makes final decision
        if (llmAdvice.getConfidenceScore() < 0.7) {
            log.warn("LLM confidence too low ({}), using rule-based agent", 
                    llmAdvice.getConfidenceScore());
            return ruleBasedAgent.makeDecision(baseline, sloPolicy);
        }
        
        log.info("DECISION: Accepting LLM advice");
        return llmAdvice;
    }
    
    public RunResult executeDecision(AgentDecision decision) {
        // ORCHESTRATOR executes, not LLM
        int newConcurrency = parseConcurrency(decision.getRecommendation());
        return loadRunner.runLoad(10, newConcurrency, 0);
    }
}
```

### Benefits

✅ **Safety:**
- LLM cannot directly execute actions
- Deterministic code makes final decisions
- Fallback to rule-based agent if LLM fails

✅ **Predictability:**
- Orchestrator logic is testable and deterministic
- LLM advice is logged but not binding
- Confidence thresholds add another safety layer

✅ **Best of Both Worlds:**
- LLM provides intelligent analysis
- Deterministic code ensures safety
- Hybrid approach balances innovation and reliability

### When to Use

- ✅ Production systems requiring high reliability
- ✅ Regulated environments (FinTech, healthcare) requiring audit trails
- ✅ Any autonomous system using LLMs
- ❌ Experimental systems where LLM errors are acceptable

---

## 4. Closed-Loop Automation Pattern

### Problem

Open-loop automation:
```
Detect problem → Execute fix → Hope it worked 🤞
```

This is dangerous because:
- No validation that fix actually worked
- No rollback if fix makes things worse
- No learning from successes/failures

### Solution

Implement closed-loop automation with validation and rollback:

```
┌─────────────────────────────────────────────────┐
│  1. DETECT: Monitor metrics, detect SLO breach │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│  2. ANALYZE: LLM analyzes root cause           │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│  3. ACT: Apply bounded recommendation           │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│  4. VALIDATE: Re-measure metrics                │
│     • If improved → Success! ✅                 │
│     • If degraded → ROLLBACK! ⚠️                │
└─────────────────────────────────────────────────┘
                     ↓
         ┌───────────┴────────────┐
         ↓                        ↓
     SUCCESS                  ROLLBACK
   (Learn & Log)          (Revert & Log)
```

### Java Implementation

```java
@Service
public class ClosedLoopOptimizer {
    private final SloBreachDetector detector;
    private final SpringAiLlmAgent llmAgent;
    private final LoadRunner loadRunner;
    
    private RunResult lastKnownGood;
    
    public OptimizationResult optimize() {
        // STEP 1: DETECT - Collect baseline metrics
        RunResult baseline = loadRunner.runLoad(10, 4, 0);
        lastKnownGood = baseline;
        
        if (!detector.isBreached(sloPolicy)) {
            return OptimizationResult.noActionNeeded(baseline);
        }
        
        log.warn("SLO BREACH DETECTED: {}", detector.getBreachReason());
        
        // STEP 2: ANALYZE - Get LLM recommendation
        AgentDecision decision = llmAgent.analyzeAndRecommend(baseline, sloPolicy);
        
        // STEP 3: ACT - Apply recommendation
        log.info("APPLYING: {}", decision.getRecommendation());
        int newConcurrency = parseConcurrency(decision.getRecommendation());
        RunResult after = loadRunner.runLoad(10, newConcurrency, 0);
        
        // STEP 4: VALIDATE - Check if improvement occurred
        boolean improved = validateImprovement(baseline, after);
        
        if (improved) {
            log.info("✅ OPTIMIZATION SUCCESSFUL: p99 improved by {:.1f}%",
                    calculateImprovementPct(baseline.getP99LatencyMs(), after.getP99LatencyMs()));
            lastKnownGood = after;
            return OptimizationResult.success(baseline, after, decision);
        } else {
            log.warn("⚠️  OPTIMIZATION FAILED: p99 degraded, ROLLING BACK");
            rollback();
            return OptimizationResult.failure(baseline, after, decision);
        }
    }
    
    private boolean validateImprovement(RunResult baseline, RunResult after) {
        // Primary metric: p99 must improve (not degrade)
        boolean p99Improved = after.getP99LatencyMs() < baseline.getP99LatencyMs();
        
        // Secondary metric: Must not cause >10% degradation in throughput
        double throughputDelta = ((after.getRequestsPerSecond() - baseline.getRequestsPerSecond()) 
                                  / baseline.getRequestsPerSecond()) * 100;
        boolean throughputAcceptable = throughputDelta > -10.0;
        
        // Tertiary metric: Must restore SLO compliance
        boolean sloRestored = after.getP99LatencyMs() < sloPolicy.getTargetP99Ms();
        
        log.info("VALIDATION: p99Improved={}, throughputOK={}, sloRestored={}",
                p99Improved, throughputAcceptable, sloRestored);
        
        return p99Improved && throughputAcceptable;
    }
    
    private void rollback() {
        log.warn("ROLLBACK: Reverting to last known good configuration");
        
        // Revert to last known good concurrency
        int safeConurrency = lastKnownGood.getConcurrency();
        loadRunner.runLoad(10, safeConcurrency, 0);
        
        // Log rollback event for analysis
        auditLog.recordRollback(lastKnownGood, "Optimization degraded performance");
    }
}
```

### Benefits

✅ **Self-Healing:**
- System automatically reverts bad changes
- No human intervention needed for rollback
- Prevents cascading failures

✅ **Learning:**
- Success/failure data informs future optimizations
- Audit trail shows what worked and what didn't
- Can feed back into LLM training

✅ **Risk Mitigation:**
- Validation step catches degradations
- Rollback ensures service availability
- Multi-metric validation prevents gaming single metrics

### When to Use

- ✅ **ALWAYS** for production autonomous systems
- ✅ High-availability services (99.9%+ SLA)
- ✅ Systems where downtime is expensive
- ❌ Batch jobs where rollback doesn't make sense

---

## 5. Multi-Agent Reasoning Pattern

### Problem

Single-agent systems have limitations:

**Pure Rule-Based Agent:**
- ✅ Fast, deterministic, predictable
- ❌ Cannot adapt to novel situations
- ❌ Brittle when assumptions change

**Pure LLM Agent:**
- ✅ Adapts to novel situations
- ✅ Considers complex context
- ❌ Slow (API latency)
- ❌ Unpredictable, can hallucinate
- ❌ Expensive (API costs)

### Solution

**Hybrid multi-agent architecture:**

```
                  ┌─────────────────────┐
                  │  ORCHESTRATOR       │
                  │  (Decision Router)  │
                  └──────────┬──────────┘
                             │
              ┌──────────────┴──────────────┐
              ↓                             ↓
    ┌──────────────────┐          ┌──────────────────┐
    │  SIMPLE AGENT    │          │  LLM AGENT       │
    │  (Rule-Based)    │          │  (AI-Powered)    │
    └──────────────────┘          └──────────────────┘
    • Fast (<1ms)                 • Slow (~500ms)
    • Deterministic               • Adaptive
    • Handles 80% of cases        • Handles edge cases
    • Free                        • Costs $$$
```

**Routing Logic:**
```
IF situation is common/well-understood:
    → Use Simple Agent (fast, cheap, reliable)
ELSE IF situation is novel/complex:
    → Use LLM Agent (adaptive, context-aware)
ELSE IF LLM fails validation:
    → Fallback to Simple Agent
```

### Java Implementation

**Simple Agent (Rule-Based):**
```java
@Service
public class SimpleAgent {
    
    public AgentDecision makeDecision(RunResult baseline, SloPolicy sloPolicy) {
        double p99 = baseline.getP99LatencyMs();
        double target = sloPolicy.getTargetP99Ms();
        int currentConcurrency = baseline.getConcurrency();
        
        // RULE 1: p99 way over target → Reduce concurrency
        if (p99 > target * 1.5) {
            int newConcurrency = Math.max(1, currentConcurrency - 1);
            return AgentDecision.builder()
                .recommendation("Set concurrency to " + newConcurrency)
                .reasoning(String.format(
                    "p99 (%.2fms) is 50%% over target (%.2fms). " +
                    "Reducing concurrency from %d to %d to decrease contention.",
                    p99, target, currentConcurrency, newConcurrency
                ))
                .confidenceScore(0.9)
                .impactLevel(AgentDecision.ImpactLevel.MEDIUM)
                .build();
        }
        
        // RULE 2: p99 slightly over target → Increase heap
        if (p99 > target && baseline.getHeapMetrics().getHeapUsagePercent() > 80.0) {
            int currentHeap = baseline.getHeapMetrics().getHeapSizeMb();
            int newHeap = (int) (currentHeap * 1.15); // +15%
            return AgentDecision.builder()
                .recommendation("Increase heap to " + newHeap + "MB")
                .reasoning(String.format(
                    "p99 breaching (%.2fms > %.2fms) and heap usage high (%.1f%%). " +
                    "Increasing heap from %dMB to %dMB to reduce GC pressure.",
                    p99, target, baseline.getHeapMetrics().getHeapUsagePercent(),
                    currentHeap, newHeap
                ))
                .recommendedHeapSizeMb(newHeap)
                .confidenceScore(0.85)
                .impactLevel(AgentDecision.ImpactLevel.LOW)
                .build();
        }
        
        // RULE 3: p99 well under target → Reduce concurrency (cost savings)
        if (p99 < target * 0.7) {
            int newConcurrency = Math.max(1, currentConcurrency - 1);
            return AgentDecision.builder()
                .recommendation("Set concurrency to " + newConcurrency)
                .reasoning(String.format(
                    "p99 (%.2fms) is well below target (%.2fms). " +
                    "Reducing concurrency from %d to %d for cost savings.",
                    p99, target, currentConcurrency, newConcurrency
                ))
                .confidenceScore(0.95)
                .impactLevel(AgentDecision.ImpactLevel.LOW)
                .build();
        }
        
        // RULE 4: No action needed
        return AgentDecision.builder()
            .recommendation("No changes needed")
            .reasoning("p99 within acceptable range")
            .confidenceScore(1.0)
            .impactLevel(AgentDecision.ImpactLevel.LOW)
            .build();
    }
}
```

**Multi-Agent Orchestrator:**
```java
@Service
public class MultiAgentOrchestrator {
    private final SimpleAgent simpleAgent;
    private final SpringAiLlmAgent llmAgent;
    private final SafetyConstraintEnforcer safetyEnforcer;
    
    public AgentDecision selectAndExecute(RunResult baseline, SloPolicy sloPolicy) {
        // ROUTING LOGIC: Decide which agent to use
        AgentStrategy strategy = selectStrategy(baseline, sloPolicy);
        
        AgentDecision decision;
        
        switch (strategy) {
            case SIMPLE:
                log.info("Using SIMPLE agent (rule-based)");
                decision = simpleAgent.makeDecision(baseline, sloPolicy);
                break;
                
            case LLM:
                log.info("Using LLM agent (AI-powered)");
                decision = llmAgent.analyzeAndRecommend(baseline, sloPolicy);
                
                // Validate LLM decision
                ValidationResult validation = safetyEnforcer.validate(decision, baseline);
                if (!validation.isValid()) {
                    log.warn("LLM decision rejected, falling back to SIMPLE agent");
                    decision = simpleAgent.makeDecision(baseline, sloPolicy);
                }
                break;
                
            default:
                throw new IllegalStateException("Unknown strategy: " + strategy);
        }
        
        return decision;
    }
    
    private AgentStrategy selectStrategy(RunResult baseline, SloPolicy sloPolicy) {
        double p99 = baseline.getP99LatencyMs();
        double target = sloPolicy.getTargetP99Ms();
        double heapUsage = baseline.getHeapMetrics().getHeapUsagePercent();
        double gcFrequency = baseline.getHeapMetrics().getGcFrequencyPerSec();
        
        // SIMPLE agent for common cases (80% of situations)
        if (p99 > target * 1.5) {
            return AgentStrategy.SIMPLE; // Clear case: reduce concurrency
        }
        
        if (p99 < target * 0.7) {
            return AgentStrategy.SIMPLE; // Clear case: cost savings
        }
        
        // LLM agent for complex cases (20% of situations)
        if (p99 > target && heapUsage > 80.0 && gcFrequency > 2.0) {
            return AgentStrategy.LLM; // Complex: multiple symptoms
        }
        
        if (p99 > target && heapUsage < 50.0) {
            return AgentStrategy.LLM; // Unusual: high p99 but low heap (non-obvious cause)
        }
        
        // Default to SIMPLE for efficiency
        return AgentStrategy.SIMPLE;
    }
}
```

### Benefits

✅ **Best of Both Worlds:**
- Fast and cheap for common cases (Simple Agent)
- Adaptive and intelligent for edge cases (LLM Agent)
- Graceful fallback if LLM fails

✅ **Cost Optimization:**
- 80% of decisions use free rule-based agent
- 20% of decisions use paid LLM agent
- Dramatic cost reduction vs pure-LLM approach

✅ **Reliability:**
- Deterministic fallback ensures service continuity
- LLM failures don't break the system
- Best-effort intelligence with guaranteed baseline

### When to Use

- ✅ Production systems requiring both speed and adaptability
- ✅ Cost-sensitive applications (LLM API costs add up)
- ✅ Systems with mix of common and edge-case scenarios
- ❌ Simple systems where rules cover 100% of cases

---

## 📊 Pattern Comparison Matrix

| Pattern | Complexity | Safety | Adaptability | Cost | Best For |
|---------|-----------|--------|--------------|------|----------|
| **O-R-E** | Medium | High | High | Medium | Self-optimizing microservices |
| **Bounded Action** | Low | Very High | N/A | Low | **REQUIRED** for any autonomous system |
| **LLM as Advisor** | Medium | High | High | Medium | Production systems using LLMs |
| **Closed-Loop** | High | Very High | High | Medium | High-availability services |
| **Multi-Agent** | High | High | Very High | Low | Cost-sensitive + adaptive systems |

---

## 🚀 Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
1. Implement **Bounded Action Space** pattern first (safety foundation)
2. Add comprehensive monitoring (p95/p99, SLO tracking)
3. Create simple rule-based agent

### Phase 2: Intelligence (Week 3-4)
1. Implement **LLM as Advisor** pattern
2. Add structured prompting and response parsing
3. Validate LLM recommendations against bounds

### Phase 3: Autonomy (Week 5-6)
1. Implement **Closed-Loop Automation** pattern
2. Add validation and rollback logic
3. Test end-to-end optimization cycles

### Phase 4: Optimization (Week 7-8)
1. Implement **Multi-Agent Reasoning** pattern
2. Optimize routing logic (when to use which agent)
3. Measure cost savings vs pure-LLM approach

### Phase 5: Production (Week 9-10)
1. Implement **O-R-E Pattern** end-to-end
2. Add comprehensive logging and audit trails
3. Production deployment with gradual rollout

---

## 🔗 References

- **Reference Implementation:** [github.com/sibasispadhi/agentic-cloud-optimizer](https://github.com/sibasispadhi/agentic-cloud-optimizer)
- **Spring AI Documentation:** [spring.io/projects/spring-ai](https://spring.io/projects/spring-ai)
- **FinTech SLA Best Practices:** [Amazon Builder's Library - SLAs](https://aws.amazon.com/builders-library/implementing-health-checks/)

---

## 💡 Key Takeaways

1. **LLMs are advisors, not executors** - Always validate LLM recommendations
2. **Safety first** - Bounded action space is non-negotiable
3. **Closed-loop is critical** - Validate changes, rollback if needed
4. **Hybrid > Pure** - Multi-agent beats single-agent
5. **Start simple** - Rule-based agents cover 80% of cases

---

**Questions? Feedback?**  
Reach out: [Your contact info]  
Presentation / talk context: reusable for architecture discussions on agentic JVM optimization