package com.cloudoptimizer.agent.artifact;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * Captures <em>why</em> an optimization run was initiated.
 *
 * <p>Answers the question: "What was this plan trying to achieve,
 * and what triggered it?"  This is distinct from the agent's recommendation
 * ({@link PlanEvidence}) which describes <em>how</em> it proposes to
 * achieve the intent.
 */
@Getter
@Builder
@ToString
@Jacksonized
public class PlanIntent {

    /** What caused this optimization run to start. */
    public enum Trigger {
        /** Started by a human operator. */
        MANUAL,
        /** Started by a scheduled job. */
        SCHEDULED,
        /** Triggered because an SLO threshold was crossed. */
        SLO_BREACH
    }

    /** What kicked off this run. */
    @JsonProperty("trigger")
    private final Trigger trigger;

    /** Human-readable description of the intent. */
    @JsonProperty("description")
    private final String description;

    /** Target p99 latency this run is aiming for, in milliseconds. */
    @JsonProperty("target_latency_ms")
    private final double targetLatencyMs;

    /** Duration (in seconds) of each workload simulation phase. */
    @JsonProperty("workload_duration_seconds")
    private final int workloadDurationSeconds;

    /** Concurrency level used for the baseline measurement. */
    @JsonProperty("baseline_concurrency")
    private final int baselineConcurrency;
}