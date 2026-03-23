package com.cloudoptimizer.agent.policy;

import com.cloudoptimizer.agent.model.RunResult;

import java.time.Instant;
import java.util.Set;

/**
 * Input object passed to a {@link PolicyEngine} for evaluation.
 *
 * <p>Captures everything the engine needs to know about the proposed change:
 * what the current state is, what the agent wants to do, how confident it is,
 * and when the last change was applied (for cooldown checking).
 *
 * <p>Use the {@link Builder} to construct instances.
 */
public final class PolicyContext {

    /** Current state of the system before applying any change. */
    private final RunResult baseline;

    /** Concurrency the agent proposes to set. */
    private final int proposedConcurrency;

    /** Heap size (MB) the agent proposes, or null if no heap change. */
    private final Integer proposedHeapSizeMb;

    /** Agent's overall confidence in its recommendation [0.0, 1.0]. */
    private final double confidenceScore;

    /** Agent's impact assessment (e.g. "HIGH"). Null if unavailable. */
    private final String impactLevel;

    /** Resources the agent is proposing to change (e.g. "jvm.concurrency"). */
    private final Set<String> proposedResources;

    /** When changes were last applied.  Null if this is the first run. */
    private final Instant lastAppliedAt;

    private PolicyContext(Builder b) {
        this.baseline           = b.baseline;
        this.proposedConcurrency = b.proposedConcurrency;
        this.proposedHeapSizeMb = b.proposedHeapSizeMb;
        this.confidenceScore    = b.confidenceScore;
        this.impactLevel        = b.impactLevel;
        this.proposedResources  = b.proposedResources;
        this.lastAppliedAt      = b.lastAppliedAt;
    }

    public RunResult getBaseline()            { return baseline; }
    public int getProposedConcurrency()       { return proposedConcurrency; }
    public Integer getProposedHeapSizeMb()    { return proposedHeapSizeMb; }
    public double getConfidenceScore()        { return confidenceScore; }
    public String getImpactLevel()            { return impactLevel; }
    public Set<String> getProposedResources() { return proposedResources; }
    public Instant getLastAppliedAt()         { return lastAppliedAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private RunResult baseline;
        private int proposedConcurrency;
        private Integer proposedHeapSizeMb;
        private double confidenceScore;
        private String impactLevel;
        private Set<String> proposedResources = Set.of();
        private Instant lastAppliedAt;

        public Builder baseline(RunResult v)          { baseline = v; return this; }
        public Builder proposedConcurrency(int v)      { proposedConcurrency = v; return this; }
        public Builder proposedHeapSizeMb(Integer v)   { proposedHeapSizeMb = v; return this; }
        public Builder confidenceScore(double v)       { confidenceScore = v; return this; }
        public Builder impactLevel(String v)           { impactLevel = v; return this; }
        public Builder proposedResources(Set<String> v){ proposedResources = v; return this; }
        public Builder lastAppliedAt(Instant v)        { lastAppliedAt = v; return this; }

        public PolicyContext build() {
            if (baseline == null) throw new IllegalStateException("baseline is required");
            return new PolicyContext(this);
        }
    }
}