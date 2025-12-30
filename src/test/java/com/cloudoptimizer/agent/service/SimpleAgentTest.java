package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.model.AgentDecision;
import com.cloudoptimizer.agent.model.AgentStrategy;
import com.cloudoptimizer.agent.model.MetricRow;
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
 * Tests the three main decision rules:
 * 1. High latency → INCREASE concurrency
 * 2. Low latency → DECREASE concurrency
 * 3. Within acceptable range → MAINTAIN concurrency
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since 2025
 */
class SimpleAgentTest {

    private SimpleAgent simpleAgent;
    private static final double TARGET_LATENCY = 100.0;
    private static final double LOW_THRESHOLD = TARGET_LATENCY * 0.7; // 70ms

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        simpleAgent = new SimpleAgent();
        // Use reflection to set private fields for testing
        setPrivateField(simpleAgent, "targetLatencyMs", TARGET_LATENCY);
        setPrivateField(simpleAgent, "artifactsDir", tempDir.toString());
    }

    /**
     * Test: High latency should trigger INCREASE in concurrency
     * Rule: median latency > target → increase concurrency
     */
    @Test
    void testHighLatencyIncreaseConcurrency() {
        // Given: Metrics with high latency (150ms > 100ms target)
        List<MetricRow> metrics = createMetrics(150.0, 10);
        int currentConcurrency = 4;

        // When: Agent makes decision
        AgentDecision decision = simpleAgent.decide(metrics, currentConcurrency);

        // Then: Should recommend INCREASE
        assertNotNull(decision);
        assertEquals(AgentStrategy.RULE_BASED, decision.getStrategy());
        assertTrue(decision.getRecommendation().toLowerCase().contains("6"),
                "Should increase concurrency from 4 to 6");
        assertTrue(decision.getReasoning().toLowerCase().contains("increas"),
                "Reasoning should mention increase");
        assertTrue(decision.getReasoning().toLowerCase().contains("exceeds"),
                "Reasoning should mention exceeding target");
        assertEquals(0.95, decision.getConfidenceScore(), 0.01);
        assertNotNull(decision.getActionItems());
        assertFalse(decision.getActionItems().isEmpty());
    }

    /**
     * Test: Low latency should trigger DECREASE in concurrency
     * Rule: median latency < 0.7 * target → decrease concurrency
     */
    @Test
    void testLowLatencyDecreaseConcurrency() {
        // Given: Metrics with low latency (50ms < 70ms threshold)
        List<MetricRow> metrics = createMetrics(50.0, 10);
        int currentConcurrency = 8;

        // When: Agent makes decision
        AgentDecision decision = simpleAgent.decide(metrics, currentConcurrency);

        // Then: Should recommend DECREASE
        assertNotNull(decision);
        assertEquals(AgentStrategy.RULE_BASED, decision.getStrategy());
        assertTrue(decision.getRecommendation().toLowerCase().contains("6"),
                "Should decrease concurrency from 8 to 6");
        assertTrue(decision.getReasoning().toLowerCase().contains("decreas"),
                "Reasoning should mention decrease");
        assertTrue(decision.getReasoning().toLowerCase().contains("below"),
                "Reasoning should mention being below target");
        assertEquals(0.95, decision.getConfidenceScore(), 0.01);
        assertEquals(AgentDecision.ImpactLevel.LOW, decision.getImpactLevel());
    }

    /**
     * Test: Latency within acceptable range should MAINTAIN concurrency
     * Rule: 0.7 * target <= median <= target → maintain
     */
    @Test
    void testAcceptableLatencyMaintainConcurrency() {
        // Given: Metrics with acceptable latency (85ms, within 70-100ms range)
        List<MetricRow> metrics = createMetrics(85.0, 10);
        int currentConcurrency = 4;

        // When: Agent makes decision
        AgentDecision decision = simpleAgent.decide(metrics, currentConcurrency);

        // Then: Should recommend MAINTAIN
        assertNotNull(decision);
        assertEquals(AgentStrategy.RULE_BASED, decision.getStrategy());
        assertTrue(decision.getRecommendation().toLowerCase().contains("4"),
                "Should maintain concurrency at 4");
        assertTrue(decision.getReasoning().toLowerCase().contains("maintain") ||
                        decision.getReasoning().toLowerCase().contains("within"),
                "Reasoning should mention maintaining or being within range");
        assertEquals(0.95, decision.getConfidenceScore(), 0.01);
        assertEquals(AgentDecision.ImpactLevel.LOW, decision.getImpactLevel());
    }

    /**
     * Test: Edge case at exact target latency
     */
    @Test
    void testExactTargetLatency() {
        // Given: Metrics exactly at target (100ms)
        List<MetricRow> metrics = createMetrics(100.0, 10);
        int currentConcurrency = 4;

        // When: Agent makes decision
        AgentDecision decision = simpleAgent.decide(metrics, currentConcurrency);

        // Then: Should maintain (not exceeding target)
        assertNotNull(decision);
        assertTrue(decision.getRecommendation().toLowerCase().contains("4"),
                "Should maintain at exact target");
    }

    /**
     * Test: Edge case at low threshold boundary
     */
    @Test
    void testLowThresholdBoundary() {
        // Given: Metrics exactly at low threshold (70ms)
        List<MetricRow> metrics = createMetrics(70.0, 10);
        int currentConcurrency = 4;

        // When: Agent makes decision
        AgentDecision decision = simpleAgent.decide(metrics, currentConcurrency);

        // Then: Should maintain (at boundary, not below)
        assertNotNull(decision);
        assertTrue(decision.getRecommendation().toLowerCase().contains("4"),
                "Should maintain at threshold boundary");
    }

    /**
     * Test: Very high latency triggers larger increase
     */
    @Test
    void testVeryHighLatency() {
        // Given: Metrics with very high latency (200ms)
        List<MetricRow> metrics = createMetrics(200.0, 10);
        int currentConcurrency = 4;

        // When: Agent makes decision
        AgentDecision decision = simpleAgent.decide(metrics, currentConcurrency);

        // Then: Should increase significantly (4 * 1.5 = 6)
        assertNotNull(decision);
        assertTrue(decision.getRecommendation().toLowerCase().contains("6"),
                "Should increase concurrency");
        assertEquals(AgentDecision.ImpactLevel.MEDIUM, decision.getImpactLevel());
    }

    /**
     * Test: Very low latency triggers decrease
     */
    @Test
    void testVeryLowLatency() {
        // Given: Metrics with very low latency (20ms)
        List<MetricRow> metrics = createMetrics(20.0, 10);
        int currentConcurrency = 8;

        // When: Agent makes decision
        AgentDecision decision = simpleAgent.decide(metrics, currentConcurrency);

        // Then: Should decrease (8 * 0.75 = 6)
        assertNotNull(decision);
        assertTrue(decision.getRecommendation().toLowerCase().contains("6"),
                "Should decrease concurrency");
    }

    /**
     * Test: Minimum concurrency floor (should not go below 1)
     */
    @Test
    void testMinimumConcurrencyFloor() {
        // Given: Very low latency with concurrency of 1
        List<MetricRow> metrics = createMetrics(20.0, 10);
        int currentConcurrency = 1;

        // When: Agent makes decision
        AgentDecision decision = simpleAgent.decide(metrics, currentConcurrency);

        // Then: Should maintain at 1 (floor)
        assertNotNull(decision);
        String recommendation = decision.getRecommendation().toLowerCase();
        // Should stay at 1 (0.75 * 1 = 0.75, floored to 1)
        assertTrue(recommendation.contains("1"), "Should not go below 1");
    }

    /**
     * Test: Decision contains required fields
     */
    @Test
    void testDecisionStructure() {
        // Given: Any metrics
        List<MetricRow> metrics = createMetrics(150.0, 10);
        int currentConcurrency = 4;

        // When: Agent makes decision
        AgentDecision decision = simpleAgent.decide(metrics, currentConcurrency);

        // Then: Should have all required fields
        assertNotNull(decision.getDecisionId());
        assertNotNull(decision.getTimestamp());
        assertEquals("concurrency-pool", decision.getResourceId());
        assertEquals("ExecutorService", decision.getResourceType());
        assertEquals(AgentStrategy.RULE_BASED, decision.getStrategy());
        assertNotNull(decision.getRecommendation());
        assertNotNull(decision.getReasoning());
        assertNotNull(decision.getConfidenceScore());
        assertTrue(decision.getConfidenceScore() >= 0.0 && decision.getConfidenceScore() <= 1.0);
        assertNotNull(decision.getImpactLevel());
        assertNotNull(decision.getActionItems());
        assertTrue(decision.getActionItems().size() >= 2);
        assertNotNull(decision.getMetricsAnalyzed());
        assertTrue(decision.getMetricsAnalyzed().contains("latencyMs"));
    }

    /**
     * Test: Empty metrics list throws exception
     */
    @Test
    void testEmptyMetricsThrowsException() {
        // Given: Empty metrics list
        List<MetricRow> metrics = new ArrayList<>();
        int currentConcurrency = 4;

        // When/Then: Should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            simpleAgent.decide(metrics, currentConcurrency);
        });
    }

    /**
     * Test: Null metrics list throws exception
     */
    @Test
    void testNullMetricsThrowsException() {
        // Given: Null metrics
        int currentConcurrency = 4;

        // When/Then: Should throw exception (either IllegalArgumentException or NullPointerException)
        assertThrows(Exception.class, () -> {
            simpleAgent.decide(null, currentConcurrency);
        });
    }

    /**
     * Test: Mixed latency values uses median correctly
     */
    @Test
    void testMedianCalculation() {
        // Given: Metrics with varied latencies [50, 100, 150, 200, 250]
        // Median should be 150
        List<MetricRow> metrics = new ArrayList<>();
        double[] latencies = {50.0, 100.0, 150.0, 200.0, 250.0};
        for (double latency : latencies) {
            metrics.add(createMetric(latency));
        }
        int currentConcurrency = 4;

        // When: Agent makes decision
        AgentDecision decision = simpleAgent.decide(metrics, currentConcurrency);

        // Then: Should use median (150ms) which exceeds target (100ms)
        // So should increase concurrency
        assertNotNull(decision);
        assertTrue(decision.getReasoning().toLowerCase().contains("increase") ||
                        decision.getReasoning().toLowerCase().contains("exceeds"),
                "Should recognize median exceeds target");
    }

    /**
     * Test: Confidence score is always 0.95 for rule-based agent
     */
    @Test
    void testConfidenceScoreConsistency() {
        // Test all three scenarios
        List<MetricRow> highLatency = createMetrics(150.0, 10);
        List<MetricRow> lowLatency = createMetrics(50.0, 10);
        List<MetricRow> normalLatency = createMetrics(85.0, 10);

        AgentDecision decision1 = simpleAgent.decide(highLatency, 4);
        AgentDecision decision2 = simpleAgent.decide(lowLatency, 8);
        AgentDecision decision3 = simpleAgent.decide(normalLatency, 4);

        // All should have same high confidence (deterministic rules)
        assertEquals(0.95, decision1.getConfidenceScore(), 0.01);
        assertEquals(0.95, decision2.getConfidenceScore(), 0.01);
        assertEquals(0.95, decision3.getConfidenceScore(), 0.01);
    }

    // Helper methods

    /**
     * Creates a list of metrics with specified latency.
     */
    private List<MetricRow> createMetrics(double latencyMs, int count) {
        List<MetricRow> metrics = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            metrics.add(createMetric(latencyMs));
        }
        return metrics;
    }

    /**
     * Creates a single metric with specified latency.
     */
    private MetricRow createMetric(double latencyMs) {
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
     * Sets a private field using reflection (for testing purposes).
     */
    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
