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
 * <p>Concurrency rules (applied in both {@code decide} and {@code decideWithHeapAnalysis}):</p>
 * <ul>
 *   <li>median latency &gt; target  → scale concurrency UP</li>
 *   <li>median latency &lt; target × lowThresholdFactor → scale concurrency DOWN</li>
 *   <li>otherwise → keep concurrency UNCHANGED</li>
 * </ul>
 *
 * <p>Heap rules (applied only in {@code decideWithHeapAnalysis}):</p>
 * <ul>
 *   <li>Rule 1 – high GC freq AND high heap usage → increase heap (large)</li>
 *   <li>Rule 2 – high GC pause avg AND medium heap usage → increase heap (small)</li>
 *   <li>Rule 3 – low GC freq AND low heap usage → decrease heap (right-size)</li>
 *   <li>Otherwise → heap is optimal, no change</li>
 * </ul>
 *
 * <p>All thresholds and multipliers are externalised to {@code application.yml}
 * under the {@code agent.rules} prefix.</p>
 *
 * @author Sibasis Padhi
 * @version 1.0
 * @since December 2025
 */
@Service
public class SimpleAgent {

    private static final Logger log = LoggerFactory.getLogger(SimpleAgent.class);

    // -------------------------------------------------------------------------
    // Core config
    // -------------------------------------------------------------------------

    @Value("${agent.target-latency-ms:100.0}")
    private double targetLatencyMs;

    @Value("${agent.artifacts-dir:artifacts}")
    private String artifactsDir;

    // -------------------------------------------------------------------------
    // Concurrency rule config
    // -------------------------------------------------------------------------

    /** Fraction of target latency below which concurrency is reduced. */
    @Value("${agent.rules.low-threshold-factor:0.7}")
    private double lowThresholdFactor;

    /** Multiplier applied to concurrency when latency exceeds target. */
    @Value("${agent.rules.concurrency-scale-up-factor:1.5}")
    private double concurrencyScaleUpFactor;

    /** Multiplier applied to concurrency when latency is well below target. */
    @Value("${agent.rules.concurrency-scale-down-factor:0.75}")
    private double concurrencyScaleDownFactor;

    // -------------------------------------------------------------------------
    // Heap rule config
    // -------------------------------------------------------------------------

    @Value("${agent.rules.heap.gc-freq-high-per-sec:1.0}")
    private double heapGcFreqHighPerSec;

    @Value("${agent.rules.heap.usage-high-percent:80.0}")
    private double heapUsageHighPercent;

    @Value("${agent.rules.heap.gc-pause-high-ms:100.0}")
    private double heapGcPauseHighMs;

    @Value("${agent.rules.heap.usage-medium-percent:70.0}")
    private double heapUsageMediumPercent;

    @Value("${agent.rules.heap.gc-freq-low-per-sec:0.5}")
    private double heapGcFreqLowPerSec;

    @Value("${agent.rules.heap.usage-low-percent:50.0}")
    private double heapUsageLowPercent;

    @Value("${agent.rules.heap.scale-up-large-factor:1.5}")
    private double heapScaleUpLargeFactor;

    @Value("${agent.rules.heap.scale-up-small-factor:1.25}")
    private double heapScaleUpSmallFactor;

    @Value("${agent.rules.heap.scale-down-factor:0.75}")
    private double heapScaleDownFactor;

    @Value("${agent.rules.heap.min-heap-mb:256}")
    private int heapMinMb;

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Makes an optimization decision based on recent latency metrics only.
     *
     * @param recentMetrics list of recent metric samples (must not be null or empty)
     * @param currentConcurrency current concurrency setting
     * @return AgentDecision with new concurrency recommendation
     */
    public AgentDecision decide(List<MetricRow> recentMetrics, int currentConcurrency) {
        log.info("SimpleAgent analyzing {} metrics with current concurrency {}",
                recentMetrics.size(), currentConcurrency);

        if (recentMetrics == null || recentMetrics.isEmpty()) {
            throw new IllegalArgumentException("Metrics list cannot be null or empty");
        }

        double medianLatency = calculateMedianLatency(recentMetrics);
        ConcurrencyResult cr = applyConcurrencyRules(medianLatency, currentConcurrency);

        AgentDecision decision = AgentDecision.builder()
                .decisionId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .resourceId("concurrency-pool")
                .resourceType("ExecutorService")
                .strategy(AgentStrategy.RULE_BASED)
                .recommendation(String.format("Set concurrency to %d", cr.newConcurrency))
                .reasoning(cr.reasoning)
                .confidenceScore(0.95)
                .impactLevel(cr.impactLevel)
                .actionItems(List.of(
                        String.format("Update concurrency from %d to %d",
                                currentConcurrency, cr.newConcurrency),
                        String.format("Monitor latency to verify expected %.2fms",
                                cr.expectedLatency)))
                .metricsAnalyzed(List.of("latencyMs"))
                .build();

        writeSimpleReasoningTrace(decision, medianLatency, currentConcurrency, cr.newConcurrency);
        log.info("SimpleAgent decision: {} -> {}", currentConcurrency, cr.newConcurrency);

        return decision;
    }

