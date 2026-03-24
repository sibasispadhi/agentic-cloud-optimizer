package com.cloudoptimizer.agent.artifact;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * Raw evidence produced by the agent during an optimization run.
 *
 * <p>Contains the agent's recommendation text, full reasoning trace,
 * confidence score, impact assessment, and SLO breach context.
 * This is the "black-box output" section of the plan — what the agent
 * actually said, preserved verbatim for auditability.
 */
@Getter
@Builder
@ToString
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanEvidence {

    /**
     * Which agent produced this evidence.
     * Typical values: {@code "SpringAiLlmAgent"} or {@code "SimpleAgent"}.
     */
    @JsonProperty("agent_type")
    private final String agentType;

    /** The agent's top-level recommendation string. */
    @JsonProperty("recommendation")
    private final String recommendation;

    /** Full reasoning trace from the agent (may be multi-line). */
    @JsonProperty("reasoning")
    private final String reasoning;

    /** Overall confidence score in the range [0.0, 1.0]. */
    @JsonProperty("confidence_score")
    private final double confidenceScore;

    /**
     * Concurrency-specific confidence, if the agent evaluates dimensions
     * independently (LLM agent with heap analysis).  Nullable.
     */
    @JsonProperty("concurrency_confidence")
    private final Double concurrencyConfidence;

    /**
     * Heap-specific confidence, if the agent evaluates dimensions
     * independently.  Nullable.
     */
    @JsonProperty("heap_confidence")
    private final Double heapConfidence;

    /** High-level impact assessment (LOW / MEDIUM / HIGH / CRITICAL). */
    @JsonProperty("impact_level")
    private final String impactLevel;

    /** Whether an SLO breach was detected and contributed to this run. */
    @JsonProperty("slo_breached")
    private final boolean sloBreached;

    /** Human-readable reason for the SLO breach, if applicable. Nullable. */
    @JsonProperty("breach_reason")
    private final String breachReason;
}