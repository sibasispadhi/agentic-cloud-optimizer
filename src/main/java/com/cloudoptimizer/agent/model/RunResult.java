package com.cloudoptimizer.agent.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/**
 * Represents the results of a load test run.
 * 
 * Captures performance metrics including latency statistics,
 * throughput, concurrency settings, and cost estimates.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since 2025
 */
@Getter
@Builder
@ToString
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RunResult {

    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private final Instant timestamp;

    @JsonProperty("concurrency")
    private final int concurrency;

    @JsonProperty("duration_seconds")
    private final int durationSeconds;

    @JsonProperty("total_requests")
    private final int totalRequests;

    @JsonProperty("successful_requests")
    private final int successfulRequests;

    @JsonProperty("failed_requests")
    private final int failedRequests;

    @JsonProperty("requests_per_second")
    private final double requestsPerSecond;

    @JsonProperty("median_latency_ms")
    private final double medianLatencyMs;

    @JsonProperty("avg_latency_ms")
    private final double avgLatencyMs;

    @JsonProperty("min_latency_ms")
    private final double minLatencyMs;

    @JsonProperty("max_latency_ms")
    private final double maxLatencyMs;

    @JsonProperty("p95_latency_ms")
    private final double p95LatencyMs;

    @JsonProperty("p99_latency_ms")
    private final double p99LatencyMs;

    @JsonProperty("cost_estimate_usd")
    private final double costEstimateUsd;

    @JsonProperty("heap_metrics")
    private final HeapMetrics heapMetrics;
}
