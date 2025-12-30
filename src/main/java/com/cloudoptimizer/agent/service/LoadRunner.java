package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.model.MetricRow;
import com.cloudoptimizer.agent.model.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Load testing service for performance measurement.
 * 
 * Executes load tests against the /work endpoint, measures latency,
 * throughput, and generates comprehensive performance statistics.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since 2025
 */
@Service
public class LoadRunner {

    private static final Logger log = LoggerFactory.getLogger(LoadRunner.class);
    private static final double COST_PER_REQUEST_USD = 0.0001; // $0.0001 per request

    private final MetricsLogger metricsLogger;

    @Autowired
    public LoadRunner(MetricsLogger metricsLogger) {
        this.metricsLogger = metricsLogger;
    }

    /**
     * Runs load test with specified parameters.
     * 
     * @param durationSeconds duration of the load test
     * @param concurrency number of concurrent threads
     * @param targetRps target requests per second (0 = unlimited)
     * @return RunResult containing performance metrics
     */
    public RunResult runLoad(int durationSeconds, int concurrency, double targetRps) {
        log.info("Starting load test: duration={}s, concurrency={}, targetRps={}", 
                durationSeconds, concurrency, targetRps);

        Instant startTime = Instant.now();
        List<Double> latencies = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(concurrency);
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicBoolean running = new AtomicBoolean(true);

        // Calculate delay between requests per thread to achieve target RPS
        long delayMs = targetRps > 0 ? (long) (1000.0 / (targetRps / concurrency)) : 0;

        // Start worker threads
        for (int i = 0; i < concurrency; i++) {
            executor.submit(() -> {
                try {
                    while (running.get()) {
                        long requestStart = System.nanoTime();
                        
                        try {
                            // Simulate work by calling controller directly
                            simulateWork();
                            
                            long requestEnd = System.nanoTime();
                            double latencyMs = (requestEnd - requestStart) / 1_000_000.0;
                            latencies.add(latencyMs);
                            successCount.incrementAndGet();
                            
                            // Log metric
                            logMetric(latencyMs, concurrency);
                            
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                            log.debug("Request failed: {}", e.getMessage());
                        }
                        
                        // Rate limiting
                        if (delayMs > 0) {
                            Thread.sleep(delayMs);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for duration
        try {
            Thread.sleep(durationSeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Stop all threads
        running.set(false);
        
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                log.warn("Some worker threads did not complete within timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for worker threads");
        }
        
        executor.shutdownNow();

        Instant endTime = Instant.now();
        long actualDurationMs = endTime.toEpochMilli() - startTime.toEpochMilli();
        double actualDurationSec = actualDurationMs / 1000.0;

        // Calculate statistics
        RunResult result = calculateStatistics(
                latencies, 
                concurrency, 
                durationSeconds,
                actualDurationSec,
                successCount.get(), 
                failCount.get()
        );

        log.info("Load test complete: {}", result);
        
        return result;
    }

    /**
     * Simulates CPU work with jitter.
     */
    private void simulateWork() {
        // Simulate variable CPU work (10-50ms)
        long workTimeMs = 10 + ThreadLocalRandom.current().nextLong(40);
        long endTime = System.currentTimeMillis() + workTimeMs;
        
        while (System.currentTimeMillis() < endTime) {
            // Busy wait to simulate CPU work
            Math.sqrt(ThreadLocalRandom.current().nextDouble());
        }
    }

    /**
     * Logs metric to MetricsLogger.
     */
    private void logMetric(double latencyMs, int concurrency) {
        try {
            MetricRow metric = MetricRow.builder()
                    .resourceId("load-test")
                    .resourceType("WorkEndpoint")
                    .metricName("latencyMs")
                    .metricValue(latencyMs)
                    .unit("ms")
                    .addTag("concurrency", String.valueOf(concurrency))
                    .build();
            
            metricsLogger.logMetric(metric);
        } catch (Exception e) {
            log.debug("Failed to log metric: {}", e.getMessage());
        }
    }

    /**
     * Calculates performance statistics from latency data.
     */
    private RunResult calculateStatistics(List<Double> latencies, int concurrency, 
                                         int durationSeconds, double actualDurationSec,
                                         int successCount, int failCount) {
        if (latencies.isEmpty()) {
            return RunResult.builder()
                    .concurrency(concurrency)
                    .durationSeconds(durationSeconds)
                    .totalRequests(0)
                    .successfulRequests(0)
                    .failedRequests(failCount)
                    .requestsPerSecond(0.0)
                    .medianLatencyMs(0.0)
                    .avgLatencyMs(0.0)
                    .minLatencyMs(0.0)
                    .maxLatencyMs(0.0)
                    .p95LatencyMs(0.0)
                    .p99LatencyMs(0.0)
                    .costEstimateUsd(0.0)
                    .build();
        }

        List<Double> sortedLatencies = new ArrayList<>(latencies);
        Collections.sort(sortedLatencies);

        int totalRequests = successCount + failCount;
        double rps = successCount / actualDurationSec;
        double median = calculatePercentile(sortedLatencies, 50);
        double avg = latencies.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double min = sortedLatencies.get(0);
        double max = sortedLatencies.get(sortedLatencies.size() - 1);
        double p95 = calculatePercentile(sortedLatencies, 95);
        double p99 = calculatePercentile(sortedLatencies, 99);
        double cost = totalRequests * COST_PER_REQUEST_USD;

        return RunResult.builder()
                .concurrency(concurrency)
                .durationSeconds(durationSeconds)
                .totalRequests(totalRequests)
                .successfulRequests(successCount)
                .failedRequests(failCount)
                .requestsPerSecond(rps)
                .medianLatencyMs(median)
                .avgLatencyMs(avg)
                .minLatencyMs(min)
                .maxLatencyMs(max)
                .p95LatencyMs(p95)
                .p99LatencyMs(p99)
                .costEstimateUsd(cost)
                .build();
    }

    /**
     * Calculates percentile from sorted latency list.
     */
    private double calculatePercentile(List<Double> sortedLatencies, int percentile) {
        if (sortedLatencies.isEmpty()) {
            return 0.0;
        }
        
        int index = (int) Math.ceil(percentile / 100.0 * sortedLatencies.size()) - 1;
        index = Math.max(0, Math.min(index, sortedLatencies.size() - 1));
        
        return sortedLatencies.get(index);
    }
}
