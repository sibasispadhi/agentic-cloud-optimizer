package com.cloudoptimizer.agent.artifact;

/**
 * Lifecycle status of an {@link OptimizationPlan}.
 *
 * <p>State machine:
 * {@code PENDING → APPLIED → VALIDATED}
 * or
 * {@code PENDING → APPLIED → ROLLED_BACK}
 * or
 * {@code PENDING → FAILED} (plan could not be applied)
 */
public enum ExecutionStatus {

    /** Plan created but changes not yet applied. */
    PENDING,

    /** Changes applied to the target system. */
    APPLIED,

    /** Post-apply validation confirmed improvements. */
    VALIDATED,

    /** Applied changes were reversed due to failed validation. */
    ROLLED_BACK,

    /** Plan application failed before changes could be confirmed. */
    FAILED
}