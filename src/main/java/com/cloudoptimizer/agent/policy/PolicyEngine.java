package com.cloudoptimizer.agent.policy;

/**
 * Evaluates a proposed optimization change against governance constraints.
 *
 * <p>Implementations receive a {@link PolicyContext} describing what the agent
 * wants to do and an {@link ActuationPolicy} describing what is allowed,
 * and return a {@link PolicyDecision} that the orchestrator acts on.
 *
 * <p>The only current implementation is {@link DefaultPolicyEngine}.  The
 * interface exists to make the contract explicit and the engine swappable
 * in tests.
 */
public interface PolicyEngine {

    /**
     * Evaluates the proposed change described by {@code context} against
     * the constraints in {@code policy}.
     *
     * @param context the proposed change and its context
     * @param policy  the active governance constraints
     * @return a decision indicating whether the change may proceed
     */
    PolicyDecision evaluate(PolicyContext context, ActuationPolicy policy);
}