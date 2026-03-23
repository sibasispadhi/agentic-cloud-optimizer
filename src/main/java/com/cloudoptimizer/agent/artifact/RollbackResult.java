package com.cloudoptimizer.agent.artifact;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * The execution result of the rollback step, triggered when validation fails.
 *
 * <p>Distinct from {@link RollbackRecipe} (which describes <em>how</em> to
 * roll back) — this records <em>what happened</em> when rollback ran
 * (or why it was skipped).
 *
 * <p>Stored inside {@link OptimizationPlan} for a complete audit trail.
 *
 * @param status      what the rollback executor did
 * @param triggeredAt when rollback was decided/attempted ({@code null} if skipped)
 * @param reason      human-readable explanation
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RollbackResult(

        @JsonProperty("status")
        RollbackStatus status,

        @JsonFormat(shape = JsonFormat.Shape.STRING,
                    pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        @JsonProperty("triggered_at")
        Instant triggeredAt,

        @JsonProperty("reason")
        String reason
) {

    /** What the rollback executor decided to do. */
    public enum RollbackStatus {
        /** Rollback was executed successfully. */
        EXECUTED,

        /** Validation passed — rollback not needed. */
        SKIPPED_VALIDATION_PASSED,

        /** Validation failed but at least one change is not reversible. */
        NOT_APPLICABLE_NON_REVERSIBLE
    }

    /** Factory: rollback executed because validation failed. */
    public static RollbackResult executed(String reason) {
        return new RollbackResult(RollbackStatus.EXECUTED, Instant.now(), reason);
    }

    /** Factory: rollback skipped because validation passed. */
    public static RollbackResult skippedValidationPassed() {
        return new RollbackResult(
                RollbackStatus.SKIPPED_VALIDATION_PASSED, null,
                "Validation passed — rollback not required");
    }

    /** Factory: rollback not applicable due to non-reversible changes. */
    public static RollbackResult notApplicable(String reason) {
        return new RollbackResult(
                RollbackStatus.NOT_APPLICABLE_NON_REVERSIBLE, Instant.now(), reason);
    }
}