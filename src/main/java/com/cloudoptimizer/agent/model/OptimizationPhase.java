package com.cloudoptimizer.agent.model;

/**
 * Enumeration of optimization phases for real-time progress tracking.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since December 2025
 */
public enum OptimizationPhase {
    INITIALIZING("Initializing", 0),
    BASELINE_RUNNING("Running Baseline Test", 25),
    BASELINE_COMPLETE("Baseline Complete", 35),
    SLO_CHECKING("Checking SLO Compliance", 40),
    LLM_ANALYZING("AI Analyzing Metrics", 50),
    LLM_COMPLETE("AI Decision Ready", 60),
    OPTIMIZATION_RUNNING("Running Optimized Test", 75),
    OPTIMIZATION_COMPLETE("Optimization Complete", 85),
    GENERATING_REPORT("Generating Report", 95),
    COMPLETE("Complete", 100);

    private final String displayName;
    private final int progressPercent;

    OptimizationPhase(String displayName, int progressPercent) {
        this.displayName = displayName;
        this.progressPercent = progressPercent;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getProgressPercent() {
        return progressPercent;
    }
}
