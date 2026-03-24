package com.cloudoptimizer.agent.benchmark;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * Quantitative evidence of cascading-failure amplification and recovery.
 *
 * <p>This is the paper-quality output of each scenario — numbers that
 * answer: "how much worse does a naive agent make it, and how much
 * better does ACO's governance-first approach make it?"
 *
 * <ul>
 *   <li>{@code amplificationFactor}     — effective load / intended load
 *       (e.g. 3.0 = retries tripled the actual request rate)</li>
 *   <li>{@code latencyDegradationRatio} — degraded p99 / baseline p99</li>
 *   <li>{@code throughputDegradationPct}— % RPS lost during the fault</li>
 *   <li>{@code naiveWorseningPct}       — % latency increase if the naive
 *       agent's recommendation is applied on top of the storm</li>
 *   <li>{@code smartRecoveryPct}        — % latency improvement when ACO's
 *       governed recommendation is applied</li>
 *   <li>{@code policyAllowed}           — whether the policy engine permitted
 *       the smart intervention (proves governance is effective, not blocking)</li>
 * </ul>
 */
@Getter
@Builder
@Jacksonized
public class AmplificationMetrics {

    /** Ratio of effective (amplified) load to intended load. */
    @JsonProperty("amplification_factor")
    private final double amplificationFactor;

    /** Ratio of degraded p99 to baseline p99. */
    @JsonProperty("latency_degradation_ratio")
    private final double latencyDegradationRatio;

    /** Percentage of throughput (RPS) lost during the fault condition. */
    @JsonProperty("throughput_degradation_pct")
    private final double throughputDegradationPct;

    /**
     * How much worse (% p99 increase) the naive agent's recommendation makes things.
     * Positive = naive intervention amplifies the problem.
     */
    @JsonProperty("naive_worsening_pct")
    private final double naiveWorseningPct;

    /**
     * How much better (% p99 improvement) ACO's smart intervention achieves.
     * Positive = governance-first approach reduces the problem.
     */
    @JsonProperty("smart_recovery_pct")
    private final double smartRecoveryPct;

    /** Whether the policy engine permitted the smart intervention. */
    @JsonProperty("policy_allowed")
    private final boolean policyAllowed;

    /** Textual policy outcome (e.g. "ALLOWED", "DENIED", "REQUIRES_APPROVAL"). */
    @JsonProperty("policy_outcome")
    private final String policyOutcome;

    // ── Factory ───────────────────────────────────────────────────────────────

    /**
     * Computes all metrics from the four key run results and policy outcome.
     *
     * @param amplificationFactor pre-computed amplification factor for the scenario
     * @param baselineP99         p99 latency under normal load (ms)
     * @param degradedP99         p99 latency during the fault (ms)
     * @param naiveP99            p99 latency if the naive agent's decision is applied (ms)
     * @param recoveredP99        p99 latency after ACO's governed intervention (ms)
     * @param baselineRps         RPS under normal load
     * @param degradedRps         RPS during the fault
     * @param policyAllowed       whether policy engine permitted the intervention
     * @param policyOutcome       policy decision label
     */
    public static AmplificationMetrics compute(
            double amplificationFactor,
            double baselineP99, double degradedP99,
            double naiveP99, double recoveredP99,
            double baselineRps, double degradedRps,
            boolean policyAllowed, String policyOutcome) {

        double latencyDegradationRatio   = safeRatio(degradedP99, baselineP99);
        double throughputDegradationPct  = safePctLoss(baselineRps, degradedRps);
        double naiveWorseningPct         = safePctChange(degradedP99, naiveP99);
        double smartRecoveryPct          = safePctLoss(degradedP99, recoveredP99);

        return AmplificationMetrics.builder()
                .amplificationFactor(amplificationFactor)
                .latencyDegradationRatio(latencyDegradationRatio)
                .throughputDegradationPct(throughputDegradationPct)
                .naiveWorseningPct(naiveWorseningPct)
                .smartRecoveryPct(smartRecoveryPct)
                .policyAllowed(policyAllowed)
                .policyOutcome(policyOutcome)
                .build();
    }

    private static double safeRatio(double numerator, double denominator) {
        return denominator == 0 ? 0 : numerator / denominator;
    }

    private static double safePctLoss(double before, double after) {
        return before == 0 ? 0 : ((before - after) / before) * 100.0;
    }

    private static double safePctChange(double before, double after) {
        return before == 0 ? 0 : ((after - before) / before) * 100.0;
    }
}