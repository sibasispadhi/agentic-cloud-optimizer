package com.cloudoptimizer.agent.benchmark;

import com.cloudoptimizer.agent.model.RunResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

/**
 * Complete, structured output from a single scenario run.
 *
 * <p>Contains every measurement needed to:
 * <ul>
 *   <li>reproduce the experiment</li>
 *   <li>understand baseline vs degraded vs recovered states</li>
 *   <li>compare naive vs governed agent behaviour</li>
 *   <li>support paper claims with concrete numbers</li>
 * </ul>
 *
 * <p>Use {@link #exitCriteriaMet()} as the single pass/fail signal in tests.
 */
@Getter
@Builder
@Jacksonized
public class ScenarioResult {

    @JsonProperty("scenario_name")
    private final String scenarioName;

    @JsonProperty("scenario_description")
    private final String scenarioDescription;

    @JsonProperty("ran_at")
    private final Instant ranAt;

    @JsonProperty("config")
    private final ScenarioConfig config;

    /** Healthy system performance before any fault is introduced. */
    @JsonProperty("baseline")
    private final RunResult baseline;

    /** System performance during the fault / stress condition. */
    @JsonProperty("degraded")
    private final RunResult degraded;

    /**
     * System performance if the <em>naive</em> latency-reactive agent's
     * recommendation is followed (may be worse than degraded).
     */
    @JsonProperty("naive_outcome")
    private final RunResult naiveOutcome;

    /** System performance after ACO's governed intervention. */
    @JsonProperty("recovered")
    private final RunResult recovered;

    /** Recommendation from a naive (no governance) agent. */
    @JsonProperty("naive_recommendation")
    private final String naiveRecommendation;

    /** Recommendation from ACO's governance-first agent. */
    @JsonProperty("smart_recommendation")
    private final String smartRecommendation;

    /** Quantified amplification and recovery evidence. */
    @JsonProperty("amplification")
    private final AmplificationMetrics amplification;

    /**
     * Human-readable findings suitable for paper / executive summary.
     * Each entry is one concise observation.
     */
    @JsonProperty("findings")
    private final List<String> findings;

    /**
     * Whether the scenario's exit criteria are met.
     * A {@code true} value means ACO demonstrated measurable improvement
     * over the naive baseline.
     */
    @JsonProperty("exit_criteria_met")
    private final boolean exitCriteriaMet;
}