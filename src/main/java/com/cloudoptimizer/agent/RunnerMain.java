package com.cloudoptimizer.agent;

import com.cloudoptimizer.agent.model.AgentDecision;
import com.cloudoptimizer.agent.model.RunResult;
import com.cloudoptimizer.agent.service.MetricsLogger;
import com.cloudoptimizer.agent.service.SimpleAgent;
import com.cloudoptimizer.agent.service.SpringAiLlmAgent;
import com.cloudoptimizer.agent.simulator.DemoWorkloadSimulator;
import com.cloudoptimizer.agent.simulator.HttpRestWorkloadSimulator;
import com.cloudoptimizer.agent.simulator.WorkloadSimulator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Main runner for Agent Cloud Optimizer.
 * 
 * Orchestrates the complete optimization workflow:
 * 1. Run baseline load test
 * 2. Analyze metrics with selected agent (simple or llm)
 * 3. Apply optimization decision
 * 4. Run post-optimization load test
 * 5. Generate comparison report
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since December 2025
 */
@Component
public class RunnerMain implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RunnerMain.class);
    
    // Configuration keys
    private static final String CONCURRENCY = "concurrency";
    private static final String MEDIAN_LATENCY_MS = "median_latency_ms";
    private static final String AVG_LATENCY_MS = "avg_latency_ms";
    private static final String P95_LATENCY_MS = "p95_latency_ms";
    private static final String REQUESTS_PER_SECOND = "requests_per_second";
    private static final String TOTAL_REQUESTS = "total_requests";
    private static final String COST_ESTIMATE_USD = "cost_estimate_usd";
    
    private final ApplicationContext context;
    private final Map<String, WorkloadSimulator> simulators;
    private final String simulatorName;
    private final String agentStrategy;
    private final String runMode;
    private final MetricsLogger metricsLogger;
    private final SimpleAgent simpleAgent;
    private final SpringAiLlmAgent llmAgent;
    private final ObjectMapper objectMapper;

    public RunnerMain(ApplicationContext context,
                     DemoWorkloadSimulator demoSimulator,
                     HttpRestWorkloadSimulator httpSimulator,
                     MetricsLogger metricsLogger,
                     SimpleAgent simpleAgent,
                     SpringAiLlmAgent llmAgent,
                     @Value("${workload.simulator:demo}") String simulatorName,
                     @Value("${agent.strategy:llm}") String agentStrategy,
                     @Value("${run.mode:web}") String runMode) {
        this.context = context;
        this.simulators = Map.of(
                "demo", demoSimulator,
                "http", httpSimulator
        );
        this.simulatorName = simulatorName;
        this.agentStrategy = agentStrategy;
        this.runMode = runMode;
        this.metricsLogger = metricsLogger;
        this.simpleAgent = simpleAgent;
        this.llmAgent = llmAgent;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public void run(String... args) throws Exception {
        // Only run in CLI mode
        if (!"cli".equalsIgnoreCase(runMode)) {
            log.info("Running in {} mode - CommandLineRunner disabled", runMode);
            log.info("Access the live dashboard at: http://localhost:8080/live-dashboard.html");
            return;
        }
        
        if (log.isInfoEnabled()) {
            log.info("=".repeat(60));
            log.info("Agent Cloud Optimizer v0.2.0 - CLI Mode");
            log.info("=".repeat(60));
        }

        // Get configuration (agent.strategy now injected via constructor)
        // Allow system property override if provided
        String effectiveStrategy = System.getProperty("agent.strategy", this.agentStrategy);
        int baselineConcurrency = Integer.parseInt(System.getProperty("baseline.concurrency", "4"));
        int loadDuration = Integer.parseInt(System.getProperty("load.duration", "10"));
        double targetRps = Double.parseDouble(System.getProperty("target.rps", "0"));

        // Get the selected simulator
        WorkloadSimulator workloadSimulator = simulators.get(simulatorName);
        if (workloadSimulator == null) {
            log.error("Unknown simulator: {}. Available: {}", simulatorName, simulators.keySet());
            System.exit(SpringApplication.exit(context, () -> 1));
            return;
        }
        
        // Health check
        log.info("Performing health check for '{}' simulator...", simulatorName);
        if (!workloadSimulator.isHealthy()) {
            log.error("Health check FAILED for '{}' simulator. Cannot proceed.", simulatorName);
            log.error("Please verify configuration and connectivity.");
            System.exit(SpringApplication.exit(context, () -> 1));
            return;
        }
        log.info("Health check PASSED for '{}' simulator.", simulatorName);
        log.info("");
        
        log.info("Configuration:");
        log.info("  Workload Simulator: {}", simulatorName);
        log.info("  Agent Strategy: {} (LLM-powered optimization - use 'simple' only for testing)", effectiveStrategy);
        log.info("  Baseline Concurrency: {}", baselineConcurrency);
        log.info("  Load Duration: {}s", loadDuration);
        log.info("  Target RPS: {}", targetRps > 0 ? targetRps : "unlimited");
        log.info("");

        // Create artifacts directory
        Path artifactsDir = Paths.get("artifacts");
        if (!Files.exists(artifactsDir)) {
            Files.createDirectories(artifactsDir);
        }

        // Phase 1: Baseline Load Test
        log.info("Phase 1: Running baseline load test with {} simulator...", workloadSimulator.getName());
        RunResult baseline = workloadSimulator.executeLoad(baselineConcurrency, loadDuration, targetRps);
        log.info("Baseline Results: {}", baseline);
        writeJson(artifactsDir.resolve("baseline.json"), baseline);

        // Phase 2: Agent Analysis
        log.info("");
        log.info("Phase 2: Analyzing metrics with {} agent (including heap optimization)...", effectiveStrategy);
        
        // Wait briefly for async metrics to flush to disk
        Thread.sleep(1000);

        AgentDecision decision;
        if ("llm".equalsIgnoreCase(effectiveStrategy)) {
            decision = llmAgent.decideWithHeapAnalysis(baseline, baselineConcurrency);
        } else {
            log.warn("Using simple agent mode - this is NOT the intended use case! LLM mode provides the core value.");
            decision = simpleAgent.decideWithHeapAnalysis(baseline, baselineConcurrency);
        }

        log.info("Agent Decision: {}", decision.getRecommendation());
        if (decision.getRecommendedHeapSizeMb() != null) {
            log.info("Recommended Heap Size: {} MB", decision.getRecommendedHeapSizeMb());
        }
        log.info("Reasoning: {}", decision.getReasoning());
        if (log.isInfoEnabled()) {
            log.info("Confidence: {}%", String.format("%.0f", decision.getConfidenceScore() * 100));
        }

        // Extract new concurrency from decision
        int newConcurrency = extractConcurrency(decision.getRecommendation(), baselineConcurrency);
        log.info("Applying new concurrency: {} -> {}", baselineConcurrency, newConcurrency);

        // Phase 3: Post-Optimization Load Test
        log.info("");
        log.info("Phase 3: Running post-optimization load test...");
        RunResult after = workloadSimulator.executeLoad(newConcurrency, loadDuration, targetRps);
        log.info("Post-Optimization Results: {}", after);
        writeJson(artifactsDir.resolve("after.json"), after);

        // Phase 4: Generate Report
        log.info("");
        log.info("Phase 4: Generating comparison report...");
        Map<String, Object> report = generateReport(baseline, after, decision, effectiveStrategy);
        writeJson(artifactsDir.resolve("report.json"), report);

        // Print Summary
        printSummary(baseline, after, decision);

        if (log.isInfoEnabled()) {
            log.info("");
            log.info("=".repeat(60));
            log.info("Optimization Complete!");
            log.info("=".repeat(60));
            log.info("Artifacts written to: {}", artifactsDir.toAbsolutePath());
            log.info("  - baseline.json");
            log.info("  - after.json");
            log.info("  - report.json");
            log.info("  - reasoning_trace_{}.txt", effectiveStrategy.equals("llm") ? "llm" : "rule");
            log.info("");
        }

        // Exit application
        System.exit(SpringApplication.exit(context, () -> 0));
    }

    /**
     * Extracts concurrency value from recommendation string.
     */
    private int extractConcurrency(String recommendation, int defaultValue) {
        try {
            // Look for pattern "Set concurrency to X" or "concurrency to X"
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

    /**
     * Generates comparison report.
     */
    private Map<String, Object> generateReport(RunResult baseline, RunResult after, 
                                               AgentDecision decision, String agentStrategy) {
        Map<String, Object> report = new HashMap<>();
        
        report.put("agent_strategy", agentStrategy);
        report.put("timestamp", java.time.Instant.now().toString());
        
        // Baseline metrics
        Map<String, Object> baselineMap = new HashMap<>();
        baselineMap.put(CONCURRENCY, baseline.getConcurrency());
        baselineMap.put(MEDIAN_LATENCY_MS, baseline.getMedianLatencyMs());
        baselineMap.put(AVG_LATENCY_MS, baseline.getAvgLatencyMs());
        baselineMap.put(P95_LATENCY_MS, baseline.getP95LatencyMs());
        baselineMap.put(REQUESTS_PER_SECOND, baseline.getRequestsPerSecond());
        baselineMap.put(TOTAL_REQUESTS, baseline.getTotalRequests());
        baselineMap.put(COST_ESTIMATE_USD, baseline.getCostEstimateUsd());
        
        // Add heap metrics if available
        if (baseline.getHeapMetrics() != null) {
            Map<String, Object> heapMap = new HashMap<>();
            heapMap.put("heap_size_mb", baseline.getHeapMetrics().getHeapSizeMb());
            heapMap.put("heap_used_mb", baseline.getHeapMetrics().getHeapUsedMb());
            heapMap.put("heap_usage_percent", baseline.getHeapMetrics().getHeapUsagePercent());
            heapMap.put("gc_count", baseline.getHeapMetrics().getGcCount());
            heapMap.put("gc_time_ms", baseline.getHeapMetrics().getGcTimeMs());
            heapMap.put("gc_pause_avg_ms", baseline.getHeapMetrics().getGcPauseTimeAvgMs());
            heapMap.put("gc_frequency_per_sec", baseline.getHeapMetrics().getGcFrequencyPerSec());
            baselineMap.put("heap_metrics", heapMap);
        }
        report.put("baseline", baselineMap);
        
        // After metrics
        Map<String, Object> afterMap = new HashMap<>();
        afterMap.put(CONCURRENCY, after.getConcurrency());
        afterMap.put(MEDIAN_LATENCY_MS, after.getMedianLatencyMs());
        afterMap.put(AVG_LATENCY_MS, after.getAvgLatencyMs());
        afterMap.put(P95_LATENCY_MS, after.getP95LatencyMs());
        afterMap.put(REQUESTS_PER_SECOND, after.getRequestsPerSecond());
        afterMap.put(TOTAL_REQUESTS, after.getTotalRequests());
        afterMap.put(COST_ESTIMATE_USD, after.getCostEstimateUsd());
        
        // Add heap metrics if available
        if (after.getHeapMetrics() != null) {
            Map<String, Object> heapMap = new HashMap<>();
            heapMap.put("heap_size_mb", after.getHeapMetrics().getHeapSizeMb());
            heapMap.put("heap_used_mb", after.getHeapMetrics().getHeapUsedMb());
            heapMap.put("heap_usage_percent", after.getHeapMetrics().getHeapUsagePercent());
            heapMap.put("gc_count", after.getHeapMetrics().getGcCount());
            heapMap.put("gc_time_ms", after.getHeapMetrics().getGcTimeMs());
            heapMap.put("gc_pause_avg_ms", after.getHeapMetrics().getGcPauseTimeAvgMs());
            heapMap.put("gc_frequency_per_sec", after.getHeapMetrics().getGcFrequencyPerSec());
            afterMap.put("heap_metrics", heapMap);
        }
        report.put("after", afterMap);
        
        // Improvements
        Map<String, Object> improvements = new HashMap<>();
        improvements.put("latency_change_ms", after.getMedianLatencyMs() - baseline.getMedianLatencyMs());
        improvements.put("latency_change_percent", 
                calculatePercentChange(baseline.getMedianLatencyMs(), after.getMedianLatencyMs()));
        improvements.put("throughput_change_rps", after.getRequestsPerSecond() - baseline.getRequestsPerSecond());
        improvements.put("throughput_change_percent", 
                calculatePercentChange(baseline.getRequestsPerSecond(), after.getRequestsPerSecond()));
        improvements.put("concurrency_change", after.getConcurrency() - baseline.getConcurrency());
        
        // Add heap improvements if available
        if (baseline.getHeapMetrics() != null && after.getHeapMetrics() != null) {
            Map<String, Object> heapImprovements = new HashMap<>();
            heapImprovements.put("heap_size_change_mb", 
                    after.getHeapMetrics().getHeapSizeMb() - baseline.getHeapMetrics().getHeapSizeMb());
            heapImprovements.put("heap_usage_change_percent", 
                    after.getHeapMetrics().getHeapUsagePercent() - baseline.getHeapMetrics().getHeapUsagePercent());
            heapImprovements.put("gc_frequency_change_per_sec", 
                    after.getHeapMetrics().getGcFrequencyPerSec() - baseline.getHeapMetrics().getGcFrequencyPerSec());
            heapImprovements.put("gc_frequency_reduction_percent", 
                    calculatePercentChange(baseline.getHeapMetrics().getGcFrequencyPerSec(), 
                            after.getHeapMetrics().getGcFrequencyPerSec()));
            heapImprovements.put("gc_pause_change_ms", 
                    after.getHeapMetrics().getGcPauseTimeAvgMs() - baseline.getHeapMetrics().getGcPauseTimeAvgMs());
            heapImprovements.put("gc_pause_improvement_percent", 
                    calculatePercentChange(baseline.getHeapMetrics().getGcPauseTimeAvgMs(), 
                            after.getHeapMetrics().getGcPauseTimeAvgMs()));
            improvements.put("heap_improvements", heapImprovements);
        }
        
        report.put("improvements", improvements);
        
        // Decision info
        Map<String, Object> decisionMap = new HashMap<>();
        decisionMap.put("recommendation", decision.getRecommendation());
        if (decision.getRecommendedHeapSizeMb() != null) {
            decisionMap.put("recommended_heap_size_mb", decision.getRecommendedHeapSizeMb());
        }
        decisionMap.put("reasoning", decision.getReasoning());
        decisionMap.put("confidence_score", decision.getConfidenceScore());
        decisionMap.put("impact_level", decision.getImpactLevel().toString());
        report.put("decision", decisionMap);
        
        return report;
    }

    /**
     * Calculates percent change between two values.
     */
    private double calculatePercentChange(double baseline, double after) {
        if (baseline == 0) {
            return 0.0;
        }
        return ((after - baseline) / baseline) * 100.0;
    }

    /**
     * Prints summary to console.
     */
    private void printSummary(RunResult baseline, RunResult after, AgentDecision decision) {
        if (!log.isInfoEnabled()) {
            return;
        }
        
        log.info("");
        log.info("=".repeat(60));
        log.info("OPTIMIZATION SUMMARY");
        log.info("=".repeat(60));
        log.info("");
        
        int concurrencyChange = after.getConcurrency() - baseline.getConcurrency();
        log.info("Concurrency:  {} → {} ({}{})", 
                baseline.getConcurrency(), 
                after.getConcurrency(),
                concurrencyChange > 0 ? "+" : "",
                concurrencyChange);
        log.info("");
        
        double latencyChange = after.getMedianLatencyMs() - baseline.getMedianLatencyMs();
        double latencyChangePercent = calculatePercentChange(baseline.getMedianLatencyMs(), after.getMedianLatencyMs());
        log.info("Latency (median):");
        log.info("  Before:  {} ms", String.format("%.2f", baseline.getMedianLatencyMs()));
        log.info("  After:   {} ms", String.format("%.2f", after.getMedianLatencyMs()));
        log.info("  Change:  {} ms ({}%)", 
                String.format("%.2f", latencyChange),
                String.format("%+.1f", latencyChangePercent));
        log.info("");
        
        double rpsChange = after.getRequestsPerSecond() - baseline.getRequestsPerSecond();
        double rpsChangePercent = calculatePercentChange(baseline.getRequestsPerSecond(), after.getRequestsPerSecond());
        log.info("Throughput (RPS):");
        log.info("  Before:  {} req/s", String.format("%.2f", baseline.getRequestsPerSecond()));
        log.info("  After:   {} req/s", String.format("%.2f", after.getRequestsPerSecond()));
        log.info("  Change:  {} req/s ({}%)", 
                String.format("%+.2f", rpsChange),
                String.format("%+.1f", rpsChangePercent));
        log.info("");
        
        log.info("Decision Confidence: {}%", String.format("%.0f", decision.getConfidenceScore() * 100));
        log.info("Impact Level: {}", decision.getImpactLevel());
    }

    /**
     * Writes object as JSON to file.
     */
    private void writeJson(Path path, Object obj) throws IOException {
        objectMapper.writeValue(path.toFile(), obj);
        log.debug("Written: {}", path);
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        SpringApplication.run(AgentCloudOptimizerApplication.class, args);
    }
}
