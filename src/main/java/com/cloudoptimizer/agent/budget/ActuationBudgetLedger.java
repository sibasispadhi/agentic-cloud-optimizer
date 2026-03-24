package com.cloudoptimizer.agent.budget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Stateless budget evaluator for a single optimization run.
 *
 * <h2>Responsibility</h2>
 * <p>Given a list of proposed resource-change deltas and the active
 * {@link ActuationBudget}, decide whether the run is within budget and
 * produce an auditable {@link BudgetConsumption} record.
 *
 * <h2>Evaluation order</h2>
 * <ol>
 *   <li>Check count: {@code changesAttempted &le; maxChangesPerRun}</li>
 *   <li>Check total delta: {@code sum(absoluteDeltaPct) &le; maxTotalDeltaPct}</li>
 * </ol>
 * The first violated constraint wins (first-fail-fast).
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li>Stateless — no instance state mutated between calls.</li>
 *   <li>Deterministic — same inputs always produce the same result.</li>
 *   <li>No Spring circular dependency risk — it depends on nothing else.</li>
 * </ul>
 */
@Service
public class ActuationBudgetLedger {

    private static final Logger log = LoggerFactory.getLogger(ActuationBudgetLedger.class);

    /**
     * Evaluates proposed changes against the active budget.
     *
     * @param deltas  per-resource deltas proposed for this run (may be empty)
     * @param budget  the active budget configuration
     * @return a {@link BudgetConsumption} describing what was consumed and
     *         whether it was within budget
     */
    public BudgetConsumption evaluate(List<ProposedChangeDelta> deltas, ActuationBudget budget) {
        int changesAttempted = deltas.size();
        double totalDeltaPct = deltas.stream()
                .mapToDouble(ProposedChangeDelta::absoluteDeltaPct)
                .sum();

        log.info("Budget gate: changes={} (max={})  totalDeltaPct={:.1f}% (max={:.1f}%)",
                changesAttempted, budget.getMaxChangesPerRun(),
                totalDeltaPct, budget.getMaxTotalDeltaPct());

        // Rule 1 — change count
        if (changesAttempted > budget.getMaxChangesPerRun()) {
            String reason = "Change count %d exceeds budget limit of %d per run"
                    .formatted(changesAttempted, budget.getMaxChangesPerRun());
            log.warn("Budget DENIED: {}", reason);
            return BudgetConsumption.denied(
                    changesAttempted, budget.getMaxChangesPerRun(),
                    totalDeltaPct, budget.getMaxTotalDeltaPct(),
                    budget.getBlastRadiusScope(), deltas, reason);
        }

        // Rule 2 — total delta
        if (totalDeltaPct > budget.getMaxTotalDeltaPct()) {
            String reason = "Total delta %.1f%% exceeds budget limit of %.1f%%"
                    .formatted(totalDeltaPct, budget.getMaxTotalDeltaPct());
            log.warn("Budget DENIED: {}", reason);
            return BudgetConsumption.denied(
                    changesAttempted, budget.getMaxChangesPerRun(),
                    totalDeltaPct, budget.getMaxTotalDeltaPct(),
                    budget.getBlastRadiusScope(), deltas, reason);
        }

        log.info("Budget ALLOWED: changes={} totalDeltaPct={:.1f}%", changesAttempted, totalDeltaPct);
        return BudgetConsumption.allowed(
                changesAttempted, budget.getMaxChangesPerRun(),
                totalDeltaPct, budget.getMaxTotalDeltaPct(),
                budget.getBlastRadiusScope(), deltas);
    }
}