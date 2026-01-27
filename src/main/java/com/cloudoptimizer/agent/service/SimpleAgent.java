package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Simple rule-based agent for cloud resource optimization.
 * 
 * Implements deterministic threshold-based logic:
 * - If median latency > target → INCREASE concurrency
 * - If median latency < 0.7 * target → DECREASE concurrency
 * - Else → KEEP SAME concurrency
 * 
 * Writes reasoning trace to artifacts/reasoning_trace_rule.txt
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since December 2025
 */
@Service
public class SimpleAgent {

    private static final Logger log = LoggerFactory.getLogger(SimpleAgent.class);
    private static final double LOW_THRESHOLD_FACTOR = 0.7;

    @Value("${agent.target-latency-ms:100.0}")
    private double targetLatencyMs;

    @Value("${agent.artifacts-dir:artifacts}")
    private String artifactsDir;

    /**
     * Makes optimization decision based on recent metrics.
     * 
     * @param recentMetrics list of recent metric samples
     * @param currentConcurrency current concurrency setting
     * @return AgentDecision with new concurrency recommendation
     */
    public AgentDecision decide(List<MetricRow> recentMetrics, int currentConcurrency) {
        log.info("SimpleAgent analyzing {} metrics with current concurrency {}", 
                recentMetrics.size(), currentConcurrency);

        if (recentMetrics == null || recentMetrics.isEmpty()) {
            throw new IllegalArgumentException("Metrics list cannot be empty");
        }

        // Calculate median latency
        double medianLatency = calculateMedianLatency(recentMetrics);
        
        // Apply rule-based logic
        int newConcurrency;
        String reasoning;
        double expectedLatency;
        AgentDecision.ImpactLevel impactLevel;

        if (medianLatency > targetLatencyMs) {
            // High latency → increase concurrency
            newConcurrency = (int) Math.ceil(currentConcurrency * 1.5);
            expectedLatency = medianLatency * 0.7;
            reasoning = String.format(
                    "Median latency (%.2fms) exceeds target (%.2fms). " +
                    "Increasing concurrency from %d to %d to improve throughput.",
                    medianLatency, targetLatencyMs, currentConcurrency, newConcurrency);
            impactLevel = AgentDecision.ImpactLevel.MEDIUM;
            
        } else if (medianLatency < targetLatencyMs * LOW_THRESHOLD_FACTOR) {
            // Low latency → decrease concurrency
            newConcurrency = Math.max(1, (int) Math.floor(currentConcurrency * 0.75));
            expectedLatency = medianLatency * 1.1;
            reasoning = String.format(
                    "Median latency (%.2fms) is well below target (%.2fms). " +
                    "Decreasing concurrency from %d to %d to reduce resource usage.",
                    medianLatency, targetLatencyMs, currentConcurrency, newConcurrency);
            impactLevel = AgentDecision.ImpactLevel.LOW;
            
        } else {
            // Within acceptable range → keep same
            newConcurrency = currentConcurrency;
            expectedLatency = medianLatency;
            reasoning = String.format(
                    "Median latency (%.2fms) is within acceptable range [%.2fms - %.2fms]. " +
                    "Maintaining current concurrency at %d.",
                    medianLatency, targetLatencyMs * LOW_THRESHOLD_FACTOR, 
                    targetLatencyMs, currentConcurrency);
            impactLevel = AgentDecision.ImpactLevel.LOW;
        }

        // Build decision
        AgentDecision decision = AgentDecision.builder()
                .decisionId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .resourceId("concurrency-pool")
                .resourceType("ExecutorService")
                .strategy(AgentStrategy.RULE_BASED)
                .recommendation(String.format("Set concurrency to %d", newConcurrency))
                .reasoning(reasoning)
                .confidenceScore(0.95)
                .impactLevel(impactLevel)
                .actionItems(java.util.List.of(
                        String.format("Update concurrency from %d to %d", currentConcurrency, newConcurrency),
                        String.format("Monitor latency to verify expected %.2fms", expectedLatency)))
                .metricsAnalyzed(java.util.List.of("latencyMs"))
                .build();

        // Write reasoning trace
        writeReasoningTrace(decision, medianLatency, currentConcurrency, newConcurrency);

        log.info("SimpleAgent decision: {} -> {} (median: {:.2f}ms, target: {:.2f}ms)",
                currentConcurrency, newConcurrency, medianLatency, targetLatencyMs);

        return decision;
    }

