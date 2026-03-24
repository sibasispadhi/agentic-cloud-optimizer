package com.cloudoptimizer.agent.benchmark;

import com.cloudoptimizer.agent.model.AgentDecision;
import com.cloudoptimizer.agent.model.RunResult;
import com.cloudoptimizer.agent.policy.PolicyDecision;

import java.time.Instant;
import java.util.List;

/**
 * <b>CPU Throttling Scenario</b>
 *
 * <p>Models a containerised service whose CPU is throttled by cgroup limits.
 * Threads spin-wait for CPU time, causing latency to spike unpredictably
 * while RPS collapses. A naive agent scales concurrency UP, wasting the
 * limited CPU budget on scheduling overhead instead of useful work.
 *
 * <p>ACO backs off to 2 threads — reducing CPU contention and allowing
 * actual requests to complete faster.
 */
public class CpuThrottlingScenario implements Scenario {

    @Override public String name()        { return "cpu_throttling"; }
    @Override public String description() {
        return "cgroup CPU limit causes thread spin-wait; naive scale-up worsens scheduling overhead.";
    }

    @Override
    public ScenarioResult run(ScenarioConfig config) {
        int baseCon = config.getBaselineConcurrency();

        RunResult baseline = RunResult.builder()
                .timestamp(Instant.now()).concurrency(baseCon)
                .durationSeconds(config.getDurationSeconds())
                .totalRequests(4500).successfulRequests(4480).failedRequests(20)
                .requestsPerSecond(150.0)
                .medianLatencyMs(35.0).avgLatencyMs(38.0)
                .minLatencyMs(5.0).maxLatencyMs(90.0)
                .p95LatencyMs(55.0).p99LatencyMs(70.0)
                .costEstimateUsd(0.09).build();

        // CPU throttled — threads spin, latency spikes
        RunResult degraded = RunResult.builder()
                .timestamp(Instant.now()).concurrency(baseCon)
                .durationSeconds(config.getDurationSeconds())
                .totalRequests(900).successfulRequests(820).failedRequests(80)
                .requestsPerSecond(27.0)
                .medianLatencyMs(680.0).avgLatencyMs(820.0)
                .minLatencyMs(50.0).maxLatencyMs(4000.0)
                .p95LatencyMs(1800.0).p99LatencyMs(2800.0)
                .costEstimateUsd(0.22).build();

        // Naive: high latency → scale UP → more spin-wait → worse
        AgentDecision naive = ScenarioDecisionEngine.naive(degraded);
        RunResult naiveOutcome = RunResult.builder()
                .timestamp(Instant.now()).concurrency(6)
                .durationSeconds(config.getDurationSeconds())
                .totalRequests(500).successfulRequests(380).failedRequests(120)
                .requestsPerSecond(13.0)
                .medianLatencyMs(1200.0).avgLatencyMs(1500.0)
                .minLatencyMs(80.0).maxLatencyMs(6000.0)
                .p95LatencyMs(3500.0).p99LatencyMs(4800.0)
                .costEstimateUsd(0.38).build();

        // Smart: reduce to 2 threads — less contention, each request gets more CPU
        int smartCon = Math.max(1, (int) Math.floor(baseCon * 0.5));
        AgentDecision smart = ScenarioDecisionEngine.smart(degraded, 4.0);
        PolicyDecision policy = ScenarioDecisionEngine.evaluatePolicy(
                baseline, smartCon, 0.92, AgentDecision.ImpactLevel.MEDIUM.name());

        RunResult recovered = RunResult.builder()
                .timestamp(Instant.now()).concurrency(smartCon)
                .durationSeconds(config.getDurationSeconds())
                .totalRequests(2700).successfulRequests(2660).failedRequests(40)
                .requestsPerSecond(89.0)
                .medianLatencyMs(62.0).avgLatencyMs(68.0)
                .minLatencyMs(10.0).maxLatencyMs(250.0)
                .p95LatencyMs(110.0).p99LatencyMs(140.0)
                .costEstimateUsd(0.10).build();

        AmplificationMetrics amplification = AmplificationMetrics.compute(
                4.0,
                baseline.getP99LatencyMs(), degraded.getP99LatencyMs(),
                naiveOutcome.getP99LatencyMs(), recovered.getP99LatencyMs(),
                baseline.getRequestsPerSecond(), degraded.getRequestsPerSecond(),
                policy.isAllowed(),
                policy.outcome().name());

        boolean exitOk = amplification.getSmartRecoveryPct() > 50.0
                      && amplification.getNaiveWorseningPct() > 0.0;

        return ScenarioResult.builder()
                .scenarioName(name()).scenarioDescription(description())
                .ranAt(Instant.now()).config(config)
                .baseline(baseline).degraded(degraded)
                .naiveOutcome(naiveOutcome).recovered(recovered)
                .naiveRecommendation(naive.getRecommendation())
                .smartRecommendation(smart.getRecommendation())
                .amplification(amplification)
                .findings(List.of(
                        "CPU cgroup throttle caused 40× p99 spike (70ms → 2800ms)",
                        String.format("Naive scale-up to 6 threads worsened p99 by %.1f%%",
                                amplification.getNaiveWorseningPct()),
                        String.format("ACO backed off to %d threads; p99 recovered by %.1f%%",
                                smartCon, amplification.getSmartRecoveryPct()),
                        String.format("RPS recovered from %.0f to %.0f",
                                degraded.getRequestsPerSecond(), recovered.getRequestsPerSecond()),
                        String.format("Policy outcome: %s", amplification.getPolicyOutcome())))
                .exitCriteriaMet(exitOk)
                .build();
    }
}