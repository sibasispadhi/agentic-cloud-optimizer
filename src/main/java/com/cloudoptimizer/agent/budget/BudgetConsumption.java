package com.cloudoptimizer.agent.budget;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Immutable snapshot of budget consumption produced by {@link ActuationBudgetLedger}.
 *
 * <p>This record is stored inside {@link com.cloudoptimizer.agent.artifact.OptimizationPlan}
 * so every run artifact carries an auditable record of what budget was
 * consumed and whether the run stayed within bounds.
 *
 * <p>Factory methods:
 * <ul>
 *   <li>{@link #allowed}  — run was within budget</li>
 *   <li>{@link #denied}   — run exceeded budget</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BudgetConsumption(

        /** How many distinct resource changes were proposed this run. */
        @JsonProperty("changes_attempted")
        int changesAttempted,

        /** Budget ceiling: max distinct changes per run. */
        @JsonProperty("max_changes_per_run")
        int maxChangesPerRun,

        /** Sum of absolute percentage deltas across all proposed changes. */
        @JsonProperty("total_delta_pct_consumed")
        double totalDeltaPctConsumed,

        /** Budget ceiling: max total delta pct per run. */
        @JsonProperty("max_total_delta_pct")
        double maxTotalDeltaPct,

        /** Descriptive scope label (e.g. {@code "single-service"}). */
        @JsonProperty("blast_radius_scope")
        String blastRadiusScope,

        /** Per-resource deltas that were proposed. */
        @JsonProperty("resource_deltas")
        List<ProposedChangeDelta> resourceDeltas,

        /** {@code true} if all budget constraints were satisfied. */
        @JsonProperty("within_budget")
        boolean withinBudget,

        /**
         * Human-readable explanation when {@code withinBudget} is {@code false}.
         * {@code null} when {@code withinBudget} is {@code true}.
         */
        @JsonProperty("denial_reason")
        String denialReason
) {

    /** Factory: budget constraints satisfied. */
    public static BudgetConsumption allowed(
            int changesAttempted, int maxChangesPerRun,
            double totalDeltaPctConsumed, double maxTotalDeltaPct,
            String blastRadiusScope, List<ProposedChangeDelta> deltas) {

        return new BudgetConsumption(
                changesAttempted, maxChangesPerRun,
                totalDeltaPctConsumed, maxTotalDeltaPct,
                blastRadiusScope, List.copyOf(deltas),
                true, null);
    }

    /** Factory: budget constraint violated. */
    public static BudgetConsumption denied(
            int changesAttempted, int maxChangesPerRun,
            double totalDeltaPctConsumed, double maxTotalDeltaPct,
            String blastRadiusScope, List<ProposedChangeDelta> deltas,
            String denialReason) {

        return new BudgetConsumption(
                changesAttempted, maxChangesPerRun,
                totalDeltaPctConsumed, maxTotalDeltaPct,
                blastRadiusScope, List.copyOf(deltas),
                false, denialReason);
    }
}