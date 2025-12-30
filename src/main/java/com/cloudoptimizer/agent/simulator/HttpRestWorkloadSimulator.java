package com.cloudoptimizer.agent.simulator;

import com.cloudoptimizer.agent.model.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HTTP REST API workload simulator.
 * 
 * Tests external HTTP endpoints with configurable:
 * - Base URL
 * - Endpoint path
 * - HTTP method (GET/POST)
 * - Concurrency and duration
 * 
 * Measures latency, throughput, and cost for optimization.
 * 
 * @author Sibasis Padhi
 * @version 0.2.0
 * @since December 2025
 */
@Component("http")
public class HttpRestWorkloadSimulator implements WorkloadSimulator {
    
    private static final Logger log = LoggerFactory.getLogger(HttpRestWorkloadSimulator.class);
    private static final double COST_PER_REQUEST_USD = 0.0001; // $0.0001 per request
    
    @Value("${http.base-url:http://localhost:8080}")
    private String baseUrl;
    
    @Value("${http.endpoint:/api/test}")
    private String endpoint;
    
    @Value("${http.method:GET}")
    private String method;
    
    private final RestTemplate restTemplate;
    
    public HttpRestWorkloadSimulator() {
        this.restTemplate = new RestTemplate();
        log.info("HttpRestWorkloadSimulator initialized");
    }
    
    @Override
    public RunResult executeLoad(int concurrency, int durationSeconds, double targetRps) {
        String fullUrl = baseUrl + endpoint;
        log.info("Starting HTTP load test: url={}, method={}, concurrency={}, duration={}s, targetRps={}",
                fullUrl, method, concurrency, durationSeconds, targetRps);
        
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
                            // Make HTTP request
                            executeHttpRequest(fullUrl);
                            
                            long requestEnd = System.nanoTime();
                            double latencyMs = (requestEnd - requestStart) / 1_000_000.0;
                            latencies.add(latencyMs);
                            successCount.incrementAndGet();
                            
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                            log.debug("HTTP request failed: {}", e.getMessage());
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
        
        // Calculate statistics
        RunResult result = calculateStatistics(
                latencies,
                concurrency,
                durationSeconds,
                successCount.get(),
                failCount.get()
        );
        
        log.info("HTTP load test complete: {}", result);
        
        return result;
    }
    
    /**
     * Executes a single HTTP request.
     */
    private void executeHttpRequest(String url) {
        if ("POST".equalsIgnoreCase(method)) {
            restTemplate.postForEntity(url, null, String.class);
        } else {
            // Default to GET
            restTemplate.getForEntity(url, String.class);
        }
    }
    
    /**
     * Calculates performance statistics from latency data.
     */
    private RunResult calculateStatistics(List<Double> latencies, int concurrency,
                                         int durationSeconds, int successCount, int failCount) {
        if (latencies.isEmpty()) {
            log.warn("No successful requests! All requests failed.");
            return RunResult.builder()
                    .concurrency(concurrency)
                    .durationSeconds(durationSeconds)
                    .totalRequests(failCount)
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
        double rps = successCount / (double) durationSeconds;
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
    
    @Override
    public String getName() {
        return "http";
    }
}
