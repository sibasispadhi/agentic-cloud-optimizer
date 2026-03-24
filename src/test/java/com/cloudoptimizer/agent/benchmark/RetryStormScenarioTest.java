package com.cloudoptimizer.agent.benchmark;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RetryStormScenario} — the primary paper scenario.
 *
 * <p>Tests verify:
 * <ul>
 *   <li>Deterministic output structure</li>
 *   <li>Naive agent <em>worsens</em> the storm (amplification)</li>
 *   <li>ACO smart agent <em>improves</em> outcomes (governance works)</li>
 *   <li>Policy engine permits the governed intervention</li>
 *   <li>Exit criteria are met with default config</li>
 * </ul>
 */
@DisplayName("RetryStormScenario")
class RetryStormScenarioTest {

    private RetryStormScenario scenario;
    private ScenarioConfig     config;
    private ScenarioResult     result;

    @BeforeEach
    void setUp() {
        scenario = new RetryStormScenario();
        config   = ScenarioConfig.defaults();
        result   = scenario.run(config);
    }

    // ── Identity ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Scenario identity")
    class IdentityTest {

        @Test @DisplayName("name is 'retry_storm'")
        void nameIsRetryStorm() {
            assertEquals("retry_storm", scenario.name());
        }

        @Test @DisplayName("description is non-empty")
        void descriptionNonEmpty() {
            assertFalse(scenario.description().isBlank());
        }

        @Test @DisplayName("result carries the scenario name")
        void resultCarriesName() {
            assertEquals("retry_storm", result.getScenarioName());
        }
    }

    // ── Structure ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Result structure")
    class StructureTest {

        @Test @DisplayName("all RunResult fields are populated")
        void allRunResultsPopulated() {
            assertNotNull(result.getBaseline());
            assertNotNull(result.getDegraded());
            assertNotNull(result.getNaiveOutcome());
            assertNotNull(result.getRecovered());
        }

        @Test @DisplayName("amplification metrics are populated")
        void amplificationPopulated() {
            assertNotNull(result.getAmplification());
        }

        @Test @DisplayName("findings list is non-empty")
        void findingsNonEmpty() {
            assertNotNull(result.getFindings());
            assertFalse(result.getFindings().isEmpty());
        }

        @Test @DisplayName("ranAt is set")
        void ranAtSet() {
            assertNotNull(result.getRanAt());
        }
    }

    // ── Amplification ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Amplification metrics — the core paper evidence")
    class AmplificationTest {

        @Test @DisplayName("amplification factor matches config (default 3.0x)")
        void amplificationFactorMatchesConfig() {
            assertEquals(3.0, result.getAmplification().getAmplificationFactor(), 0.01);
        }

        @Test @DisplayName("degraded p99 is higher than baseline p99")
        void degradedWorseThanBaseline() {
            assertTrue(result.getDegraded().getP99LatencyMs()
                    > result.getBaseline().getP99LatencyMs());
        }

        @Test @DisplayName("latency degradation ratio is >= 2.0x")
        void latencyDegradationAtLeast2x() {
            assertTrue(result.getAmplification().getLatencyDegradationRatio() >= 2.0,
                    "Expected >= 2× p99 degradation but got: "
                    + result.getAmplification().getLatencyDegradationRatio());
        }

        @Test @DisplayName("naive intervention WORSENS p99 (positive worsening %)")
        void naiveInterventionWorsens() {
            assertTrue(result.getAmplification().getNaiveWorseningPct() > 0,
                    "Naive agent should amplify the storm, not improve it");
        }

        @Test @DisplayName("naive outcome p99 > degraded p99")
        void naiveOutcomeWorseThanDegraded() {
            assertTrue(result.getNaiveOutcome().getP99LatencyMs()
                    > result.getDegraded().getP99LatencyMs());
        }

        @Test @DisplayName("throughput degrades >= 50% during storm")
        void throughputDegradesDuringStorm() {
            assertTrue(result.getAmplification().getThroughputDegradationPct() >= 50.0,
                    "Expected >= 50%% RPS loss during storm");
        }
    }

    // ── Smart recovery ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Smart (ACO) recovery")
    class SmartRecoveryTest {

        @Test @DisplayName("smart recovery improves p99 by > 50%")
        void smartRecoveryOver50Pct() {
            assertTrue(result.getAmplification().getSmartRecoveryPct() > 50.0,
                    "Expected > 50%% p99 improvement from ACO intervention, got: "
                    + result.getAmplification().getSmartRecoveryPct());
        }

        @Test @DisplayName("recovered p99 < degraded p99")
        void recoveredBetterThanDegraded() {
            assertTrue(result.getRecovered().getP99LatencyMs()
                    < result.getDegraded().getP99LatencyMs());
        }

        @Test @DisplayName("recovered p99 < naive outcome p99")
        void recoveredBetterThanNaive() {
            assertTrue(result.getRecovered().getP99LatencyMs()
                    < result.getNaiveOutcome().getP99LatencyMs());
        }

        @Test @DisplayName("policy allowed the smart intervention")
        void policyAllowedSmartIntervention() {
            assertTrue(result.getAmplification().isPolicyAllowed(),
                    "Policy engine should permit the governed back-off; outcome: "
                    + result.getAmplification().getPolicyOutcome());
        }
    }

    // ── Exit criteria ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Exit criteria")
    class ExitCriteriaTest {

        @Test @DisplayName("exit criteria met with default config")
        void exitCriteriaMet() {
            assertTrue(result.isExitCriteriaMet(),
                    "Retry storm exit criteria not met — findings: " + result.getFindings());
        }

        @Test @DisplayName("custom amplification factor 5.0x also meets exit criteria")
        void customHighAmplificationMeetsCriteria() {
            ScenarioConfig highAmp = ScenarioConfig.builder()
                    .retryAmplificationFactor(5.0).build();
            ScenarioResult r = scenario.run(highAmp);
            assertTrue(r.isExitCriteriaMet());
            assertEquals(5.0, r.getAmplification().getAmplificationFactor(), 0.01);
        }
    }
}