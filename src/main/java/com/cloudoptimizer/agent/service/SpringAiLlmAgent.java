package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * LLM-based agent for cloud resource optimization using Spring AI + Ollama.
 * 
 * Leverages local LLM (llama2/mistral) to analyze metrics and generate
 * optimization recommendations. Runs 100% offline with no API keys required.
 * 
 * Outputs structured JSON decisions and writes reasoning traces to
 * artifacts/reasoning_trace_llm.txt
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since December 2025
 */
@Service
public class SpringAiLlmAgent {

    private static final Logger log = LoggerFactory.getLogger(SpringAiLlmAgent.class);
    
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final LlmPromptBuilder promptBuilder;

    @Value("${agent.target-latency-ms:100.0}")
    private double targetLatencyMs;

    @Value("${agent.artifacts-dir:artifacts}")
    private String artifactsDir;

    @Autowired
    public SpringAiLlmAgent(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.objectMapper = new ObjectMapper();
        this.promptBuilder = new LlmPromptBuilder();
    }

    /**
     * Makes optimization decision using LLM analysis.
     * 
     * @param recentMetrics list of recent metric samples
     * @param currentConcurrency current concurrency setting
     * @return AgentDecision with LLM-generated recommendation
     */
    public AgentDecision decide(List<MetricRow> recentMetrics, int currentConcurrency) {
        log.info("SpringAiLlmAgent analyzing {} metrics with current concurrency {}", 
                recentMetrics.size(), currentConcurrency);

        if (recentMetrics == null || recentMetrics.isEmpty()) {
            throw new IllegalArgumentException("Metrics list cannot be empty");
        }

        try {
            // Build structured prompt
            String prompt = promptBuilder.buildPrompt(recentMetrics, currentConcurrency, targetLatencyMs);
            
            log.debug("LLM Prompt: {}", prompt);
            
            // Query LLM
            ChatResponse response = chatClient.call(new Prompt(prompt));
            String llmOutput = response.getResult().getOutput().getContent();
            
            log.debug("LLM Response: {}", llmOutput);
            
            // Parse JSON response
            AgentDecision decision = parseJsonResponse(llmOutput, currentConcurrency);
            
            // Write reasoning trace
            writeReasoningTrace(decision, prompt, llmOutput, currentConcurrency);
            
            log.info("SpringAiLlmAgent decision: {} -> {} (confidence: {:.2f})",
                    currentConcurrency, 
                    extractNewConcurrency(llmOutput), 
                    decision.getConfidenceScore());
            
            return decision;
            
        } catch (Exception e) {
            log.error("LLM agent failed, returning fallback decision", e);
            return createFallbackDecision(currentConcurrency, e);
        }
    }

    /**
     * Makes optimization decision with heap analysis using LLM.
     * 
     * @param baselineResult baseline test results with heap metrics
     * @param currentConcurrency current concurrency setting
     * @return AgentDecision with LLM-generated recommendation including heap analysis
     */
    public AgentDecision decideWithHeapAnalysis(RunResult baselineResult, int currentConcurrency) {
        log.info("SpringAiLlmAgent analyzing baseline result with heap metrics");

        if (baselineResult == null) {
            throw new IllegalArgumentException("Baseline result cannot be null");
        }

        try {
            // Build structured prompt with heap metrics
            String prompt = promptBuilder.buildPromptWithHeapMetrics(baselineResult, currentConcurrency, targetLatencyMs);
            
            log.debug("LLM Prompt (with heap): {}", prompt);
            
            // Query LLM
            ChatResponse response = chatClient.call(new Prompt(prompt));
            String llmOutput = response.getResult().getOutput().getContent();
            
            log.debug("LLM Response (with heap): {}", llmOutput);
            
            // Parse JSON response with heap recommendations
            AgentDecision decision = parseJsonResponseWithHeap(llmOutput, currentConcurrency, baselineResult);
            
            // Write reasoning trace
            writeHeapReasoningTrace(decision, prompt, llmOutput, baselineResult);
            
            log.info("SpringAiLlmAgent decision: concurrency {} -> {}, heap -> {}MB (confidence: {:.2f})",
                    currentConcurrency, 
                    decision.getRecommendation().contains("Set concurrency to") ? 
                        extractNewConcurrency(llmOutput) : currentConcurrency,
                    decision.getRecommendedHeapSizeMb(),
                    decision.getConfidenceScore());
            
            return decision;
            
        } catch (Exception e) {
            log.error("LLM agent failed with heap analysis, returning fallback decision", e);
            return createFallbackDecisionWithHeap(currentConcurrency, baselineResult, e);
        }
    }

