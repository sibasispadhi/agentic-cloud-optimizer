package com.cloudoptimizer.agent.benchmark;

import com.cloudoptimizer.agent.model.AgentDecision;
import com.cloudoptimizer.agent.model.AgentStrategy;
import com.cloudoptimizer.agent.model.RunResult;
import com.cloudoptimizer.agent.policy.ActuationPolicy;
import com.cloudoptimizer.agent.policy.DefaultPolicyEngine;
import com.cloudoptimizer.agent.policy.PolicyContext;
import com.cloudoptimizer.agent.policy.PolicyDecision;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Spring-free utility that provides two decision paths used by benchmark scenarios:
 *
 * <ol>
 *   <li><b>Naive</b> — pure latency-reactive: if latency is high, scale UP.
 *       Models the behaviour of an agent with no cascading-failure awareness.
 *       This is the "amplifying" path the paper warns against.</li>
 *   <li><b>Smart</b> — governance-first: respects policy bounds, backs off
 *       under retry-storm conditions, reduces load to break the feedback loop.
 *       This is ACO's recommended path.</li>
 * </ol>
 *
 * <p>Rule constants mirror the defaults from {@code application.yml} so that
 * test outcomes match real runtime behaviour.
 */
public final class ScenarioDecisionEngine {

    // mirrors application.yml defaults
    private static final double TARGET_LATENCY_MS       = 100.0;
    private static final double LOW_THRESHOLD_FACTOR    = 0.7;
    private static final double SCALE_UP_FACTOR         = 1.5;
    private static final double SCALE_DOWN_FACTOR       = 0.75;
    private static final double CONFIDENCE              = 0.95;

    private ScenarioDecisionEngine() { /* utility class */ }

    // ── Naive path ────────────────────────────────────────────────────────────

    /**
     * Produces the recommendation a naive latency-reactive agent would make.
     * High latency → scale UP. Low latency → scale DOWN.
     * No awareness of retry storms, saturation, or cascading failures.
     */
    public static AgentDecision naive(RunResult degraded) {
        double median = degraded.getMedianLatencyMs();
        int    current = degraded.getConcurrency();
        int    proposed;
        String reasoning;
        AgentDecision.ImpactLevel impact;

        if (median > TARGET_LATENCY_MS) {
            proposed  = (int) Math.ceil(current * SCALE_UP_FACTOR);
            reasoning = String.format(
                    "[NAIVE] Latency %.1fms > target %.1fms — scaling UP from %d to %d. "
                    + "WARNING: may amplify retry storms.",
                    median, TARGET_LATENCY_MS, current, proposed);
            impact = AgentDecision.ImpactLevel.MEDIUM;
        } else if (median < TARGET_LATENCY_MS * LOW_THRESHOLD_FACTOR) {
            proposed  = Math.max(1, (int) Math.floor(current * SCALE_DOWN_FACTOR));
            reasoning = String.format(
                    "[NAIVE] Latency %.1fms well below target — scaling DOWN from %d to %d.",
                    median, current, proposed);
            impact = AgentDecision.ImpactLevel.LOW;
        } else {
            proposed  = current;
            reasoning = String.format("[NAIVE] Latency %.1fms within range — no change.", median);
            impact = AgentDecision.ImpactLevel.LOW;
        }

        return AgentDecision.builder()
                .decisionId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .strategy(AgentStrategy.RULE_BASED)
                .recommendation(String.format("Set concurrency to %d", proposed))
                .reasoning(reasoning)
                .confidenceScore(CONFIDENCE)
                .impactLevel(impact)
                .actionItems(List.of(String.format("Update concurrency from %d to %d", current, proposed)))
                .build();
    }

    // ── Smart (ACO governance-first) path ─────────────────────────────────────

    /**
     * Produces ACO's governance-first recommendation for a degraded state.
     * Under high-amplification conditions, backs off instead of scaling up.
     *
     * @param degraded          the degraded run result
     * @param amplificationFactor how much the actual load exceeds intended load
     */
    public static AgentDecision smart(RunResult degraded, double amplificationFactor) {
        double median  = degraded.getMedianLatencyMs();
        int    current = degraded.getConcurrency();

        // Under amplification: back off to break the feedback loop
        int proposed;
        String reasoning;
        AgentDecision.ImpactLevel impact;

        if (amplificationFactor >= 2.0 && median > TARGET_LATENCY_MS) {
            proposed  = Math.max(1, (int) Math.floor(current * SCALE_DOWN_FACTOR));
            reasoning = String.format(
                    "[ACO] Amplification factor %.1fx detected with high latency %.1fms. "
                    + "Backing off from %d to %d to break retry-storm feedback loop.",
                    amplificationFactor, median, current, proposed);
            impact = AgentDecision.ImpactLevel.MEDIUM;
        } else if (median > TARGET_LATENCY_MS) {
            proposed  = (int) Math.ceil(current * SCALE_UP_FACTOR);
            reasoning = String.format(
                    "[ACO] Latency %.1fms elevated, no amplification detected — scaling UP to %d.",
                    median, proposed);
            impact = AgentDecision.ImpactLevel.MEDIUM;
        } else {
            proposed  = current;
            reasoning = String.format("[ACO] System stable at %.1fms — maintaining concurrency %d.", median, current);
            impact = AgentDecision.ImpactLevel.LOW;
        }

        return AgentDecision.builder()
                .decisionId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .strategy(AgentStrategy.RULE_BASED)
                .recommendation(String.format("Set concurrency to %d", proposed))
                .reasoning(reasoning)
                .confidenceScore(CONFIDENCE)
                .impactLevel(impact)
                .actionItems(List.of(String.format("Update concurrency from %d to %d", current, proposed)))
                .build();
    }

    // ── Policy evaluation ─────────────────────────────────────────────────────

    /**
     * Evaluates a proposed concurrency change against the default policy.
     * Uses {@link ActuationPolicy} defaults so results match runtime.
     */
    public static PolicyDecision evaluatePolicy(RunResult baseline, int proposedConcurrency,
                                                double confidenceScore, String impactLevel) {
        ActuationPolicy policy = new ActuationPolicy();
        DefaultPolicyEngine engine = new DefaultPolicyEngine();

        PolicyContext ctx = PolicyContext.builder()
                .baseline(baseline)
                .proposedConcurrency(proposedConcurrency)
                .confidenceScore(confidenceScore)
                .impactLevel(impactLevel)
                .proposedResources(Set.of("jvm.concurrency"))
                .build();

        return engine.evaluate(ctx, policy);
    }
}