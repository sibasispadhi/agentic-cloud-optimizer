package com.cloudoptimizer.agent.autonomy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the autonomy gate.
 *
 * <p>Bound from the {@code agent.autonomy.*} namespace in
 * {@code application.yml}.  All values have safe production defaults.
 */
@Component
@ConfigurationProperties(prefix = "agent.autonomy")
public class AutonomyConfig {

    /**
     * The active autonomy mode.
     * Defaults to {@link AutonomyMode#AUTO_POLICY_GOVERNED} which replicates
     * the pre-Phase-4 behaviour exactly — policy + budget gates enforced.
     */
    private AutonomyMode mode = AutonomyMode.AUTO_POLICY_GOVERNED;

    /**
     * For {@link AutonomyMode#AUTO_LOW_RISK}: the maximum impact level that
     * the agent is allowed to auto-actuate without human approval.
     *
     * <p>If the agent's decision carries a higher impact level, the run is
     * escalated to REQUIRES_APPROVAL regardless of confidence.
     * Valid values match {@link com.cloudoptimizer.agent.model.AgentDecision.ImpactLevel}
     * names: {@code LOW}, {@code MEDIUM}, {@code HIGH}, {@code CRITICAL}.
     */
    private String lowRiskMaxImpactLevel = "LOW";

    /**
     * For {@link AutonomyMode#AUTO_LOW_RISK}: the minimum agent confidence
     * score required for auto-actuation.  Range [0.0, 1.0].
     *
     * <p>If confidence is below this threshold the run is escalated to
     * REQUIRES_APPROVAL even when impact level is within bounds.
     */
    private double lowRiskMinConfidence = 0.75;

    // ── Getters & setters (required by @ConfigurationProperties) ─────────────

    public AutonomyMode getMode()                        { return mode; }
    public void setMode(AutonomyMode mode)               { this.mode = mode; }

    public String getLowRiskMaxImpactLevel()             { return lowRiskMaxImpactLevel; }
    public void setLowRiskMaxImpactLevel(String v)       { this.lowRiskMaxImpactLevel = v; }

    public double getLowRiskMinConfidence()              { return lowRiskMinConfidence; }
    public void setLowRiskMinConfidence(double v)        { this.lowRiskMinConfidence = v; }
}