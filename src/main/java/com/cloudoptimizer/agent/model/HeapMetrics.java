package com.cloudoptimizer.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents JVM heap and garbage collection metrics.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since January 2026
 */
@Getter
@Builder
@ToString
public class HeapMetrics {
    
    @JsonProperty("heap_size_mb")
    private final long heapSizeMb;
    
    @JsonProperty("heap_used_mb")
    private final long heapUsedMb;
    
    @JsonProperty("heap_usage_percent")
    private final double heapUsagePercent;
    
    @JsonProperty("gc_count")
    private final long gcCount;
    
    @JsonProperty("gc_time_ms")
    private final long gcTimeMs;
    
    @JsonProperty("gc_pause_time_avg_ms")
    private final double gcPauseTimeAvgMs;
    
    @JsonProperty("gc_frequency_per_sec")
    private final double gcFrequencyPerSec;
}
