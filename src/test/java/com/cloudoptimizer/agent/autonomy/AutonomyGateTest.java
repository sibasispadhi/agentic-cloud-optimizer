package com.cloudoptimizer.agent.autonomy;

import com.cloudoptimizer.agent.model.AgentDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AutonomyGate}.
 *
 * <p>No Spring context required — gate is stateless.
 * Tests cover all four modes plus boundary conditions for AUTO_LOW_RISK.
 */
class AutonomyGateTest {

    private AutonomyGate gate;
    private AutonomyConfig config;

    @BeforeEach
    void setUp() {
        gate = new AutonomyGate();
        config = defaultConfig();
    }

    private static AutonomyConfig defaultConfig() {
        AutonomyConfig c = new AutonomyConfig();
        c.setMode(AutonomyMode.AUTO_POLICY_GOVERNED);
        c.setLowRiskMaxImpactLevel("LOW");
        c.setLowRiskMinConfidence(0.75);
        return c;
    }

    private static AgentDecision decision(String impactLevel, double confidence) {
        AgentDecision.ImpactLevel level = impactLevel == null ? null
                : AgentDecision.ImpactLevel.valueOf(impactLevel);
        return AgentDecision.builder()
                .recommendation("increase concurrency to 6")
                .reasoning("test")
                .confidenceScore(confidence)
                .concurrencyConfidence(confidence)
                .impactLevel(level)
                .build();
    }

    // ── Mode: OBSERVE_ONLY ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Mode: OBSERVE_ONLY")
    class ObserveOnlyTest {

        @Test
        @DisplayName("always blocks actuation regardless of decision")
        void alwaysBlocked() {
            config.setMode(AutonomyMode.OBSERVE_ONLY);
            AutonomyGateResult result = gate.evaluate(decision("LOW", 0.99), config);
            assertFalse(result.actuationPermitted());
            assertEquals(AutonomyGateResult.Outcome.OBSERVE_ONLY_BLOCKED, result.outcome());
            assertEquals(AutonomyMode.OBSERVE_ONLY, result.mode());
        }

        @Test
        @DisplayName("OBSERVE_ONLY reason is non-null and descriptive")
        void reasonIsDescriptive() {
            config.setMode(AutonomyMode.OBSERVE_ONLY);
            String reason = gate.evaluate(decision("CRITICAL", 0.99), config).reason();
            assertNotNull(reason);
            assertTrue(reason.toUpperCase().contains("OBSERVE"));
        }
    }

    // ── Mode: ADVISORY_ONLY ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Mode: ADVISORY_ONLY")
    class AdvisoryOnlyTest {

        @Test
        @DisplayName("always blocks actuation regardless of decision")
        void alwaysBlocked() {
            config.setMode(AutonomyMode.ADVISORY_ONLY);
            AutonomyGateResult result = gate.evaluate(decision("LOW", 0.99), config);
            assertFalse(result.actuationPermitted());
            assertEquals(AutonomyGateResult.Outcome.ADVISORY_ONLY_BLOCKED, result.outcome());
        }

        @Test
        @DisplayName("ADVISORY_ONLY reason mentions advisory")
        void reasonMentionsAdvisory() {
            config.setMode(AutonomyMode.ADVISORY_ONLY);
            String reason = gate.evaluate(decision("LOW", 0.99), config).reason();
            assertNotNull(reason);
            assertTrue(reason.toUpperCase().contains("ADVISORY"));
        }
    }

    // ── Mode: AUTO_POLICY_GOVERNED ────────────────────────────────────────────

    @Nested
    @DisplayName("Mode: AUTO_POLICY_GOVERNED")
    class AutoPolicyGovernedTest {

        @Test
        @DisplayName("always permits actuation regardless of decision")
        void alwaysPermits() {
            config.setMode(AutonomyMode.AUTO_POLICY_GOVERNED);
            assertTrue(gate.evaluate(decision("CRITICAL", 0.0), config).actuationPermitted());
        }

        @Test
        @DisplayName("outcome is PROCEED")
        void outcomeIsProceed() {
            config.setMode(AutonomyMode.AUTO_POLICY_GOVERNED);
            assertEquals(AutonomyGateResult.Outcome.PROCEED,
                    gate.evaluate(decision("HIGH", 0.5), config).outcome());
        }
    }

    // ── Mode: AUTO_LOW_RISK ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Mode: AUTO_LOW_RISK")
    class AutoLowRiskTest {

        @BeforeEach
        void setMode() { config.setMode(AutonomyMode.AUTO_LOW_RISK); }

        @Test
        @DisplayName("LOW impact + high confidence → permitted")
        void lowImpact_highConfidence_permitted() {
            AutonomyGateResult r = gate.evaluate(decision("LOW", 0.90), config);
            assertTrue(r.actuationPermitted());
            assertEquals(AutonomyGateResult.Outcome.PROCEED, r.outcome());
        }

