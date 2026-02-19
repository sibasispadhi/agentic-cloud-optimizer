package com.cloudoptimizer.agent.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Service Level Objective (SLO) policy for performance monitoring.
 * 
 * Defines thresholds and detection windows for SLO breach detection.
 * Critical for FinTech applications where transaction latency SLAs
 * must be enforced (e.g., p99 < 100ms).
 * 
 * <p>This is a MONITORING/ALERTING mechanism, not governance.
 * It triggers agent analysis but does not constrain agent actions.</p>
 * 
 * <p><b>Example FinTech SLO:</b></p>
 * <pre>
 * SloPolicy policy = SloPolicy.builder()
 *     .targetP99Ms(100.0)              // p99 must be under 100ms
 *     .breachThreshold(1.2)            // Breach if > 120ms (20% over)
 *     .breachWindowIntervals(3)        // Must breach for 3 consecutive checks
 *     .build();
 * </pre>
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since 2026
 */
@Getter
@Builder
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SloPolicy {

    /**
     * Target p99 latency in milliseconds.
     * Example: 100.0 for FinTech transaction SLA.
     */
    @JsonProperty("target_p99_ms")
    @Builder.Default
    private final double targetP99Ms = 100.0;

    /**
     * Breach threshold multiplier.
     * Breach occurs when p99 > (targetP99Ms * breachThreshold).
     * Example: 1.2 means breach at 20% over target.
     */
    @JsonProperty("breach_threshold")
    @Builder.Default
    private final double breachThreshold = 1.2;

    /**
     * Number of consecutive intervals that must breach before triggering.
     * Prevents false alarms from transient spikes.
     * Example: 3 means must breach for 3 consecutive measurements.
     */
    @JsonProperty("breach_window_intervals")
    @Builder.Default
    private final int breachWindowIntervals = 3;

    /**
     * Optional: Target p95 latency in milliseconds.
     * If set, p95 breaches will also be detected.
     */
    @JsonProperty("target_p95_ms")
    private final Double targetP95Ms;

    /**
     * Optional: Target median latency in milliseconds.
     * If set, median breaches will also be detected.
     */
    @JsonProperty("target_median_ms")
    private final Double targetMedianMs;

    /**
     * Description of this SLO policy for logging/reporting.
     */
    @JsonProperty("description")
    @Builder.Default
    private final String description = "Default FinTech p99 SLO";

    /**
     * Calculates the absolute breach threshold in milliseconds.
     * 
     * @return threshold in ms where breach occurs
     */
    public double getAbsoluteP99ThresholdMs() {
        return targetP99Ms * breachThreshold;
    }

    /**
     * Checks if a p99 value breaches the SLO.
     * 
     * @param p99Ms measured p99 latency in milliseconds
     * @return true if p99 exceeds threshold
     */
    public boolean isP99Breached(double p99Ms) {
        return p99Ms > getAbsoluteP99ThresholdMs();
    }

    /**
     * Checks if a p95 value breaches the SLO (if p95 target is set).
     * 
     * @param p95Ms measured p95 latency in milliseconds
     * @return true if p95 exceeds threshold, false if no p95 target set
     */
    public boolean isP95Breached(double p95Ms) {
        if (targetP95Ms == null) {
            return false;
        }
        return p95Ms > (targetP95Ms * breachThreshold);
    }

    /**
     * Checks if median value breaches the SLO (if median target is set).
     * 
     * @param medianMs measured median latency in milliseconds
     * @return true if median exceeds threshold, false if no median target set
     */
    public boolean isMedianBreached(double medianMs) {
        if (targetMedianMs == null) {
            return false;
        }
        return medianMs > (targetMedianMs * breachThreshold);
    }
}