    /**
     * Makes an optimization decision from a full {@link RunResult},
     * covering both concurrency and JVM heap sizing.
     *
     * @param baselineResult baseline test results including optional heap metrics
     * @param currentConcurrency current concurrency setting
     * @return AgentDecision with concurrency and heap recommendations
     */
    public AgentDecision decideWithHeapAnalysis(RunResult baselineResult, int currentConcurrency) {
        log.info("SimpleAgent analyzing baseline result with heap metrics");

        if (baselineResult == null) {
            throw new IllegalArgumentException("Baseline result cannot be null");
        }

        double medianLatency = baselineResult.getMedianLatencyMs();
        ConcurrencyResult cr = applyConcurrencyRules(medianLatency, currentConcurrency);
        HeapResult hr = applyHeapRules(baselineResult.getHeapMetrics(), cr.impactLevel);

        List<String> actionItems = new ArrayList<>();
        actionItems.add(String.format("Update concurrency from %d to %d",
                currentConcurrency, cr.newConcurrency));

        List<String> metricsAnalyzed = new ArrayList<>();
        metricsAnalyzed.add("latencyMs");

        AgentDecision.AgentDecisionBuilder builder = AgentDecision.builder()
                .decisionId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .resourceId("service-optimization")
                .resourceType("JavaService")
                .strategy(AgentStrategy.RULE_BASED)
                .recommendation(String.format("Set concurrency to %d", cr.newConcurrency))
                .reasoning(cr.reasoning + hr.reasoning)
                .confidenceScore(0.95)
                .impactLevel(hr.impactLevel);

        if (hr.recommendedHeapMb != null) {
            builder.recommendedHeapSizeMb(hr.recommendedHeapMb);
            actionItems.add(String.format("Update heap size to %dMB", hr.recommendedHeapMb));
            metricsAnalyzed.add("heapMetrics");
        }

        AgentDecision decision = builder
                .actionItems(actionItems)
                .metricsAnalyzed(metricsAnalyzed)
                .build();

        writeHeapReasoningTrace(decision, baselineResult, currentConcurrency, cr.newConcurrency);
        log.info("SimpleAgent decision: concurrency {} -> {}, heap -> {}MB",
                currentConcurrency, cr.newConcurrency, hr.recommendedHeapMb);

        return decision;
    }

    // =========================================================================
    // Config accessors (used by tests and callers that need the live thresholds)
    // =========================================================================

    public double getTargetLatencyMs()           { return targetLatencyMs; }
    public String getArtifactsDir()              { return artifactsDir; }
    public double getLowThresholdFactor()        { return lowThresholdFactor; }
    public double getConcurrencyScaleUpFactor()  { return concurrencyScaleUpFactor; }
    public double getConcurrencyScaleDownFactor(){ return concurrencyScaleDownFactor; }

    // =========================================================================
    // Internal rule application
    // =========================================================================

    /**
     * Applies the three concurrency rules and returns a result record.
     * This is the single, authoritative implementation used by both public methods.
     */
    private ConcurrencyResult applyConcurrencyRules(double medianLatency, int currentConcurrency) {
        if (medianLatency > targetLatencyMs) {
            int next = (int) Math.ceil(currentConcurrency * concurrencyScaleUpFactor);
            return new ConcurrencyResult(
                    next,
                    medianLatency * lowThresholdFactor,
                    AgentDecision.ImpactLevel.MEDIUM,
                    String.format(
                            "Median latency (%.2fms) exceeds target (%.2fms). "
                            + "Increasing concurrency from %d to %d to improve throughput.",
                            medianLatency, targetLatencyMs, currentConcurrency, next));
        }

        if (medianLatency < targetLatencyMs * lowThresholdFactor) {
            int next = Math.max(1, (int) Math.floor(currentConcurrency * concurrencyScaleDownFactor));
            return new ConcurrencyResult(
                    next,
                    medianLatency * 1.1,
                    AgentDecision.ImpactLevel.LOW,
                    String.format(
                            "Median latency (%.2fms) is well below target (%.2fms). "
                            + "Decreasing concurrency from %d to %d to reduce resource usage.",
                            medianLatency, targetLatencyMs, currentConcurrency, next));
        }

        return new ConcurrencyResult(
                currentConcurrency,
                medianLatency,
                AgentDecision.ImpactLevel.LOW,
                String.format(
                        "Median latency (%.2fms) is within acceptable range [%.2fms - %.2fms]. "
                        + "Maintaining current concurrency at %d.",
                        medianLatency, targetLatencyMs * lowThresholdFactor,
                        targetLatencyMs, currentConcurrency));
    }

