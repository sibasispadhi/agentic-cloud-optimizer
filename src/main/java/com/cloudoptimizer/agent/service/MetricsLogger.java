package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.model.MetricRow;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Enterprise-grade metrics logging service for cloud resource monitoring.
 * 
 * Provides high-performance, thread-safe logging of metric data to disk
 * with daily rotation, JSON formatting, and async processing capabilities.
 * Implements buffering and batching for optimal I/O performance.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since 2025
 */
@Service
public class MetricsLogger {

    private static final Logger log = LoggerFactory.getLogger(MetricsLogger.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final ObjectMapper objectMapper;
    private final Path metricsDirectory;
    private final ConcurrentHashMap<String, ReentrantLock> fileLocks;

    /**
     * Constructs a new MetricsLogger with Spring-injected configuration.
     * 
     * @param metricsDirectory metrics directory path from configuration
     * @throws IOException if metrics directory cannot be created
     */
    @Autowired
    public MetricsLogger(@Value("${metrics.directory:data/metrics}") String metricsDirectory) throws IOException {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.INDENT_OUTPUT); // Compact JSON for JSONL
        
        this.metricsDirectory = Paths.get(metricsDirectory);
        this.fileLocks = new ConcurrentHashMap<>();
        
        initializeMetricsDirectory();
    }

    /**
     * Constructs a MetricsLogger with custom metrics directory (for testing).
     * 
     * @param metricsDirectory custom directory path for metrics storage
     * @param forTesting marker parameter to distinguish from Spring constructor
     * @throws IOException if metrics directory cannot be created
     */
    public MetricsLogger(String metricsDirectory, boolean forTesting) throws IOException {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.INDENT_OUTPUT); // Compact JSON for JSONL format
        
        this.metricsDirectory = Paths.get(metricsDirectory);
        this.fileLocks = new ConcurrentHashMap<>();
        
