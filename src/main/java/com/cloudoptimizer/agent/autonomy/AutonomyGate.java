package com.cloudoptimizer.agent.autonomy;

import com.cloudoptimizer.agent.model.AgentDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Stateless autonomy gate evaluated before the policy engine.
 *
 * <h2>Evaluation order (fail-fast)</h2>
 * <ol>
 *   <li>{@link AutonomyMode#OBSERVE_ONLY} → always blocked</li>
 *   <li>{@link AutonomyMode#ADVISORY_ONLY} → always blocked</li>
 *   <li>{@link AutonomyMode#AUTO_LOW_RISK} → check impact, then confidence</li>
 *   <li>{@link AutonomyMode#AUTO_POLICY_GOVERNED} → always permitted</li>
 * </ol>
 *
 * <p>The gate returns an {@link AutonomyGateResult} that is stored in the
 * plan artifact regardless of outcome.
 */
@Service
public class AutonomyGate {

    private static final Logger log = LoggerFactory.getLogger(AutonomyGate.class);

    /**
     * Impact level names ordered from least to most severe.
     * Used to compare {@code lowRiskMaxImpactLevel} against decision impact.
     */
    private static final List<String> IMPACT_SEVERITY_ORDER =
            List.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    /**
     * Evaluates whether actuation is permitted given the current
     * {@link AutonomyConfig} and the agent's decision.
     *
     * @param decision the agent's recommendation for this run
     * @param config   the active autonomy configuration
     * @return an {@link AutonomyGateResult} describing the outcome
     */
    public AutonomyGateResult evaluate(AgentDecision decision, AutonomyConfig config) {
        AutonomyMode mode = config.getMode();
        log.info("Autonomy gate: mode={} confidence={:.2f} impact={}",
                mode, decision.getConfidenceScore(),
                decision.getImpactLevel() != null ? decision.getImpactLevel().name() : "UNKNOWN");

        return switch (mode) {
            case OBSERVE_ONLY -> {
                log.info("Autonomy gate: OBSERVE_ONLY — actuation blocked");
                yield AutonomyGateResult.observeOnly();
            }
            case ADVISORY_ONLY -> {
                log.info("Autonomy gate: ADVISORY_ONLY — plan emitted, after-test skipped");
                yield AutonomyGateResult.advisoryOnly();
            }
            case AUTO_LOW_RISK -> evaluateLowRisk(decision, config);
            case AUTO_POLICY_GOVERNED -> {
                log.info("Autonomy gate: AUTO_POLICY_GOVERNED — proceeding to policy gate");
                yield AutonomyGateResult.proceed(mode, "AUTO_POLICY_GOVERNED: policy and budget gates govern all decisions");
            }
        };
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private AutonomyGateResult evaluateLowRisk(AgentDecision decision, AutonomyConfig config) {
        String impactLevel = decision.getImpactLevel() != null
                ? decision.getImpactLevel().name() : "UNKNOWN";
        String maxAllowed = config.getLowRiskMaxImpactLevel().toUpperCase();

        if (exceedsImpactThreshold(impactLevel, maxAllowed)) {
            log.warn("Autonomy gate: LOW_RISK blocked by impact {} > {}", impactLevel, maxAllowed);
            return AutonomyGateResult.blockedByImpact(impactLevel, maxAllowed);
        }

        double confidence = decision.getConfidenceScore();
        double minRequired = config.getLowRiskMinConfidence();
        if (confidence < minRequired) {
            log.warn("Autonomy gate: LOW_RISK blocked by confidence {:.2f} < {:.2f}", confidence, minRequired);
            return AutonomyGateResult.blockedByConfidence(confidence, minRequired);
        }

        log.info("Autonomy gate: AUTO_LOW_RISK — impact={} confidence={:.2f} — proceeding",
                impactLevel, confidence);
        return AutonomyGateResult.proceed(AutonomyMode.AUTO_LOW_RISK,
                "AUTO_LOW_RISK: impact %s within threshold %s, confidence %.0f%% >= %.0f%%"
                        .formatted(impactLevel, maxAllowed,
                                confidence * 100, minRequired * 100));
    }

    /**
     * Returns {@code true} if {@code candidateLevel} is more severe than
     * {@code maxAllowedLevel} (based on {@link #IMPACT_SEVERITY_ORDER}).
     *
     * <p>Unknown levels (not in the list) are treated as maximally severe.
     */
    static boolean exceedsImpactThreshold(String candidateLevel, String maxAllowedLevel) {
        int candidateIdx = IMPACT_SEVERITY_ORDER.indexOf(candidateLevel.toUpperCase());
        int maxIdx = IMPACT_SEVERITY_ORDER.indexOf(maxAllowedLevel.toUpperCase());
        // Treat unknown levels as beyond CRITICAL
        if (candidateIdx < 0) return true;
        if (maxIdx < 0) return false;
        return candidateIdx > maxIdx;
    }
}