package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.artifact.PlanChange;
import com.cloudoptimizer.agent.artifact.RollbackResult;
import com.cloudoptimizer.agent.artifact.ValidationResult;
import com.cloudoptimizer.agent.model.RunResult;
import com.cloudoptimizer.agent.simulator.WorkloadSimulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Executes rollback when post-optimization validation fails.
 *
 * <h2>Rollback semantics</h2>
 * <p>ACO cannot directly modify production JVM settings.  Within the
 * simulator environment, rollback means re-running the load test at the
 * baseline concurrency to confirm the system returns to a known-good state.
 *
 * <h2>Decision tree</h2>
 * <ol>
 *   <li>If validation passed → {@link RollbackResult#skippedValidationPassed()}</li>
 *   <li>If any change is marked {@code reversible=false} →
 *       {@link RollbackResult#notApplicable(String)}</li>
 *   <li>Otherwise: re-run load test at baseline concurrency →
 *       {@link RollbackResult#executed(String)}</li>
 * </ol>
 */
@Service
public class RollbackExecutor {

    private static final Logger log = LoggerFactory.getLogger(RollbackExecutor.class);

    private final WorkloadSimulator workloadSimulator;

    public RollbackExecutor(WorkloadSimulator workloadSimulator) {
        this.workloadSimulator = workloadSimulator;
    }

    /**
     * Decides whether to roll back and executes if appropriate.
     *
     * @param validationResult outcome of the validation step
     * @param changes          the changes applied this run
     * @param baselineConcurrency the concurrency value to restore
     * @param loadDurationSeconds how long to run the rollback verification test
     * @param targetRps         target RPS for the verification test (0 = unlimited)
     * @return a {@link RollbackResult} describing what happened
     */
    public RollbackResult maybeRollback(ValidationResult validationResult,
                                        List<PlanChange> changes,
                                        int baselineConcurrency,
                                        int loadDurationSeconds,
                                        double targetRps) {

        if (validationResult.status() == ValidationResult.ValidationStatus.PASSED) {
            log.info("Rollback: skipped — validation passed");
            return RollbackResult.skippedValidationPassed();
        }

        List<PlanChange> nonReversible = changes.stream()
                .filter(c -> !c.isReversible())
                .toList();

        if (!nonReversible.isEmpty()) {
            String resources = nonReversible.stream()
                    .map(PlanChange::getResource).toList().toString();
            String reason = "Changes %s are marked non-reversible — rollback skipped"
                    .formatted(resources);
            log.warn("Rollback: NOT_APPLICABLE — {}", reason);
            return RollbackResult.notApplicable(reason);
        }

        log.info("Rollback: executing — re-running baseline at concurrency={}",
                baselineConcurrency);
        try {
            workloadSimulator.executeLoad(baselineConcurrency, loadDurationSeconds, targetRps);
            String reason = "Rollback verification run completed at concurrency=%d"
                    .formatted(baselineConcurrency);
            log.info("Rollback: EXECUTED — {}", reason);
            return RollbackResult.executed(reason);
        } catch (Exception e) {
            String reason = "Rollback verification run failed: " + e.getMessage();
            log.error("Rollback: EXECUTED but verification failed — {}", reason, e);
            return RollbackResult.executed(reason);
        }
    }
}