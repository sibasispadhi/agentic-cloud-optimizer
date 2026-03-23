package com.cloudoptimizer.agent.benchmark;

/**
 * Immutable input parameters for a benchmark scenario.
 *
 * <p>All values have sane defaults via {@link #defaults()} so callers only
 * need to override what they care about.
 */
public final class ScenarioConfig {

    private final int    baselineConcurrency;
    private final int    durationSeconds;
    private final double targetLatencyMs;
    private final double targetRps;
    private final double retryAmplificationFactor; // for retry-storm family scenarios

    private ScenarioConfig(Builder b) {
        this.baselineConcurrency      = b.baselineConcurrency;
        this.durationSeconds          = b.durationSeconds;
        this.targetLatencyMs          = b.targetLatencyMs;
        this.targetRps                = b.targetRps;
        this.retryAmplificationFactor = b.retryAmplificationFactor;
    }

    public int    getBaselineConcurrency()      { return baselineConcurrency; }
    public int    getDurationSeconds()          { return durationSeconds; }
    public double getTargetLatencyMs()          { return targetLatencyMs; }
    public double getTargetRps()                { return targetRps; }
    public double getRetryAmplificationFactor() { return retryAmplificationFactor; }

    /** Returns a config matching the plan's recommended benchmark defaults. */
    public static ScenarioConfig defaults() {
        return builder().build();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int    baselineConcurrency      = 4;
        private int    durationSeconds          = 30;
        private double targetLatencyMs          = 100.0;
        private double targetRps                = 0.0;
        private double retryAmplificationFactor = 3.0;

        public Builder baselineConcurrency(int v)      { baselineConcurrency = v;      return this; }
        public Builder durationSeconds(int v)          { durationSeconds = v;          return this; }
        public Builder targetLatencyMs(double v)       { targetLatencyMs = v;          return this; }
        public Builder targetRps(double v)             { targetRps = v;                return this; }
        public Builder retryAmplificationFactor(double v){ retryAmplificationFactor = v; return this; }

        public ScenarioConfig build() { return new ScenarioConfig(this); }
    }
}