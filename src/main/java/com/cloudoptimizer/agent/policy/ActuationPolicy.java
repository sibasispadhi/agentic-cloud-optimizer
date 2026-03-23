package com.cloudoptimizer.agent.policy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configuration-backed policy constraints used by {@link DefaultPolicyEngine}.
 *
 * <p>All values are bound from the {@code agent.policy.*} namespace in
 * {@code application.yml}.  Defaults here match the defaults in that file;
 * the YAML values always take precedence at runtime.
 *
 * <p>Uses mutable setters so Spring's property-binding mechanism can inject
 * values without requiring a custom {@code @ConstructorBinding} configuration.
 */
@Component
@ConfigurationProperties(prefix = "agent.policy")
public class ActuationPolicy {

    /**
     * Maximum allowed concurrency delta as a percentage of the baseline.
     * Example: 50.0 means the concurrency may not increase or decrease by
     * more than 50% in a single run.
     */
    private double maxConcurrencyDeltaPct = 50.0;

    /**
     * Maximum allowed heap size delta as a percentage of the current heap.
     * Only evaluated when a heap change is proposed.
     */
    private double maxHeapDeltaPct = 50.0;

    /** Absolute minimum allowed concurrency. */
    private int minConcurrency = 1;

    /** Absolute maximum allowed concurrency. */
    private int maxConcurrency = 100;

    /** Absolute minimum allowed heap size in MB. */
    private int minHeapMb = 128;

    /** Absolute maximum allowed heap size in MB. */
    private int maxHeapMb = 16384;

    /**
     * Minimum agent confidence required to proceed automatically.
     * Decisions below this threshold are escalated to
     * {@link PolicyDecision.Outcome#REQUIRES_APPROVAL}.
     */
    private double minConfidenceToAct = 0.60;

    /**
     * Minimum seconds that must elapse between two applied changes.
     * A run attempted before the cooldown expires raises a violation.
     * Set to 0 to disable cooldown enforcement.
     */
    private int cooldownSeconds = 0;

    /**
     * Resource identifiers that must never be changed by the agent.
     * Any plan containing a change to a forbidden resource is denied.
     * Example: {@code ["jvm.heap_size_mb"]} to lock heap size.
     */
    private List<String> forbiddenResources = List.of();

    /**
     * Impact levels that require human approval rather than auto-execution.
     * Example: {@code ["CRITICAL"]} to require approval for critical changes.
     */
    private List<String> impactLevelsRequiringApproval = List.of("CRITICAL");

    // ── Getters & setters (required by @ConfigurationProperties) ─────────────

    public double getMaxConcurrencyDeltaPct()       { return maxConcurrencyDeltaPct; }
    public void setMaxConcurrencyDeltaPct(double v)  { maxConcurrencyDeltaPct = v; }

    public double getMaxHeapDeltaPct()               { return maxHeapDeltaPct; }
    public void setMaxHeapDeltaPct(double v)          { maxHeapDeltaPct = v; }

    public int getMinConcurrency()                   { return minConcurrency; }
    public void setMinConcurrency(int v)              { minConcurrency = v; }

    public int getMaxConcurrency()                   { return maxConcurrency; }
    public void setMaxConcurrency(int v)              { maxConcurrency = v; }

    public int getMinHeapMb()                        { return minHeapMb; }
    public void setMinHeapMb(int v)                   { minHeapMb = v; }

    public int getMaxHeapMb()                        { return maxHeapMb; }
    public void setMaxHeapMb(int v)                   { maxHeapMb = v; }

    public double getMinConfidenceToAct()            { return minConfidenceToAct; }
    public void setMinConfidenceToAct(double v)       { minConfidenceToAct = v; }

    public int getCooldownSeconds()                  { return cooldownSeconds; }
    public void setCooldownSeconds(int v)             { cooldownSeconds = v; }

    public List<String> getForbiddenResources()      { return forbiddenResources; }
    public void setForbiddenResources(List<String> v) { forbiddenResources = v; }

    public List<String> getImpactLevelsRequiringApproval()      { return impactLevelsRequiringApproval; }
    public void setImpactLevelsRequiringApproval(List<String> v) { impactLevelsRequiringApproval = v; }
}