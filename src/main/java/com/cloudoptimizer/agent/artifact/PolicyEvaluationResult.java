package com.cloudoptimizer.agent.artifact;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

/**
 * Result of evaluating an {@link OptimizationPlan} against safety and governance policies.
 *
 * <p><b>Phase 2 note:</b> this is a stub model.  The real policy evaluation engine
 * lives in Phase 2.  For now, all plans are stamped {@link Status#PENDING_EVALUATION}
 * until that engine is wired in.  The shape of this class is intentionally final
 * so Phase 2 only needs to populate it — not redesign it.
 *
 * <p>A plan may not be applied if {@link Status#DENIED}.  Plans with
 * {@link Status#APPROVED_WITH_WARNINGS} may proceed but warnings are surfaced to
 * operators.  Plans with {@link Status#REQUIRES_APPROVAL} must be acknowledged
 * by a human before execution.
 */
@Getter
@Builder
@ToString
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PolicyEvaluationResult {

    /**
     * Possible outcomes of policy evaluation.
     */
    public enum Status {
        /** Not yet evaluated — initial state for all new plans. */
        PENDING_EVALUATION,
        /** All policy checks passed. */
        APPROVED,
        /** Passed but non-blocking warnings were found (logged and surfaced). */
        APPROVED_WITH_WARNINGS,
        /** Blocked until a human operator explicitly approves. */
        REQUIRES_APPROVAL,
        /** At least one hard policy constraint was violated; plan cannot proceed. */
        DENIED
    }

    /** Outcome of policy evaluation. */
    @JsonProperty("status")
    private final Status status;

    /** When policy evaluation completed.  Null if {@link Status#PENDING_EVALUATION}. */
    @JsonProperty("evaluated_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private final Instant evaluatedAt;

    /**
     * Hard policy violations that caused a {@link Status#DENIED} result.
     * Empty unless status is {@code DENIED}.
     */
    @JsonProperty("violations")
    @Builder.Default
    private final List<String> violations = List.of();

    /**
     * Non-blocking warnings raised during evaluation.
     * Present even when status is {@link Status#APPROVED_WITH_WARNINGS}.
     */
    @JsonProperty("warnings")
    @Builder.Default
    private final List<String> warnings = List.of();

    // ── Factory helpers ───────────────────────────────────────────────────────

    /** Returns the default stub result used before Phase 2 policy engine is wired in. */
    public static PolicyEvaluationResult pending() {
        return PolicyEvaluationResult.builder()
                .status(Status.PENDING_EVALUATION)
                .build();
    }
}