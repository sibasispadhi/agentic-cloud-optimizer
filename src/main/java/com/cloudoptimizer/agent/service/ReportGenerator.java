package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.model.AgentDecision;
import com.cloudoptimizer.agent.model.HeapMetrics;
import com.cloudoptimizer.agent.model.RunResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds the human-facing JSON report for an optimization run.
 *
 * <p>Extracted from {@link OptimizationOrchestrator} to keep that class
 * focused on workflow sequencing and under the 600-line limit.
 */
@Service
public class ReportGenerator {

    // ── JSON key constants ────────────────────────────────────────────────────

    private static final String CONCURRENCY        = "concurrency";
    private static final String MEDIAN_LATENCY_MS  = "median_latency_ms";
    private static final String AVG_LATENCY_MS     = "avg_latency_ms";
    private static final String P95_LATENCY_MS     = "p95_latency_ms";
    private static final String REQUESTS_PER_SECOND = "requests_per_second";
    private static final String TOTAL_REQUESTS     = "total_requests";
    private static final String COST_ESTIMATE_USD  = "cost_estimate_usd";

    // ── SLO configuration ─────────────────────────────────────────────────────

    @Value("${slo.target-p99-ms:100.0}")
    private double sloTargetP99Ms;

    @Value("${slo.breach-threshold:1.2}")
    private double sloBreachThreshold;

    @Value("${slo.enabled:false}")
    private boolean sloEnabled;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Assembles the full run report map.
     *
     * @param baseline       baseline load-test result
     * @param after          after-optimization load-test result
     * @param decision       agent's decision
     * @param agentStrategy  strategy label ("llm" or "simple")
     * @param sloBreached    whether an SLO breach triggered this run
     * @param breachReason   human-readable breach description, or null
     * @return serialisable report map
     */
    public Map<String, Object> generateReport(RunResult baseline, RunResult after,
                                              AgentDecision decision, String agentStrategy,
                                              boolean sloBreached, String breachReason) {
        Map<String, Object> report = new HashMap<>();
        report.put("agent_strategy", agentStrategy);
        report.put("timestamp", Instant.now().toString());

        report.put("baseline", buildRunSnapshot(baseline));
        report.put("after",    buildRunSnapshot(after));
        report.put("improvements", buildImprovements(baseline, after));
        report.put("decision",     buildDecision(decision));
        report.put("slo_compliance", buildSloSection(baseline, after, sloBreached, breachReason));
        return report;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Builds a JSON-serialisable snapshot of a single load-test run result. */
    public Map<String, Object> buildRunSnapshot(RunResult r) {
        Map<String, Object> m = new HashMap<>();
        m.put(CONCURRENCY, r.getConcurrency());
        m.put(MEDIAN_LATENCY_MS, r.getMedianLatencyMs());
        m.put(AVG_LATENCY_MS, r.getAvgLatencyMs());
        m.put(P95_LATENCY_MS, r.getP95LatencyMs());
        m.put("p99_latency_ms", r.getP99LatencyMs());
        m.put(REQUESTS_PER_SECOND, r.getRequestsPerSecond());
        m.put(TOTAL_REQUESTS, r.getTotalRequests());
        m.put(COST_ESTIMATE_USD, r.getCostEstimateUsd());
        if (r.getHeapMetrics() != null) {
            m.put("heap_metrics", convertHeapMetrics(r.getHeapMetrics()));
        }
        return m;
    }

    private Map<String, Object> buildImprovements(RunResult baseline, RunResult after) {
        Map<String, Object> m = new HashMap<>();
        m.put("latency_change_ms",
                after.getMedianLatencyMs() - baseline.getMedianLatencyMs());
        m.put("latency_change_percent",
                pct(baseline.getMedianLatencyMs(), after.getMedianLatencyMs()));
        m.put("p95_change_ms",
                after.getP95LatencyMs() - baseline.getP95LatencyMs());
        m.put("p95_change_percent",
                pct(baseline.getP95LatencyMs(), after.getP95LatencyMs()));
        m.put("p99_change_ms",
                after.getP99LatencyMs() - baseline.getP99LatencyMs());
        m.put("p99_change_percent",
                pct(baseline.getP99LatencyMs(), after.getP99LatencyMs()));
        m.put("throughput_change_rps",
                after.getRequestsPerSecond() - baseline.getRequestsPerSecond());
        m.put("throughput_change_percent",
                pct(baseline.getRequestsPerSecond(), after.getRequestsPerSecond()));
        m.put("concurrency_change",
                after.getConcurrency() - baseline.getConcurrency());
        return m;
    }

    private Map<String, Object> buildDecision(AgentDecision decision) {
        Map<String, Object> m = new HashMap<>();
        m.put("recommendation", decision.getRecommendation());
        if (decision.getRecommendedHeapSizeMb() != null) {
            m.put("recommended_heap_size_mb", decision.getRecommendedHeapSizeMb());
        }
        m.put("reasoning", decision.getReasoning());
        m.put("confidence_score", decision.getConfidenceScore());
        if (decision.getConcurrencyConfidence() != null) {
            m.put("concurrency_confidence", decision.getConcurrencyConfidence());
        }
        if (decision.getHeapConfidence() != null) {
            m.put("heap_confidence", decision.getHeapConfidence());
        }
        m.put("impact_level", decision.getImpactLevel().toString());
        return m;
    }

    private Map<String, Object> buildSloSection(RunResult baseline, RunResult after,
                                                boolean sloBreached, String breachReason) {
        Map<String, Object> m = new HashMap<>();
        m.put("enabled", sloEnabled);
        m.put("breached", sloBreached);
        if (breachReason != null) m.put("breach_reason", breachReason);
        m.put("target_p99_ms", sloTargetP99Ms);
        m.put("breach_threshold_ms", sloTargetP99Ms * sloBreachThreshold);
        m.put("baseline_p99_ms", baseline.getP99LatencyMs());
        m.put("after_p99_ms", after.getP99LatencyMs());
        m.put("slo_restored_after_optimization",
                after.getP99LatencyMs() <= sloTargetP99Ms * sloBreachThreshold);
        return m;
    }

    private static Map<String, Object> convertHeapMetrics(HeapMetrics heap) {
        Map<String, Object> m = new HashMap<>();
        m.put("heap_size_mb", heap.getHeapSizeMb());
        m.put("heap_used_mb", heap.getHeapUsedMb());
        m.put("heap_usage_percent", heap.getHeapUsagePercent());
        m.put("gc_count", heap.getGcCount());
        m.put("gc_time_ms", heap.getGcTimeMs());
        m.put("gc_pause_time_avg_ms", heap.getGcPauseTimeAvgMs());
        m.put("gc_frequency_per_sec", heap.getGcFrequencyPerSec());
        return m;
    }

    /** Calculates percent change from {@code from} to {@code to}. */
    private static double pct(double from, double to) {
        return from == 0 ? 0.0 : ((to - from) / from) * 100.0;
    }
}