package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.model.AgentDecision;
import com.cloudoptimizer.agent.model.AgentStrategy;
import com.cloudoptimizer.agent.model.RunResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Parser for LLM JSON responses.
 * 
 * Handles extraction and parsing of JSON from LLM outputs, including
 * markdown code blocks and various response formats.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since January 2026
 */
@Component
public class LlmResponseParser {

    private static final Logger log = LoggerFactory.getLogger(LlmResponseParser.class);
    private final ObjectMapper objectMapper;

    public LlmResponseParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Parses LLM JSON response into AgentDecision.
     * Expected format: {"newConcurrency":8,"expectedLatencyMs":78.3,"explanation":"..."}
     */
    public AgentDecision parseJsonResponse(String llmOutput, int currentConcurrency, double targetLatencyMs) {
        try {
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
     * Parses LLM JSON response with heap recommendations into AgentDecision.
     */
    public AgentDecision parseJsonResponseWithHeap(String llmOutput, int currentConcurrency, RunResult baselineResult) {
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
                    .decisionId(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
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
     * Extracts JSON from LLM output (handles markdown code blocks).
     */
    public String extractJson(String llmOutput) {
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
    public int extractNewConcurrency(String llmOutput) {
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
}