        @Test
        @DisplayName("LOW impact + confidence exactly at threshold → permitted")
        void lowImpact_exactThreshold_permitted() {
            config.setLowRiskMinConfidence(0.75);
            assertTrue(gate.evaluate(decision("LOW", 0.75), config).actuationPermitted());
        }

        @Test
        @DisplayName("LOW impact + confidence just below threshold → blocked by confidence")
        void lowImpact_justBelowThreshold_blocked() {
            config.setLowRiskMinConfidence(0.75);
            AutonomyGateResult r = gate.evaluate(decision("LOW", 0.74), config);
            assertFalse(r.actuationPermitted());
            assertEquals(AutonomyGateResult.Outcome.LOW_RISK_BLOCKED_CONFIDENCE, r.outcome());
        }

        @ParameterizedTest(name = "impact={0} threshold=LOW → permitted={1}")
        @CsvSource({
                "LOW,    true",
                "MEDIUM, false",
                "HIGH,   false",
                "CRITICAL, false",
        })
        void impactBoundary(String impact, boolean expectedPermit) {
            config.setLowRiskMaxImpactLevel("LOW");
            AutonomyGateResult r = gate.evaluate(decision(impact, 0.99), config);
            assertEquals(expectedPermit, r.actuationPermitted());
        }

        @ParameterizedTest(name = "impact={0} threshold=MEDIUM → permitted={1}")
        @CsvSource({
                "LOW,    true",
                "MEDIUM, true",
                "HIGH,   false",
                "CRITICAL, false",
        })
        void impactBoundary_mediumThreshold(String impact, boolean expectedPermit) {
            config.setLowRiskMaxImpactLevel("MEDIUM");
            AutonomyGateResult r = gate.evaluate(decision(impact, 0.99), config);
            assertEquals(expectedPermit, r.actuationPermitted());
        }

        @Test
        @DisplayName("MEDIUM impact + threshold MEDIUM + high confidence → permitted")
        void mediumImpact_mediumThreshold_permitted() {
            config.setLowRiskMaxImpactLevel("MEDIUM");
            assertTrue(gate.evaluate(decision("MEDIUM", 0.99), config).actuationPermitted());
        }

        @Test
        @DisplayName("impact check runs before confidence check (impact wins)")
        void impactCheckRunsFirst() {
            // HIGH impact exceeds LOW threshold AND confidence is above min
            AutonomyGateResult r = gate.evaluate(decision("HIGH", 0.99), config);
            assertFalse(r.actuationPermitted());
            assertEquals(AutonomyGateResult.Outcome.LOW_RISK_BLOCKED_IMPACT, r.outcome());
        }

        @Test
        @DisplayName("null impact level treated as maximally severe (blocked)")
        void nullImpact_treatedAsSevere() {
            AutonomyGateResult r = gate.evaluate(decision(null, 0.99), config);
            assertFalse(r.actuationPermitted());
            assertEquals(AutonomyGateResult.Outcome.LOW_RISK_BLOCKED_IMPACT, r.outcome());
        }
    }

    // ── exceedsImpactThreshold (package-visible helper) ───────────────────────

    @Nested
    @DisplayName("AutonomyGate.exceedsImpactThreshold helper")
    class ExceedsImpactThresholdTest {

        @ParameterizedTest(name = "candidate={0} max={1} → exceeds={2}")
        @CsvSource({
                "LOW,      LOW,      false",
                "MEDIUM,   LOW,      true",
                "HIGH,     LOW,      true",
                "CRITICAL, LOW,      true",
                "LOW,      MEDIUM,   false",
                "MEDIUM,   MEDIUM,   false",
                "HIGH,     MEDIUM,   true",
                "CRITICAL, HIGH,     true",
                "HIGH,     HIGH,     false",
                "CRITICAL, CRITICAL, false",
        })
        void boundaries(String candidate, String max, boolean expected) {
            assertEquals(expected, AutonomyGate.exceedsImpactThreshold(candidate, max));
        }

        @Test
        @DisplayName("unknown candidate level is treated as exceeding any threshold")
        void unknownCandidate_exceedsAll() {
            assertTrue(AutonomyGate.exceedsImpactThreshold("UNKNOWN", "LOW"));
            assertTrue(AutonomyGate.exceedsImpactThreshold("UNKNOWN", "CRITICAL"));
        }

        @Test
        @DisplayName("unknown max level never blocks (treats as unbounded)")
        void unknownMax_neverBlocks() {
            assertFalse(AutonomyGate.exceedsImpactThreshold("CRITICAL", "UNKNOWN"));
        }
    }
}