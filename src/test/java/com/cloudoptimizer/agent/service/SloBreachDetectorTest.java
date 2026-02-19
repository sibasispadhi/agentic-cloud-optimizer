package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.model.RunResult;
import com.cloudoptimizer.agent.model.SloPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SLO breach detection.
 * 
 * Tests threshold-based breach detection with configurable windows
 * for FinTech transaction SLAs.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since 2026
 */
public class SloBreachDetectorTest {

    private SloBreachDetector detector;
    private SloPolicy defaultPolicy;

    @BeforeEach
    public void setUp() {
        detector = new SloBreachDetector();
        
        // Default FinTech policy: p99 must be under 100ms
        defaultPolicy = SloPolicy.builder()
                .targetP99Ms(100.0)
                .breachThreshold(1.2)  // Breach at 120ms
                .breachWindowIntervals(3)
                .description("FinTech p99 < 100ms SLA")
                .build();
    }

    @Test
    public void testNoBreachWhenUnderThreshold() {
        // Given: 3 measurements all under threshold (p99 = 90ms)
        for (int i = 0; i < 3; i++) {
            RunResult result = createRunResult(90.0);
            detector.recordMetric(result);
        }

        // When: Check for breach
        boolean breached = detector.isBreached(defaultPolicy);

        // Then: No breach
        assertFalse(breached, "Should not breach when p99 < 120ms");
        assertNull(detector.getBreachReason(), "Should have no breach reason");
        
        System.out.println("✅ No breach: p99=90ms < 120ms threshold");
    }

    @Test
    public void testBreachWhenConsecutivelyOver() {
        // Given: 3 consecutive measurements over threshold (p99 = 150ms)
        for (int i = 0; i < 3; i++) {
            RunResult result = createRunResult(150.0);
            detector.recordMetric(result);
        }

        // When: Check for breach
        boolean breached = detector.isBreached(defaultPolicy);

        // Then: Breach detected!
        assertTrue(breached, "Should breach when p99 > 120ms for 3 intervals");
        assertNotNull(detector.getBreachReason(), "Should have breach reason");
        assertTrue(detector.getBreachReason().contains("150.00ms"), 
                "Breach reason should mention actual p99");
        assertTrue(detector.getBreachReason().contains("120.00ms"), 
                "Breach reason should mention threshold");
        
        System.out.println("✅ Breach detected: p99=150ms > 120ms for 3 intervals");
        System.out.println("   Reason: " + detector.getBreachReason());
    }

    @Test
    public void testNoBreachWhenWindowBroken() {
        // Given: 2 over, 1 under, 1 over (window broken)
        detector.recordMetric(createRunResult(150.0));  // Over
        detector.recordMetric(createRunResult(150.0));  // Over
        detector.recordMetric(createRunResult(80.0));   // UNDER (breaks window!)
        detector.recordMetric(createRunResult(150.0));  // Over

        // When: Check for breach
        boolean breached = detector.isBreached(defaultPolicy);

        // Then: No breach (window was broken)
        assertFalse(breached, "Should not breach when consecutive window is broken");
        
        System.out.println("✅ No breach: Window broken by measurement 3 (p99=80ms)");
    }

    @Test
    public void testBreachAfterWindowRestored() {
        // Given: Window broken, then 3 more consecutive breaches
        detector.recordMetric(createRunResult(150.0));  // Over
        detector.recordMetric(createRunResult(80.0));   // Under (breaks window)
        detector.recordMetric(createRunResult(150.0));  // Over
        detector.recordMetric(createRunResult(150.0));  // Over
        detector.recordMetric(createRunResult(150.0));  // Over (3 consecutive!)

        // When: Check for breach
        boolean breached = detector.isBreached(defaultPolicy);

        // Then: Breach detected (last 3 are consecutive)
        assertTrue(breached, "Should breach on last 3 consecutive measurements");
        
        System.out.println("✅ Breach restored: Last 3 measurements consecutive over threshold");
    }

    @Test
    public void testInsufficientMeasurements() {
        // Given: Only 2 measurements (need 3 for breach)
        detector.recordMetric(createRunResult(150.0));
        detector.recordMetric(createRunResult(150.0));

        // When: Check for breach
        boolean breached = detector.isBreached(defaultPolicy);

        // Then: No breach (insufficient data)
        assertFalse(breached, "Should not breach with only 2 measurements");
        
        System.out.println("✅ No breach: Insufficient measurements (2 < 3 required)");
    }

