package com.cloudoptimizer.agent.controller;

import com.cloudoptimizer.agent.model.MetricRow;
import com.cloudoptimizer.agent.service.MetricsLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST controller for cloud resource optimization workloads.
 * 
 * Provides endpoints for metric ingestion, batch processing,
 * and system health monitoring for enterprise cloud optimization operations.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since 2025
 */
@RestController
@RequestMapping("/v1/work")
public class WorkController {

    private static final Logger log = LoggerFactory.getLogger(WorkController.class);
    
    // Response field constants
    private static final String STATUS = "status";
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";
    private static final String TIMESTAMP = "timestamp";
    private static final String ACCEPTED = "accepted";

    private final MetricsLogger metricsLogger;

    @Autowired
    public WorkController(MetricsLogger metricsLogger) {
        this.metricsLogger = metricsLogger;
    }

    /**
     * Health check endpoint for load balancer and monitoring systems.
     * 
     * @return health status response
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                STATUS, "UP",
                TIMESTAMP, Instant.now().toString(),
                "service", "agent-cloud-optimizer",
                "metricsDirectory", metricsLogger.getMetricsDirectory().toString()
        ));
    }

    /**
     * Ingests a single metric data point.
     * 
     * @param metricRow the metric data to ingest
     * @return accepted response with metric ID
     */
    @PostMapping("/metrics")
    public ResponseEntity<Map<String, Object>> ingestMetric(@RequestBody MetricRow metricRow) {
        log.info("Received metric: {} for resource: {}", 
                metricRow.getMetricName(), metricRow.getResourceId());

        try {
            metricsLogger.logMetric(metricRow);
            
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                    STATUS, ACCEPTED,
                    "resourceId", metricRow.getResourceId(),
                    "metricName", metricRow.getMetricName(),
                    TIMESTAMP, metricRow.getTimestamp().toString()
            ));
        } catch (Exception e) {
            log.error("Failed to ingest metric", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    STATUS, ERROR,
                    MESSAGE, "Failed to ingest metric: " + e.getMessage()
            ));
        }
    }

    /**
     * Ingests multiple metrics in batch for high-throughput scenarios.
     * 
     * @param metrics list of metric data points
     * @return accepted response with batch statistics
     */
    @PostMapping("/metrics/batch")
    public ResponseEntity<Map<String, Object>> ingestMetricsBatch(@RequestBody List<MetricRow> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    STATUS, ERROR,
                    MESSAGE, "Metrics list cannot be empty"
            ));
        }

        log.info("Received batch of {} metrics", metrics.size());

        try {
            metricsLogger.logMetricsBatch(metrics);
            
            Map<String, Long> resourceTypeCounts = metrics.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            MetricRow::getResourceType,
                            java.util.stream.Collectors.counting()
                    ));

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                    STATUS, ACCEPTED,
                    "totalMetrics", metrics.size(),
                    "resourceTypes", resourceTypeCounts,
                    TIMESTAMP, Instant.now().toString()
            ));
        } catch (Exception e) {
            log.error("Failed to ingest metrics batch", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    STATUS, ERROR,
                    MESSAGE, "Failed to ingest metrics batch: " + e.getMessage()
            ));
        }
    }

    /**
     * Retrieves metrics directory information.
     * 
     * @return metrics storage information
     */
    @GetMapping("/metrics/info")
    public ResponseEntity<Map<String, Object>> getMetricsInfo() {
        try {
            return ResponseEntity.ok(Map.of(
                    "metricsDirectory", metricsLogger.getMetricsDirectory().toString(),
                    TIMESTAMP, Instant.now().toString()
            ));
        } catch (Exception e) {
            log.error("Failed to retrieve metrics info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    STATUS, ERROR,
                    MESSAGE, "Failed to retrieve metrics info: " + e.getMessage()
            ));
        }
    }

    /**
     * Triggers cleanup of old metric files.
     * 
     * @param retentionDays number of days to retain metrics (default: 30)
     * @return cleanup statistics
     */
    @DeleteMapping("/metrics/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupOldMetrics(
            @RequestParam(defaultValue = "30") int retentionDays) {
        
        if (retentionDays <= 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    STATUS, ERROR,
                    MESSAGE, "Retention days must be positive"
            ));
        }

        log.info("Triggering cleanup of metrics older than {} days", retentionDays);

        try {
            int deletedCount = metricsLogger.cleanupOldMetrics(retentionDays);
            
            return ResponseEntity.ok(Map.of(
                    STATUS, "success",
                    "deletedFiles", deletedCount,
                    "retentionDays", retentionDays,
                    TIMESTAMP, Instant.now().toString()
            ));
        } catch (Exception e) {
            log.error("Failed to cleanup old metrics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    STATUS, ERROR,
                    MESSAGE, "Failed to cleanup metrics: " + e.getMessage()
            ));
        }
    }

    /**
     * Exception handler for validation errors.
     * 
     * @param ex the exception
     * @return error response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(IllegalArgumentException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
                STATUS, ERROR,
                MESSAGE, ex.getMessage(),
                TIMESTAMP, Instant.now().toString()
        ));
    }

    /**
     * Exception handler for illegal state errors.
     * 
     * @param ex the exception
     * @return error response
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleStateException(IllegalStateException ex) {
        log.error("State error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                STATUS, ERROR,
                MESSAGE, ex.getMessage(),
                TIMESTAMP, Instant.now().toString()
        ));
    }
}
