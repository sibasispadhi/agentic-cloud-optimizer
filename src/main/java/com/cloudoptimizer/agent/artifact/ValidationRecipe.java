package com.cloudoptimizer.agent.artifact;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/**
 * Describes how to validate that an applied {@link OptimizationPlan} actually improved things.
 *
 * <p>After changes are applied the orchestrator reruns a load test and compares
 * the result against this recipe.  If {@link #passed} comes back {@code false},
 * the {@link RollbackRecipe} is executed.
 *
 * <p>{@link #passed} and {@link #validatedAt} are populated after execution;
 * they are {@code null} when the plan is first created.
 */
@Getter
@Builder
@ToString
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationRecipe {

    /**
     * Validation method identifier.
     * Currently only {@code "load_test"} is supported.
     */
    @JsonProperty("method")
    @Builder.Default
    private final String method = "load_test";

    /** How long (in seconds) the validation load test should run. */
    @JsonProperty("duration_seconds")
    private final int durationSeconds;

    /**
     * The metric to compare against the threshold.
     * Example: {@code "p99_latency_ms"}, {@code "median_latency_ms"}.
     */
    @JsonProperty("target_metric")
    @Builder.Default
    private final String targetMetric = "p99_latency_ms";

    /**
     * Maximum acceptable value for {@link #targetMetric}.
     * The plan is considered validated if the measured value is at or below this threshold.
     */
    @JsonProperty("threshold")
    private final double threshold;

    /**
     * Whether validation passed.  {@code null} until validation has run.
     */
    @JsonProperty("passed")
    private final Boolean passed;

    /** When validation completed.  {@code null} until validation has run. */
    @JsonProperty("validated_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private final Instant validatedAt;
}