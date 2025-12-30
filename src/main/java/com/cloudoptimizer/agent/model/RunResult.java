package com.cloudoptimizer.agent.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

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

    /**
     * Private constructor for builder pattern.
     */
    private RunResult(Builder builder) {
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.concurrency = builder.concurrency;
        this.durationSeconds = builder.durationSeconds;
        this.totalRequests = builder.totalRequests;
        this.successfulRequests = builder.successfulRequests;
        this.failedRequests = builder.failedRequests;
        this.requestsPerSecond = builder.requestsPerSecond;
        this.medianLatencyMs = builder.medianLatencyMs;
        this.avgLatencyMs = builder.avgLatencyMs;
        this.minLatencyMs = builder.minLatencyMs;
        this.maxLatencyMs = builder.maxLatencyMs;
        this.p95LatencyMs = builder.p95LatencyMs;
        this.p99LatencyMs = builder.p99LatencyMs;
        this.costEstimateUsd = builder.costEstimateUsd;
    }

    // Getters

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public int getSuccessfulRequests() {
        return successfulRequests;
    }

    public int getFailedRequests() {
        return failedRequests;
    }

    public double getRequestsPerSecond() {
        return requestsPerSecond;
    }

    public double getMedianLatencyMs() {
        return medianLatencyMs;
    }

    public double getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public double getMinLatencyMs() {
        return minLatencyMs;
    }

    public double getMaxLatencyMs() {
        return maxLatencyMs;
    }

    public double getP95LatencyMs() {
        return p95LatencyMs;
    }

    public double getP99LatencyMs() {
        return p99LatencyMs;
    }

    public double getCostEstimateUsd() {
        return costEstimateUsd;
    }

    /**
     * Creates a new builder instance.
     * 
     * @return new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing RunResult instances.
     */
    public static class Builder {
        private Instant timestamp;
        private int concurrency;
        private int durationSeconds;
        private int totalRequests;
        private int successfulRequests;
        private int failedRequests;
        private double requestsPerSecond;
        private double medianLatencyMs;
        private double avgLatencyMs;
        private double minLatencyMs;
        private double maxLatencyMs;
        private double p95LatencyMs;
        private double p99LatencyMs;
        private double costEstimateUsd;

        private Builder() {
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder concurrency(int concurrency) {
            this.concurrency = concurrency;
            return this;
        }

        public Builder durationSeconds(int durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }

        public Builder totalRequests(int totalRequests) {
            this.totalRequests = totalRequests;
            return this;
        }

        public Builder successfulRequests(int successfulRequests) {
            this.successfulRequests = successfulRequests;
            return this;
        }

        public Builder failedRequests(int failedRequests) {
            this.failedRequests = failedRequests;
            return this;
        }

        public Builder requestsPerSecond(double requestsPerSecond) {
            this.requestsPerSecond = requestsPerSecond;
            return this;
        }

        public Builder medianLatencyMs(double medianLatencyMs) {
            this.medianLatencyMs = medianLatencyMs;
            return this;
        }

        public Builder avgLatencyMs(double avgLatencyMs) {
            this.avgLatencyMs = avgLatencyMs;
            return this;
        }

        public Builder minLatencyMs(double minLatencyMs) {
            this.minLatencyMs = minLatencyMs;
            return this;
        }

        public Builder maxLatencyMs(double maxLatencyMs) {
            this.maxLatencyMs = maxLatencyMs;
            return this;
        }

        public Builder p95LatencyMs(double p95LatencyMs) {
            this.p95LatencyMs = p95LatencyMs;
            return this;
        }

        public Builder p99LatencyMs(double p99LatencyMs) {
            this.p99LatencyMs = p99LatencyMs;
            return this;
        }

        public Builder costEstimateUsd(double costEstimateUsd) {
            this.costEstimateUsd = costEstimateUsd;
            return this;
        }

        public RunResult build() {
            return new RunResult(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RunResult runResult = (RunResult) o;
        return Objects.equals(timestamp, runResult.timestamp) &&
                concurrency == runResult.concurrency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, concurrency);
    }

    @Override
    public String toString() {
        return String.format("RunResult{concurrency=%d, rps=%.2f, medianLatency=%.2fms, " +
                        "avgLatency=%.2fms, totalRequests=%d, cost=$%.4f}",
                concurrency, requestsPerSecond, medianLatencyMs, avgLatencyMs, 
                totalRequests, costEstimateUsd);
    }
}
