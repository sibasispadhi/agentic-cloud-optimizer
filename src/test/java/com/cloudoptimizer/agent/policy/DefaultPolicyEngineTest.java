package com.cloudoptimizer.agent.policy;

import com.cloudoptimizer.agent.model.HeapMetrics;
import com.cloudoptimizer.agent.model.RunResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DefaultPolicyEngine}.
 *
 * <p>Each nested class targets one rule.  Table-driven {@code @CsvSource}
 * tests cover boundary conditions for numeric rules.
 */
class DefaultPolicyEngineTest {

    private DefaultPolicyEngine engine;
    private ActuationPolicy policy;

    @BeforeEach
    void setUp() {
        engine = new DefaultPolicyEngine();
        policy = defaultPolicy();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static ActuationPolicy defaultPolicy() {
        ActuationPolicy p = new ActuationPolicy();
        p.setMaxConcurrencyDeltaPct(50.0);
        p.setMaxHeapDeltaPct(50.0);
        p.setMinConcurrency(1);
        p.setMaxConcurrency(100);
        p.setMinHeapMb(128);
        p.setMaxHeapMb(16384);
        p.setMinConfidenceToAct(0.60);
        p.setCooldownSeconds(0);
        p.setForbiddenResources(List.of());
        p.setImpactLevelsRequiringApproval(List.of("CRITICAL"));
        return p;
    }

    private static RunResult baselineWith(int concurrency) {
        return RunResult.builder()
                .concurrency(concurrency)
                .medianLatencyMs(50.0).avgLatencyMs(55.0)
                .p95LatencyMs(90.0).p99LatencyMs(95.0)
                .requestsPerSecond(200.0).totalRequests(2000)
                .costEstimateUsd(0.10)
                .build();
    }

    private static RunResult baselineWithHeap(int concurrency, double heapSizeMb) {
        HeapMetrics heap = HeapMetrics.builder()
                .heapSizeMb((long) heapSizeMb).heapUsedMb((long)(heapSizeMb * 0.6))
                .heapUsagePercent(60.0).gcCount(5L).gcTimeMs(50L)
                .gcPauseTimeAvgMs(10.0).gcFrequencyPerSec(0.5)
                .build();
        return RunResult.builder()
                .concurrency(concurrency)
                .medianLatencyMs(50.0).avgLatencyMs(55.0)
                .p95LatencyMs(90.0).p99LatencyMs(95.0)
                .requestsPerSecond(200.0).totalRequests(2000)
                .costEstimateUsd(0.10).heapMetrics(heap)
                .build();
    }

    private static PolicyContext ctxFor(RunResult baseline, int proposed,
                                       double confidence, String impact) {
        Set<String> resources = proposed != baseline.getConcurrency()
                ? Set.of("jvm.concurrency") : Set.of();
        return PolicyContext.builder()
                .baseline(baseline)
                .proposedConcurrency(proposed)
                .confidenceScore(confidence)
                .impactLevel(impact)
                .proposedResources(resources)
                .build();
    }

    // ── Rule 1: MAX_CONCURRENCY_DELTA ─────────────────────────────────────────

    @Nested
    @DisplayName("Rule 1 — MAX_CONCURRENCY_DELTA")
    class ConcurrencyDeltaTest {

        @ParameterizedTest(name = "baseline={0} proposed={1} limit={2}% → {3}")
        @CsvSource({
                "4, 6,  50.0, ALLOWED",     // 50% up — exactly at limit
                "4, 7,  50.0, DENIED",      // 75% up — over limit
                "4, 2,  50.0, ALLOWED",     // 50% down — exactly at limit
                "4, 1,  50.0, DENIED",      // 75% down — over limit
                "10, 11, 10.0, ALLOWED",    // 10% up — exactly at limit
                "10, 12, 10.0, DENIED",     // 20% up — over limit
        })
        void concurrencyDeltaBoundary(int baseline, int proposed,
                                      double limitPct, String expectedOutcome) {
            policy.setMaxConcurrencyDeltaPct(limitPct);
            PolicyContext ctx = ctxFor(baselineWith(baseline), proposed, 0.9, "LOW");
            assertEquals(expectedOutcome, engine.evaluate(ctx, policy).outcome().name());
        }

        @Test
        @DisplayName("no concurrency change — always passes this rule")
        void noChange_passes() {
            PolicyContext ctx = ctxFor(baselineWith(4), 4, 0.9, "LOW");
            assertFalse(engine.evaluate(ctx, policy).isDenied());
        }
    }

    // ── Rule 2: CONCURRENCY_BOUNDS ────────────────────────────────────────────

    @Nested
    @DisplayName("Rule 2 — CONCURRENCY_BOUNDS (absolute min/max)")
    class ConcurrencyBoundsTest {

        @Test
        @DisplayName("below min → DENIED with CONCURRENCY_BOUNDS violation")
        void belowMin_isDenied() {
            policy.setMinConcurrency(5);
            PolicyDecision d = engine.evaluate(ctxFor(baselineWith(6), 3, 0.9, "LOW"), policy);
            assertTrue(d.isDenied());
            assertTrue(d.violations().stream().anyMatch(v -> v.rule().equals("CONCURRENCY_BOUNDS")));
        }

        @Test
        @DisplayName("above max → DENIED with CONCURRENCY_BOUNDS violation")
        void aboveMax_isDenied() {
            policy.setMaxConcurrency(10);
            PolicyDecision d = engine.evaluate(ctxFor(baselineWith(8), 12, 0.9, "LOW"), policy);
            assertTrue(d.isDenied());
            assertTrue(d.violations().stream().anyMatch(v -> v.rule().equals("CONCURRENCY_BOUNDS")));
        }

        @Test
        @DisplayName("exactly at max → not denied by this rule")
        void atMax_isAllowed() {
            policy.setMaxConcurrency(10);
            assertFalse(engine.evaluate(ctxFor(baselineWith(8), 10, 0.9, "LOW"), policy).isDenied());
        }
    }

    // ── Rules 3 & 4: HEAP ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Rules 3 & 4 — Heap delta and absolute bounds")
    class HeapTest {

        @Test
        @DisplayName("heap delta exceeds limit → DENIED with MAX_HEAP_DELTA")
        void heapDeltaExceedsLimit_isDenied() {
            policy.setMaxHeapDeltaPct(25.0);
            PolicyContext ctx = PolicyContext.builder()
                    .baseline(baselineWithHeap(4, 1024.0))
                    .proposedConcurrency(4)
                    .proposedHeapSizeMb(1400)   // ~36.7% > 25% limit
                    .confidenceScore(0.9).impactLevel("MEDIUM")
                    .proposedResources(Set.of("jvm.heap_size_mb"))
                    .build();
            PolicyDecision d = engine.evaluate(ctx, policy);
            assertTrue(d.isDenied());
            assertTrue(d.violations().stream().anyMatch(v -> v.rule().equals("MAX_HEAP_DELTA")));
        }

        @Test
        @DisplayName("heap delta within limit → no heap delta violation")
        void heapDeltaWithinLimit_noViolation() {
            PolicyContext ctx = PolicyContext.builder()
                    .baseline(baselineWithHeap(4, 1024.0))
                    .proposedConcurrency(4)
                    .proposedHeapSizeMb(1400)   // ~36.7% < 50% limit
                    .confidenceScore(0.9).impactLevel("MEDIUM")
                    .proposedResources(Set.of("jvm.heap_size_mb"))
                    .build();
            assertFalse(engine.evaluate(ctx, policy).violations()
                    .stream().anyMatch(v -> v.rule().equals("MAX_HEAP_DELTA")));
        }

        @Test
        @DisplayName("proposed heap below absolute min → DENIED with HEAP_BOUNDS")
        void heapBelowMin_isDenied() {
            policy.setMinHeapMb(256);
            PolicyContext ctx = PolicyContext.builder()
                    .baseline(baselineWithHeap(4, 512.0))
                    .proposedConcurrency(4)
                    .proposedHeapSizeMb(100)
                    .confidenceScore(0.9).impactLevel("LOW")
                    .proposedResources(Set.of("jvm.heap_size_mb"))
                    .build();
            PolicyDecision d = engine.evaluate(ctx, policy);
            assertTrue(d.isDenied());
            assertTrue(d.violations().stream().anyMatch(v -> v.rule().equals("HEAP_BOUNDS")));
        }

        @Test
        @DisplayName("no heap change proposed — heap rules skipped entirely")
        void noHeapChange_rulesSkipped() {
            PolicyContext ctx = PolicyContext.builder()
                    .baseline(baselineWithHeap(4, 1024.0))
                    .proposedConcurrency(4)
                    .confidenceScore(0.9).impactLevel("LOW")
                    .proposedResources(Set.of())
                    .build();
            assertFalse(engine.evaluate(ctx, policy).violations()
                    .stream().anyMatch(v -> v.rule().startsWith("HEAP")
                            || v.rule().startsWith("MAX_HEAP")));
        }
    }

    // ── Rule 5: MIN_CONFIDENCE ────────────────────────────────────────────────

    @Nested
    @DisplayName("Rule 5 — MIN_CONFIDENCE")
    class ConfidenceTest {

        @ParameterizedTest(name = "confidence={0} threshold={1} → {2}")
        @CsvSource({
                "0.80, 0.60, ALLOWED",
                "0.60, 0.60, ALLOWED",           // exactly at threshold
                "0.59, 0.60, REQUIRES_APPROVAL",
                "0.00, 0.60, REQUIRES_APPROVAL",
        })
        void confidenceGating(double confidence, double threshold, String expectedOutcome) {
            policy.setMinConfidenceToAct(threshold);
            assertEquals(expectedOutcome,
                    engine.evaluate(ctxFor(baselineWith(4), 5, confidence, "LOW"), policy)
                            .outcome().name());
        }
    }

    // ── Rule 6: FORBIDDEN_RESOURCE ────────────────────────────────────────────

    @Nested
    @DisplayName("Rule 6 — FORBIDDEN_RESOURCE")
    class ForbiddenResourceTest {

        @Test
        @DisplayName("change to forbidden resource → DENIED")
        void forbiddenResource_isDenied() {
            policy.setForbiddenResources(List.of("jvm.heap_size_mb"));
            PolicyContext ctx = PolicyContext.builder()
                    .baseline(baselineWithHeap(4, 1024.0))
                    .proposedConcurrency(4).proposedHeapSizeMb(1200)
                    .confidenceScore(0.9).impactLevel("LOW")
                    .proposedResources(Set.of("jvm.heap_size_mb"))
                    .build();
            PolicyDecision d = engine.evaluate(ctx, policy);
            assertTrue(d.isDenied());
            assertTrue(d.violations().stream().anyMatch(v -> v.rule().equals("FORBIDDEN_RESOURCE")));
        }

        @Test
        @DisplayName("non-forbidden resource — no forbidden violation")
        void nonForbidden_noViolation() {
            policy.setForbiddenResources(List.of("jvm.heap_size_mb"));
            assertFalse(engine.evaluate(ctxFor(baselineWith(4), 6, 0.9, "LOW"), policy)
                    .violations().stream().anyMatch(v -> v.rule().equals("FORBIDDEN_RESOURCE")));
        }

        @Test
        @DisplayName("empty forbidden list — never raises FORBIDDEN_RESOURCE")
        void emptyList_neverDenies() {
            policy.setForbiddenResources(List.of());
            PolicyContext ctx = PolicyContext.builder()
                    .baseline(baselineWithHeap(4, 1024.0))
                    .proposedConcurrency(6).proposedHeapSizeMb(1500)
                    .confidenceScore(0.9).impactLevel("LOW")
                    .proposedResources(Set.of("jvm.concurrency", "jvm.heap_size_mb"))
                    .build();
            assertFalse(engine.evaluate(ctx, policy).violations()
                    .stream().anyMatch(v -> v.rule().equals("FORBIDDEN_RESOURCE")));
        }
    }

    // ── Rule 7: IMPACT_LEVEL_APPROVAL ────────────────────────────────────────

    @Nested
    @DisplayName("Rule 7 — IMPACT_LEVEL_APPROVAL")
    class ImpactLevelTest {

        @Test
        @DisplayName("CRITICAL impact → REQUIRES_APPROVAL")
        void criticalImpact_requiresApproval() {
            PolicyDecision d = engine.evaluate(ctxFor(baselineWith(4), 5, 0.9, "CRITICAL"), policy);
            assertTrue(d.requiresApproval());
            assertTrue(d.warnings().stream().anyMatch(w -> w.rule().equals("IMPACT_LEVEL_APPROVAL")));
        }

        @Test
        @DisplayName("HIGH impact → ALLOWED_WITH_WARNINGS (not denied)")
        void highImpact_warningButAllowed() {
            PolicyDecision d = engine.evaluate(ctxFor(baselineWith(4), 5, 0.9, "HIGH"), policy);
            assertFalse(d.isDenied());
            assertFalse(d.requiresApproval());
            assertTrue(d.warnings().stream().anyMatch(w -> w.rule().equals("HIGH_IMPACT_CHANGE")));
        }

        @Test
        @DisplayName("LOW impact with no other issues → cleanly ALLOWED")
        void lowImpact_cleanlyAllowed() {
            assertEquals(PolicyDecision.Outcome.ALLOWED,
                    engine.evaluate(ctxFor(baselineWith(4), 5, 0.9, "LOW"), policy).outcome());
        }
    }

    // ── Rule 8 (placeholder): COOLDOWN_WINDOW ────────────────────────────────

    @Nested
    @DisplayName("Rule 8 — COOLDOWN_WINDOW")
    class CooldownTest {

        @Test
        @DisplayName("cooldown=0 — never raises a violation")
        void cooldownDisabled_neverViolates() {
            policy.setCooldownSeconds(0);
            PolicyContext ctx = PolicyContext.builder()
                    .baseline(baselineWith(4)).proposedConcurrency(5)
                    .confidenceScore(0.9).impactLevel("LOW")
                    .lastAppliedAt(Instant.now().minusSeconds(5))
                    .build();
            assertFalse(engine.evaluate(ctx, policy).violations()
                    .stream().anyMatch(v -> v.rule().equals("COOLDOWN_WINDOW")));
        }

        @Test
        @DisplayName("within cooldown window → DENIED with COOLDOWN_WINDOW")
        void withinCooldown_isDenied() {
            policy.setCooldownSeconds(300);
            PolicyContext ctx = PolicyContext.builder()
                    .baseline(baselineWith(4)).proposedConcurrency(5)
                    .confidenceScore(0.9).impactLevel("LOW")
                    .lastAppliedAt(Instant.now().minusSeconds(60))
                    .build();
            PolicyDecision d = engine.evaluate(ctx, policy);
            assertTrue(d.isDenied());
            assertTrue(d.violations().stream().anyMatch(v -> v.rule().equals("COOLDOWN_WINDOW")));
        }

        @Test
        @DisplayName("cooldown elapsed → no COOLDOWN_WINDOW violation")
        void cooldownElapsed_noViolation() {
            policy.setCooldownSeconds(60);
            PolicyContext ctx = PolicyContext.builder()
                    .baseline(baselineWith(4)).proposedConcurrency(5)
                    .confidenceScore(0.9).impactLevel("LOW")
                    .lastAppliedAt(Instant.now().minusSeconds(120))
                    .build();
            assertFalse(engine.evaluate(ctx, policy).violations()
                    .stream().anyMatch(v -> v.rule().equals("COOLDOWN_WINDOW")));
        }

        @Test
        @DisplayName("no lastAppliedAt + cooldown > 0 → cooldown skipped")
        void noLastApplied_cooldownSkipped() {
            policy.setCooldownSeconds(300);
            PolicyContext ctx = PolicyContext.builder()
                    .baseline(baselineWith(4)).proposedConcurrency(5)
                    .confidenceScore(0.9).impactLevel("LOW")
                    .build();
            assertFalse(engine.evaluate(ctx, policy).violations()
                    .stream().anyMatch(v -> v.rule().equals("COOLDOWN_WINDOW")));
        }
    }

    // ── Outcome precedence ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Outcome precedence: DENIED > REQUIRES_APPROVAL > WARNINGS > ALLOWED")
    class OutcomePrecedenceTest {

        @Test
        @DisplayName("violation + low confidence → DENIED (violation wins)")
        void violationDominatesApproval() {
            policy.setMaxConcurrencyDeltaPct(10.0);  // triggers violation
            policy.setMinConfidenceToAct(0.99);       // triggers approval requirement
            assertEquals(PolicyDecision.Outcome.DENIED,
                    engine.evaluate(ctxFor(baselineWith(4), 10, 0.50, "LOW"), policy).outcome());
        }

        @Test
        @DisplayName("no violation + low confidence → REQUIRES_APPROVAL")
        void approvalRequiredWithNoViolation() {
            policy.setMinConfidenceToAct(0.99);
            assertEquals(PolicyDecision.Outcome.REQUIRES_APPROVAL,
                    engine.evaluate(ctxFor(baselineWith(4), 5, 0.50, "LOW"), policy).outcome());
        }

        @Test
        @DisplayName("no violation + no approval + HIGH impact → ALLOWED_WITH_WARNINGS")
        void warningsOnly_allowedWithWarnings() {
            assertEquals(PolicyDecision.Outcome.ALLOWED_WITH_WARNINGS,
                    engine.evaluate(ctxFor(baselineWith(4), 5, 0.9, "HIGH"), policy).outcome());
        }

        @Test
        @DisplayName("all clear → ALLOWED")
        void allClear_isAllowed() {
            assertEquals(PolicyDecision.Outcome.ALLOWED,
                    engine.evaluate(ctxFor(baselineWith(4), 5, 0.9, "LOW"), policy).outcome());
        }
    }

    // ── PolicyDecision.toPolicyEvaluationResult ───────────────────────────────

    @Nested
    @DisplayName("PolicyDecision → PolicyEvaluationResult conversion")
    class ConversionTest {

        @Test
        @DisplayName("ALLOWED → APPROVED with non-null evaluatedAt")
        void allowedMapsToApproved() {
            var result = PolicyDecision.allowed().toPolicyEvaluationResult();
            assertEquals("APPROVED", result.getStatus().name());
            assertNotNull(result.getEvaluatedAt());
        }

        @Test
        @DisplayName("DENIED → DENIED, violation messages preserved")
        void deniedMapsToDenied() {
            var d = PolicyDecision.denied(
                    List.of(PolicyViolation.of("SOME_RULE", "test violation")), List.of());
            var result = d.toPolicyEvaluationResult();
            assertEquals("DENIED", result.getStatus().name());
            assertTrue(result.getViolations().contains("test violation"));
        }

        @Test
        @DisplayName("REQUIRES_APPROVAL → REQUIRES_APPROVAL, warning messages preserved")
        void requiresApprovalMapsCorrectly() {
            var d = PolicyDecision.requiresApproval(
                    List.of(PolicyWarning.of("MIN_CONFIDENCE", "confidence too low")));
            var result = d.toPolicyEvaluationResult();
            assertEquals("REQUIRES_APPROVAL", result.getStatus().name());
            assertTrue(result.getWarnings().contains("confidence too low"));
        }

        @Test
        @DisplayName("ALLOWED_WITH_WARNINGS → APPROVED_WITH_WARNINGS")
        void warningsMapsToApprovedWithWarnings() {
            var d = PolicyDecision.allowedWithWarnings(
                    List.of(PolicyWarning.of("HIGH_IMPACT_CHANGE", "high impact")));
            assertEquals("APPROVED_WITH_WARNINGS", d.toPolicyEvaluationResult().getStatus().name());
        }
    }
}