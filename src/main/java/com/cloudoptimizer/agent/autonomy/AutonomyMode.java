package com.cloudoptimizer.agent.autonomy;

/**
 * Specifies how much autonomy the ACO agent is permitted to exercise.
 *
 * <p>Modes form a spectrum from fully passive to fully automated:
 *
 * <pre>
 *   OBSERVE_ONLY ─────────────────────────────── fully passive
 *       Run baseline, emit recommendation in plan artifact.
 *       After-test NEVER runs.  Policy gate NEVER runs.
 *
 *   ADVISORY_ONLY ────────────────────────────── recommendation only
 *       Run baseline + agent decision, emit plan with proposed changes.
 *       After-test NEVER runs.  Policy gate NEVER runs.
 *
 *   AUTO_LOW_RISK ────────────────────────────── supervised automation
 *       Auto-actuate only when impact level <= configured threshold
 *       AND confidence >= configured minimum.  Otherwise the run is
 *       upgraded to REQUIRES_APPROVAL and blocked before the after-test.
 *       Policy gate ALWAYS runs after the autonomy gate.
 *
 *   AUTO_POLICY_GOVERNED ─────────────────────── full automation (default)
 *       Current behaviour: policy engine + budget ledger gate every run.
 *       All rules evaluated; agent runs autonomously within those bounds.
 * </pre>
 */
public enum AutonomyMode {

    /**
     * Purely observational: baseline is measured and recommendation is
     * produced, but no changes are ever applied.
     */
    OBSERVE_ONLY,

    /**
     * Advisory: recommendation + proposed plan are emitted as an artifact
     * but the after-test is skipped.  Useful for human review workflows.
     */
    ADVISORY_ONLY,

    /**
     * Supervised: actuation is allowed only for low-risk, high-confidence
     * changes.  All others are escalated to REQUIRES_APPROVAL.
     */
    AUTO_LOW_RISK,

    /**
     * Fully autonomous within policy and budget constraints.
     * This is the default operating mode.
     */
    AUTO_POLICY_GOVERNED
}