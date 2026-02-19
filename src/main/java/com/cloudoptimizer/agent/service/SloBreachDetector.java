package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.model.RunResult;
import com.cloudoptimizer.agent.model.SloPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Detects Service Level Objective (SLO) breaches for performance monitoring.
 * 
 * <p>Implements threshold-based breach detection with configurable windows
 * to prevent false alarms from transient spikes. Critical for FinTech
 * applications where p99 latency SLAs must be strictly enforced.</p>
 * 
 * <p><b>Architecture Layer:</b> Monitoring/Alerting (NOT governance)</p>
 * <ul>
 *   <li>✅ Detects SLO breaches → Triggers agent analysis</li>
 *   <li>❌ Does NOT constrain agent actions (that's ARG governance)</li>
 * </ul>
 * 
 * <p><b>Example Usage:</b></p>
 * <pre>
 * SloPolicy policy = SloPolicy.builder()
 *     .targetP99Ms(100.0)
 *     .breachThreshold(1.2)
 *     .breachWindowIntervals(3)
 *     .build();
 * 
 * SloBreachDetector detector = new SloBreachDetector();
 * detector.recordMetric(baselineRun);
 * detector.recordMetric(afterRun);
 * 
 * if (detector.isBreached(policy)) {
 *     System.out.println("SLO BREACH: " + detector.getBreachReason());
 *     // Trigger agent optimization
 * }
 * </pre>
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since 2026
 */
@Service
public class SloBreachDetector {

    private static final Logger log = LoggerFactory.getLogger(SloBreachDetector.class);
    
    /**
     * Recent measurement history for breach detection.
     * Uses LinkedList for efficient add/remove at head/tail.
     */
    private final LinkedList<RunResult> recentMeasurements = new LinkedList<>();
    
    /**
     * Maximum measurements to retain in history.
     * Prevents unbounded memory growth.
     */
    private static final int MAX_HISTORY_SIZE = 100;
    
    /**
     * Last detected breach reason (for reporting).
     */
    private String lastBreachReason = null;

    /**
     * Records a new metric measurement for breach detection.
     * 
     * @param runResult the load test run result to record
     */
    public void recordMetric(RunResult runResult) {
        if (runResult == null) {
            log.warn("Attempted to record null RunResult");
            return;
        }
        
        recentMeasurements.addLast(runResult);
        
        // Limit history size
        while (recentMeasurements.size() > MAX_HISTORY_SIZE) {
            recentMeasurements.removeFirst();
        }
        
        log.debug("Recorded metric: p99={}ms (history size: {})", 
                runResult.getP99LatencyMs(), recentMeasurements.size());
    }

    /**
     * Checks if SLO is currently breached based on recent measurements.
     * 
     * <p>Breach occurs when consecutive measurements exceed threshold
     * for the configured window size.</p>
     * 
     * @param policy the SLO policy defining thresholds and windows
     * @return true if SLO is breached
     */
    public boolean isBreached(SloPolicy policy) {
        if (policy == null) {
            log.warn("Cannot check breach with null policy");
            return false;
        }
        
        int requiredWindow = policy.getBreachWindowIntervals();
        
        if (recentMeasurements.size() < requiredWindow) {
            log.debug("Insufficient measurements ({} < {}) for breach detection", 
                    recentMeasurements.size(), requiredWindow);
            return false;
        }
        
        // Check last N consecutive measurements
        List<RunResult> window = new ArrayList<>();
        int startIndex = Math.max(0, recentMeasurements.size() - requiredWindow);
        for (int i = startIndex; i < recentMeasurements.size(); i++) {
            window.add(recentMeasurements.get(i));
        }
        
        return detectBreach(window, policy);
    }

    /**
     * Internal breach detection logic.
     * 
     * @param window list of consecutive measurements to check
     * @param policy SLO policy
     * @return true if all measurements in window breach SLO
     */
    private boolean detectBreach(List<RunResult> window, SloPolicy policy) {
        List<String> breachReasons = new ArrayList<>();
        
        for (int i = 0; i < window.size(); i++) {
            RunResult result = window.get(i);
            boolean intervalBreached = false;
            StringBuilder reason = new StringBuilder();
            
            // Check p99 breach (primary SLO)
            if (policy.isP99Breached(result.getP99LatencyMs())) {
                reason.append(String.format("p99=%.2fms > %.2fms", 
                        result.getP99LatencyMs(), 
                        policy.getAbsoluteP99ThresholdMs()));
                intervalBreached = true;
            }
            
            // Check p95 breach (if policy has p95 target)
            if (policy.isP95Breached(result.getP95LatencyMs())) {
                if (reason.length() > 0) reason.append(", ");
                reason.append(String.format("p95=%.2fms > %.2fms", 
                        result.getP95LatencyMs(), 
                        policy.getTargetP95Ms() * policy.getBreachThreshold()));
                intervalBreached = true;
            }
            
            // Check median breach (if policy has median target)
            if (policy.isMedianBreached(result.getMedianLatencyMs())) {
                if (reason.length() > 0) reason.append(", ");
                reason.append(String.format("median=%.2fms > %.2fms", 
                        result.getMedianLatencyMs(), 
                        policy.getTargetMedianMs() * policy.getBreachThreshold()));
                intervalBreached = true;
            }
            
            if (!intervalBreached) {
                // Window broken - not all consecutive intervals breached
                log.debug("SLO breach window broken at interval {} (no breach)", i);
                lastBreachReason = null;
                return false;
            }
            
            breachReasons.add(String.format("Interval %d: %s", i + 1, reason.toString()));
        }
        
        // All intervals in window breached!
        lastBreachReason = String.format(
                "SLO BREACH detected over %d consecutive intervals:\n  - %s\n  Policy: %s",
                window.size(),
                String.join("\n  - ", breachReasons),
                policy.getDescription()
        );
        
        log.warn("SLO BREACH DETECTED: {}", lastBreachReason);
        
        return true;
    }

    /**
     * Gets the reason for the last detected breach.
     * 
     * @return breach reason string, or null if no recent breach
     */
    public String getBreachReason() {
        return lastBreachReason;
    }

    /**
     * Gets the current measurement history size.
     * 
     * @return number of measurements in history
     */
    public int getHistorySize() {
        return recentMeasurements.size();
    }

    /**
     * Clears measurement history.
     * Useful for testing or resetting state.
     */
    public void clearHistory() {
        recentMeasurements.clear();
        lastBreachReason = null;
        log.info("Cleared SLO breach detector history");
    }

    /**
     * Gets a defensive copy of recent measurements.
     * 
     * @return list of recent measurements (copy, not live view)
     */
    public List<RunResult> getRecentMeasurements() {
        return new ArrayList<>(recentMeasurements);
    }
}