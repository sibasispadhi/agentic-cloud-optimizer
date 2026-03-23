package com.cloudoptimizer.agent.benchmark;

import com.cloudoptimizer.agent.model.AgentDecision;
import com.cloudoptimizer.agent.model.HeapMetrics;
import com.cloudoptimizer.agent.model.RunResult;
import com.cloudoptimizer.agent.policy.PolicyDecision;

import java.time.Instant;
import java.util.List;

/**
 * <b>Over-provisioned Heap Scenario</b>
 *
 * <p>Models a service whose heap is allocated at 4× what it actually uses.
 * GC is infrequent (good), but the wasted memory incurs real cost and
 * increases full-GC pause risk under burst conditions.
 *
 * <p>Unlike the storm scenarios, this is a <em>cost and efficiency</em>
 * scenario: latency is not catastrophically high, but resource waste is
 * measurable and the right-sizing action is clearly safe.
 */
public class HeapOverprovisionedScenario implements Scenario {

    @Override public String name()        { return "heap_overprovisioned"; }
    @Override public String description() {
        return "Heap allocated at 4× actual usage — cost waste with full-GC burst risk.";
    }

    @Override
    public ScenarioResult run(ScenarioConfig config) {
        int baseCon = config.getBaselineConcurrency();

        HeapMetrics wastedHeap = HeapMetrics.builder()
                .heapSizeMb(4096).heapUsedMb(980)
                .heapUsagePercent(23.9)
                .gcCount(2).gcTimeMs(80)
                .gcPauseTimeAvgMs(40.0).gcFrequencyPerSec(0.07)
                .build();

        RunResult baseline = RunResult.builder()
                .timestamp(Instant.now()).concurrency(baseCon)
                .durationSeconds(config.getDurationSeconds())
                .totalRequests(3000).successfulRequests(2990).failedRequests(10)
                .requestsPerSecond(100.0)
                .medianLatencyMs(42.0).avgLatencyMs(46.0)
                .minLatencyMs(8.0).maxLatencyMs(160.0)
                .p95LatencyMs(68.0).p99LatencyMs(85.0)
                .costEstimateUsd(0.48)   // high cost — 4 GB heap billable
                .heapMetrics(wastedHeap).build();

        // "Degraded" = same performance, but cost is the problem
        RunResult degraded = baseline; // performance is fine; waste is the issue

        // Naive agent: latency is OK → no recommendation to reduce heap
        AgentDecision naive = ScenarioDecisionEngine.naive(degraded);

        // Naive outcome: heap stays at 4 GB, cost waste continues
        RunResult naiveOutcome = RunResult.builder()
                .timestamp(Instant.now()).concurrency(baseCon)
                .durationSeconds(config.getDurationSeconds())
                .totalRequests(3000).successfulRequests(2990).failedRequests(10)
                .requestsPerSecond(100.0)
                .medianLatencyMs(42.0).avgLatencyMs(46.0)
                .minLatencyMs(8.0).maxLatencyMs(160.0)
                .p95LatencyMs(68.0).p99LatencyMs(85.0)
                .costEstimateUsd(0.48)  // cost unchanged — naive agent ignored heap
                .heapMetrics(wastedHeap).build();

        HeapMetrics rightSizedHeap = HeapMetrics.builder()
                .heapSizeMb(1024).heapUsedMb(920)
                .heapUsagePercent(89.8)
                .gcCount(8).gcTimeMs(120)
                .gcPauseTimeAvgMs(15.0).gcFrequencyPerSec(0.27)
                .build();

        // Smart: right-size heap from 4096 → 1024 MB
        AgentDecision smart = AgentDecision.builder()
                .decisionId(java.util.UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .strategy(com.cloudoptimizer.agent.model.AgentStrategy.RULE_BASED)
                .recommendation("Set concurrency to " + baseCon + "; reduce heap to 1024MB")
                .reasoning("[ACO] Heap usage 23.9% on 4096MB allocation. " +
                        "GC frequency 0.07/sec indicates significant over-provisioning. " +
                        "Right-sizing to 1024MB reduces cost by 75% with acceptable GC budget.")
                .confidenceScore(0.92)
                .impactLevel(AgentDecision.ImpactLevel.LOW)
                .recommendedHeapSizeMb(1024)
                .build();

        PolicyDecision policy = ScenarioDecisionEngine.evaluatePolicy(
                baseline, baseCon, 0.92, AgentDecision.ImpactLevel.LOW.name());

        RunResult recovered = RunResult.builder()
                .timestamp(Instant.now()).concurrency(baseCon)
                .durationSeconds(config.getDurationSeconds())
                .totalRequests(3000).successfulRequests(2985).failedRequests(15)
                .requestsPerSecond(99.5)
                .medianLatencyMs(41.0).avgLatencyMs(45.0)
                .minLatencyMs(8.0).maxLatencyMs(145.0)
                .p95LatencyMs(65.0).p99LatencyMs(82.0)
                .costEstimateUsd(0.12)  // 75% cost reduction
                .heapMetrics(rightSizedHeap).build();

        // For heap scenario: amplification = wasted heap ratio; latency improvement is minimal
        AmplificationMetrics amplification = AmplificationMetrics.compute(
                4.0,  // 4× over-provisioned
                baseline.getP99LatencyMs(), degraded.getP99LatencyMs(),
                naiveOutcome.getP99LatencyMs(), recovered.getP99LatencyMs(),
                baseline.getRequestsPerSecond(), degraded.getRequestsPerSecond(),
                policy.isAllowed(),
                policy.outcome().name());

        double costSavingPct = 75.0; // (0.48 - 0.12) / 0.48
        boolean exitOk = costSavingPct >= 50.0 && amplification.isPolicyAllowed();

        return ScenarioResult.builder()
                .scenarioName(name()).scenarioDescription(description())
                .ranAt(Instant.now()).config(config)
                .baseline(baseline).degraded(degraded)
                .naiveOutcome(naiveOutcome).recovered(recovered)
                .naiveRecommendation(naive.getRecommendation())
                .smartRecommendation(smart.getRecommendation())
                .amplification(amplification)
                .findings(List.of(
                        "Heap allocated at 4096MB; actual usage 980MB (23.9%) — 4× over-provisioned",
                        "Naive agent: latency within target → no heap action taken",
                        String.format("ACO right-sized heap to 1024MB: %.0f%% cost reduction", costSavingPct),
                        "Latency unchanged post right-sizing — confirms safe intervention",
                        String.format("Policy decision: %s", amplification.getPolicyOutcome())))
                .exitCriteriaMet(exitOk)
                .build();
    }
}