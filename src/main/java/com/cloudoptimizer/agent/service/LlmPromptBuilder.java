package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.model.HeapMetrics;
import com.cloudoptimizer.agent.model.MetricRow;
import com.cloudoptimizer.agent.model.RunResult;

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
     * Builds optimization prompt including heap metrics analysis.
     * 
     * @param baselineResult baseline test results with heap metrics
     * @param currentConcurrency current concurrency setting
     * @param targetLatencyMs target latency threshold
     * @return formatted prompt string
     */
    public String buildPromptWithHeapMetrics(RunResult baselineResult, int currentConcurrency, double targetLatencyMs) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert cloud infrastructure optimization assistant specializing in JVM performance tuning.\n");
        prompt.append("\n");
        prompt.append("TASK: Analyze performance and heap metrics, then recommend optimal concurrency AND heap size settings.\n");
        prompt.append("\n");
        
        prompt.append("CURRENT CONFIGURATION:\n");
        prompt.append(String.format("- Current Concurrency: %d threads\n", currentConcurrency));
        prompt.append(String.format("- Target Latency: %.2f ms\n", targetLatencyMs));
        
        HeapMetrics heapMetrics = baselineResult.getHeapMetrics();
        if (heapMetrics != null) {
            prompt.append(String.format("- Current Heap Size: %d MB\n", heapMetrics.getHeapSizeMb()));
        }
        prompt.append("\n");
        
        prompt.append("PERFORMANCE METRICS:\n");
        prompt.append(String.format("- Median Latency: %.2f ms\n", baselineResult.getMedianLatencyMs()));
        prompt.append(String.format("- P95 Latency: %.2f ms\n", baselineResult.getP95LatencyMs()));
        prompt.append(String.format("- Avg Latency: %.2f ms\n", baselineResult.getAvgLatencyMs()));
        prompt.append(String.format("- Throughput: %.2f req/s\n", baselineResult.getRequestsPerSecond()));
        prompt.append("\n");
        
        if (heapMetrics != null) {
            prompt.append("HEAP & GC METRICS:\n");
            prompt.append(String.format("- Heap Usage: %.1f%% (%d MB used of %d MB)\n", 
                    heapMetrics.getHeapUsagePercent(), 
                    heapMetrics.getHeapUsedMb(), 
                    heapMetrics.getHeapSizeMb()));
            prompt.append(String.format("- GC Frequency: %.2f collections/second\n", heapMetrics.getGcFrequencyPerSec()));
            prompt.append(String.format("- GC Pause Time (avg): %.2f ms\n", heapMetrics.getGcPauseTimeAvgMs()));
            prompt.append(String.format("- Total GC Time: %d ms during test\n", heapMetrics.getGcTimeMs()));
            prompt.append("\n");
        }
        
        // Calculate latency status for explicit guidance
        double latencyPercent = (baselineResult.getMedianLatencyMs() / targetLatencyMs) * 100;
        
        prompt.append("LATENCY ANALYSIS:\n");
        prompt.append(String.format("- Current median latency: %.2f ms\n", baselineResult.getMedianLatencyMs()));
        prompt.append(String.format("- Target latency: %.2f ms\n", targetLatencyMs));
        prompt.append(String.format("- Current is %.1f%% of target\n", latencyPercent));
        if (latencyPercent > 100) {
            prompt.append(String.format("- STATUS: EXCEEDS TARGET by %.1f%% - NEEDS IMPROVEMENT\n", latencyPercent - 100));
        } else if (latencyPercent >= 90) {
            prompt.append("- STATUS: AT TARGET - maintain current settings\n");
        } else {
            prompt.append("- STATUS: BELOW TARGET - already meeting goal\n");
        }
        prompt.append("\n");
        
        prompt.append("OPTIMIZATION GOAL: Ensure latency stays at or below target while maintaining system stability.\n");
        prompt.append("\n");
        
        prompt.append("DECISION GUIDELINES:\n");
        prompt.append("CONCURRENCY:\n");
        prompt.append("- If STATUS is EXCEEDS TARGET: Increase threads by 2-4 to reduce latency\n");
        prompt.append("- If STATUS is AT TARGET: Maintain current threads\n");
        prompt.append("- If STATUS is BELOW TARGET: Keep current threads (already meeting goal)\n");
        prompt.append("\n");
        prompt.append("HEAP:\n");
        prompt.append("- If GC pressure exists (freq > 1/sec OR usage > 80%): Increase heap to reduce GC overhead\n");
        prompt.append("- If GC pause time > 100ms: Increase heap to improve performance\n");
        prompt.append("- If GC is healthy (freq < 0.5/sec, usage < 50%): Keep current heap size\n");
        prompt.append("\n");
        
        prompt.append("REQUIRED OUTPUT FORMAT (JSON only, no markdown):\n");
        prompt.append("{\n");
        prompt.append("  \"newConcurrency\": <integer between 1-100>,\n");
        prompt.append("  \"recommendedHeapSizeMb\": <integer heap size in MB>,\n");
        prompt.append("  \"expectedLatencyMs\": <estimated latency after changes>,\n");
        prompt.append("  \"concurrencyReasoning\": \"<2-3 sentences explaining why this concurrency level is optimal>\",\n");
        prompt.append("  \"heapReasoning\": \"<2-3 sentences explaining why this heap size is optimal>\"\n");
        prompt.append("}\n");
        prompt.append("\n");
        prompt.append("Respond with ONLY the JSON object, no additional text.\n");
        
        return prompt.toString();
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
