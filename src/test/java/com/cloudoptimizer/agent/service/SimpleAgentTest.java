package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SimpleAgent rule-based optimization logic.
 *
 * <p>Covers:</p>
 * <ul>
 *   <li>All three concurrency rules (increase / decrease / maintain)</li>
 *   <li>Boundary conditions for each concurrency rule</li>
 *   <li>All four heap-sizing rules</li>
 *   <li>Null / empty input validation</li>
 *   <li>Decision structure completeness</li>
 * </ul>
 *
 * @author Sibasis Padhi
 * @version 1.0
 * @since 2025
 */
class SimpleAgentTest {

    private SimpleAgent agent;

    // Mirror the defaults from application.yml so tests are self-consistent
    private static final double TARGET_LATENCY         = 100.0;
    private static final double LOW_THRESHOLD_FACTOR   = 0.7;
    private static final double SCALE_UP_FACTOR        = 1.5;
    private static final double SCALE_DOWN_FACTOR      = 0.75;
    private static final double GC_FREQ_HIGH           = 1.0;
    private static final double USAGE_HIGH             = 80.0;
    private static final double GC_PAUSE_HIGH          = 100.0;
    private static final double USAGE_MEDIUM           = 70.0;
    private static final double GC_FREQ_LOW            = 0.5;
    private static final double USAGE_LOW              = 50.0;
    private static final double HEAP_SCALE_UP_LARGE    = 1.5;
    private static final double HEAP_SCALE_UP_SMALL    = 1.25;
    private static final double HEAP_SCALE_DOWN        = 0.75;
    private static final int    HEAP_MIN_MB            = 256;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        agent = new SimpleAgent();
        setField("targetLatencyMs",         TARGET_LATENCY);
        setField("artifactsDir",            tempDir.toString());
        setField("lowThresholdFactor",      LOW_THRESHOLD_FACTOR);
        setField("concurrencyScaleUpFactor",   SCALE_UP_FACTOR);
        setField("concurrencyScaleDownFactor", SCALE_DOWN_FACTOR);
        setField("heapGcFreqHighPerSec",    GC_FREQ_HIGH);
        setField("heapUsageHighPercent",    USAGE_HIGH);
        setField("heapGcPauseHighMs",       GC_PAUSE_HIGH);
        setField("heapUsageMediumPercent",  USAGE_MEDIUM);
        setField("heapGcFreqLowPerSec",     GC_FREQ_LOW);
        setField("heapUsageLowPercent",     USAGE_LOW);
        setField("heapScaleUpLargeFactor",  HEAP_SCALE_UP_LARGE);
        setField("heapScaleUpSmallFactor",  HEAP_SCALE_UP_SMALL);
        setField("heapScaleDownFactor",     HEAP_SCALE_DOWN);
        setField("heapMinMb",               HEAP_MIN_MB);
    }

    // =========================================================================
    // Concurrency rule tests  (decide)
    // =========================================================================

    @Test
    void testHighLatencyIncreaseConcurrency() {
        // 150ms > 100ms target  →  4 * 1.5 = 6
        AgentDecision d = agent.decide(metrics(150.0, 10), 4);

        assertNotNull(d);
        assertEquals(AgentStrategy.RULE_BASED, d.getStrategy());
        assertTrue(d.getRecommendation().toLowerCase().contains("6"),
                "Should increase concurrency to 6");
        assertTrue(d.getReasoning().toLowerCase().contains("increas"));
        assertTrue(d.getReasoning().toLowerCase().contains("exceeds"));
        assertEquals(0.95, d.getConfidenceScore(), 0.01);
        assertFalse(d.getActionItems().isEmpty());
    }

    @Test
    void testLowLatencyDecreaseConcurrency() {
        // 50ms < 70ms threshold  →  8 * 0.75 = 6
        AgentDecision d = agent.decide(metrics(50.0, 10), 8);

        assertNotNull(d);
        assertTrue(d.getRecommendation().toLowerCase().contains("6"),
                "Should decrease concurrency to 6");
        assertTrue(d.getReasoning().toLowerCase().contains("decreas"));
        assertTrue(d.getReasoning().toLowerCase().contains("below"));
        assertEquals(AgentDecision.ImpactLevel.LOW, d.getImpactLevel());
    }

    @Test
    void testAcceptableLatencyMaintainConcurrency() {
        // 85ms is in [70ms, 100ms] range  →  keep 4
        AgentDecision d = agent.decide(metrics(85.0, 10), 4);

        assertNotNull(d);
        assertTrue(d.getRecommendation().toLowerCase().contains("4"));
        assertTrue(d.getReasoning().toLowerCase().contains("maintain")
                || d.getReasoning().toLowerCase().contains("within"));
        assertEquals(AgentDecision.ImpactLevel.LOW, d.getImpactLevel());
    }

    @Test
    void testExactTargetLatency() {
        // exactly 100ms  →  within range, maintain
        AgentDecision d = agent.decide(metrics(100.0, 10), 4);
        assertTrue(d.getRecommendation().toLowerCase().contains("4"));
    }

    @Test
    void testLowThresholdBoundary() {
        // exactly 70ms (= target × 0.7)  →  NOT below threshold, maintain
        AgentDecision d = agent.decide(metrics(70.0, 10), 4);
        assertTrue(d.getRecommendation().toLowerCase().contains("4"),
                "At exact boundary should maintain");
    }

    @Test
    void testVeryHighLatency() {
        // 200ms  →  4 * 1.5 = 6, MEDIUM impact
        AgentDecision d = agent.decide(metrics(200.0, 10), 4);
        assertTrue(d.getRecommendation().toLowerCase().contains("6"));
        assertEquals(AgentDecision.ImpactLevel.MEDIUM, d.getImpactLevel());
    }

    @Test
    void testVeryLowLatency() {
        // 20ms  →  8 * 0.75 = 6
        AgentDecision d = agent.decide(metrics(20.0, 10), 8);
        assertTrue(d.getRecommendation().toLowerCase().contains("6"));
    }

    @Test
    void testMinimumConcurrencyFloor() {
        // concurrency 1 with low latency  →  must stay at 1 (floor)
        AgentDecision d = agent.decide(metrics(20.0, 10), 1);
        assertTrue(d.getRecommendation().toLowerCase().contains("1"),
                "Should not go below 1");
    }

    @Test
    void testDecisionStructure() {
        AgentDecision d = agent.decide(metrics(150.0, 10), 4);

        assertNotNull(d.getDecisionId());
        assertNotNull(d.getTimestamp());
        assertEquals("concurrency-pool", d.getResourceId());
        assertEquals("ExecutorService", d.getResourceType());
        assertEquals(AgentStrategy.RULE_BASED, d.getStrategy());
        assertNotNull(d.getRecommendation());
        assertNotNull(d.getReasoning());
        assertTrue(d.getConfidenceScore() >= 0.0 && d.getConfidenceScore() <= 1.0);
        assertNotNull(d.getImpactLevel());
        assertTrue(d.getActionItems().size() >= 2);
        assertTrue(d.getMetricsAnalyzed().contains("latencyMs"));
    }

    @Test
    void testEmptyMetricsThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> agent.decide(new ArrayList<>(), 4));
    }

    @Test
    void testNullMetricsThrowsException() {
        assertThrows(Exception.class,
                () -> agent.decide(null, 4));
    }

    @Test
    void testMedianCalculation() {
        // Latencies: [50, 100, 150, 200, 250]  →  median = 150ms  →  exceed target
        List<MetricRow> mixed = new ArrayList<>();
        for (double l : new double[]{50.0, 100.0, 150.0, 200.0, 250.0}) {
            mixed.add(metric(l));
        }
        AgentDecision d = agent.decide(mixed, 4);
        assertTrue(d.getReasoning().toLowerCase().contains("increase")
                || d.getReasoning().toLowerCase().contains("exceeds"));
    }

    @Test
    void testConfidenceScoreConsistency() {
        AgentDecision d1 = agent.decide(metrics(150.0, 10), 4);
        AgentDecision d2 = agent.decide(metrics(50.0, 10), 8);
        AgentDecision d3 = agent.decide(metrics(85.0, 10), 4);

        assertEquals(0.95, d1.getConfidenceScore(), 0.01);
        assertEquals(0.95, d2.getConfidenceScore(), 0.01);
        assertEquals(0.95, d3.getConfidenceScore(), 0.01);
    }

    // =========================================================================
    // Heap analysis tests  (decideWithHeapAnalysis)
    // =========================================================================

    @Test
    void testHeapRule1_HighGcFreqAndHighUsage_ScalesUpLarge() {
        // GC freq 1.5/sec (> 1.0) AND heap usage 85% (> 80%)  →  heap * 1.5
        RunResult baseline = baselineResult(150.0, 512, 85.0, 1.5, 50.0);
        AgentDecision d = agent.decideWithHeapAnalysis(baseline, 4);

        assertNotNull(d.getRecommendedHeapSizeMb());
        assertEquals((int) Math.ceil(512 * HEAP_SCALE_UP_LARGE),
                d.getRecommendedHeapSizeMb());
        assertTrue(d.getReasoning().toLowerCase().contains("gc frequency"));
        assertTrue(d.getReasoning().toLowerCase().contains("increasing heap"));
        assertEquals(AgentDecision.ImpactLevel.MEDIUM, d.getImpactLevel());
        assertTrue(d.getMetricsAnalyzed().contains("heapMetrics"));
    }

    @Test
    void testHeapRule2_HighGcPauseAndMediumUsage_ScalesUpSmall() {
        // GC pause 150ms (> 100ms) AND heap usage 75% (> 70%)  →  heap * 1.25
        // GC freq 0.6 so Rule 1 does NOT trigger
        RunResult baseline = baselineResult(150.0, 512, 75.0, 0.6, 150.0);
        AgentDecision d = agent.decideWithHeapAnalysis(baseline, 4);

        assertNotNull(d.getRecommendedHeapSizeMb());
        assertEquals((int) Math.ceil(512 * HEAP_SCALE_UP_SMALL),
                d.getRecommendedHeapSizeMb());
        assertTrue(d.getReasoning().toLowerCase().contains("gc pause time"));
        assertTrue(d.getReasoning().toLowerCase().contains("increasing heap"));
        assertEquals(AgentDecision.ImpactLevel.MEDIUM, d.getImpactLevel());
    }

    @Test
    void testHeapRule3_LowGcFreqAndLowUsage_ScalesDown() {
        // GC freq 0.3/sec (< 0.5) AND heap usage 30% (< 50%)  →  heap * 0.75, floored at 256MB
        RunResult baseline = baselineResult(85.0, 1024, 30.0, 0.3, 20.0);
        AgentDecision d = agent.decideWithHeapAnalysis(baseline, 4);

        assertNotNull(d.getRecommendedHeapSizeMb());
        int expected = (int) Math.max(HEAP_MIN_MB, Math.floor(1024 * HEAP_SCALE_DOWN));
        assertEquals(expected, d.getRecommendedHeapSizeMb());
        assertTrue(d.getReasoning().toLowerCase().contains("over-provisioning"));
        assertTrue(d.getReasoning().toLowerCase().contains("decreasing heap"));
    }

    @Test
    void testHeapRule3_MinFloorEnforced() {
        // Tiny heap (200MB) + low GC  →  0.75 * 200 = 150, but floor is 256MB
        RunResult baseline = baselineResult(85.0, 200, 20.0, 0.2, 10.0);
        AgentDecision d = agent.decideWithHeapAnalysis(baseline, 4);

        assertNotNull(d.getRecommendedHeapSizeMb());
        assertEquals(HEAP_MIN_MB, d.getRecommendedHeapSizeMb(),
                "Floor of " + HEAP_MIN_MB + "MB must be respected");
    }

    @Test
    void testHeapOptimal_NoChange() {
        // GC freq 0.7 (between 0.5 and 1.0), heap 60% (between 50% and 80%)  →  maintain
        RunResult baseline = baselineResult(85.0, 512, 60.0, 0.7, 40.0);
        AgentDecision d = agent.decideWithHeapAnalysis(baseline, 4);

        assertNotNull(d.getRecommendedHeapSizeMb());
        assertEquals(512, d.getRecommendedHeapSizeMb(),
                "Optimal heap should be unchanged");
        assertTrue(d.getReasoning().toLowerCase().contains("maintaining heap"));
    }

    @Test
    void testDecideWithHeapAnalysis_NoHeapMetrics() {
        // Null heap metrics  →  decision still made, no heap recommendation
        RunResult baseline = RunResult.builder()
                .concurrency(4)
                .medianLatencyMs(150.0)
                .build();
        AgentDecision d = agent.decideWithHeapAnalysis(baseline, 4);

        assertNotNull(d);
        assertNull(d.getRecommendedHeapSizeMb(),
                "No heap recommendation when metrics are absent");
        assertFalse(d.getMetricsAnalyzed().contains("heapMetrics"));
    }

    @Test
    void testDecideWithHeapAnalysis_NullBaseline() {
        assertThrows(IllegalArgumentException.class,
                () -> agent.decideWithHeapAnalysis(null, 4));
    }

    @Test
    void testDecideWithHeapAnalysis_ConcurrencyAndHeapBothRecommended() {
        // High latency AND high GC pressure  →  both concurrency AND heap change
        RunResult baseline = baselineResult(150.0, 512, 85.0, 1.5, 50.0);
        AgentDecision d = agent.decideWithHeapAnalysis(baseline, 4);

        // Concurrency increased
        assertTrue(d.getRecommendation().toLowerCase().contains("6"));
        // Heap also updated
        assertNotNull(d.getRecommendedHeapSizeMb());
        // Both action items present
        assertTrue(d.getActionItems().size() >= 2);
        assertTrue(d.getMetricsAnalyzed().contains("latencyMs"));
        assertTrue(d.getMetricsAnalyzed().contains("heapMetrics"));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private List<MetricRow> metrics(double latencyMs, int count) {
        List<MetricRow> list = new ArrayList<>();
        for (int i = 0; i < count; i++) list.add(metric(latencyMs));
        return list;
    }

    private MetricRow metric(double latencyMs) {
        return MetricRow.builder()
                .resourceId("test-resource")
                .resourceType("TestService")
                .metricName("latencyMs")
                .metricValue(latencyMs)
                .unit("ms")
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates a RunResult with embedded HeapMetrics for heap rule tests.
     *
     * @param medianLatency   latency in ms
     * @param heapSizeMb      current heap size in MB
     * @param heapUsagePct    heap usage %
     * @param gcFreqPerSec    GC events per second
     * @param gcPauseAvgMs    average GC pause in ms
     */
    private RunResult baselineResult(double medianLatency, long heapSizeMb,
                                     double heapUsagePct, double gcFreqPerSec,
                                     double gcPauseAvgMs) {
        HeapMetrics hm = HeapMetrics.builder()
                .heapSizeMb(heapSizeMb)
                .heapUsedMb((long) (heapSizeMb * heapUsagePct / 100))
                .heapUsagePercent(heapUsagePct)
                .gcFrequencyPerSec(gcFreqPerSec)
                .gcPauseTimeAvgMs(gcPauseAvgMs)
                .gcCount(100)
                .gcTimeMs((long) (gcPauseAvgMs * 100))
                .build();
        return RunResult.builder()
                .concurrency(4)
                .medianLatencyMs(medianLatency)
                .heapMetrics(hm)
                .build();
    }

    /** Injects a value into a private field via reflection. */
    private void setField(String name, Object value) {
        try {
            java.lang.reflect.Field f = SimpleAgent.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(agent, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + name, e);
        }
    }
}