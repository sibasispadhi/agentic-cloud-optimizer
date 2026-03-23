package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.artifact.*;
import com.cloudoptimizer.agent.autonomy.AutonomyGateResult;
import com.cloudoptimizer.agent.budget.BudgetConsumption;
import com.cloudoptimizer.agent.model.AgentDecision;
import com.cloudoptimizer.agent.model.RunResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Assembles an {@link OptimizationPlan} from the data collected during an
 * optimization run.
 *
 * <p>Extracted from {@link OptimizationOrchestrator} so that plan construction
 * is testable in isolation and the orchestrator stays focused on workflow
 * sequencing.
 *
 * <p>Two entry points:
 * <ul>
 *   <li>{@link #buildPlan} — full run: baseline + after snapshot + policy result</li>
 *   <li>{@link #buildBlockedPlan} — policy denied: no after snapshot, status = FAILED</li>
 * </ul>
 */
@Service
public class PlanAssembler {

    @Value("${slo.target-p99-ms:100.0}")
    private double sloTargetP99Ms;

    @Value("${slo.breach-threshold:1.2}")
    private double sloBreachThreshold;

    @Value("${load.duration:10}")
    private int loadDuration;

    @Value("${baseline.concurrency:4}")
    private int baselineConcurrency;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Builds a complete plan for a run that was executed and validated.
     *
     * @param baseline          load-test result before changes
     * @param after             load-test result after changes
     * @param decision          agent's recommendation
     * @param strategy          agent strategy label ("llm" or "simple")
     * @param sloBreached       whether an SLO breach triggered this run
     * @param breachReason      human-readable breach description, or null
     * @param policyResult      result of policy engine evaluation
     * @param budgetConsumption actuation budget snapshot (Phase 3); may be null
     * @param autonomyDecision  autonomy gate result (Phase 4); may be null
     */
    public OptimizationPlan buildPlan(RunResult baseline, RunResult after,
                                      AgentDecision decision, String strategy,
                                      boolean sloBreached, String breachReason,
                                      PolicyEvaluationResult policyResult,
                                      BudgetConsumption budgetConsumption,
                                      AutonomyGateResult autonomyDecision) {
        boolean validated = after.getP99LatencyMs() <= sloTargetP99Ms * sloBreachThreshold;

        ValidationRecipe validation = ValidationRecipe.builder()
                .durationSeconds(loadDuration)
                .threshold(sloTargetP99Ms * sloBreachThreshold)
                .passed(validated)
                .validatedAt(Instant.now())
                .build();

        return basePlanBuilder(baseline, decision, strategy, sloBreached, breachReason, policyResult)
                .autonomyDecision(autonomyDecision)
                .budgetConsumption(budgetConsumption)
                .changes(buildChanges(baseline, after, decision))
                .validationRecipe(validation)
                .rollbackRecipe(buildRollback(baseline, decision))
                .optimizedSnapshot(after)
                .status(ExecutionStatus.VALIDATED)
                .build();
    }

    /**
     * Builds a plan for a run that was blocked by the policy engine or
     * the actuation budget gate.
     * No after-snapshot is recorded; status is {@link ExecutionStatus#FAILED}.
     *
     * @param baseline          load-test result before the blocked change
     * @param decision          agent's (denied) recommendation
     * @param strategy          agent strategy label
     * @param sloBreached       whether an SLO breach triggered this run
     * @param breachReason      human-readable breach description, or null
     * @param policyResult      result of policy engine evaluation (DENIED)
     * @param budgetConsumption actuation budget snapshot, or null if budget
     *                          gate was not reached (policy denied first)
     * @param autonomyDecision  autonomy gate result (Phase 4); may be null
     */
    public OptimizationPlan buildBlockedPlan(RunResult baseline,
                                             AgentDecision decision, String strategy,
                                             boolean sloBreached, String breachReason,
                                             PolicyEvaluationResult policyResult,
                                             BudgetConsumption budgetConsumption,
                                             AutonomyGateResult autonomyDecision) {
        return basePlanBuilder(baseline, decision, strategy, sloBreached, breachReason, policyResult)
                .autonomyDecision(autonomyDecision)
                .budgetConsumption(budgetConsumption)
                .changes(buildProposedChanges(baseline, decision))
                .validationRecipe(ValidationRecipe.builder()
                        .durationSeconds(loadDuration)
                        .threshold(sloTargetP99Ms * sloBreachThreshold)
                        .build())
                .rollbackRecipe(buildRollback(baseline, decision))
                .status(ExecutionStatus.FAILED)
                .build();
    }

    /**
     * Builds an advisory-only plan for runs in {@code OBSERVE_ONLY} or
     * {@code ADVISORY_ONLY} mode.  No after-snapshot; status is
     * {@link ExecutionStatus#ADVISORY}.
     *
     * @param baseline         baseline load-test result
     * @param decision         agent's recommendation (proposed only, not applied)
     * @param strategy         agent strategy label
     * @param sloBreached      whether an SLO breach triggered this run
     * @param breachReason     human-readable breach description, or null
     * @param autonomyDecision autonomy gate result recording why actuation was skipped
     */
    public OptimizationPlan buildAdvisoryPlan(RunResult baseline,
                                              AgentDecision decision, String strategy,
                                              boolean sloBreached, String breachReason,
                                              AutonomyGateResult autonomyDecision) {
        return basePlanBuilder(baseline, decision, strategy, sloBreached, breachReason,
                        PolicyEvaluationResult.pending())
                .autonomyDecision(autonomyDecision)
                .changes(buildProposedChanges(baseline, decision))
                .validationRecipe(ValidationRecipe.builder()
                        .durationSeconds(loadDuration)
                        .threshold(sloTargetP99Ms * sloBreachThreshold)
                        .build())
                .rollbackRecipe(buildRollback(baseline, decision))
                .status(ExecutionStatus.ADVISORY)
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Builds the parts common to all plan variants. */
    private OptimizationPlan.OptimizationPlanBuilder basePlanBuilder(
            RunResult baseline, AgentDecision decision, String strategy,
            boolean sloBreached, String breachReason,
            PolicyEvaluationResult policyResult) {

        PlanMetadata metadata = PlanMetadata.builder()
                .planId(UUID.randomUUID().toString())
                .generatedAt(Instant.now())
                .agentStrategy(strategy)
                .build();

        PlanIntent intent = PlanIntent.builder()
                .trigger(sloBreached ? PlanIntent.Trigger.SLO_BREACH : PlanIntent.Trigger.MANUAL)
                .description(sloBreached
                        ? "SLO breach detected — autonomous remediation triggered"
                        : "Manual optimization run")
                .targetLatencyMs(sloTargetP99Ms)
                .workloadDurationSeconds(loadDuration)
                .baselineConcurrency(baselineConcurrency)
                .build();

        PlanEvidence evidence = PlanEvidence.builder()
                .agentType(strategy)
                .recommendation(decision.getRecommendation())
                .reasoning(decision.getReasoning())
                .confidenceScore(decision.getConfidenceScore())
                .concurrencyConfidence(decision.getConcurrencyConfidence())
                .heapConfidence(decision.getHeapConfidence())
                .impactLevel(decision.getImpactLevel() != null
                        ? decision.getImpactLevel().name() : null)
                .sloBreached(sloBreached)
                .breachReason(breachReason)
                .build();

        return OptimizationPlan.builder()
                .metadata(metadata)
                .intent(intent)
                .baselineSnapshot(baseline)
                .evidence(evidence)
                .policyResult(policyResult);
    }

    /** Changes derived from actual before/after run results. */
    private List<PlanChange> buildChanges(RunResult baseline, RunResult after,
                                          AgentDecision decision) {
        List<PlanChange> changes = new ArrayList<>();

        if (baseline.getConcurrency() != after.getConcurrency()) {
            changes.add(PlanChange.builder()
                    .resource("jvm.concurrency")
                    .fromValue(String.valueOf(baseline.getConcurrency()))
                    .toValue(String.valueOf(after.getConcurrency()))
                    .rationale(decision.getReasoning())
                    .confidence(decision.getConcurrencyConfidence() != null
                            ? decision.getConcurrencyConfidence()
                            : decision.getConfidenceScore())
                    .build());
        }

        if (decision.getRecommendedHeapSizeMb() != null && baseline.getHeapMetrics() != null) {
            changes.add(PlanChange.builder()
                    .resource("jvm.heap_size_mb")
                    .fromValue(String.valueOf(baseline.getHeapMetrics().getHeapSizeMb()))
                    .toValue(String.valueOf(decision.getRecommendedHeapSizeMb()))
                    .rationale(decision.getReasoning())
                    .confidence(decision.getHeapConfidence() != null
                            ? decision.getHeapConfidence()
                            : decision.getConfidenceScore())
                    .build());
        }

        return changes;
    }

    /** Proposed changes from agent decision — used when the after-test never ran. */
    private List<PlanChange> buildProposedChanges(RunResult baseline, AgentDecision decision) {
        List<PlanChange> changes = new ArrayList<>();
        int proposedConcurrency = extractConcurrency(decision.getRecommendation(),
                baseline.getConcurrency());

        if (proposedConcurrency != baseline.getConcurrency()) {
            changes.add(PlanChange.builder()
                    .resource("jvm.concurrency")
                    .fromValue(String.valueOf(baseline.getConcurrency()))
                    .toValue(String.valueOf(proposedConcurrency))
                    .rationale(decision.getReasoning())
                    .confidence(decision.getConcurrencyConfidence() != null
                            ? decision.getConcurrencyConfidence()
                            : decision.getConfidenceScore())
                    .build());
        }

        if (decision.getRecommendedHeapSizeMb() != null && baseline.getHeapMetrics() != null) {
            changes.add(PlanChange.builder()
                    .resource("jvm.heap_size_mb")
                    .fromValue(String.valueOf(baseline.getHeapMetrics().getHeapSizeMb()))
                    .toValue(String.valueOf(decision.getRecommendedHeapSizeMb()))
                    .rationale(decision.getReasoning())
                    .confidence(decision.getHeapConfidence() != null
                            ? decision.getHeapConfidence()
                            : decision.getConfidenceScore())
                    .build());
        }

        return changes;
    }

    private RollbackRecipe buildRollback(RunResult baseline, AgentDecision decision) {
        Map<String, Object> restoreParams = new LinkedHashMap<>();
        restoreParams.put("jvm.concurrency", baseline.getConcurrency());
        if (baseline.getHeapMetrics() != null) {
            restoreParams.put("jvm.heap_size_mb", baseline.getHeapMetrics().getHeapSizeMb());
        }
        return RollbackRecipe.builder()
                .restoreParams(restoreParams)
                .triggerCondition(String.format(
                        "p99 latency exceeds %.0fms after optimization",
                        sloTargetP99Ms * sloBreachThreshold))
                .build();
    }

    private static int extractConcurrency(String recommendation, int defaultValue) {
        try {
            String[] parts = recommendation.toLowerCase().split("concurrency to ");
            if (parts.length > 1) {
                return Integer.parseInt(parts[1].split("\\D")[0]);
            }
        } catch (Exception ignored) { }
        return defaultValue;
    }
}