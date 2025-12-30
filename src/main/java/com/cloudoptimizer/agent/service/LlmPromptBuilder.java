package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.model.MetricRow;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds structured prompts for LLM-based optimization analysis.
 * 
 * Creates detailed prompts containing metric data, current configuration,
 * and instructions for JSON-formatted responses.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since 2025
 */
public class LlmPromptBuilder {

    /**
     * Builds optimization prompt for LLM analysis.
     * 
     * @param metrics recent metric samples
     * @param currentConcurrency current concurrency setting
     * @param targetLatencyMs target latency threshold
     * @return formatted prompt string
     */
    public String buildPrompt(List<MetricRow> metrics, int currentConcurrency, double targetLatencyMs) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert cloud infrastructure optimization assistant.%n");
        prompt.append("%n");
        prompt.append("TASK: Analyze the following performance metrics and recommend an optimal concurrency setting.%n");
        prompt.append("%n");
        
        prompt.append("CURRENT CONFIGURATION:%n");
        prompt.append(String.format("- Current Concurrency: %d threads%n", currentConcurrency));
        prompt.append(String.format("- Target Latency: %.2f ms%n", targetLatencyMs));
        prompt.append("%n");
        
        prompt.append("RECENT METRICS:%n");
        prompt.append(formatMetrics(metrics));
        prompt.append("%n");
        
        prompt.append("ANALYSIS GUIDELINES:%n");
        prompt.append("1. If median latency > target: Consider INCREASING concurrency%n");
        prompt.append("2. If median latency < 70%% of target: Consider DECREASING concurrency%n");
        prompt.append("3. If within acceptable range: MAINTAIN current setting%n");
        prompt.append("4. Balance throughput vs resource efficiency%n");
        prompt.append("5. Avoid extreme changes (max ±50%% adjustment)%n");
        prompt.append("%n");
        
        prompt.append("REQUIRED OUTPUT FORMAT (JSON only, no markdown):%n");
        prompt.append("{%n");
        prompt.append("  \"newConcurrency\": <integer between 1-100>,%n");
        prompt.append("  \"expectedLatencyMs\": <estimated latency after change>,%n");
        prompt.append("  \"explanation\": \"<2-3 sentence reasoning for the decision>\"%n");
        prompt.append("}%n");
        prompt.append("%n");
        prompt.append("Respond with ONLY the JSON object, no additional text.%n");
        
        return String.format(prompt.toString());
    }

    /**
     * Formats metrics data for prompt inclusion.
     */
    private String formatMetrics(List<MetricRow> metrics) {
        if (metrics.isEmpty()) {
            return "No metrics available%n";
        }
        
        // Extract latency values
        List<Double> latencies = metrics.stream()
                .filter(m -> "latencyMs".equals(m.getMetricName()))
                .map(MetricRow::getMetricValue)
                .collect(Collectors.toList());
        
        if (latencies.isEmpty()) {
            return "No latency metrics found%n";
        }
        
        // Calculate statistics
        double min = latencies.stream().min(Double::compare).orElse(0.0);
        double max = latencies.stream().max(Double::compare).orElse(0.0);
        double avg = latencies.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double median = calculateMedian(latencies);
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("- Sample Count: %d%n", latencies.size()));
        sb.append(String.format("- Min Latency: %.2f ms%n", min));
        sb.append(String.format("- Max Latency: %.2f ms%n", max));
        sb.append(String.format("- Avg Latency: %.2f ms%n", avg));
        sb.append(String.format("- Median Latency: %.2f ms%n", median));
        sb.append(String.format("- Recent samples: %s%n", 
                formatRecentSamples(latencies)));
        
        return sb.toString();
    }

    /**
     * Calculates median from list of values.
     */
    private double calculateMedian(List<Double> values) {
        List<Double> sorted = values.stream().sorted().collect(Collectors.toList());
        int size = sorted.size();
        
        if (size == 0) {
            return 0.0;
        }
        
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }

    /**
     * Formats recent samples for display.
     */
    private String formatRecentSamples(List<Double> latencies) {
        int displayCount = Math.min(5, latencies.size());
        List<Double> recent = latencies.subList(
                Math.max(0, latencies.size() - displayCount), 
                latencies.size());
        
        return recent.stream()
                .map(v -> String.format("%.1f", v))
                .collect(Collectors.joining(", ")) + " ms";
    }
}
