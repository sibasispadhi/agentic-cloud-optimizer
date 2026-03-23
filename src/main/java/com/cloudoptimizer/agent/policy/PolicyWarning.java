package com.cloudoptimizer.agent.policy;

/**
 * A non-blocking advisory raised during policy evaluation.
 *
 * <p>Warnings do not prevent execution but are surfaced in the
 * {@link PolicyDecision} and persisted in the plan's
 * {@link com.cloudoptimizer.agent.artifact.PolicyEvaluationResult}.
 *
 * @param rule    short identifier for the advisory rule (e.g. {@code "HIGH_IMPACT_CHANGE"})
 * @param message human-readable explanation of the advisory
 */
public record PolicyWarning(String rule, String message) {

    public static PolicyWarning of(String rule, String message) {
        return new PolicyWarning(rule, message);
    }
}