    /**
     * Parses LLM JSON response into AgentDecision.
     * Expected format: {"newConcurrency":8,"expectedLatencyMs":78.3,"explanation":"..."}
     */
    private AgentDecision parseJsonResponse(String llmOutput, int currentConcurrency) {
        try {
            // Extract JSON from response (may contain markdown code blocks)
            String jsonStr = extractJson(llmOutput);
            
            JsonNode json = objectMapper.readTree(jsonStr);
            
            int newConcurrency = json.has("newConcurrency") 
                    ? json.get("newConcurrency").asInt() 
                    : currentConcurrency;
            
            double expectedLatency = json.has("expectedLatencyMs") 
                    ? json.get("expectedLatencyMs").asDouble() 
                    : targetLatencyMs;
            
            String explanation = json.has("explanation") 
                    ? json.get("explanation").asText() 
                    : "LLM provided optimization recommendation";
            
            // Validate concurrency bounds
            newConcurrency = Math.max(1, Math.min(newConcurrency, 100));
            
            // Determine impact level
            int change = Math.abs(newConcurrency - currentConcurrency);
            AgentDecision.ImpactLevel impactLevel = determineImpactLevel(change, currentConcurrency);
            
            // Calculate confidence based on change magnitude
            double confidence = calculateConfidence(change, currentConcurrency);
            
            return AgentDecision.builder()
                    .decisionId(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .resourceId("concurrency-pool")
                    .resourceType("ExecutorService")
                    .strategy(AgentStrategy.BALANCED)
                    .recommendation(String.format("Set concurrency to %d", newConcurrency))
                    .reasoning(explanation)
                    .confidenceScore(confidence)
                    .impactLevel(impactLevel)
                    .actionItems(java.util.List.of(
                            String.format("Update concurrency from %d to %d", currentConcurrency, newConcurrency),
                            String.format("Monitor latency to verify expected %.2fms", expectedLatency),
                            "Review LLM reasoning trace for detailed analysis"))
                    .metricsAnalyzed(java.util.List.of("latencyMs"))
                    .risks(java.util.List.of("LLM recommendations should be validated before production deployment"))
                    .build();
            
        } catch (Exception e) {
            log.error("Failed to parse LLM JSON response", e);
            throw new RuntimeException("Invalid LLM response format", e);
        }
    }

    /**
     * Extracts JSON from LLM output (handles markdown code blocks).
     */
    private String extractJson(String llmOutput) {
        // Remove markdown code blocks if present
        String cleaned = llmOutput.trim();
        
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        cleaned = cleaned.trim();
        
        // Find JSON object boundaries
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        
        return cleaned;
    }

    /**
     * Extracts new concurrency value from LLM output.
     */
    private int extractNewConcurrency(String llmOutput) {
        try {
            String jsonStr = extractJson(llmOutput);
            JsonNode json = objectMapper.readTree(jsonStr);
            return json.has("newConcurrency") ? json.get("newConcurrency").asInt() : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Determines impact level based on change magnitude.
     */
    private AgentDecision.ImpactLevel determineImpactLevel(int change, int currentConcurrency) {
        double changePercent = (double) change / currentConcurrency * 100;
        
        if (changePercent > 50) {
            return AgentDecision.ImpactLevel.CRITICAL;
        } else if (changePercent > 25) {
            return AgentDecision.ImpactLevel.HIGH;
        } else if (changePercent > 10) {
            return AgentDecision.ImpactLevel.MEDIUM;
        } else {
            return AgentDecision.ImpactLevel.LOW;
        }
    }

    /**
     * Calculates confidence score based on change magnitude.
     */
    private double calculateConfidence(int change, int currentConcurrency) {
        double changePercent = (double) change / currentConcurrency * 100;
        
        // Higher confidence for smaller changes
        if (changePercent < 10) {
            return 0.90;
        } else if (changePercent < 25) {
            return 0.80;
        } else if (changePercent < 50) {
            return 0.70;
        } else {
            return 0.60;
        }
    }

    /**
     * Creates fallback decision when LLM fails.
     */
    private AgentDecision createFallbackDecision(int currentConcurrency, Exception error) {
        return AgentDecision.builder()
                .decisionId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .resourceId("concurrency-pool")
                .resourceType("ExecutorService")
                .strategy(AgentStrategy.BALANCED)
                .recommendation(String.format("Maintain concurrency at %d (LLM unavailable)", 
                        currentConcurrency))
                .reasoning(String.format("LLM agent encountered error: %s. " +
                        "Maintaining current configuration as safe fallback.", 
                        error.getMessage()))
                .confidenceScore(0.50)
                .impactLevel(AgentDecision.ImpactLevel.LOW)
                .actionItems(java.util.List.of(
                        "Investigate LLM connectivity issues",
                        "Verify Ollama service is running"))
                .risks(java.util.List.of("Operating without AI-powered optimization"))
                .build();
    }

    /**
     * Writes reasoning trace to artifacts directory.
     */
    private void writeReasoningTrace(AgentDecision decision, String prompt, 
                                     String llmOutput, int currentConcurrency) {
        try {
            Path artifactsPath = Paths.get(artifactsDir);
            if (!Files.exists(artifactsPath)) {
                Files.createDirectories(artifactsPath);
            }

            Path tracePath = artifactsPath.resolve("reasoning_trace_llm.txt");
            
            StringBuilder trace = new StringBuilder();
            trace.append("=".repeat(60)).append("%n");
            trace.append("LLM Agent Decision Trace%n");
            trace.append("=".repeat(60)).append("%n");
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
            trace.append("=".repeat(60)).append("%n%n");

            Files.writeString(tracePath, trace.toString(), 
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
            log.info("LLM reasoning trace written to {}", tracePath);
            
        } catch (IOException e) {
            log.error("Failed to write LLM reasoning trace", e);
        }
    }

    /**
     * Parses LLM JSON response with heap recommendations into AgentDecision.
     */
    private AgentDecision parseJsonResponseWithHeap(String llmOutput, int currentConcurrency, RunResult baselineResult) {
        try {
            String jsonStr = extractJson(llmOutput);
            JsonNode json = objectMapper.readTree(jsonStr);
            
            int newConcurrency = json.has("newConcurrency") ? json.get("newConcurrency").asInt() : currentConcurrency;
            Integer recommendedHeapMb = json.has("recommendedHeapSizeMb") ? json.get("recommendedHeapSizeMb").asInt() : null;
            double expectedLatency = json.has("expectedLatencyMs") ? json.get("expectedLatencyMs").asDouble() : baselineResult.getMedianLatencyMs();
            
            // Extract separate reasoning fields from LLM response
            String concurrencyReasoning = json.has("concurrencyReasoning") ? json.get("concurrencyReasoning").asText() : "";
            String heapReasoning = json.has("heapReasoning") ? json.get("heapReasoning").asText() : "";
            
            // Combine reasoning into full explanation
            String fullReasoning = concurrencyReasoning;
            if (!heapReasoning.isEmpty()) {
                fullReasoning += (fullReasoning.isEmpty() ? "" : " ") + heapReasoning;
            }
            if (fullReasoning.isEmpty()) {
                fullReasoning = "LLM analysis with heap metrics";
            }
            
            int change = Math.abs(newConcurrency - currentConcurrency);
            AgentDecision.ImpactLevel impactLevel = determineImpactLevel(change, currentConcurrency);
            double confidence = calculateConfidence(change, currentConcurrency);
            
            return AgentDecision.builder()
                    .decisionId(java.util.UUID.randomUUID().toString())
                    .timestamp(java.time.Instant.now())
                    .resourceId("service-optimization")
                    .resourceType("JavaService")
                    .strategy(AgentStrategy.BALANCED)
                    .recommendation(String.format("Set concurrency to %d", newConcurrency))
                    .reasoning(fullReasoning)
                    .confidenceScore(confidence)
                    .impactLevel(impactLevel)
                    .recommendedHeapSizeMb(recommendedHeapMb)
                    .actionItems(java.util.List.of(
                            String.format("Update concurrency from %d to %d", currentConcurrency, newConcurrency),
                            String.format("Monitor latency to verify expected %.2fms", expectedLatency),
                            recommendedHeapMb != null ? String.format("Update heap size to %dMB", recommendedHeapMb) : "Monitor heap metrics"))
                    .metricsAnalyzed(java.util.List.of("latencyMs", "heapMetrics"))
                    .risks(java.util.List.of("LLM recommendations should be validated before production deployment"))
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse LLM JSON response with heap", e);
            throw new RuntimeException("Invalid LLM response format", e);
        }
    }

    /**
     * Writes heap reasoning trace to artifacts directory.
     */
    private void writeHeapReasoningTrace(AgentDecision decision, String prompt, String llmOutput, RunResult baselineResult) {
        try {
            Path artifactsPath = Paths.get(artifactsDir);
            if (!Files.exists(artifactsPath)) {
                Files.createDirectories(artifactsPath);
            }
            
            Path tracePath = artifactsPath.resolve("reasoning_trace_llm.txt");
            StringBuilder trace = new StringBuilder();
            
            trace.append("=".repeat(60)).append("%n");
            trace.append("LLM REASONING TRACE (WITH HEAP ANALYSIS)%n");
            trace.append("Timestamp: ").append(java.time.Instant.now()).append("%n");
            trace.append("=".repeat(60)).append("%n%n");
            
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
            
            trace.append("--- LLM Response ---%n");
            trace.append(llmOutput).append("%n%n");
            
            trace.append("--- Parsed Decision ---%n");
            trace.append(String.format("Recommendation: %s%n", decision.getRecommendation()));
            trace.append(String.format("Heap Recommendation: %s MB%n", decision.getRecommendedHeapSizeMb()));
            trace.append(String.format("Confidence: %.0f%%%n", decision.getConfidenceScore() * 100));
            trace.append("%n");
            
            Files.writeString(tracePath, trace.toString(), 
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            
            log.info("LLM heap reasoning trace written to {}", tracePath);
        } catch (IOException e) {
            log.error("Failed to write LLM heap reasoning trace", e);
        }
    }

    /**
     * Creates fallback decision with heap analysis when LLM fails.
     */
    private AgentDecision createFallbackDecisionWithHeap(int currentConcurrency, RunResult baselineResult, Exception error) {
        return AgentDecision.builder()
                .decisionId(java.util.UUID.randomUUID().toString())
                .timestamp(java.time.Instant.now())
                .resourceId("service-optimization")
                .resourceType("JavaService")
                .strategy(AgentStrategy.BALANCED)
                .recommendation(String.format("Maintain concurrency at %d (LLM unavailable)", currentConcurrency))
                .reasoning(String.format("LLM agent encountered error: %s. Maintaining current configuration as safe fallback.", 
                        error.getMessage()))
                .confidenceScore(0.50)
                .impactLevel(AgentDecision.ImpactLevel.LOW)
                .recommendedHeapSizeMb(baselineResult.getHeapMetrics() != null ? 
                        (int) baselineResult.getHeapMetrics().getHeapSizeMb() : null)
                .actionItems(java.util.List.of(
                        "Investigate LLM connectivity issues",
                        "Verify Ollama service is running"))
                .risks(java.util.List.of("Operating without AI-powered optimization"))
                .build();
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
