package com.cloudoptimizer.agent.benchmark;

/**
 * Common contract for all ACO benchmark scenarios.
 *
 * <p>Each scenario is a self-contained, deterministic experiment that:
 * <ol>
 *   <li>Establishes a baseline (healthy system state)</li>
 *   <li>Introduces a fault or stress condition</li>
 *   <li>Shows what a <em>naive</em> latency-reactive agent would do (and why it can backfire)</li>
 *   <li>Shows what ACO's policy-governed agent recommends</li>
 *   <li>Quantifies the outcome difference via {@link AmplificationMetrics}</li>
 * </ol>
 *
 * <p>Scenarios are intentionally <b>Spring-free</b> so they are unit-testable
 * without an application context.
 *
 * @see ScenarioRunner
 * @see RetryStormScenario
 */
public interface Scenario {

    /** Short, stable identifier — used as the JSON key and test assertion handle. */
    String name();

    /** One-sentence description suitable for paper / documentation. */
    String description();

    /**
     * Runs the scenario with the given configuration and returns structured results.
     *
     * <p>Implementations must be deterministic: the same {@code config} must
     * always produce the same {@link ScenarioResult}.
     *
     * @param config input parameters (concurrency, latency targets, etc.)
     * @return fully populated result including amplification metrics
     */
    ScenarioResult run(ScenarioConfig config);
}