package com.cloudoptimizer.agent;

import com.cloudoptimizer.agent.model.AgentDecision;
import com.cloudoptimizer.agent.model.MetricRow;
import com.cloudoptimizer.agent.model.RunResult;
import com.cloudoptimizer.agent.service.LoadRunner;
import com.cloudoptimizer.agent.simulator.WorkloadSimulator;
import com.cloudoptimizer.agent.simulator.DemoWorkloadSimulator;
import com.cloudoptimizer.agent.simulator.HttpRestWorkloadSimulator;
import org.springframework.beans.factory.annotation.Value;
import com.cloudoptimizer.agent.service.MetricsLogger;
import com.cloudoptimizer.agent.service.SimpleAgent;
import com.cloudoptimizer.agent.service.SpringAiLlmAgent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
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
                     @Value("${workload.simulator:demo}") String simulatorName) {
        this.context = context;
        this.simulators = Map.of(
                "demo", demoSimulator,
                "http", httpSimulator
        );
        this.simulatorName = simulatorName;
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
        if (log.isInfoEnabled()) {
            log.info("=".repeat(60));
            log.info("Agent Cloud Optimizer v1.0.0");
            log.info("=".repeat(60));
        }

        // Get configuration
        String agentStrategy = System.getProperty("agent.strategy", "simple");
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
        
        log.info("Configuration:");
        log.info("  Workload Simulator: {}", simulatorName);
        log.info("  Agent Strategy: {}", agentStrategy);
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
        log.info("Phase 2: Analyzing metrics with {} agent...", agentStrategy);
        
        // Wait briefly for async metrics to flush to disk
        Thread.sleep(1000);
        
        // Get recent metrics
        List<MetricRow> recentMetrics = metricsLogger.readRecent(20);
        log.info("Analyzing {} recent metrics", recentMetrics.size());

        AgentDecision decision;
        if ("llm".equalsIgnoreCase(agentStrategy)) {
            decision = llmAgent.decide(recentMetrics, baselineConcurrency);
        } else {
            decision = simpleAgent.decide(recentMetrics, baselineConcurrency);
        }

        log.info("Agent Decision: {}", decision.getRecommendation());
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
        Map<String, Object> report = generateReport(baseline, after, decision, agentStrategy);
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
            log.info("  - reasoning_trace_{}.txt", agentStrategy);
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
        report.put("improvements", improvements);
        
        // Decision info
        Map<String, Object> decisionMap = new HashMap<>();
        decisionMap.put("recommendation", decision.getRecommendation());
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
