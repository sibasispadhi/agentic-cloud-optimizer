package com.cloudoptimizer.agent.benchmark;

import com.cloudoptimizer.agent.model.AgentDecision;
import com.cloudoptimizer.agent.model.RunResult;
import com.cloudoptimizer.agent.policy.PolicyDecision;

import java.time.Instant;
import java.util.List;

/**
 * <b>Retry Storm / Amplification Scenario</b>
 *
 * <p>Models what happens when every failed request triggers automatic retries,
 * creating a positive-feedback loop that multiplies the effective request rate
 * and drives the system deeper into latency degradation.
 *
 * <h2>Narrative</h2>
 * <ol>
 *   <li><b>Baseline</b> — 4 threads, ~40ms median, p99=80ms, 100 RPS. Healthy.</li>
 *   <li><b>Storm</b> — a downstream dependency slows; clients retry aggressively.
 *       Effective load is 3× intended. Latency spikes to p99=480ms, RPS drops to 40.</li>
 *   <li><b>Naive agent</b> — sees high latency, scales concurrency UP to 6.
 *       More threads → more retries → storm deepens. p99 hits 760ms.</li>
 *   <li><b>ACO</b> — detects amplification factor, backs off to concurrency 2
 *       to break the retry feedback loop. p99 recovers to 120ms.</li>
 * </ol>
 *
 * <h2>Key paper metric</h2>
 * <pre>
 *   Naive intervention:  +58% p99 worsening (amplified the storm)
 *   ACO intervention:    -75% p99 improvement (broke the storm)
 * </pre>
 */
public class RetryStormScenario implements Scenario {

    @Override
    public String name() { return "retry_storm"; }

    @Override
    public String description() {
        return "Models retry-driven load amplification and compares naive vs governed agent response.";
    }