    /**
     * Applies the four heap-sizing rules.
     * Returns an empty result (no change) when heapMetrics is null.
     */
    private HeapResult applyHeapRules(HeapMetrics h, AgentDecision.ImpactLevel baseImpact) {
        if (h == null) {
            return new HeapResult(null, baseImpact, "");
        }

        long currentMb      = h.getHeapSizeMb();
        double usagePct     = h.getHeapUsagePercent();
        double gcFreq       = h.getGcFrequencyPerSec();
        double gcPauseAvg   = h.getGcPauseTimeAvgMs();

        // Rule 1 — high GC frequency + high heap usage → increase heap (large)
        if (gcFreq > heapGcFreqHighPerSec && usagePct > heapUsageHighPercent) {
            int next = (int) Math.ceil(currentMb * heapScaleUpLargeFactor);
            return new HeapResult(
                    next,
                    AgentDecision.ImpactLevel.MEDIUM,
                    String.format(
                            " High GC frequency (%.2f/sec) and heap usage (%.1f%%) detected. "
                            + "Increasing heap from %dMB to %dMB to reduce GC pressure.",
                            gcFreq, usagePct, currentMb, next));
        }

        // Rule 2 — high GC pause time + medium heap usage → increase heap (small)
        if (gcPauseAvg > heapGcPauseHighMs && usagePct > heapUsageMediumPercent) {
            int next = (int) Math.ceil(currentMb * heapScaleUpSmallFactor);
            return new HeapResult(
                    next,
                    AgentDecision.ImpactLevel.MEDIUM,
                    String.format(
                            " High GC pause time (%.2fms avg) impacting latency. "
                            + "Increasing heap from %dMB to %dMB to reduce pause frequency.",
                            gcPauseAvg, currentMb, next));
        }

        // Rule 3 — low GC frequency + low heap usage → right-size heap down
        if (gcFreq < heapGcFreqLowPerSec && usagePct < heapUsageLowPercent) {
            int next = (int) Math.max(heapMinMb, Math.floor(currentMb * heapScaleDownFactor));
            return new HeapResult(
                    next,
                    baseImpact,
                    String.format(
                            " Low GC frequency (%.2f/sec) and heap usage (%.1f%%) indicate "
                            + "over-provisioning. Decreasing heap from %dMB to %dMB to optimize cost.",
                            gcFreq, usagePct, currentMb, next));
        }

        // Optimal — no change
        return new HeapResult(
                (int) currentMb,
                baseImpact,
                String.format(
                        " Heap metrics are within acceptable range "
                        + "(usage: %.1f%%, GC freq: %.2f/sec). Maintaining heap at %dMB.",
                        usagePct, gcFreq, currentMb));
    }

    // =========================================================================
    // Median latency helper
    // =========================================================================

    private double calculateMedianLatency(List<MetricRow> metrics) {
        List<Double> latencies = new ArrayList<>();
        for (MetricRow m : metrics) {
            if ("latencyMs".equals(m.getMetricName())) {
                latencies.add(m.getMetricValue());
            }
        }

        if (latencies.isEmpty()) {
            log.warn("No latency metrics found; falling back to target latency");
            return targetLatencyMs;
        }

        Collections.sort(latencies);
        int size = latencies.size();
        return (size % 2 == 0)
                ? (latencies.get(size / 2 - 1) + latencies.get(size / 2)) / 2.0
                : latencies.get(size / 2);
    }

    // =========================================================================
    // Reasoning trace writers
    // =========================================================================

