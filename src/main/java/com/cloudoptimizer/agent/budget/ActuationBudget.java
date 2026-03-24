package com.cloudoptimizer.agent.budget;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration-backed actuation budget for a single optimization run.
 *
 * <p>Values are bound from the {@code agent.budget.*} namespace in
 * {@code application.yml}.  Defaults are intentionally permissive so
 * existing behaviour is preserved out of the box.
 *
 * <p>Enforced by {@link ActuationBudgetLedger} before the after-test runs.
 */
@Component
@ConfigurationProperties(prefix = "agent.budget")
public class ActuationBudget {

    /**
     * Maximum number of distinct resource changes allowed per run.
     * Example: 2 means the agent may change at most two resources
     * (e.g. concurrency AND heap) in one optimization cycle.
     */
    private int maxChangesPerRun = 5;

    /**
     * Maximum sum of absolute percentage changes across all resources
     * in a single run.
     *
     * <p>Example: if concurrency changes by 25% and heap by 30%,
     * total delta = 55%.  A budget of 75% would allow this;
     * a budget of 50% would deny it.
     */
    private double maxTotalDeltaPct = 75.0;

    /**
     * Descriptive label for the blast radius scope of changes made by ACO.
     * Informational only — used in artifact output and audit trails.
     * Example values: {@code "single-service"}, {@code "environment"}, {@code "region"}.
     */
    private String blastRadiusScope = "single-service";

    // ── Getters & setters (required by @ConfigurationProperties) ─────────────

    public int getMaxChangesPerRun()              { return maxChangesPerRun; }
    public void setMaxChangesPerRun(int v)         { maxChangesPerRun = v; }

    public double getMaxTotalDeltaPct()            { return maxTotalDeltaPct; }
    public void setMaxTotalDeltaPct(double v)       { maxTotalDeltaPct = v; }

    public String getBlastRadiusScope()            { return blastRadiusScope; }
    public void setBlastRadiusScope(String v)       { blastRadiusScope = v; }
}