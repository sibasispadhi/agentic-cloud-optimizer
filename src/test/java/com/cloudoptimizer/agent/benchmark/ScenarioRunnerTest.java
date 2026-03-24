package com.cloudoptimizer.agent.benchmark;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ScenarioRunner} and all five benchmark scenarios.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Runner collects all 5 results cleanly</li>
 *   <li>Every scenario meets its exit criteria</li>
 *   <li>Suite report is correctly aggregated</li>
 *   <li>Each scenario shows governance outperforming naive approach</li>
 * </ul>
 */
@DisplayName("ScenarioRunner + All Scenarios")
class ScenarioRunnerTest {

    private static final ScenarioConfig CONFIG = ScenarioConfig.defaults();

    // ── Individual scenario exit-criteria smoke tests ─────────────────────────

    @Nested
    @DisplayName("Individual scenario exit criteria")
    class IndividualScenariosTest {

        @Test @DisplayName("RetryStormScenario meets exit criteria")
        void retryStorm() {
            assertTrue(new RetryStormScenario().run(CONFIG).isExitCriteriaMet());
        }

        @Test @DisplayName("ThreadSaturationScenario meets exit criteria")
        void threadSaturation() {
            assertTrue(new ThreadSaturationScenario().run(CONFIG).isExitCriteriaMet());
        }

        @Test @DisplayName("HeapOverprovisionedScenario meets exit criteria")
        void heapOverprovisioned() {
            assertTrue(new HeapOverprovisionedScenario().run(CONFIG).isExitCriteriaMet());
        }

        @Test @DisplayName("CpuThrottlingScenario meets exit criteria")
        void cpuThrottling() {
            assertTrue(new CpuThrottlingScenario().run(CONFIG).isExitCriteriaMet());
        }

        @Test @DisplayName("BurstTrafficScenario meets exit criteria")
        void burstTraffic() {
            assertTrue(new BurstTrafficScenario().run(CONFIG).isExitCriteriaMet());
        }
    }

    // ── Governance wins for every storm scenario ──────────────────────────────

    @Nested
    @DisplayName("Governance outperforms naive approach")
    class GovernanceWinsTest {

        @Test @DisplayName("retry storm: ACO recovered p99 < naive p99")
        void retryStormRecoveredBetterThanNaive() {
            ScenarioResult r = new RetryStormScenario().run(CONFIG);
            assertTrue(r.getRecovered().getP99LatencyMs() < r.getNaiveOutcome().getP99LatencyMs());
        }

        @Test @DisplayName("thread saturation: ACO recovered p99 < naive p99")
        void threadSaturationRecoveredBetterThanNaive() {
            ScenarioResult r = new ThreadSaturationScenario().run(CONFIG);
            assertTrue(r.getRecovered().getP99LatencyMs() < r.getNaiveOutcome().getP99LatencyMs());
        }

        @Test @DisplayName("CPU throttling: ACO recovered p99 < naive p99")
        void cpuThrottlingRecoveredBetterThanNaive() {
            ScenarioResult r = new CpuThrottlingScenario().run(CONFIG);
            assertTrue(r.getRecovered().getP99LatencyMs() < r.getNaiveOutcome().getP99LatencyMs());
        }

        @Test @DisplayName("burst traffic: ACO recovered p99 < naive p99")
        void burstTrafficRecoveredBetterThanNaive() {
            ScenarioResult r = new BurstTrafficScenario().run(CONFIG);
            assertTrue(r.getRecovered().getP99LatencyMs() < r.getNaiveOutcome().getP99LatencyMs());
        }
    }

    // ── Runner suite report ───────────────────────────────────────────────────

    @Nested
    @DisplayName("ScenarioRunner.runAll()")
    class RunnerTest {

        @Test @DisplayName("returns exactly 5 results")
        void returns5Results() {
            ScenarioRunner.SuiteReport report = new ScenarioRunner().runAll();
            assertEquals(5, report.getScenarioCount());
            assertEquals(5, report.getResults().size());
        }

        @Test @DisplayName("all 5 exit criteria met")
        void allExitCriteriaMet() {
            ScenarioRunner.SuiteReport report = new ScenarioRunner().runAll();
            assertTrue(report.isAllExitCriteriaMet(),
                    "Not all exit criteria met: " + report.getSummary());
        }

        @Test @DisplayName("suite summary map contains all 5 scenario names")
        void summaryContainsAllNames() {
            ScenarioRunner.SuiteReport report = new ScenarioRunner().runAll();
            assertTrue(report.getSummary().containsKey("retry_storm"));
            assertTrue(report.getSummary().containsKey("thread_saturation"));
            assertTrue(report.getSummary().containsKey("heap_overprovisioned"));
            assertTrue(report.getSummary().containsKey("cpu_throttling"));
            assertTrue(report.getSummary().containsKey("burst_traffic"));
        }

        @Test @DisplayName("runner tolerates a failing scenario — others still run")
        void runnerToleratesFailure() {
            Scenario alwaysFails = new Scenario() {
                @Override public String name()        { return "bad_scenario"; }
                @Override public String description() { return "always fails"; }
                @Override public ScenarioResult run(ScenarioConfig c) {
                    throw new RuntimeException("intentional test failure");
                }
            };

            ScenarioRunner runner = new ScenarioRunner(
                    List.of(alwaysFails, new RetryStormScenario()), CONFIG);
            ScenarioRunner.SuiteReport report = runner.runAll();

            assertEquals(2, report.getResults().size());
            // bad_scenario fails; retry_storm should still pass
            assertFalse(report.getSummary().get("bad_scenario"));
            assertTrue(report.getSummary().get("retry_storm"));
        }

        @Test @DisplayName("ranAt is populated")
        void ranAtPopulated() {
            assertNotNull(new ScenarioRunner().runAll().getRanAt());
        }
    }

    // ── AmplificationMetrics edge cases ───────────────────────────────────────

    @Nested
    @DisplayName("AmplificationMetrics.compute() edge cases")
    class AmplificationMetricsTest {

        @Test @DisplayName("zero baseline RPS → throughput degradation = 0 (no divide by zero)")
        void zeroBaselineRpsNoDivisionError() {
            AmplificationMetrics m = AmplificationMetrics.compute(
                    2.0, 100, 300, 400, 150, 0, 0, true, "ALLOWED");
            assertEquals(0.0, m.getThroughputDegradationPct(), 0.001);
        }

        @Test @DisplayName("zero degraded p99 → no division error in naive worsening")
        void zeroDegradedP99NoDivisionError() {
            AmplificationMetrics m = AmplificationMetrics.compute(
                    2.0, 80, 0, 0, 0, 100, 50, true, "ALLOWED");
            assertEquals(0.0, m.getNaiveWorseningPct(), 0.001);
            assertEquals(0.0, m.getSmartRecoveryPct(), 0.001);
        }
    }
}