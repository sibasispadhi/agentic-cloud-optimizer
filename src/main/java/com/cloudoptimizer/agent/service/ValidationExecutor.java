package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.artifact.ValidationResult;
import com.cloudoptimizer.agent.model.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Executes the post-optimization validation check.
 *
 * <h2>Responsibility</h2>
 * <p>Given the after-test {@link RunResult} and the SLO p99 threshold,
 * determine whether the optimization improved (or maintained) latency
 * within the configured bounds.
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li>Stateless — safe to inject as a singleton.</li>
 *   <li>No simulator calls — consumes an already-measured result.</li>
 *   <li>Deterministic — same inputs always produce the same outcome.</li>
 * </ul>
 */
@Service
public class ValidationExecutor {

    private static final Logger log = LoggerFactory.getLogger(ValidationExecutor.class);

    /**
     * Validates the after-test result against the SLO p99 threshold.
     *
     * @param after           load-test result from the optimized run
     * @param sloThresholdMs  the maximum acceptable p99 latency in milliseconds;
     *                        should already include any breach-threshold multiplier
     * @return a {@link ValidationResult} recording the outcome
     */
    public ValidationResult validate(RunResult after, double sloThresholdMs) {
        double measuredP99 = after.getP99LatencyMs();
        boolean passed = measuredP99 <= sloThresholdMs;

        if (passed) {
            log.info("Validation PASSED: p99={:.1f}ms ≤ threshold={:.1f}ms",
                    measuredP99, sloThresholdMs);
            return ValidationResult.passed(measuredP99, sloThresholdMs);
        } else {
            log.warn("Validation FAILED: p99={:.1f}ms > threshold={:.1f}ms",
                    measuredP99, sloThresholdMs);
            return ValidationResult.failed(measuredP99, sloThresholdMs);
        }
    }
}