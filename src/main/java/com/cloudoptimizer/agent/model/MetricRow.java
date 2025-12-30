package com.cloudoptimizer.agent.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetricRow {

    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private final Instant timestamp;

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

    /**
     * Constructor for Jackson deserialization.
     */
    @JsonCreator
    public MetricRow(
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("resource_id") String resourceId,
            @JsonProperty("resource_type") String resourceType,
            @JsonProperty("metric_name") String metricName,
            @JsonProperty("metric_value") Double metricValue,
            @JsonProperty("unit") String unit,
            @JsonProperty("region") String region,
            @JsonProperty("tags") Map<String, String> tags,
            @JsonProperty("metadata") Map<String, Object> metadata) {
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.metricName = metricName;
        this.metricValue = metricValue;
        this.unit = unit;
        this.region = region;
        this.tags = tags != null ? Map.copyOf(tags) : Map.of();
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    /**
     * Private constructor for builder pattern.
     */
    private MetricRow(Builder builder) {
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.resourceId = builder.resourceId;
        this.resourceType = builder.resourceType;
        this.metricName = builder.metricName;
        this.metricValue = builder.metricValue;
        this.unit = builder.unit;
        this.region = builder.region;
        this.tags = builder.tags != null ? Map.copyOf(builder.tags) : Map.of();
        this.metadata = builder.metadata != null ? Map.copyOf(builder.metadata) : Map.of();
    }

    // Getters

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getMetricName() {
        return metricName;
    }

    public Double getMetricValue() {
        return metricValue;
    }

    public String getUnit() {
        return unit;
    }

    public String getRegion() {
        return region;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
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
     * Builder class for constructing MetricRow instances.
     */
    public static class Builder {
        private Instant timestamp;
        private String resourceId;
        private String resourceType;
        private String metricName;
        private Double metricValue;
        private String unit;
        private String region;
        private Map<String, String> tags;
        private Map<String, Object> metadata;

        private Builder() {
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder resourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder metricName(String metricName) {
            this.metricName = metricName;
            return this;
        }

        public Builder metricValue(Double metricValue) {
            this.metricValue = metricValue;
            return this;
        }

        public Builder unit(String unit) {
            this.unit = unit;
            return this;
        }

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public Builder tags(Map<String, String> tags) {
            this.tags = tags != null ? new HashMap<>(tags) : null;
            return this;
        }

        public Builder addTag(String key, String value) {
            if (this.tags == null) {
                this.tags = new HashMap<>();
            }
            this.tags.put(key, value);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata != null ? new HashMap<>(metadata) : null;
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            if (this.metadata == null) {
                this.metadata = new HashMap<>();
            }
            this.metadata.put(key, value);
            return this;
        }

        /**
         * Builds and validates the MetricRow instance.
         * 
         * @return new MetricRow instance
         * @throws IllegalStateException if required fields are missing
         */
        public MetricRow build() {
            validateRequiredFields();
            return new MetricRow(this);
        }

        private void validateRequiredFields() {
            if (resourceId == null || resourceId.isBlank()) {
                throw new IllegalStateException("resourceId is required");
            }
            if (resourceType == null || resourceType.isBlank()) {
                throw new IllegalStateException("resourceType is required");
            }
            if (metricName == null || metricName.isBlank()) {
                throw new IllegalStateException("metricName is required");
            }
            if (metricValue == null) {
                throw new IllegalStateException("metricValue is required");
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetricRow metricRow = (MetricRow) o;
        return Objects.equals(timestamp, metricRow.timestamp) &&
                Objects.equals(resourceId, metricRow.resourceId) &&
                Objects.equals(metricName, metricRow.metricName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, resourceId, metricName);
    }

    @Override
    public String toString() {
        return "MetricRow{" +
                "timestamp=" + timestamp +
                ", resourceId='" + resourceId + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", metricName='" + metricName + '\'' +
                ", metricValue=" + metricValue +
                ", unit='" + unit + '\'' +
                ", region='" + region + '\'' +
                ", tags=" + tags +
                '}';
    }
}