    @Test
    public void testP95AndP99CombinedBreach() {
        // Given: Policy with both p95 and p99 targets
        SloPolicy strictPolicy = SloPolicy.builder()
                .targetP99Ms(100.0)
                .targetP95Ms(80.0)
                .breachThreshold(1.2)
                .breachWindowIntervals(2)
                .description("Strict multi-metric SLA")
                .build();

        // Given: 2 measurements breaching both p95 and p99
        RunResult result1 = RunResult.builder()
                .timestamp(Instant.now())
                .concurrency(4)
                .durationSeconds(10)
                .totalRequests(1000)
                .successfulRequests(1000)
                .failedRequests(0)
                .requestsPerSecond(100.0)
                .medianLatencyMs(50.0)
                .avgLatencyMs(60.0)
                .minLatencyMs(10.0)
                .maxLatencyMs(200.0)
                .p95LatencyMs(120.0)  // Breaches (> 96ms)
                .p99LatencyMs(150.0)  // Breaches (> 120ms)
                .costEstimateUsd(0.1)
                .heapMetrics(null)
                .build();

        detector.recordMetric(result1);
        detector.recordMetric(result1);

        // When: Check for breach
        boolean breached = detector.isBreached(strictPolicy);

        // Then: Breach detected (both metrics over)
        assertTrue(breached, "Should breach when both p95 and p99 exceed thresholds");
        assertTrue(detector.getBreachReason().contains("p95"), 
                "Breach reason should mention p95");
        assertTrue(detector.getBreachReason().contains("p99"), 
                "Breach reason should mention p99");
        
        System.out.println("✅ Multi-metric breach: Both p95 and p99 exceeded");
        System.out.println("   Reason: " + detector.getBreachReason());
    }

    @Test
    public void testClearHistory() {
        // Given: Some measurements recorded
        detector.recordMetric(createRunResult(150.0));
        detector.recordMetric(createRunResult(150.0));
        detector.recordMetric(createRunResult(150.0));
        
        assertTrue(detector.isBreached(defaultPolicy), "Should initially be breached");

        // When: Clear history
        detector.clearHistory();

        // Then: No breach (history cleared)
        assertFalse(detector.isBreached(defaultPolicy), "Should not breach after clearing");
        assertEquals(0, detector.getHistorySize(), "History size should be 0");
        assertNull(detector.getBreachReason(), "Breach reason should be cleared");
        
        System.out.println("✅ History cleared successfully");
    }

    @Test
    public void testFinTechScenario() {
        // Realistic FinTech scenario: Transaction processing SLA
        System.out.println("\n📊 FinTech Transaction SLA Scenario:");
        System.out.println("   Target: p99 < 100ms (breach at 120ms)");
        System.out.println("   Window: 3 consecutive intervals\n");

        // Hour 1: Normal operation (p99 = 85ms)
        System.out.println("Hour 1: Normal operation");
        for (int i = 0; i < 3; i++) {
            detector.recordMetric(createRunResult(85.0));
            System.out.println("  Measurement " + (i + 1) + ": p99=85ms ✅");
        }
        assertFalse(detector.isBreached(defaultPolicy));
        System.out.println("  Status: SLA COMPLIANT\n");

        // Hour 2: Gradual degradation (p99 = 110ms)
        System.out.println("Hour 2: Traffic spike - approaching SLA limit");
        detector.recordMetric(createRunResult(110.0));
        System.out.println("  Measurement 4: p99=110ms ⚠️  (within SLA, but elevated)");
        assertFalse(detector.isBreached(defaultPolicy));
        System.out.println("  Status: SLA COMPLIANT (monitoring)\n");

        // Hour 3: SLA breach (p99 = 135ms)
        System.out.println("Hour 3: Performance degradation - SLA BREACH");
        detector.recordMetric(createRunResult(135.0));
        System.out.println("  Measurement 5: p99=135ms ❌ BREACH!");
        detector.recordMetric(createRunResult(135.0));
        System.out.println("  Measurement 6: p99=135ms ❌ BREACH!");
        detector.recordMetric(createRunResult(135.0));
        System.out.println("  Measurement 7: p99=135ms ❌ BREACH!");
        
        assertTrue(detector.isBreached(defaultPolicy));
        System.out.println("\n  🚨 SLO BREACH DETECTED!");
        System.out.println("  → Triggering autonomous agent for root cause analysis");
        System.out.println("  → Agent analyzing: heap usage, GC patterns, thread contention");
        System.out.println("  → Proposing bounded optimization (±25% parameter changes)\n");
        
        System.out.println("✅ FinTech scenario test passed!");
    }

    /**
     * Helper: Creates a RunResult with specified p99 latency.
     */
    private RunResult createRunResult(double p99Ms) {
        return RunResult.builder()
                .timestamp(Instant.now())
                .concurrency(4)
                .durationSeconds(10)
                .totalRequests(1000)
                .successfulRequests(1000)
                .failedRequests(0)
                .requestsPerSecond(100.0)
                .medianLatencyMs(p99Ms * 0.5)  // Median typically lower
                .avgLatencyMs(p99Ms * 0.6)
                .minLatencyMs(10.0)
                .maxLatencyMs(p99Ms * 1.2)
                .p95LatencyMs(p99Ms * 0.8)
                .p99LatencyMs(p99Ms)
                .costEstimateUsd(0.1)
                .heapMetrics(null)
                .build();
    }
}