package com.cloudoptimizer.agent.budget;

/**
 * A single resource change expressed as an absolute percentage delta.
 *
 * <p>The orchestrator computes these from the difference between the
 * baseline measurement and the agent's recommendation and hands them
 * to {@link ActuationBudgetLedger} for budget evaluation.
 *
 * <p>Example:
 * <pre>
 *   // concurrency: 4 → 6 = +50%
 *   ProposedChangeDelta.of("jvm.concurrency", 50.0);
 *
 *   // heap: 1024 MB → 768 MB = -25%  → absoluteDeltaPct = 25.0
 *   ProposedChangeDelta.of("jvm.heap_size_mb", 25.0);
 * </pre>
 *
 * @param resource        Short resource identifier (e.g. {@code "jvm.concurrency"})
 * @param absoluteDeltaPct Absolute value of the percentage change (&ge; 0)
 */
public record ProposedChangeDelta(String resource, double absoluteDeltaPct) {

    /**
     * Validates invariants on construction.
     * {@code absoluteDeltaPct} must be &ge; 0 — direction is tracked by the
     * plan change, not the budget.
     */
    public ProposedChangeDelta {
        if (resource == null || resource.isBlank()) {
            throw new IllegalArgumentException("resource must not be blank");
        }
        if (absoluteDeltaPct < 0) {
            throw new IllegalArgumentException(
                    "absoluteDeltaPct must be >= 0, got: " + absoluteDeltaPct);
        }
    }

    /** Convenience factory. */
    public static ProposedChangeDelta of(String resource, double absoluteDeltaPct) {
        return new ProposedChangeDelta(resource, absoluteDeltaPct);
    }

    /**
     * Computes the absolute percentage delta between two values.
     * Returns 0 if {@code from} is 0 to avoid division by zero.
     */
    public static ProposedChangeDelta compute(String resource, double from, double to) {
        double delta = from == 0 ? 0 : Math.abs((to - from) / from) * 100.0;
        return new ProposedChangeDelta(resource, delta);
    }
}