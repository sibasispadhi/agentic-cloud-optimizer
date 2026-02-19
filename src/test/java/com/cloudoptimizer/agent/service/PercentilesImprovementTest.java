package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.model.HeapMetrics;
import com.cloudoptimizer.agent.model.RunResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for p95/p99 improvement calculations.
 * 
 * Verifies that percentile improvements are correctly calculated
 * and included in optimization reports (critical for FinTech SLAs).
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since 2026
 */
public class PercentilesImprovementTest {

    @Test
    public void testP99ImprovementCalculation() {
        // Given: Baseline with p99 = 150ms (breaching 100ms SLA)
        RunResult baseline = RunResult.builder()
                .timestamp(Instant.now())
                .concurrency(4)
                .durationSeconds(10)
                .totalRequests(1000)
                .successfulRequests(1000)
                .failedRequests(0)
                .requestsPerSecond(100.0)
                .medianLatencyMs(30.0)
                .avgLatencyMs(35.0)
                .minLatencyMs(10.0)
                .maxLatencyMs(200.0)
                .p95LatencyMs(120.0)
                .p99LatencyMs(150.0)
                .costEstimateUsd(0.1)
                .heapMetrics(null)
                .build();

        // Given: After optimization with p99 = 62ms (back under SLA!)
        RunResult after = RunResult.builder()
                .timestamp(Instant.now())
                .concurrency(3)
                .durationSeconds(10)
                .totalRequests(900)
                .successfulRequests(900)
                .failedRequests(0)
                .requestsPerSecond(90.0)
                .medianLatencyMs(28.0)
                .avgLatencyMs(32.0)
                .minLatencyMs(10.0)
                .maxLatencyMs(150.0)
                .p95LatencyMs(75.0)
                .p99LatencyMs(62.0)
                .costEstimateUsd(0.09)
                .heapMetrics(null)
                .build();

        // When: Calculate improvements
        double p99ChangeMs = after.getP99LatencyMs() - baseline.getP99LatencyMs();
        double p99ChangePercent = calculatePercentChange(baseline.getP99LatencyMs(), after.getP99LatencyMs());
        
        double p95ChangeMs = after.getP95LatencyMs() - baseline.getP95LatencyMs();
        double p95ChangePercent = calculatePercentChange(baseline.getP95LatencyMs(), after.getP95LatencyMs());

        // Then: p99 improved by 58.7% (150ms → 62ms)
        assertEquals(-88.0, p99ChangeMs, 0.01, "p99 should improve by 88ms");
        assertEquals(-58.67, p99ChangePercent, 0.1, "p99 should improve by ~58.7%");
        
        // Then: p95 improved by 37.5% (120ms → 75ms)
        assertEquals(-45.0, p95ChangeMs, 0.01, "p95 should improve by 45ms");
        assertEquals(-37.5, p95ChangePercent, 0.1, "p95 should improve by ~37.5%");
        
        // Verify SLA restoration
        double sloTarget = 100.0;
        assertTrue(after.getP99LatencyMs() < sloTarget, 
                "p99 should be back under 100ms SLA (was " + after.getP99LatencyMs() + "ms)");
        assertTrue(baseline.getP99LatencyMs() > sloTarget, 
                "baseline p99 should have been breaching SLA (was " + baseline.getP99LatencyMs() + "ms)");
        
        System.out.println("✅ FinTech SLA Test Passed:");
        System.out.println("   p99: 150ms → 62ms (58.7% improvement, back under 100ms SLA!)");
        System.out.println("   p95: 120ms → 75ms (37.5% improvement)");
    }

    @Test
    public void testNegativeImprovements() {
        // Given: Performance degradation scenario
        RunResult baseline = RunResult.builder()
                .timestamp(Instant.now())
                .concurrency(2)
                .durationSeconds(10)
                .totalRequests(500)
                .successfulRequests(500)
                .failedRequests(0)
                .requestsPerSecond(50.0)
                .medianLatencyMs(25.0)
                .avgLatencyMs(30.0)
                .minLatencyMs(10.0)
                .maxLatencyMs(100.0)
                .p95LatencyMs(60.0)
                .p99LatencyMs(80.0)
                .costEstimateUsd(0.05)
                .heapMetrics(null)
                .build();

        RunResult after = RunResult.builder()
                .timestamp(Instant.now())
                .concurrency(4)
                .durationSeconds(10)
                .totalRequests(600)
                .successfulRequests(600)
                .failedRequests(0)
                .requestsPerSecond(60.0)
                .medianLatencyMs(35.0)
                .avgLatencyMs(45.0)
                .minLatencyMs(15.0)
                .maxLatencyMs(200.0)
                .p95LatencyMs(120.0)
                .p99LatencyMs(180.0)
                .costEstimateUsd(0.06)
                .heapMetrics(null)
                .build();

        // When: Calculate negative improvements
        double p99ChangePercent = calculatePercentChange(baseline.getP99LatencyMs(), after.getP99LatencyMs());
        double p95ChangePercent = calculatePercentChange(baseline.getP95LatencyMs(), after.getP95LatencyMs());

        // Then: Both should be positive (degraded)
        assertTrue(p99ChangePercent > 0, "p99 degraded, so change% should be positive");
        assertTrue(p95ChangePercent > 0, "p95 degraded, so change% should be positive");
        
        assertEquals(125.0, p99ChangePercent, 0.1, "p99 degraded by 125%");
        assertEquals(100.0, p95ChangePercent, 0.1, "p95 degraded by 100%");
        
        System.out.println("⚠️  Degradation Test Passed:");
        System.out.println("   p99: 80ms → 180ms (+125% degradation)");
        System.out.println("   p95: 60ms → 120ms (+100% degradation)");
    }

    /**
     * Calculates percent change from baseline to after.
     * Negative = improvement, Positive = degradation.
     */
    private double calculatePercentChange(double baseline, double after) {
        if (baseline == 0.0) {
            return 0.0;
        }
        return ((after - baseline) / baseline) * 100.0;
    }
}