    private void writeSimpleReasoningTrace(AgentDecision decision, double medianLatency,
                                           int oldConcurrency, int newConcurrency) {
        try {
            Path dir = ensureArtifactsDir();
            StringBuilder sb = new StringBuilder();
            sb.append("=".repeat(60)).append("\n");
            sb.append("Simple Agent Decision Trace\n");
            sb.append("=".repeat(60)).append("\n");
            sb.append("Timestamp:          ").append(decision.getTimestamp()).append("\n");
            sb.append("Decision ID:        ").append(decision.getDecisionId()).append("\n\n");
            sb.append("--- Input ---\n");
            sb.append(String.format("Current Concurrency: %d%n", oldConcurrency));
            sb.append(String.format("Target Latency:      %.2f ms%n", targetLatencyMs));
            sb.append(String.format("Low Threshold:       %.2f ms (%.0f%% of target)%n",
                    targetLatencyMs * lowThresholdFactor, lowThresholdFactor * 100));
            sb.append("\n--- Analysis ---\n");
            sb.append(String.format("Median Latency: %.2f ms%n", medianLatency));
            sb.append("\n--- Decision ---\n");
            sb.append(String.format("Change: %d -> %d (%+d)%n",
                    oldConcurrency, newConcurrency, newConcurrency - oldConcurrency));
            sb.append(String.format("Confidence: %.0f%%%n", decision.getConfidenceScore() * 100));
            sb.append(String.format("Impact Level: %s%n", decision.getImpactLevel()));
            sb.append("\n--- Reasoning ---\n");
            sb.append(decision.getReasoning()).append("\n\n");
            sb.append("--- Action Items ---\n");
            for (int i = 0; i < decision.getActionItems().size(); i++) {
                sb.append(String.format("%d. %s%n", i + 1, decision.getActionItems().get(i)));
            }
            sb.append("\n").append("=".repeat(60)).append("\n\n");

            Files.writeString(dir.resolve("reasoning_trace_rule.txt"), sb.toString(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.info("Reasoning trace written to {}", dir.resolve("reasoning_trace_rule.txt"));
        } catch (IOException e) {
            log.error("Failed to write reasoning trace", e);
        }
    }

    private void writeHeapReasoningTrace(AgentDecision decision, RunResult baseline,
                                         int oldConcurrency, int newConcurrency) {
        try {
            Path dir = ensureArtifactsDir();
            StringBuilder sb = new StringBuilder();
            sb.append("=".repeat(60)).append("\n");
            sb.append("Simple Agent Decision Trace (with Heap Analysis)\n");
            sb.append("=".repeat(60)).append("\n");
            sb.append("Timestamp:   ").append(decision.getTimestamp()).append("\n");
            sb.append("Decision ID: ").append(decision.getDecisionId()).append("\n\n");
            sb.append("--- Baseline Metrics ---\n");
            sb.append(String.format("Concurrency:    %d%n", oldConcurrency));
            sb.append(String.format("Median Latency: %.2f ms%n", baseline.getMedianLatencyMs()));
            sb.append(String.format("Target Latency: %.2f ms%n", targetLatencyMs));
            HeapMetrics hm = baseline.getHeapMetrics();
            if (hm != null) {
                sb.append(String.format("Heap Size:      %d MB%n", hm.getHeapSizeMb()));
                sb.append(String.format("Heap Usage:     %.1f%%%n", hm.getHeapUsagePercent()));
                sb.append(String.format("GC Frequency:   %.2f/sec%n", hm.getGcFrequencyPerSec()));
                sb.append(String.format("GC Pause Avg:   %.2f ms%n", hm.getGcPauseTimeAvgMs()));
            }
            sb.append("\n--- Decision ---\n");
            sb.append(String.format("New Concurrency: %d (change: %+d)%n",
                    newConcurrency, newConcurrency - oldConcurrency));
            if (decision.getRecommendedHeapSizeMb() != null) {
                sb.append(String.format("Recommended Heap: %dMB%n",
                        decision.getRecommendedHeapSizeMb()));
            }
            sb.append(String.format("Confidence: %.0f%%%n", decision.getConfidenceScore() * 100));
            sb.append(String.format("Impact Level: %s%n", decision.getImpactLevel()));
            sb.append("\n--- Reasoning ---\n");
            sb.append(decision.getReasoning()).append("\n\n");
            sb.append("--- Action Items ---\n");
            for (int i = 0; i < decision.getActionItems().size(); i++) {
                sb.append(String.format("%d. %s%n", i + 1, decision.getActionItems().get(i)));
            }
            sb.append("\n").append("=".repeat(60)).append("\n\n");

            Files.writeString(dir.resolve("reasoning_trace_rule.txt"), sb.toString(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Reasoning trace written to {}", dir.resolve("reasoning_trace_rule.txt"));
        } catch (IOException e) {
            log.error("Failed to write heap reasoning trace", e);
        }
    }

    private Path ensureArtifactsDir() throws IOException {
        Path dir = Paths.get(artifactsDir);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    // =========================================================================
    // Private result carriers (avoid returning parallel arrays)
    // =========================================================================

    private record ConcurrencyResult(
            int newConcurrency,
            double expectedLatency,
            AgentDecision.ImpactLevel impactLevel,
            String reasoning) {}

    private record HeapResult(
            Integer recommendedHeapMb,
            AgentDecision.ImpactLevel impactLevel,
            String reasoning) {}
}