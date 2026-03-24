package com.cloudoptimizer.agent.policy;

/**
 * A hard constraint that was breached during policy evaluation.
 *
 * <p>Any violation results in a {@link PolicyDecision.Outcome#DENIED} decision.
 * The plan may not proceed until the violation is resolved.
 *
 * @param rule    short identifier for the rule that fired (e.g. {@code "MAX_CONCURRENCY_DELTA"})
 * @param message human-readable explanation of why the rule fired and what the limit was
 */
public record PolicyViolation(String rule, String message) {

    public static PolicyViolation of(String rule, String message) {
        return new PolicyViolation(rule, message);
    }
}