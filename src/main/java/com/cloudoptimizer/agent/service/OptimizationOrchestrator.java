package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.model.*;
import com.cloudoptimizer.agent.simulator.WorkloadSimulator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service that orchestrates the optimization workflow and emits real-time progress events.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since December 2025
 */
@Service
public class OptimizationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OptimizationOrchestrator.class);
    
    // Configuration keys
    private static final String CONCURRENCY = "concurrency";
    private static final String MEDIAN_LATENCY_MS = "median_latency_ms";
    private static final String AVG_LATENCY_MS = "avg_latency_ms";
    private static final String P95_LATENCY_MS = "p95_latency_ms";
    private static final String REQUESTS_PER_SECOND = "requests_per_second";
    private static final String TOTAL_REQUESTS = "total_requests";
    private static final String COST_ESTIMATE_USD = "cost_estimate_usd";
    
    private final WorkloadSimulator workloadSimulator;
    private final MetricsLogger metricsLogger;
    private final SimpleAgent simpleAgent;
    private final SpringAiLlmAgent llmAgent;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${baseline.concurrency:4}")
    private int baselineConcurrency;
    
    @Value("${load.duration:10}")
    private int loadDuration;
    
    @Value("${target.rps:0}")
    private double targetRps;
    
    @Value("${agent.strategy:llm}")
    private String agentStrategy;

    public OptimizationOrchestrator(WorkloadSimulator workloadSimulator,
                                   MetricsLogger metricsLogger,
                                   SimpleAgent simpleAgent,
                                   SpringAiLlmAgent llmAgent,
                                   SimpMessagingTemplate messagingTemplate) {
        this.workloadSimulator = workloadSimulator;
        this.metricsLogger = metricsLogger;
        this.simpleAgent = simpleAgent;
        this.llmAgent = llmAgent;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Runs the complete optimization workflow asynchronously with real-time progress updates.
     */
    public CompletableFuture<Map<String, Object>> runOptimization() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeOptimization();
            } catch (Exception e) {
                log.error("Optimization failed", e);
                emitEvent(ProgressEvent.errorEvent("Optimization failed: " + e.getMessage()));
                throw new RuntimeException("Optimization failed", e);
            }
        });
    }

    private Map<String, Object> executeOptimization() throws Exception {
        // Phase 0: Initialize
        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.INITIALIZING, "Initializing optimization workflow..."));
        
        Path artifactsDir = Paths.get("artifacts");
        if (!Files.exists(artifactsDir)) {
            Files.createDirectories(artifactsDir);
        }
        
        // Health check
        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.INITIALIZING, "Performing health check..."));
        if (!workloadSimulator.isHealthy()) {
            throw new IllegalStateException("Simulator health check failed");
        }
        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.INITIALIZING, "Health check passed!"));

        // Phase 1: Baseline Load Test
        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.BASELINE_RUNNING, 
                String.format("Running baseline test with %d threads for %d seconds...", baselineConcurrency, loadDuration)));
        
        RunResult baseline = workloadSimulator.executeLoad(baselineConcurrency, loadDuration, targetRps);
        writeJson(artifactsDir.resolve("baseline.json"), baseline);
        
        // Emit baseline metrics
        Map<String, Object> baselineData = new HashMap<>();
        baselineData.put(CONCURRENCY, baseline.getConcurrency());
        baselineData.put(MEDIAN_LATENCY_MS, baseline.getMedianLatencyMs());
        baselineData.put(AVG_LATENCY_MS, baseline.getAvgLatencyMs());
        baselineData.put(P95_LATENCY_MS, baseline.getP95LatencyMs());
        baselineData.put(REQUESTS_PER_SECOND, baseline.getRequestsPerSecond());
        baselineData.put(TOTAL_REQUESTS, baseline.getTotalRequests());
        baselineData.put(COST_ESTIMATE_USD, baseline.getCostEstimateUsd());
        if (baseline.getHeapMetrics() != null) {
            baselineData.put("heap_metrics", convertHeapMetrics(baseline.getHeapMetrics()));
        }
        emitEvent(ProgressEvent.metricUpdate("Baseline complete", baselineData));
        
        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.BASELINE_COMPLETE, 
                String.format("Baseline complete! Median latency: %.2fms, RPS: %.0f", 
                        baseline.getMedianLatencyMs(), baseline.getRequestsPerSecond())));

        // Phase 2: LLM Analysis
        Thread.sleep(1000); // Wait for metrics to flush
        
        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.LLM_ANALYZING, "AI is analyzing performance metrics..."));
        emitEvent(ProgressEvent.reasoningUpdate("🧠 Reading recent metrics..."));
        
        List<MetricRow> recentMetrics = metricsLogger.readRecent(20);
        emitEvent(ProgressEvent.reasoningUpdate(String.format("📊 Analyzing %d metric samples", recentMetrics.size())));
        
        AgentDecision decision;
        if ("llm".equalsIgnoreCase(agentStrategy)) {
            emitEvent(ProgressEvent.reasoningUpdate("🤖 LLM Agent evaluating patterns (including heap metrics)..."));
            decision = llmAgent.decideWithHeapAnalysis(baseline, baselineConcurrency);
        } else {
            emitEvent(ProgressEvent.reasoningUpdate("⚙️ Simple Agent applying rules (including heap analysis)..."));
            decision = simpleAgent.decideWithHeapAnalysis(baseline, baselineConcurrency);
        }

        // Emit reasoning
        emitEvent(ProgressEvent.reasoningUpdate("💡 " + decision.getReasoning()));
        emitEvent(ProgressEvent.reasoningUpdate(String.format("✨ Confidence: %.0f%%", decision.getConfidenceScore() * 100)));
        
        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.LLM_COMPLETE, 
                "AI Decision: " + decision.getRecommendation()));

        // Extract new concurrency
        int newConcurrency = extractConcurrency(decision.getRecommendation(), baselineConcurrency);
        emitEvent(ProgressEvent.reasoningUpdate(
                String.format("🎯 Applying recommendation: %d → %d threads", baselineConcurrency, newConcurrency)));

        // Phase 3: Post-Optimization Load Test
        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.OPTIMIZATION_RUNNING, 
                String.format("Running optimized test with %d threads...", newConcurrency)));
        
        RunResult after = workloadSimulator.executeLoad(newConcurrency, loadDuration, targetRps);
        writeJson(artifactsDir.resolve("after.json"), after);
        
        Map<String, Object> afterData = new HashMap<>();
        afterData.put(CONCURRENCY, after.getConcurrency());
        afterData.put(MEDIAN_LATENCY_MS, after.getMedianLatencyMs());
        afterData.put(AVG_LATENCY_MS, after.getAvgLatencyMs());
        afterData.put(P95_LATENCY_MS, after.getP95LatencyMs());
        afterData.put(REQUESTS_PER_SECOND, after.getRequestsPerSecond());
        afterData.put(TOTAL_REQUESTS, after.getTotalRequests());
        afterData.put(COST_ESTIMATE_USD, after.getCostEstimateUsd());
        if (after.getHeapMetrics() != null) {
            afterData.put("heap_metrics", convertHeapMetrics(after.getHeapMetrics()));
        }
        emitEvent(ProgressEvent.metricUpdate("Optimization complete", afterData));
        
        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.OPTIMIZATION_COMPLETE, 
                String.format("Optimized test complete! Median latency: %.2fms, RPS: %.0f", 
                        after.getMedianLatencyMs(), after.getRequestsPerSecond())));

        // Phase 4: Generate Report
        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.GENERATING_REPORT, "Generating comparison report..."));
        
        Map<String, Object> report = generateReport(baseline, after, decision, agentStrategy);
        writeJson(artifactsDir.resolve("report.json"), report);
        
        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.COMPLETE, "Optimization complete!"));
        emitEvent(ProgressEvent.completeEvent(report));
        
        return report;
    }

    private void emitEvent(ProgressEvent event) {
        messagingTemplate.convertAndSend("/topic/optimization-progress", event);
        log.debug("Emitted event: {} - {}", event.getEventType(), event.getMessage());
    }

    private int extractConcurrency(String recommendation, int defaultValue) {
        try {
            String[] parts = recommendation.toLowerCase().split("concurrency to ");
            if (parts.length > 1) {
                String numberPart = parts[1].split("\\D")[0];
                return Integer.parseInt(numberPart);
            }
        } catch (Exception e) {
            log.warn("Could not extract concurrency from recommendation, using default");
        }
        return defaultValue;
    }

    private Map<String, Object> generateReport(RunResult baseline, RunResult after, 
                                               AgentDecision decision, String agentStrategy) {
        Map<String, Object> report = new HashMap<>();
        
        report.put("agent_strategy", agentStrategy);
        report.put("timestamp", java.time.Instant.now().toString());
        
        Map<String, Object> baselineMap = new HashMap<>();
        baselineMap.put(CONCURRENCY, baseline.getConcurrency());
        baselineMap.put(MEDIAN_LATENCY_MS, baseline.getMedianLatencyMs());
        baselineMap.put(AVG_LATENCY_MS, baseline.getAvgLatencyMs());
        baselineMap.put(P95_LATENCY_MS, baseline.getP95LatencyMs());
        baselineMap.put(REQUESTS_PER_SECOND, baseline.getRequestsPerSecond());
        baselineMap.put(TOTAL_REQUESTS, baseline.getTotalRequests());
        baselineMap.put(COST_ESTIMATE_USD, baseline.getCostEstimateUsd());
        if (baseline.getHeapMetrics() != null) {
            baselineMap.put("heap_metrics", convertHeapMetrics(baseline.getHeapMetrics()));
        }
        report.put("baseline", baselineMap);
        
        Map<String, Object> afterMap = new HashMap<>();
        afterMap.put(CONCURRENCY, after.getConcurrency());
        afterMap.put(MEDIAN_LATENCY_MS, after.getMedianLatencyMs());
        afterMap.put(AVG_LATENCY_MS, after.getAvgLatencyMs());
        afterMap.put(P95_LATENCY_MS, after.getP95LatencyMs());
        afterMap.put(REQUESTS_PER_SECOND, after.getRequestsPerSecond());
        afterMap.put(TOTAL_REQUESTS, after.getTotalRequests());
        afterMap.put(COST_ESTIMATE_USD, after.getCostEstimateUsd());
        if (after.getHeapMetrics() != null) {
            afterMap.put("heap_metrics", convertHeapMetrics(after.getHeapMetrics()));
        }
        report.put("after", afterMap);
        
        Map<String, Object> improvements = new HashMap<>();
        improvements.put("latency_change_ms", after.getMedianLatencyMs() - baseline.getMedianLatencyMs());
        improvements.put("latency_change_percent", 
                calculatePercentChange(baseline.getMedianLatencyMs(), after.getMedianLatencyMs()));
        improvements.put("throughput_change_rps", after.getRequestsPerSecond() - baseline.getRequestsPerSecond());
        improvements.put("throughput_change_percent", 
                calculatePercentChange(baseline.getRequestsPerSecond(), after.getRequestsPerSecond()));
        improvements.put("concurrency_change", after.getConcurrency() - baseline.getConcurrency());
        report.put("improvements", improvements);
        
        Map<String, Object> decisionMap = new HashMap<>();
        decisionMap.put("recommendation", decision.getRecommendation());
        if (decision.getRecommendedHeapSizeMb() != null) {
            decisionMap.put("recommended_heap_size_mb", decision.getRecommendedHeapSizeMb());
        }
        decisionMap.put("reasoning", decision.getReasoning());
        decisionMap.put("confidence_score", decision.getConfidenceScore());
        if (decision.getConcurrencyConfidence() != null) {
            decisionMap.put("concurrency_confidence", decision.getConcurrencyConfidence());
        }
        if (decision.getHeapConfidence() != null) {
            decisionMap.put("heap_confidence", decision.getHeapConfidence());
        }
        decisionMap.put("impact_level", decision.getImpactLevel().toString());
        report.put("decision", decisionMap);
        
        return report;
    }

    private double calculatePercentChange(double baseline, double after) {
        if (baseline == 0) {
            return 0.0;
        }
        return ((after - baseline) / baseline) * 100.0;
    }

    private Map<String, Object> convertHeapMetrics(HeapMetrics heap) {
        Map<String, Object> heapMap = new HashMap<>();
        heapMap.put("heap_size_mb", heap.getHeapSizeMb());
        heapMap.put("heap_used_mb", heap.getHeapUsedMb());
        heapMap.put("heap_usage_percent", heap.getHeapUsagePercent());
        heapMap.put("gc_count", heap.getGcCount());
        heapMap.put("gc_time_ms", heap.getGcTimeMs());
        heapMap.put("gc_pause_time_avg_ms", heap.getGcPauseTimeAvgMs());
        heapMap.put("gc_frequency_per_sec", heap.getGcFrequencyPerSec());
        return heapMap;
    }

    private void writeJson(Path path, Object obj) throws IOException {
        objectMapper.writeValue(path.toFile(), obj);
        log.debug("Written: {}", path);
    }
}
