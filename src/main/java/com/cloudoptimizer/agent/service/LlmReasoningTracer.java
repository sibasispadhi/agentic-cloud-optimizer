package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.model.AgentDecision;
import com.cloudoptimizer.agent.model.HeapMetrics;
import com.cloudoptimizer.agent.model.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * Writes reasoning traces for LLM decisions to artifacts directory.
 * 
 * Provides comprehensive audit trail of AI decision-making including
 * prompts, responses, and parsed decisions.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since January 2026
 */
@Component
public class LlmReasoningTracer {

    private static final Logger log = LoggerFactory.getLogger(LlmReasoningTracer.class);
    private static final String SEPARATOR = "=".repeat(60);

    /**
     * Writes basic reasoning trace to artifacts directory.
     */
    public void writeReasoningTrace(AgentDecision decision, String prompt, 
                                    String llmOutput, int currentConcurrency,
                                    double targetLatencyMs, String artifactsDir) {
        try {
            Path artifactsPath = Paths.get(artifactsDir);
            if (!Files.exists(artifactsPath)) {
                Files.createDirectories(artifactsPath);
            }

            Path tracePath = artifactsPath.resolve("reasoning_trace_llm.txt");
            
            StringBuilder trace = new StringBuilder();
            trace.append(SEPARATOR).append("%n");
            trace.append("LLM Agent Decision Trace%n");
            trace.append(SEPARATOR).append("%n");
            trace.append(String.format("Timestamp: %s%n", decision.getTimestamp().toString()));
            trace.append(String.format("Decision ID: %s%n", decision.getDecisionId()));
            trace.append("%n");
            
            trace.append("--- Input Parameters ---%n");
            trace.append(String.format("Current Concurrency: %d%n", currentConcurrency));
            trace.append(String.format("Target Latency: %.2f ms%n", targetLatencyMs));
            trace.append("%n");
            
            trace.append("--- LLM Prompt ---%n");
            trace.append(prompt).append("%n");
            trace.append("%n");
            
            trace.append("--- LLM Response ---%n");
            trace.append(llmOutput).append("%n");
            trace.append("%n");
            
            trace.append("--- Parsed Decision ---%n");
            trace.append(String.format("Recommendation: %s%n", decision.getRecommendation()));
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
            
            trace.append("--- Risks ---%n");
            for (int i = 0; i < decision.getRisks().size(); i++) {
                trace.append(String.format("%d. %s%n", i + 1, 
                        decision.getRisks().get(i)));
            }
            trace.append("%n");
            trace.append(SEPARATOR).append("%n%n");

            Files.writeString(tracePath, trace.toString(), 
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
            log.info("LLM reasoning trace written to {}", tracePath);
            
        } catch (IOException e) {
            log.error("Failed to write LLM reasoning trace", e);
        }
    }

    /**
     * Writes heap reasoning trace to artifacts directory.
     */
    public void writeHeapReasoningTrace(AgentDecision decision, String prompt, 
                                        String llmOutput, RunResult baselineResult, 
                                        String artifactsDir) {
        try {
            Path artifactsPath = Paths.get(artifactsDir);
            if (!Files.exists(artifactsPath)) {
                Files.createDirectories(artifactsPath);
            }
            
            Path tracePath = artifactsPath.resolve("reasoning_trace_llm.txt");
            StringBuilder trace = new StringBuilder();
            
            trace.append(SEPARATOR).append("%n");
            trace.append("LLM REASONING TRACE (WITH HEAP ANALYSIS)%n");
            trace.append("Timestamp: ").append(Instant.now()).append("%n");
            trace.append(SEPARATOR).append("%n%n");
            
            trace.append("--- Baseline Metrics ---%n");
            trace.append(String.format("Median Latency: %.2fms%n", baselineResult.getMedianLatencyMs()));
            trace.append(String.format("P95 Latency: %.2fms%n", baselineResult.getP95LatencyMs()));
            trace.append(String.format("Throughput: %.2f req/s%n", baselineResult.getRequestsPerSecond()));
            
            if (baselineResult.getHeapMetrics() != null) {
                HeapMetrics heap = baselineResult.getHeapMetrics();
                trace.append(String.format("Heap Usage: %.1f%% (%d/%d MB)%n", 
                        heap.getHeapUsagePercent(), heap.getHeapUsedMb(), heap.getHeapSizeMb()));
                trace.append(String.format("GC Frequency: %.2f/sec%n", heap.getGcFrequencyPerSec()));
                trace.append(String.format("GC Pause Avg: %.2fms%n", heap.getGcPauseTimeAvgMs()));
            }
            trace.append("%n");
            
            trace.append("--- LLM Prompt ---%n");
            trace.append(prompt).append("%n%n");
            
            trace.append("--- LLM Response ---%n");
            trace.append(llmOutput).append("%n%n");
            
            trace.append("--- Parsed Decision ---%n");
            trace.append(String.format("Recommendation: %s%n", decision.getRecommendation()));
            if (decision.getRecommendedHeapSizeMb() != null) {
                trace.append(String.format("Heap Recommendation: %s MB%n", decision.getRecommendedHeapSizeMb()));
            }
            trace.append(String.format("Confidence: %.0f%%%n", decision.getConfidenceScore() * 100));
            trace.append(String.format("Impact Level: %s%n", decision.getImpactLevel()));
            trace.append("%n");
            
            trace.append("--- Reasoning ---%n");
            trace.append(decision.getReasoning()).append("%n");
            trace.append("%n");
            
            trace.append("--- Action Items ---%n");
            for (int i = 0; i < decision.getActionItems().size(); i++) {
                trace.append(String.format("%d. %s%n", i + 1, decision.getActionItems().get(i)));
            }
            trace.append("%n");
            
            trace.append(SEPARATOR).append("%n%n");
            
            Files.writeString(tracePath, trace.toString(), 
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
            log.info("LLM heap reasoning trace written to {}", tracePath);
        } catch (IOException e) {
            log.error("Failed to write LLM heap reasoning trace", e);
        }
    }
}
