package com.cloudoptimizer.agent.policy;

import com.cloudoptimizer.agent.artifact.PolicyEvaluationResult;

import java.time.Instant;
import java.util.List;

/**
 * The runtime output of a {@link PolicyEngine} evaluation.
 *
 * <p>Carries the outcome plus the full list of violations and warnings
 * that contributed to it.  Converts to a serializable
 * {@link PolicyEvaluationResult} for persistence in the
 * {@link com.cloudoptimizer.agent.artifact.OptimizationPlan}.
 *
 * <h2>Outcome precedence</h2>
 * <ol>
 *   <li>{@link Outcome#DENIED} — any hard violation present</li>
 *   <li>{@link Outcome#REQUIRES_APPROVAL} — no violations, but at least one
 *       rule flagged the action as needing human sign-off</li>
 *   <li>{@link Outcome#ALLOWED_WITH_WARNINGS} — no violations, no approval
 *       required, but at least one advisory warning was raised</li>
 *   <li>{@link Outcome#ALLOWED} — all checks passed cleanly</li>
 * </ol>
 */
public record PolicyDecision(
        Outcome outcome,
        List<PolicyViolation> violations,
        List<PolicyWarning> warnings
) {

    public enum Outcome {
        ALLOWED,
        ALLOWED_WITH_WARNINGS,
        REQUIRES_APPROVAL,
        DENIED
    }

    // ── Factory methods ───────────────────────────────────────────────────────

    public static PolicyDecision allowed() {
        return new PolicyDecision(Outcome.ALLOWED, List.of(), List.of());
    }

    public static PolicyDecision allowedWithWarnings(List<PolicyWarning> warnings) {
        return new PolicyDecision(Outcome.ALLOWED_WITH_WARNINGS, List.of(), warnings);
    }

    public static PolicyDecision requiresApproval(List<PolicyWarning> warnings) {
        return new PolicyDecision(Outcome.REQUIRES_APPROVAL, List.of(), warnings);
    }

    public static PolicyDecision denied(List<PolicyViolation> violations,
                                        List<PolicyWarning> warnings) {
        return new PolicyDecision(Outcome.DENIED, violations, warnings);
    }

    // ── Convenience predicates ────────────────────────────────────────────────

    public boolean isAllowed() {
        return outcome == Outcome.ALLOWED || outcome == Outcome.ALLOWED_WITH_WARNINGS;
    }

    public boolean isDenied() {
        return outcome == Outcome.DENIED;
    }

    public boolean requiresApproval() {
        return outcome == Outcome.REQUIRES_APPROVAL;
    }

    // ── Conversion ────────────────────────────────────────────────────────────

    /**
     * Converts this runtime decision to the serializable artifact form.
     * Maps outcome → {@link PolicyEvaluationResult.Status} and
     * copies violation/warning messages for persistence.
     */
    public PolicyEvaluationResult toPolicyEvaluationResult() {
        PolicyEvaluationResult.Status status = switch (outcome) {
            case ALLOWED             -> PolicyEvaluationResult.Status.APPROVED;
            case ALLOWED_WITH_WARNINGS -> PolicyEvaluationResult.Status.APPROVED_WITH_WARNINGS;
            case REQUIRES_APPROVAL   -> PolicyEvaluationResult.Status.REQUIRES_APPROVAL;
            case DENIED              -> PolicyEvaluationResult.Status.DENIED;
        };

        return PolicyEvaluationResult.builder()
                .status(status)
                .evaluatedAt(Instant.now())
                .violations(violations.stream().map(PolicyViolation::message).toList())
                .warnings(warnings.stream().map(PolicyWarning::message).toList())
                .build();
    }
}