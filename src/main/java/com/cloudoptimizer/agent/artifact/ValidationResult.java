package com.cloudoptimizer.agent.artifact;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * The execution result of the validation step that runs after optimization.
 *
 * <p>Distinct from {@link ValidationRecipe} (which describes <em>how</em> to
 * validate) — this records <em>what happened</em> when validation ran.
 *
 * <p>Stored inside {@link OptimizationPlan} so every run has an auditable,
 * machine-readable record of whether it improved system behaviour.
 *
 * @param status       whether validation passed or failed
 * @param measuredP99Ms p99 latency measured in the after-test (ms)
 * @param thresholdMs  the limit measuredP99Ms was compared against (ms)
 * @param evaluatedAt  when the validation comparison was made
 * @param reason       human-readable explanation of the outcome
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationResult(

        @JsonProperty("status")
        ValidationStatus status,

        @JsonProperty("measured_p99_ms")
        double measuredP99Ms,

        @JsonProperty("threshold_ms")
        double thresholdMs,

        @JsonFormat(shape = JsonFormat.Shape.STRING,
                    pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        @JsonProperty("evaluated_at")
        Instant evaluatedAt,

        @JsonProperty("reason")
        String reason
) {

    /** Outcome of the post-optimization validation check. */
    public enum ValidationStatus { PASSED, FAILED }

    /** Factory: validation passed. */
    public static ValidationResult passed(double measuredP99Ms, double thresholdMs) {
        return new ValidationResult(
                ValidationStatus.PASSED, measuredP99Ms, thresholdMs, Instant.now(),
                "p99 latency %.1fms is within threshold %.1fms"
                        .formatted(measuredP99Ms, thresholdMs));
    }

    /** Factory: validation failed. */
    public static ValidationResult failed(double measuredP99Ms, double thresholdMs) {
        return new ValidationResult(
                ValidationStatus.FAILED, measuredP99Ms, thresholdMs, Instant.now(),
                "p99 latency %.1fms exceeds threshold %.1fms"
                        .formatted(measuredP99Ms, thresholdMs));
    }
}