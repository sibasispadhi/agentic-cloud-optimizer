package com.cloudoptimizer.agent.artifact;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/**
 * Immutable metadata header attached to every {@link OptimizationPlan}.
 *
 * <p>Contains identifiers and provenance information for a specific optimization run:
 * who generated it, when, using which agent strategy, and at which schema version.
 */
@Getter
@Builder
@ToString
@Jacksonized
public class PlanMetadata {

    /** Unique plan identifier (UUID format). */
    @JsonProperty("plan_id")
    private final String planId;

    /**
     * Schema version of this plan document.
     * Increment when the structure changes in a backward-incompatible way.
     */
    @JsonProperty("schema_version")
    @Builder.Default
    private final String schemaVersion = "1.0";

    /** Wall-clock time when this plan was generated. */
    @JsonProperty("generated_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private final Instant generatedAt;

    /**
     * Agent strategy used to produce the recommendation.
     * Typical values: {@code "llm"} or {@code "simple"}.
     */
    @JsonProperty("agent_strategy")
    private final String agentStrategy;

    /** Human-readable label for the service or workload being optimized. */
    @JsonProperty("service_label")
    @Builder.Default
    private final String serviceLabel = "cloud-optimizer-service";
}