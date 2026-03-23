package com.cloudoptimizer.agent.artifact;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * Describes a single recommended configuration change within an {@link OptimizationPlan}.
 *
 * <p>A plan may contain multiple changes (e.g., adjust concurrency AND heap size).
 * Each change is self-contained: it records what is being changed, from which
 * value, to which value, and why.
 *
 * <p>Example resources: {@code "jvm.concurrency"}, {@code "jvm.heap_size_mb"}.
 */
@Getter
@Builder
@ToString
@Jacksonized
public class PlanChange {

    /**
     * Identifies the resource dimension being tuned.
     * Use dot-notation, e.g., {@code "jvm.concurrency"} or {@code "jvm.heap_size_mb"}.
     */
    @JsonProperty("resource")
    private final String resource;

    /** Current (pre-change) value as a string for display purposes. */
    @JsonProperty("from_value")
    private final String fromValue;

    /** Proposed (post-change) value as a string for display purposes. */
    @JsonProperty("to_value")
    private final String toValue;

    /**
     * Agent's rationale for this specific change.
     * Extracted directly from the agent's reasoning output.
     */
    @JsonProperty("rationale")
    private final String rationale;

    /**
     * Confidence in this specific change, in the range [0.0, 1.0].
     * May differ from the overall plan confidence when multiple changes
     * are evaluated independently.
     */
    @JsonProperty("confidence")
    private final double confidence;
}