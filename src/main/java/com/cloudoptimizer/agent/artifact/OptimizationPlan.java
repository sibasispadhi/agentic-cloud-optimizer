package com.cloudoptimizer.agent.artifact;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.cloudoptimizer.agent.autonomy.AutonomyGateResult;
import com.cloudoptimizer.agent.budget.BudgetConsumption;
import com.cloudoptimizer.agent.model.RunResult;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * Root artifact produced by a single optimization run.
 *
 * <p>An {@code OptimizationPlan} is the single source of truth for what happened,
 * why it happened, and what should happen next.  It is persisted to disk
 * (JSON + YAML) by {@link com.cloudoptimizer.agent.artifact.PlanWriter} at the
 * end of every run, regardless of outcome.
 *
 * <h2>Section summary</h2>
 * <ul>
 *   <li>{@link #metadata}         — who, what, when</li>
 *   <li>{@link #intent}           — why this run was initiated</li>
 *   <li>{@link #baselineSnapshot} — measured state before any change</li>
 *   <li>{@link #changes}          — the changes the agent recommends</li>
 *   <li>{@link #evidence}         — raw output from the agent</li>
 *   <li>{@link #policyResult}     — governance evaluation (Phase 2)</li>
 *   <li>{@link #autonomyDecision} — autonomy gate result (Phase 4)</li>
 *   <li>{@link #budgetConsumption}— actuation budget accounting (Phase 3)</li>
 *   <li>{@link #validationRecipe} — how to confirm the change worked</li>
 *   <li>{@link #rollbackRecipe}   — how to undo if validation fails</li>
 *   <li>{@link #optimizedSnapshot}— measured state after change (if applied)</li>
 *   <li>{@link #status}           — current lifecycle stage</li>
 * </ul>
 */
@Getter
@Builder
@ToString
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OptimizationPlan {

    /** Provenance header: plan ID, schema version, timestamp, agent strategy. */
    @JsonProperty("metadata")
    private final PlanMetadata metadata;

    /** Why this run was triggered and what it is trying to achieve. */
    @JsonProperty("intent")
    private final PlanIntent intent;

    /** Full load-test result captured <em>before</em> any changes are applied. */
    @JsonProperty("baseline_snapshot")
    private final RunResult baselineSnapshot;

    /** Ordered list of configuration changes the agent recommends. */
    @JsonProperty("changes")
    private final List<PlanChange> changes;

    /** Verbatim agent output: recommendation, reasoning, confidence. */
    @JsonProperty("evidence")
    private final PlanEvidence evidence;

    /**
     * Governance evaluation result.
     * Starts as {@link PolicyEvaluationResult#pending()} until Phase 2
     * policy engine is wired in.
     */
    @JsonProperty("policy_result")
    private final PolicyEvaluationResult policyResult;

    /**
     * Autonomy gate result (Phase 4).
     * Records which mode was active and whether actuation was permitted.
     * {@code null} for plans produced before Phase 4 was wired in.
     */
    @JsonProperty("autonomy_decision")
    private final AutonomyGateResult autonomyDecision;

    /**
     * Actuation budget consumption snapshot (Phase 3).
     * Records how many changes were attempted, total delta pct consumed,
     * and whether the run stayed within configured budget limits.
     * {@code null} when budget evaluation was skipped (e.g. run was blocked
     * before reaching the budget gate).
     */
    @JsonProperty("budget_consumption")
    private final BudgetConsumption budgetConsumption;

    /** How to confirm the applied changes actually worked. */
    @JsonProperty("validation_recipe")
    private final ValidationRecipe validationRecipe;

    /** How to undo the changes if validation fails. */
    @JsonProperty("rollback_recipe")
    private final RollbackRecipe rollbackRecipe;

    /**
     * Full load-test result captured <em>after</em> changes are applied.
     * {@code null} until changes have been applied and tested.
     */
    @JsonProperty("optimized_snapshot")
    private final RunResult optimizedSnapshot;

    /** Current lifecycle state of this plan. */
    @JsonProperty("status")
    @Builder.Default
    private final ExecutionStatus status = ExecutionStatus.PENDING;
}