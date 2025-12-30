package com.cloudoptimizer.agent.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MetricRow model class.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since 2025
 */
class MetricRowTest {

    @Test
    void testBuilderWithRequiredFields() {
        MetricRow metric = MetricRow.builder()
                .resourceId("i-1234567890abcdef0")
                .resourceType("EC2")
                .metricName("CPUUtilization")
                .metricValue(75.5)
                .build();

        assertNotNull(metric);
        assertEquals("i-1234567890abcdef0", metric.getResourceId());
        assertEquals("EC2", metric.getResourceType());
        assertEquals("CPUUtilization", metric.getMetricName());
        assertEquals(75.5, metric.getMetricValue());
        assertNotNull(metric.getTimestamp());
    }

    @Test
    void testBuilderWithAllFields() {
        Instant now = Instant.now();
        Map<String, String> tags = Map.of("env", "prod", "team", "platform");
        Map<String, Object> metadata = Map.of("instance_type", "t3.large", "az", "us-east-1a");

        MetricRow metric = MetricRow.builder()
                .timestamp(now)
                .resourceId("i-1234567890abcdef0")
                .resourceType("EC2")
                .metricName("CPUUtilization")
                .metricValue(75.5)
                .unit("Percent")
                .region("us-east-1")
                .tags(tags)
                .metadata(metadata)
                .build();

        assertEquals(now, metric.getTimestamp());
        assertEquals("i-1234567890abcdef0", metric.getResourceId());
        assertEquals("EC2", metric.getResourceType());
        assertEquals("CPUUtilization", metric.getMetricName());
        assertEquals(75.5, metric.getMetricValue());
        assertEquals("Percent", metric.getUnit());
        assertEquals("us-east-1", metric.getRegion());
        assertEquals(2, metric.getTags().size());
        assertEquals("prod", metric.getTags().get("env"));
        assertEquals(2, metric.getMetadata().size());
    }

    @Test
    void testBuilderWithAddTag() {
        MetricRow metric = MetricRow.builder()
                .resourceId("i-1234567890abcdef0")
                .resourceType("EC2")
                .metricName("CPUUtilization")
                .metricValue(75.5)
                .addTag("env", "prod")
                .addTag("team", "platform")
                .build();

        assertEquals(2, metric.getTags().size());
        assertEquals("prod", metric.getTags().get("env"));
        assertEquals("platform", metric.getTags().get("team"));
    }

    @Test
    void testBuilderWithAddMetadata() {
        MetricRow metric = MetricRow.builder()
                .resourceId("i-1234567890abcdef0")
                .resourceType("EC2")
                .metricName("CPUUtilization")
                .metricValue(75.5)
                .addMetadata("instance_type", "t3.large")
                .addMetadata("az", "us-east-1a")
                .build();

        assertEquals(2, metric.getMetadata().size());
        assertEquals("t3.large", metric.getMetadata().get("instance_type"));
        assertEquals("us-east-1a", metric.getMetadata().get("az"));
    }

    @Test
    void testBuilderMissingResourceId() {
        assertThrows(IllegalStateException.class, () -> {
            MetricRow.builder()
                    .resourceType("EC2")
                    .metricName("CPUUtilization")
                    .metricValue(75.5)
                    .build();
        });
    }

    @Test
    void testBuilderMissingResourceType() {
        assertThrows(IllegalStateException.class, () -> {
            MetricRow.builder()
                    .resourceId("i-1234567890abcdef0")
                    .metricName("CPUUtilization")
                    .metricValue(75.5)
                    .build();
        });
    }

    @Test
    void testBuilderMissingMetricName() {
        assertThrows(IllegalStateException.class, () -> {
            MetricRow.builder()
                    .resourceId("i-1234567890abcdef0")
                    .resourceType("EC2")
                    .metricValue(75.5)
                    .build();
        });
    }

    @Test
    void testBuilderMissingMetricValue() {
        assertThrows(IllegalStateException.class, () -> {
            MetricRow.builder()
                    .resourceId("i-1234567890abcdef0")
                    .resourceType("EC2")
                    .metricName("CPUUtilization")
                    .build();
        });
    }

    @Test
    void testEqualsAndHashCode() {
        Instant now = Instant.now();
        
        MetricRow metric1 = MetricRow.builder()
                .timestamp(now)
                .resourceId("i-1234567890abcdef0")
                .resourceType("EC2")
                .metricName("CPUUtilization")
                .metricValue(75.5)
                .build();

        MetricRow metric2 = MetricRow.builder()
                .timestamp(now)
                .resourceId("i-1234567890abcdef0")
                .resourceType("EC2")
                .metricName("CPUUtilization")
                .metricValue(75.5)
                .build();

        assertEquals(metric1, metric2);
        assertEquals(metric1.hashCode(), metric2.hashCode());
    }

    @Test
    void testToString() {
        MetricRow metric = MetricRow.builder()
                .resourceId("i-1234567890abcdef0")
                .resourceType("EC2")
                .metricName("CPUUtilization")
                .metricValue(75.5)
                .unit("Percent")
                .region("us-east-1")
                .build();

        String toString = metric.toString();
        assertTrue(toString.contains("i-1234567890abcdef0"));
        assertTrue(toString.contains("EC2"));
        assertTrue(toString.contains("CPUUtilization"));
        assertTrue(toString.contains("75.5"));
    }

    @Test
    void testImmutability() {
        Map<String, String> tags = new java.util.HashMap<>();
        tags.put("env", "prod");

        MetricRow metric = MetricRow.builder()
                .resourceId("i-1234567890abcdef0")
                .resourceType("EC2")
                .metricName("CPUUtilization")
                .metricValue(75.5)
                .tags(tags)
                .build();

        // Modify original map
        tags.put("env", "dev");
        tags.put("new", "value");

        // Metric should still have original values
        assertEquals(1, metric.getTags().size());
        assertEquals("prod", metric.getTags().get("env"));
    }
}
