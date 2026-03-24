package com.cloudoptimizer.agent.autonomy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Outcome of the autonomy gate evaluation.
 *
 * <p>Produced by {@link AutonomyGate} and stored inside the
 * {@link com.cloudoptimizer.agent.artifact.OptimizationPlan} artifact
 * so every run records why it was (or was not) executed.
 *
 * @param mode        the active {@link AutonomyMode} at evaluation time
 * @param outcome     coarse outcome category
 * @param actuationPermitted {@code true} if the orchestrator should continue
 *                   to the policy gate and after-test
 * @param reason      human-readable explanation
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AutonomyGateResult(

        @JsonProperty("mode")
        AutonomyMode mode,

        @JsonProperty("outcome")
        Outcome outcome,

        @JsonProperty("actuation_permitted")
        boolean actuationPermitted,

        @JsonProperty("reason")
        String reason
) {

    /**
     * Coarse categorisation of why actuation was or was not permitted.
     */
    public enum Outcome {
        /** Autonomy mode permits full policy-governed actuation. */
        PROCEED,

        /** OBSERVE_ONLY mode: run blocked before policy gate. */
        OBSERVE_ONLY_BLOCKED,

        /** ADVISORY_ONLY mode: plan emitted but after-test skipped. */
        ADVISORY_ONLY_BLOCKED,

        /** AUTO_LOW_RISK: impact level exceeds allowed threshold. */
        LOW_RISK_BLOCKED_IMPACT,

        /** AUTO_LOW_RISK: agent confidence below required minimum. */
        LOW_RISK_BLOCKED_CONFIDENCE
    }

    // ── Factory methods ───────────────────────────────────────────────────────

    /** Actuation fully permitted — proceed to policy gate and after-test. */
    public static AutonomyGateResult proceed(AutonomyMode mode, String reason) {
        return new AutonomyGateResult(mode, Outcome.PROCEED, true, reason);
    }

    /** OBSERVE_ONLY: observation complete, no actuation. */
    public static AutonomyGateResult observeOnly() {
        return new AutonomyGateResult(
                AutonomyMode.OBSERVE_ONLY, Outcome.OBSERVE_ONLY_BLOCKED, false,
                "Mode is OBSERVE_ONLY — baseline measured, recommendation emitted, no changes applied");
    }

    /** ADVISORY_ONLY: plan emitted as advisory artifact, no actuation. */
    public static AutonomyGateResult advisoryOnly() {
        return new AutonomyGateResult(
                AutonomyMode.ADVISORY_ONLY, Outcome.ADVISORY_ONLY_BLOCKED, false,
                "Mode is ADVISORY_ONLY — proposed changes listed in plan artifact, after-test skipped");
    }

    /** AUTO_LOW_RISK: impact level is not low enough. */
    public static AutonomyGateResult blockedByImpact(String impactLevel, String allowedUpTo) {
        return new AutonomyGateResult(
                AutonomyMode.AUTO_LOW_RISK, Outcome.LOW_RISK_BLOCKED_IMPACT, false,
                "AUTO_LOW_RISK gate: impact level %s exceeds allowed threshold %s — escalated to REQUIRES_APPROVAL"
                        .formatted(impactLevel, allowedUpTo));
    }

    /** AUTO_LOW_RISK: confidence is below the required minimum. */
    public static AutonomyGateResult blockedByConfidence(double confidence, double minRequired) {
        return new AutonomyGateResult(
                AutonomyMode.AUTO_LOW_RISK, Outcome.LOW_RISK_BLOCKED_CONFIDENCE, false,
                "AUTO_LOW_RISK gate: confidence %.0f%% is below required %.0f%% — escalated to REQUIRES_APPROVAL"
                        .formatted(confidence * 100, minRequired * 100));
    }
}