    /**
     * Makes optimization decision based on baseline test results including heap metrics.
     * 
     * @param baselineResult baseline test results with heap metrics
     * @param currentConcurrency current concurrency setting
     * @return AgentDecision with concurrency and heap recommendations
     */
    public AgentDecision decideWithHeapAnalysis(RunResult baselineResult, int currentConcurrency) {
        log.info("SimpleAgent analyzing baseline result with heap metrics");

        if (baselineResult == null) {
            throw new IllegalArgumentException("Baseline result cannot be null");
        }

        double medianLatency = baselineResult.getMedianLatencyMs();
        HeapMetrics heapMetrics = baselineResult.getHeapMetrics();
        
        // Analyze concurrency (same as before)
        int newConcurrency;
        String concurrencyReasoning;
        AgentDecision.ImpactLevel impactLevel;

        if (medianLatency > targetLatencyMs) {
            newConcurrency = (int) Math.ceil(currentConcurrency * 1.5);
            concurrencyReasoning = String.format(
                    "Median latency (%.2fms) exceeds target (%.2fms). " +
                    "Increasing concurrency from %d to %d.",
                    medianLatency, targetLatencyMs, currentConcurrency, newConcurrency);
            impactLevel = AgentDecision.ImpactLevel.MEDIUM;
        } else if (medianLatency < targetLatencyMs * LOW_THRESHOLD_FACTOR) {
            newConcurrency = Math.max(1, (int) Math.floor(currentConcurrency * 0.75));
            concurrencyReasoning = String.format(
                    "Median latency (%.2fms) is well below target (%.2fms). " +
                    "Decreasing concurrency from %d to %d.",
                    medianLatency, targetLatencyMs, currentConcurrency, newConcurrency);
            impactLevel = AgentDecision.ImpactLevel.LOW;
        } else {
            newConcurrency = currentConcurrency;
            concurrencyReasoning = String.format(
                    "Median latency (%.2fms) is within acceptable range. " +
                    "Maintaining concurrency at %d.",
                    medianLatency, currentConcurrency);
            impactLevel = AgentDecision.ImpactLevel.LOW;
        }

        // Analyze heap metrics
        Integer recommendedHeapMb = null;
        String heapReasoning = "";
        
        if (heapMetrics != null) {
            long currentHeapMb = heapMetrics.getHeapSizeMb();
            double heapUsage = heapMetrics.getHeapUsagePercent();
            double gcFrequency = heapMetrics.getGcFrequencyPerSec();
            double gcPauseAvg = heapMetrics.getGcPauseTimeAvgMs();
            
            // Rule 1: High GC frequency + high heap usage = increase heap
            if (gcFrequency > 1.0 && heapUsage > 80.0) {
                recommendedHeapMb = (int) Math.ceil(currentHeapMb * 1.5);
                heapReasoning = String.format(
                        " High GC frequency (%.2f/sec) and heap usage (%.1f%%) detected. " +
                        "Increasing heap from %dMB to %dMB to reduce GC pressure.",
                        gcFrequency, heapUsage, currentHeapMb, recommendedHeapMb);
                impactLevel = AgentDecision.ImpactLevel.MEDIUM;
                
            // Rule 2: High GC pause time = increase heap
            } else if (gcPauseAvg > 100.0 && heapUsage > 70.0) {
                recommendedHeapMb = (int) Math.ceil(currentHeapMb * 1.25);
                heapReasoning = String.format(
                        " High GC pause time (%.2fms avg) impacting latency. " +
                        "Increasing heap from %dMB to %dMB to reduce pause frequency.",
                        gcPauseAvg, currentHeapMb, recommendedHeapMb);
                impactLevel = AgentDecision.ImpactLevel.MEDIUM;
                
            // Rule 3: Low heap usage + low GC = decrease heap (save cost)
            } else if (gcFrequency < 0.5 && heapUsage < 50.0) {
                recommendedHeapMb = (int) Math.max(256, Math.floor(currentHeapMb * 0.75));
                heapReasoning = String.format(
                        " Low GC frequency (%.2f/sec) and heap usage (%.1f%%) indicate over-provisioning. " +
                        "Decreasing heap from %dMB to %dMB to optimize cost.",
                        gcFrequency, heapUsage, currentHeapMb, recommendedHeapMb);
                
            } else {
                // Heap is optimal
                recommendedHeapMb = (int) currentHeapMb;
                heapReasoning = String.format(
                        " Heap metrics are within acceptable range (usage: %.1f%%, GC freq: %.2f/sec). " +
                        "Maintaining heap at %dMB.",
                        heapUsage, gcFrequency, currentHeapMb);
            }
        }

        // Combine reasoning
        String fullReasoning = concurrencyReasoning + heapReasoning;

        // Build decision
        AgentDecision.AgentDecisionBuilder decisionBuilder = AgentDecision.builder()
                .decisionId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .resourceId("service-optimization")
                .resourceType("JavaService")
                .strategy(AgentStrategy.RULE_BASED)
                .recommendation(String.format("Set concurrency to %d", newConcurrency))
                .reasoning(fullReasoning)
                .confidenceScore(0.95)
                .impactLevel(impactLevel);

        // Build action items and metrics lists
        java.util.List<String> actionItems = new java.util.ArrayList<>();
        actionItems.add(String.format("Update concurrency from %d to %d", currentConcurrency, newConcurrency));
        
        java.util.List<String> metricsAnalyzed = new java.util.ArrayList<>();
        metricsAnalyzed.add("latencyMs");

        if (recommendedHeapMb != null) {
            decisionBuilder.recommendedHeapSizeMb(recommendedHeapMb);
            actionItems.add(String.format("Update heap size to %dMB", recommendedHeapMb));
            metricsAnalyzed.add("heapMetrics");
        }

        decisionBuilder.actionItems(actionItems).metricsAnalyzed(metricsAnalyzed);
        
        AgentDecision decision = decisionBuilder.build();

        // Write reasoning trace
        writeHeapReasoningTrace(decision, baselineResult, currentConcurrency, newConcurrency);

        log.info("SimpleAgent decision: concurrency {} -> {}, heap -> {}MB", 
                currentConcurrency, newConcurrency, recommendedHeapMb);

        return decision;
    }