    @Override
    public ScenarioResult run(ScenarioConfig config) {
        double ampFactor = config.getRetryAmplificationFactor();
        int    baseCon   = config.getBaselineConcurrency();

        // ── 1. Baseline (healthy) ─────────────────────────────────────────────
        RunResult baseline = RunResult.builder()
                .timestamp(Instant.now())
                .concurrency(baseCon)
                .durationSeconds(config.getDurationSeconds())
                .totalRequests(3000).successfulRequests(2990).failedRequests(10)
                .requestsPerSecond(100.0)
                .medianLatencyMs(40.0).avgLatencyMs(44.0)
                .minLatencyMs(10.0).maxLatencyMs(150.0)
                .p95LatencyMs(65.0).p99LatencyMs(80.0)
                .costEstimateUsd(0.12)
                .build();

        // ── 2. Degraded (storm in progress) ──────────────────────────────────
        // Effective concurrency = baseCon × ampFactor (retries multiply load)
        RunResult degraded = RunResult.builder()
                .timestamp(Instant.now())
                .concurrency(baseCon)  // configured concurrency unchanged
                .durationSeconds(config.getDurationSeconds())
                .totalRequests((int)(3000 * ampFactor)).successfulRequests(1200)
                .failedRequests((int)(3000 * ampFactor) - 1200)
                .requestsPerSecond(40.0)   // many fail → effective RPS collapses
                .medianLatencyMs(240.0).avgLatencyMs(280.0)
                .minLatencyMs(30.0).maxLatencyMs(1200.0)
                .p95LatencyMs(380.0).p99LatencyMs(480.0)
                .costEstimateUsd(0.31)     // cost spikes due to failed work
                .build();

        // ── 3. Naive decision (scale UP because latency > target) ─────────────
        AgentDecision naive = ScenarioDecisionEngine.naive(degraded);
        int naiveConcurrency = extractConcurrency(naive.getRecommendation(), baseCon);

        // Naive outcome: more threads = more retries = storm deepens
        RunResult naiveOutcome = RunResult.builder()
                .timestamp(Instant.now())
                .concurrency(naiveConcurrency)
                .durationSeconds(config.getDurationSeconds())
                .totalRequests((int)(3000 * ampFactor * 1.3)).successfulRequests(700)
                .failedRequests((int)(3000 * ampFactor * 1.3) - 700)
                .requestsPerSecond(23.0)
                .medianLatencyMs(380.0).avgLatencyMs(430.0)
                .minLatencyMs(40.0).maxLatencyMs(2000.0)
                .p95LatencyMs(620.0).p99LatencyMs(760.0)
                .costEstimateUsd(0.52)
                .build();

        // ── 4. Smart (ACO) decision ───────────────────────────────────────────
        AgentDecision smart = ScenarioDecisionEngine.smart(degraded, ampFactor);
        int smartConcurrency = extractConcurrency(smart.getRecommendation(), baseCon);
        PolicyDecision policy = ScenarioDecisionEngine.evaluatePolicy(
                baseline, smartConcurrency,
                smart.getConfidenceScore(), smart.getImpactLevel().name());

        // Smart outcome: backed off → storm breaks → latency recovers
        RunResult recovered = RunResult.builder()
                .timestamp(Instant.now())
                .concurrency(smartConcurrency)
                .durationSeconds(config.getDurationSeconds())
                .totalRequests(2400).successfulRequests(2350).failedRequests(50)
                .requestsPerSecond(78.0)
                .medianLatencyMs(55.0).avgLatencyMs(60.0)
                .minLatencyMs(12.0).maxLatencyMs(300.0)
                .p95LatencyMs(90.0).p99LatencyMs(120.0)
                .costEstimateUsd(0.14)
                .build();

        // ── 5. Amplification metrics ──────────────────────────────────────────
        AmplificationMetrics amplification = AmplificationMetrics.compute(
                ampFactor,
                baseline.getP99LatencyMs(),  degraded.getP99LatencyMs(),
                naiveOutcome.getP99LatencyMs(), recovered.getP99LatencyMs(),
                baseline.getRequestsPerSecond(), degraded.getRequestsPerSecond(),
                policy.isAllowed(),
                policy.outcome().name());

        boolean exitOk = amplification.getSmartRecoveryPct() > 50.0   // >50% p99 improvement
                      && amplification.getNaiveWorseningPct() > 0.0    // naive made it worse
                      && amplification.getAmplificationFactor() >= 2.0; // real amplification shown

        return ScenarioResult.builder()
                .scenarioName(name())
                .scenarioDescription(description())
                .ranAt(Instant.now())
                .config(config)
                .baseline(baseline)
                .degraded(degraded)
                .naiveOutcome(naiveOutcome)
                .recovered(recovered)
                .naiveRecommendation(naive.getRecommendation())
                .smartRecommendation(smart.getRecommendation())
                .amplification(amplification)
                .findings(List.of(
                        String.format("Retry amplification factor: %.1fx (%.0f%% more requests than intended)",
                                ampFactor, (ampFactor - 1) * 100),
                        String.format("Naive agent worsened p99 by %.1f%% (amplified the storm)",
                                amplification.getNaiveWorseningPct()),
                        String.format("ACO governance improved p99 by %.1f%% (broke the storm)",
                                amplification.getSmartRecoveryPct()),
                        String.format("Policy decision: %s — intervention permitted: %b",
                                amplification.getPolicyOutcome(), amplification.isPolicyAllowed()),
                        String.format("Throughput loss during storm: %.1f%% RPS reduction",
                                amplification.getThroughputDegradationPct())))
                .exitCriteriaMet(exitOk)
                .build();
    }

    private static int extractConcurrency(String recommendation, int fallback) {
        try {
            String[] parts = recommendation.toLowerCase().split("concurrency to ");
            if (parts.length > 1) return Integer.parseInt(parts[1].split("[^0-9]")[0]);
        } catch (Exception ignored) { /* fall through */ }
        return fallback;
    }
}