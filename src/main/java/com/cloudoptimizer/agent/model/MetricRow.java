package com.cloudoptimizer.agent.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

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
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetricRow {

    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Builder.Default
    private final Instant timestamp = Instant.now();

    @JsonProperty("resource_id")
    private final String resourceId;

    @JsonProperty("resource_type")
    private final String resourceType;

    @JsonProperty("metric_name")
    private final String metricName;

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
