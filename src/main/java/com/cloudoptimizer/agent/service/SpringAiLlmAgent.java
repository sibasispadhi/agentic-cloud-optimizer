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
 * <p>Queries a local LLM (e.g. llama2/mistral via Ollama) with a structured prompt
 * containing recent JVM metrics. The LLM returns a JSON recommendation that is
 * parsed, bounds-checked, and returned as a typed {@link AgentDecision}.
 *
 * <p><b>Execution path:</b>
 * <ol>
 *   <li>{@link #decide} or {@link #decideWithHeapAnalysis} is called by the orchestrator.</li>
 *   <li>A prompt is built via {@link LlmPromptBuilder}.</li>
 *   <li>The prompt is sent to Ollama via {@link ChatClient}.</li>
 *   <li>The LLM JSON response is parsed and bounds-checked.</li>
 *   <li>A reasoning trace is appended to {@code artifacts/reasoning_trace_llm.txt}.</li>
 *   <li>If the LLM is unreachable, a safe fallback decision is returned.</li>
 * </ol>
 *
 * <p><b>Artifact outputs:</b> {@code artifacts/reasoning_trace_llm.txt}
 *
 * @author Sibasis Padhi
 */
@Service
public class SpringAiLlmAgent {

    private static final Logger log = LoggerFactory.getLogger(SpringAiLlmAgent.class);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final LlmPromptBuilder promptBuilder;

    // ── General config ────────────────────────────────────────────────────────

    @Value("${agent.target-latency-ms:100.0}")
    private double targetLatencyMs;

    @Value("${agent.artifacts-dir:artifacts}")
    private String artifactsDir;

    // ── Concurrency output bounds ─────────────────────────────────────────────

    @Value("${agent.llm.min-concurrency:1}")
    private int minConcurrency;

    @Value("${agent.llm.max-concurrency:100}")
    private int maxConcurrency;

    // ── Impact level thresholds (% change vs current concurrency) ─────────────

    @Value("${agent.llm.impact.critical-change-pct:50.0}")
    private double impactCriticalChangePct;

    @Value("${agent.llm.impact.high-change-pct:25.0}")
    private double impactHighChangePct;

    @Value("${agent.llm.impact.medium-change-pct:10.0}")
    private double impactMediumChangePct;

    // ── Confidence scores by change magnitude ─────────────────────────────────

    @Value("${agent.llm.confidence.low-change:0.90}")
    private double confidenceLowChange;

    @Value("${agent.llm.confidence.medium-change:0.80}")
    private double confidenceMediumChange;

    @Value("${agent.llm.confidence.large-change:0.70}")
    private double confidenceLargeChange;

    @Value("${agent.llm.confidence.very-large-change:0.60}")
    private double confidenceVeryLargeChange;

    @Value("${agent.llm.fallback-confidence:0.50}")
    private double fallbackConfidence;

    @Autowired
    public SpringAiLlmAgent(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.objectMapper = new ObjectMapper();
        this.promptBuilder = new LlmPromptBuilder();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Makes an optimization decision using LLM analysis of recent metrics.
     *
     * @param recentMetrics    list of recent metric samples (must not be empty)
     * @param currentConcurrency current concurrency setting
     * @return {@link AgentDecision} with LLM-generated recommendation
     */
    public AgentDecision decide(List<MetricRow> recentMetrics, int currentConcurrency) {
        if (recentMetrics == null || recentMetrics.isEmpty()) {
            throw new IllegalArgumentException("Metrics list cannot be empty");
        }

        log.info("SpringAiLlmAgent analyzing {} metrics with current concurrency {}",
                recentMetrics.size(), currentConcurrency);

        try {
            String prompt = promptBuilder.buildPrompt(recentMetrics, currentConcurrency, targetLatencyMs);
            log.debug("LLM Prompt: {}", prompt);

            ChatResponse response = chatClient.call(new Prompt(prompt));
            String llmOutput = response.getResult().getOutput().getContent();
            log.debug("LLM Response: {}", llmOutput);

            AgentDecision decision = parseJsonResponse(llmOutput, currentConcurrency);
            writeReasoningTrace(decision, prompt, llmOutput, currentConcurrency);

            log.info("SpringAiLlmAgent decision: {} -> {} (confidence: {})",
                    currentConcurrency, decision.getRecommendation(), decision.getConfidenceScore());

            return decision;

        } catch (Exception e) {
            log.error("LLM agent failed, returning fallback decision", e);
            return createFallbackDecision(currentConcurrency, e.getMessage(), null);
        }
    }

    /**
     * Makes an optimization decision with heap analysis using LLM.
     *
     * @param baselineResult     baseline test results including heap metrics
     * @param currentConcurrency current concurrency setting
     * @return {@link AgentDecision} with LLM-generated recommendation including heap analysis
     */
    public AgentDecision decideWithHeapAnalysis(RunResult baselineResult, int currentConcurrency) {
        log.info("SpringAiLlmAgent analyzing baseline result with heap metrics");

        if (baselineResult == null) {
            throw new IllegalArgumentException("Baseline result cannot be null");
        }

        try {
            String prompt = promptBuilder.buildPromptWithHeapMetrics(baselineResult, currentConcurrency, targetLatencyMs);
            log.debug("LLM Prompt (with heap): {}", prompt);

            ChatResponse response = chatClient.call(new Prompt(prompt));
            String llmOutput = response.getResult().getOutput().getContent();
            log.debug("LLM Response (with heap): {}", llmOutput);

            AgentDecision decision = parseJsonResponseWithHeap(llmOutput, currentConcurrency, baselineResult);
            writeHeapReasoningTrace(decision, prompt, llmOutput, baselineResult);

            log.info("SpringAiLlmAgent decision: concurrency {} -> {}, heap {}MB (confidence: {})",
                    currentConcurrency, decision.getRecommendation(),
                    decision.getRecommendedHeapSizeMb(), decision.getConfidenceScore());

            return decision;

        } catch (Exception e) {
            log.error("LLM agent failed with heap analysis, returning fallback decision", e);
            Integer currentHeap = baselineResult.getHeapMetrics() != null
                    ? (int) baselineResult.getHeapMetrics().getHeapSizeMb() : null;
            return createFallbackDecision(currentConcurrency, e.getMessage(), currentHeap);
        }
    }

    // ── Accessors (used by tests) ──────────────────────────────────────────────

    public double getTargetLatencyMs()  { return targetLatencyMs; }
    public String getArtifactsDir()     { return artifactsDir; }

    // ── Parsing ───────────────────────────────────────────────────────────────

    /**
     * Parses a plain-concurrency LLM JSON response into an {@link AgentDecision}.
     * Expected format: {@code {"newConcurrency":8, "expectedLatencyMs":78.3, "explanation":"..."}}
     */
    private AgentDecision parseJsonResponse(String llmOutput, int currentConcurrency) {
        try {
            String jsonStr = extractJson(llmOutput);
            JsonNode json = objectMapper.readTree(jsonStr);

            int newConcurrency = json.has("newConcurrency")
                    ? json.get("newConcurrency").asInt() : currentConcurrency;
            double expectedLatency = json.has("expectedLatencyMs")
                    ? json.get("expectedLatencyMs").asDouble() : targetLatencyMs;
            String explanation = json.has("explanation")
                    ? json.get("explanation").asText() : "LLM provided optimization recommendation";

            newConcurrency = Math.max(minConcurrency, Math.min(newConcurrency, maxConcurrency));

            int change = Math.abs(newConcurrency - currentConcurrency);
            AgentDecision.ImpactLevel impact = determineImpactLevel(change, currentConcurrency);
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
                    .impactLevel(impact)
                    .actionItems(List.of(
                            String.format("Update concurrency from %d to %d", currentConcurrency, newConcurrency),
                            String.format("Monitor latency to verify expected %.2fms", expectedLatency),
                            "Review LLM reasoning trace for detailed analysis"))
                    .metricsAnalyzed(List.of("latencyMs"))
                    .risks(List.of("LLM recommendations should be validated before production deployment"))
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse LLM JSON response", e);
            throw new RuntimeException("Invalid LLM response format", e);
        }
    }

    /**
     * Parses an LLM JSON response that includes heap recommendations.
     */
    private AgentDecision parseJsonResponseWithHeap(String llmOutput, int currentConcurrency,
                                                    RunResult baselineResult) {
        try {
            String jsonStr = extractJson(llmOutput);
            JsonNode json = objectMapper.readTree(jsonStr);

            int newConcurrency = json.has("newConcurrency")
                    ? json.get("newConcurrency").asInt() : currentConcurrency;
            Integer recommendedHeapMb = json.has("recommendedHeapSizeMb")
                    ? json.get("recommendedHeapSizeMb").asInt() : null;
            double expectedLatency = json.has("expectedLatencyMs")
                    ? json.get("expectedLatencyMs").asDouble() : baselineResult.getMedianLatencyMs();

            String concurrencyReasoning = json.has("concurrencyReasoning")
                    ? json.get("concurrencyReasoning").asText() : "";
            String heapReasoning = json.has("heapReasoning")
                    ? json.get("heapReasoning").asText() : "";
            String fullReasoning = concurrencyReasoning.isEmpty() ? heapReasoning
                    : (heapReasoning.isEmpty() ? concurrencyReasoning
                    : concurrencyReasoning + " " + heapReasoning);
            if (fullReasoning.isEmpty()) fullReasoning = "LLM analysis with heap metrics";

            newConcurrency = Math.max(minConcurrency, Math.min(newConcurrency, maxConcurrency));
            int change = Math.abs(newConcurrency - currentConcurrency);

            return AgentDecision.builder()
                    .decisionId(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .resourceId("service-optimization")
                    .resourceType("JavaService")
                    .strategy(AgentStrategy.BALANCED)
                    .recommendation(String.format("Set concurrency to %d", newConcurrency))
                    .reasoning(fullReasoning)
                    .confidenceScore(calculateConfidence(change, currentConcurrency))
                    .impactLevel(determineImpactLevel(change, currentConcurrency))
                    .recommendedHeapSizeMb(recommendedHeapMb)
                    .actionItems(List.of(
                            String.format("Update concurrency from %d to %d", currentConcurrency, newConcurrency),
                            String.format("Monitor latency to verify expected %.2fms", expectedLatency),
                            recommendedHeapMb != null
                                    ? String.format("Update heap size to %dMB", recommendedHeapMb)
                                    : "Monitor heap metrics"))
                    .metricsAnalyzed(List.of("latencyMs", "heapMetrics"))
                    .risks(List.of("LLM recommendations should be validated before production deployment"))
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse LLM JSON response with heap", e);
            throw new RuntimeException("Invalid LLM response format", e);
        }
    }

    /**
     * Extracts the JSON object from LLM output, stripping markdown code fences if present.
     */
    private String extractJson(String llmOutput) {
        String cleaned = llmOutput.trim();
        if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
        else if (cleaned.startsWith("```"))  cleaned = cleaned.substring(3);
        if (cleaned.endsWith("```"))          cleaned = cleaned.substring(0, cleaned.length() - 3);
        cleaned = cleaned.trim();

        int start = cleaned.indexOf('{');
        int end   = cleaned.lastIndexOf('}');
        return (start >= 0 && end > start) ? cleaned.substring(start, end + 1) : cleaned;
    }

    // ── Policy helpers ────────────────────────────────────────────────────────

    private AgentDecision.ImpactLevel determineImpactLevel(int change, int currentConcurrency) {
        double pct = (double) change / currentConcurrency * 100;
        if (pct >= impactCriticalChangePct) return AgentDecision.ImpactLevel.CRITICAL;
        if (pct >= impactHighChangePct)     return AgentDecision.ImpactLevel.HIGH;
        if (pct >= impactMediumChangePct)   return AgentDecision.ImpactLevel.MEDIUM;
        return AgentDecision.ImpactLevel.LOW;
    }

    private double calculateConfidence(int change, int currentConcurrency) {
        double pct = (double) change / currentConcurrency * 100;
        if (pct < impactMediumChangePct)  return confidenceLowChange;
        if (pct < impactHighChangePct)    return confidenceMediumChange;
        if (pct < impactCriticalChangePct) return confidenceLargeChange;
        return confidenceVeryLargeChange;
    }

    // ── Fallback ──────────────────────────────────────────────────────────────

    /**
     * Returns a safe hold-current decision when the LLM is unavailable.
     *
     * @param currentConcurrency current concurrency to hold
     * @param errorMessage       error message from the caught exception
     * @param currentHeapMb      heap size to echo back (null if not applicable)
     */
    private AgentDecision createFallbackDecision(int currentConcurrency,
                                                  String errorMessage,
                                                  Integer currentHeapMb) {
        return AgentDecision.builder()
                .decisionId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .resourceId("concurrency-pool")
                .resourceType("ExecutorService")
                .strategy(AgentStrategy.BALANCED)
                .recommendation(String.format("Maintain concurrency at %d (LLM unavailable)",
                        currentConcurrency))
                .reasoning(String.format("LLM agent encountered error: %s. " +
                        "Maintaining current configuration as safe fallback.", errorMessage))
                .confidenceScore(fallbackConfidence)
                .impactLevel(AgentDecision.ImpactLevel.LOW)
                .recommendedHeapSizeMb(currentHeapMb)
                .actionItems(List.of(
                        "Investigate LLM connectivity issues",
                        "Verify Ollama service is running"))
                .risks(List.of("Operating without AI-powered optimization"))
                .build();
    }

    // ── Artifact writers ──────────────────────────────────────────────────────

    /** Ensures the artifacts directory exists before writing trace files. */
    private Path ensureArtifactsDir() throws IOException {
        Path dir = Paths.get(artifactsDir);
        if (!Files.exists(dir)) Files.createDirectories(dir);
        return dir;
    }

    private void writeReasoningTrace(AgentDecision decision, String prompt,
                                     String llmOutput, int currentConcurrency) {
        try {
            Path tracePath = ensureArtifactsDir().resolve("reasoning_trace_llm.txt");

            StringBuilder sb = new StringBuilder();
            sb.append("=".repeat(60)).append("\n");
            sb.append("LLM Agent Decision Trace\n");
            sb.append("=".repeat(60)).append("\n");
            sb.append("Timestamp:    ").append(decision.getTimestamp()).append("\n");
            sb.append("Decision ID:  ").append(decision.getDecisionId()).append("\n\n");

            sb.append("--- Input Parameters ---\n");
            sb.append(String.format("Current Concurrency: %d%n", currentConcurrency));
            sb.append(String.format("Target Latency:      %.2f ms%n", targetLatencyMs));
            sb.append("\n");

            sb.append("--- LLM Prompt ---\n");
            sb.append(prompt).append("\n\n");

            sb.append("--- LLM Response ---\n");
            sb.append(llmOutput).append("\n\n");

            sb.append("--- Parsed Decision ---\n");
            sb.append(String.format("Recommendation: %s%n", decision.getRecommendation()));
            sb.append(String.format("Confidence:     %.0f%%%n", decision.getConfidenceScore() * 100));
            sb.append(String.format("Impact Level:   %s%n", decision.getImpactLevel()));
            sb.append("\n");

            sb.append("--- Reasoning ---\n");
            sb.append(decision.getReasoning()).append("\n\n");

            sb.append("--- Action Items ---\n");
            List<String> items = decision.getActionItems();
            for (int i = 0; i < items.size(); i++) {
                sb.append(String.format("%d. %s%n", i + 1, items.get(i)));
            }
            sb.append("\n");

            sb.append("--- Risks ---\n");
            List<String> risks = decision.getRisks();
            for (int i = 0; i < risks.size(); i++) {
                sb.append(String.format("%d. %s%n", i + 1, risks.get(i)));
            }
            sb.append("\n").append("=".repeat(60)).append("\n\n");

            Files.writeString(tracePath, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.info("LLM reasoning trace written to {}", tracePath);

        } catch (IOException e) {
            log.error("Failed to write LLM reasoning trace", e);
        }
    }

    private void writeHeapReasoningTrace(AgentDecision decision, String prompt,
                                         String llmOutput, RunResult baselineResult) {
        try {
            Path tracePath = ensureArtifactsDir().resolve("reasoning_trace_llm.txt");

            StringBuilder sb = new StringBuilder();
            sb.append("=".repeat(60)).append("\n");
            sb.append("LLM Reasoning Trace (with Heap Analysis)\n");
            sb.append("Timestamp: ").append(Instant.now()).append("\n");
            sb.append("=".repeat(60)).append("\n\n");

            sb.append("--- Baseline Metrics ---\n");
            sb.append(String.format("Median Latency:  %.2f ms%n", baselineResult.getMedianLatencyMs()));
            sb.append(String.format("P95 Latency:     %.2f ms%n", baselineResult.getP95LatencyMs()));
            sb.append(String.format("Throughput:      %.2f req/s%n", baselineResult.getRequestsPerSecond()));

            HeapMetrics heap = baselineResult.getHeapMetrics();
            if (heap != null) {
                sb.append(String.format("Heap Usage:      %.1f%% (%d/%d MB)%n",
                        heap.getHeapUsagePercent(), heap.getHeapUsedMb(), heap.getHeapSizeMb()));
                sb.append(String.format("GC Frequency:    %.2f/sec%n", heap.getGcFrequencyPerSec()));
                sb.append(String.format("GC Pause Avg:    %.2f ms%n", heap.getGcPauseTimeAvgMs()));
            }
            sb.append("\n");

            sb.append("--- LLM Prompt ---\n");
            sb.append(prompt).append("\n\n");

            sb.append("--- LLM Response ---\n");
            sb.append(llmOutput).append("\n\n");

            sb.append("--- Parsed Decision ---\n");
            sb.append(String.format("Recommendation:    %s%n", decision.getRecommendation()));
            sb.append(String.format("Heap Recommended:  %s MB%n", decision.getRecommendedHeapSizeMb()));
            sb.append(String.format("Confidence:        %.0f%%%n", decision.getConfidenceScore() * 100));
            sb.append("\n").append("=".repeat(60)).append("\n\n");

            Files.writeString(tracePath, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.info("LLM heap reasoning trace written to {}", tracePath);

        } catch (IOException e) {
            log.error("Failed to write LLM heap reasoning trace", e);
        }
    }
}