package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.model.HeapMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * Collects JVM heap and garbage collection metrics.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since January 2026
 */
@Service
public class GcMetricsCollector {
    
    private static final Logger log = LoggerFactory.getLogger(GcMetricsCollector.class);
    private static final long MB = 1024L * 1024L;
    
    private long previousGcCount = 0;
    private long previousGcTime = 0;
    
    /**
     * Collects current heap and GC metrics.
     * 
     * @param durationSeconds duration of the measurement period
     * @return HeapMetrics containing current metrics
     */
    public HeapMetrics collectMetrics(double durationSeconds) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        
        long heapSizeMb = heapUsage.getMax() / MB;
        long heapUsedMb = heapUsage.getUsed() / MB;
        double heapUsagePercent = (heapUsedMb * 100.0) / heapSizeMb;
        
        // Collect GC metrics from all garbage collectors
        long totalGcCount = 0;
        long totalGcTime = 0;
        
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = gcBean.getCollectionCount();
            long time = gcBean.getCollectionTime();
            
            if (count > 0) {
                totalGcCount += count;
                totalGcTime += time;
            }
        }
        
        // Calculate GC frequency and average pause time
        long gcCountDelta = totalGcCount - previousGcCount;
        long gcTimeDelta = totalGcTime - previousGcTime;
        
        double gcFrequencyPerSec = durationSeconds > 0 ? gcCountDelta / durationSeconds : 0;
        double gcPauseTimeAvgMs = gcCountDelta > 0 ? (double) gcTimeDelta / gcCountDelta : 0;
        
        // Update previous values for next collection
        previousGcCount = totalGcCount;
        previousGcTime = totalGcTime;
        
        HeapMetrics metrics = HeapMetrics.builder()
                .heapSizeMb(heapSizeMb)
                .heapUsedMb(heapUsedMb)
                .heapUsagePercent(heapUsagePercent)
                .gcCount(gcCountDelta)
                .gcTimeMs(gcTimeDelta)
                .gcPauseTimeAvgMs(gcPauseTimeAvgMs)
                .gcFrequencyPerSec(gcFrequencyPerSec)
                .build();
        
        log.debug("Collected heap metrics: {}", metrics);
        
        return metrics;
    }
    
    /**
     * Resets the collector state for a new measurement period.
     */
    public void reset() {
        // Collect current values to establish baseline
        long totalGcCount = 0;
        long totalGcTime = 0;
        
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            totalGcCount += gcBean.getCollectionCount();
            totalGcTime += gcBean.getCollectionTime();
        }
        
        previousGcCount = totalGcCount;
        previousGcTime = totalGcTime;
        
        log.debug("Reset GC metrics collector");
    }
}
