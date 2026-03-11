package com.cloudoptimizer.agent.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a single metric data point for cloud resource monitoring.
 * 
 * This immutable data structure captures time-series metrics from cloud
 * infrastructure including resource utilization, performance indicators,
 * and cost data.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since 2025
 */
@Getter
@Builder
@Jacksonized  // Enables Jackson to use Lombok builder with defaults
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetricRow {

    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Builder.Default
    private final Instant timestamp = Instant.now();

    @JsonProperty("resource_id")
    @Builder.Default
    private final String resourceId = "unknown";

    @JsonProperty("resource_type")
    @Builder.Default
    private final String resourceType = "unknown";

    @JsonProperty("metric_name")
    @Builder.Default
    private final String metricName = "unnamed";

    @JsonProperty("metric_value")
    private final Double metricValue;

    @JsonProperty("unit")
    private final String unit;

    @JsonProperty("region")
    private final String region;

    @JsonProperty("tags")
    private final Map<String, String> tags;

    @JsonProperty("metadata")
    private final Map<String, Object> metadata;
}