    /**
     * Writes reasoning trace including heap analysis to artifacts directory.
     */
    private void writeHeapReasoningTrace(AgentDecision decision, RunResult baselineResult,
                                         int currentConcurrency, int newConcurrency) {
        try {
            Path artifactsPath = Paths.get(artifactsDir);
            if (!Files.exists(artifactsPath)) {
                Files.createDirectories(artifactsPath);
            }

            Path tracePath = artifactsPath.resolve("reasoning_trace_rule.txt");
            
            StringBuilder trace = new StringBuilder();
            trace.append("=".repeat(60)).append("\n");
            trace.append("Simple Agent Decision Trace (with Heap Analysis)\n");
            trace.append("=".repeat(60)).append("\n");
            trace.append(String.format("Timestamp: %s\n", decision.getTimestamp().toString()));
            trace.append(String.format("Decision ID: %s\n", decision.getDecisionId()));
            trace.append("\n");
            
            trace.append("--- Baseline Metrics ---\n");
            trace.append(String.format("Concurrency: %d\n", currentConcurrency));
            trace.append(String.format("Median Latency: %.2f ms\n", baselineResult.getMedianLatencyMs()));
            trace.append(String.format("Target Latency: %.2f ms\n", targetLatencyMs));
            
            HeapMetrics heapMetrics = baselineResult.getHeapMetrics();
            if (heapMetrics != null) {
                trace.append(String.format("Heap Size: %d MB\n", heapMetrics.getHeapSizeMb()));
                trace.append(String.format("Heap Usage: %.1f%%\n", heapMetrics.getHeapUsagePercent()));
                trace.append(String.format("GC Frequency: %.2f/sec\n", heapMetrics.getGcFrequencyPerSec()));
                trace.append(String.format("GC Pause Avg: %.2f ms\n", heapMetrics.getGcPauseTimeAvgMs()));
            }
            trace.append("\n");
            
            trace.append("--- Decision ---\n");
            trace.append(String.format("New Concurrency: %d (change: %+d)\n", 
                    newConcurrency, newConcurrency - currentConcurrency));
            if (decision.getRecommendedHeapSizeMb() != null) {
                trace.append(String.format("Recommended Heap: %d MB\n", 
                        decision.getRecommendedHeapSizeMb()));
            }
            trace.append(String.format("Confidence: %.0f%%\n", decision.getConfidenceScore() * 100));
            trace.append(String.format("Impact Level: %s\n", decision.getImpactLevel()));
            trace.append("\n");
            
            trace.append("--- Reasoning ---\n");
            trace.append(decision.getReasoning()).append("\n\n");
            
            trace.append("--- Action Items ---\n");
            for (int i = 0; i < decision.getActionItems().size(); i++) {
                trace.append(String.format("%d. %s\n", i + 1, decision.getActionItems().get(i)));
            }
            trace.append("\n");
            trace.append("=".repeat(60)).append("\n\n");

            Files.writeString(tracePath, trace.toString(), 
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            log.info("Reasoning trace written to {}", tracePath);
            
        } catch (IOException e) {
            log.error("Failed to write reasoning trace", e);
        }
    }

    /**
     * Calculates median latency from metric samples.
     * 
     * @param metrics list of metrics containing latency data
     * @return median latency in milliseconds
     */
    private double calculateMedianLatency(List<MetricRow> metrics) {
        List<Double> latencies = new ArrayList<>();
        
        for (MetricRow metric : metrics) {
            if ("latencyMs".equals(metric.getMetricName())) {
                latencies.add(metric.getMetricValue());
            }
        }

        if (latencies.isEmpty()) {
            log.warn("No latency metrics found, using default value");
            return targetLatencyMs;
        }

        Collections.sort(latencies);
        int size = latencies.size();
        
        if (size % 2 == 0) {
            return (latencies.get(size / 2 - 1) + latencies.get(size / 2)) / 2.0;
        } else {
            return latencies.get(size / 2);
        }
    }

    /**
     * Writes reasoning trace to artifacts directory.
     * 
     * @param decision the agent decision
     * @param medianLatency calculated median latency
     * @param currentConcurrency current concurrency value
     * @param newConcurrency recommended concurrency value
     */
    private void writeReasoningTrace(AgentDecision decision, double medianLatency,
                                     int currentConcurrency, int newConcurrency) {
        try {
            Path artifactsPath = Paths.get(artifactsDir);
            if (!Files.exists(artifactsPath)) {
                Files.createDirectories(artifactsPath);
            }

            Path tracePath = artifactsPath.resolve("reasoning_trace_rule.txt");
            
            StringBuilder trace = new StringBuilder();
            trace.append("=".repeat(60)).append("\n");
            trace.append("Simple Agent Decision Trace\n");
            trace.append("=".repeat(60)).append("\n");
            trace.append(String.format("Timestamp: %s\n", 
                    decision.getTimestamp().toString()));
            trace.append(String.format("Decision ID: %s\n", decision.getDecisionId()));
            trace.append("\n");
            
            trace.append("--- Input Parameters ---%n");
            trace.append(String.format("Current Concurrency: %d%n", currentConcurrency));
            trace.append(String.format("Target Latency: %.2f ms%n", targetLatencyMs));
            trace.append(String.format("Low Threshold: %.2f ms (%.0f%% of target)%n", 
                    targetLatencyMs * LOW_THRESHOLD_FACTOR, LOW_THRESHOLD_FACTOR * 100));
            trace.append("%n");
            
            trace.append("--- Analysis ---%n");
            trace.append(String.format("Median Latency: %.2f ms%n", medianLatency));
            
            if (medianLatency > targetLatencyMs) {
                trace.append(String.format("Condition: Median (%.2f) > Target (%.2f)%n", 
                        medianLatency, targetLatencyMs));
                trace.append("Rule Applied: INCREASE concurrency%n");
            } else if (medianLatency < targetLatencyMs * LOW_THRESHOLD_FACTOR) {
                trace.append(String.format("Condition: Median (%.2f) < Low Threshold (%.2f)%n", 
                        medianLatency, targetLatencyMs * LOW_THRESHOLD_FACTOR));
                trace.append("Rule Applied: DECREASE concurrency%n");
            } else {
                trace.append("Condition: Within acceptable range%n");
                trace.append("Rule Applied: MAINTAIN concurrency%n");
            }
            trace.append("%n");
            
            trace.append("--- Decision ---%n");
            trace.append(String.format("New Concurrency: %d%n", newConcurrency));
            trace.append(String.format("Change: %d → %d (%+d)%n", 
                    currentConcurrency, newConcurrency, newConcurrency - currentConcurrency));
            trace.append(String.format("Confidence: %.0f%%%n", 
                    decision.getConfidenceScore() * 100));
            trace.append(String.format("Impact Level: %s%n", decision.getImpactLevel()));
            trace.append("%n");
            
            trace.append("--- Reasoning ---%n");
            trace.append(decision.getReasoning()).append("%n");
            trace.append("%n");
            
            trace.append("--- Action Items ---%n");
            for (int i = 0; i < decision.getActionItems().size(); i++) {
                trace.append(String.format("%d. %s%n", i + 1, 
                        decision.getActionItems().get(i)));
            }
            trace.append("%n");
            trace.append("=".repeat(60)).append("%n%n");

            Files.writeString(tracePath, trace.toString(), 
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
            log.info("Reasoning trace written to {}", tracePath);
            
        } catch (IOException e) {
            log.error("Failed to write reasoning trace", e);
        }
    }

    /**
     * Gets the configured target latency threshold.
     * 
     * @return target latency in milliseconds
     */
    public double getTargetLatencyMs() {
        return targetLatencyMs;
    }

    /**
     * Gets the artifacts directory path.
     * 
     * @return artifacts directory path
     */
    public String getArtifactsDir() {
        return artifactsDir;
    }
}
