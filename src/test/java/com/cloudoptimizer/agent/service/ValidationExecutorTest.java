package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.artifact.ValidationResult;
import com.cloudoptimizer.agent.model.RunResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ValidationExecutor}.
 *
 * <p>Stateless service — no Spring context required.
 */
class ValidationExecutorTest {

    private ValidationExecutor executor;

    @BeforeEach
    void setUp() { executor = new ValidationExecutor(); }

    private static RunResult runResultWithP99(double p99Ms) {
        return RunResult.builder()
                .concurrency(6)
                .p99LatencyMs(p99Ms)
                .medianLatencyMs(p99Ms * 0.5)
                .requestsPerSecond(100)
                .build();
    }

    @Test
    @DisplayName("PASSED when p99 equals threshold exactly")
    void passed_exactlyAtThreshold() {
        ValidationResult r = executor.validate(runResultWithP99(100.0), 100.0);
        assertEquals(ValidationResult.ValidationStatus.PASSED, r.status());
        assertEquals(100.0, r.measuredP99Ms());
    }

    @Test
    @DisplayName("PASSED when p99 is below threshold")
    void passed_belowThreshold() {
        ValidationResult r = executor.validate(runResultWithP99(80.0), 120.0);
        assertEquals(ValidationResult.ValidationStatus.PASSED, r.status());
        assertEquals(80.0, r.measuredP99Ms());
        assertEquals(120.0, r.thresholdMs());
    }

    @Test
    @DisplayName("FAILED when p99 exceeds threshold by 1ms")
    void failed_justAboveThreshold() {
        ValidationResult r = executor.validate(runResultWithP99(100.1), 100.0);
        assertEquals(ValidationResult.ValidationStatus.FAILED, r.status());
    }

    @Test
    @DisplayName("FAILED when p99 greatly exceeds threshold")
    void failed_farAboveThreshold() {
        ValidationResult r = executor.validate(runResultWithP99(200.0), 100.0);
        assertEquals(ValidationResult.ValidationStatus.FAILED, r.status());
        assertEquals(200.0, r.measuredP99Ms());
    }

    @Test
    @DisplayName("evaluatedAt is populated on both PASSED and FAILED results")
    void evaluatedAt_alwaysPopulated() {
        assertNotNull(executor.validate(runResultWithP99(50.0), 100.0).evaluatedAt());
        assertNotNull(executor.validate(runResultWithP99(150.0), 100.0).evaluatedAt());
    }

    @Test
    @DisplayName("reason is non-null and contains p99 info")
    void reason_containsP99Info() {
        String reason = executor.validate(runResultWithP99(80.0), 100.0).reason();
        assertNotNull(reason);
        assertTrue(reason.contains("80") || reason.contains("p99") || reason.contains("threshold"),
                "Expected reason to mention latency or threshold, got: " + reason);
    }
}