        initializeMetricsDirectory();
    }

    /**
     * Initializes the metrics directory structure.
     * 
     * @throws IOException if directory creation fails
     */
    private void initializeMetricsDirectory() throws IOException {
        if (!Files.exists(metricsDirectory)) {
            Files.createDirectories(metricsDirectory);
            log.info("Created metrics directory: {}", metricsDirectory);
        }
    }

    /**
     * Logs a single metric row synchronously.
     * 
     * @param metric the metric row to log
     * @return CompletableFuture that completes when logging is done
     */
    public CompletableFuture<Void> logMetric(MetricRow metric) {
        return CompletableFuture.runAsync(() -> {
            try {
                String jsonLine = objectMapper.writeValueAsString(metric);
                writeToFile(metric.getResourceType(), jsonLine);
                log.debug("Logged metric: {} for resource: {}", 
                        metric.getMetricName(), metric.getResourceId());
            } catch (Exception e) {
                log.error("Failed to log metric: {}", metric, e);
                throw new MetricsLoggingException("Metric logging failed", e);
            }
        });
    }

    /**
     * Logs multiple metric rows in batch.
     * 
     * @param metrics list of metric rows to log
     * @return CompletableFuture that completes when all metrics are logged
     */
    public CompletableFuture<Void> logMetricsBatch(List<MetricRow> metrics) {
        return CompletableFuture.runAsync(() -> {
            if (metrics == null || metrics.isEmpty()) {
                log.warn("Attempted to log empty metrics batch");
                return;
            }

            try {
                // Group metrics by resource type for efficient file I/O
                metrics.stream()
                        .collect(java.util.stream.Collectors.groupingBy(MetricRow::getResourceType))
                        .forEach((resourceType, metricList) -> {
                            try {
                                StringBuilder batchContent = new StringBuilder();
                                for (MetricRow metric : metricList) {
                                    String jsonLine = objectMapper.writeValueAsString(metric);
                                    batchContent.append(jsonLine).append(System.lineSeparator());
                                }
                                writeToFile(resourceType, batchContent.toString().trim());
                                log.info("Logged batch of {} metrics for resource type: {}", 
                                        metricList.size(), resourceType);
                            } catch (Exception e) {
                                log.error("Failed to log metrics batch for resource type: {}", 
                                        resourceType, e);
                                throw new MetricsLoggingException(
                                        "Failed to log metrics batch for resource type: " + resourceType, e);
                            }
                        });
            } catch (Exception e) {
                log.error("Failed to process metrics batch", e);
                throw new MetricsLoggingException("Batch metric logging failed", e);
            }
        });
    }

    /**
     * Writes metric data to file with thread-safe locking.
     * 
     * @param resourceType the resource type for file naming
     * @param content the content to write
     * @throws IOException if file write fails
     */
    private void writeToFile(String resourceType, String content) throws IOException {
        String fileName = generateFileName(resourceType);
        Path filePath = metricsDirectory.resolve(fileName);
        
        // Get or create lock for this file
        ReentrantLock lock = fileLocks.computeIfAbsent(fileName, k -> new ReentrantLock());
        
        lock.lock();
        try {
            Files.writeString(filePath, 
                    content + System.lineSeparator(), 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.APPEND);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Generates a date-based file name for metric storage.
     * 
     * @param resourceType the resource type
     * @return formatted file name with date
     */
    private String generateFileName(String resourceType) {
        String date = LocalDate.now().format(DATE_FORMATTER);
        String sanitizedType = resourceType.toLowerCase().replaceAll("[^a-z0-9-]", "_");
        return String.format("metrics_%s_%s.jsonl", sanitizedType, date);
    }

    /**
     * Retrieves the metrics directory path.
     * 
     * @return Path to metrics directory
     */
    public Path getMetricsDirectory() {
        return metricsDirectory;
    }

    /**
     * Reads the most recent N metric rows from log files.
     * 
     * @param count number of recent metrics to read
     * @return list of recent MetricRow objects
     */
    public List<MetricRow> readRecent(int count) {
        List<MetricRow> metrics = new java.util.ArrayList<>();
        
        try (var stream = Files.list(metricsDirectory)) {
            // Get all metric files sorted by modification time (newest first)
            List<Path> files = stream
                    .filter(path -> path.getFileName().toString().startsWith("metrics_"))
                    .filter(path -> path.getFileName().toString().endsWith(".jsonl"))
                    .sorted((p1, p2) -> {
                        try {
                            return Files.getLastModifiedTime(p2)
                                    .compareTo(Files.getLastModifiedTime(p1));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .toList();
            
            // Read lines from files until we have enough metrics
            for (Path file : files) {
                if (metrics.size() >= count) {
                    break;
                }
                
                try {
                    List<String> lines = Files.readAllLines(file);
                    // Read from end of file (most recent)
                    for (int i = lines.size() - 1; i >= 0 && metrics.size() < count; i--) {
                        String line = lines.get(i).trim();
                        if (line.isEmpty()) {
                            continue;
                        }
                        
                        try {
                            MetricRow metric = objectMapper.readValue(line, MetricRow.class);
                            metrics.add(metric);
                        } catch (Exception e) {
                            log.warn("Failed to parse metric line: {}", e.getMessage());
                        }
                    }
                } catch (IOException e) {
                    log.warn("Failed to read file {}: {}", file, e.getMessage());
                }
            }
            
            log.debug("Read {} recent metrics", metrics.size());
            
        } catch (IOException e) {
            log.error("Failed to read recent metrics", e);
        }
        
        return metrics;
    }

    /**
     * Cleans up old metric files beyond retention period.
     * 
     * @param retentionDays number of days to retain metrics
     * @return number of files deleted
     */
    public int cleanupOldMetrics(int retentionDays) {
        if (retentionDays <= 0) {
            throw new IllegalArgumentException("Retention days must be positive");
        }

        int deletedCount = 0;
        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
        
        try (var stream = Files.list(metricsDirectory)) {
            var filesToDelete = stream
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        if (!fileName.startsWith("metrics_") || !fileName.endsWith(".jsonl")) {
                            return false;
                        }
                        
                        try {
                            // Extract date from filename
                            String datePart = fileName.substring(fileName.lastIndexOf("_") + 1, 
                                    fileName.lastIndexOf("."));
                            LocalDate fileDate = LocalDate.parse(datePart, DATE_FORMATTER);
                            return fileDate.isBefore(cutoffDate);
                        } catch (Exception e) {
                            log.warn("Could not parse date from filename: {}", fileName);
                            return false;
                        }
                    })
                    .toList();
            
            for (Path path : filesToDelete) {
                try {
                    Files.delete(path);
                    log.info("Deleted old metric file: {}", path.getFileName());
                    deletedCount++;
                } catch (IOException e) {
                    log.error("Failed to delete file: {}", path, e);
                }
            }
            
            log.info("Cleaned up {} old metric files (retention: {} days)", 
                    deletedCount, retentionDays);
        } catch (IOException e) {
            log.error("Failed to cleanup old metrics", e);
        }
        
        return deletedCount;
    }
}
