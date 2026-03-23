package com.cloudoptimizer.agent.budget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ActuationBudgetLedger}.
 *
 * <p>The ledger is pure / stateless — no Spring context needed.
 * Tests cover both rules (change count, total delta pct) plus boundary
 * and edge-case behaviour.
 */
class ActuationBudgetLedgerTest {

    private ActuationBudgetLedger ledger;
    private ActuationBudget budget;

    @BeforeEach
    void setUp() {
        ledger = new ActuationBudgetLedger();
        budget = defaultBudget();
    }

    private static ActuationBudget defaultBudget() {
        ActuationBudget b = new ActuationBudget();
        b.setMaxChangesPerRun(3);
        b.setMaxTotalDeltaPct(75.0);
        b.setBlastRadiusScope("single-service");
        return b;
    }

    // ── Empty input ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Empty delta list")
    class EmptyTest {

        @Test
        @DisplayName("zero changes → always within budget")
        void noChanges_alwaysAllowed() {
            BudgetConsumption result = ledger.evaluate(List.of(), budget);
            assertTrue(result.withinBudget());
            assertEquals(0, result.changesAttempted());
            assertEquals(0.0, result.totalDeltaPctConsumed(), 0.001);
            assertNull(result.denialReason());
        }
    }

    // ── Rule 1: change count ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Rule 1 — max changes per run")
    class ChangeCountTest {

        @ParameterizedTest(name = "changes={0} limit={1} → {2}")
        @CsvSource({
                "1, 3, true",    // well under
                "3, 3, true",    // exactly at limit
                "4, 3, false",   // one over
                "10, 3, false",  // way over
                "1, 1, true",    // limit of 1, exactly at it
                "2, 1, false",   // limit of 1, two proposed
        })
        void changeCountBoundary(int numChanges, int limit, boolean expectedWithin) {
            budget.setMaxChangesPerRun(limit);
            budget.setMaxTotalDeltaPct(10_000.0); // effectively disabled

            List<ProposedChangeDelta> deltas = buildDeltas(numChanges, 5.0);
            BudgetConsumption result = ledger.evaluate(deltas, budget);

            assertEquals(expectedWithin, result.withinBudget(),
                    "Expected withinBudget=%s for changes=%d limit=%d"
                            .formatted(expectedWithin, numChanges, limit));
        }

        @Test
        @DisplayName("denied — denial reason references change count")
        void denied_reasonMentionsCount() {
            budget.setMaxChangesPerRun(1);
            budget.setMaxTotalDeltaPct(10_000.0);
            BudgetConsumption result = ledger.evaluate(
                    List.of(ProposedChangeDelta.of("jvm.concurrency", 20.0),
                            ProposedChangeDelta.of("jvm.heap_size_mb", 10.0)),
                    budget);
            assertFalse(result.withinBudget());
            assertNotNull(result.denialReason());
            assertTrue(result.denialReason().contains("2"),   // changesAttempted
                    "denial reason should mention 2: " + result.denialReason());
            assertTrue(result.denialReason().contains("1"),   // limit
                    "denial reason should mention limit 1: " + result.denialReason());
        }
    }

    // ── Rule 2: total delta pct ───────────────────────────────────────────────

    @Nested
    @DisplayName("Rule 2 — max total delta pct")
    class TotalDeltaTest {

        @ParameterizedTest(name = "delta={0}% limit={1}% → {2}")
        @CsvSource({
                "50.0,  75.0, true",    // well under
                "75.0,  75.0, true",    // exactly at limit
                "75.01, 75.0, false",   // just over
                "0.0,   75.0, true",    // zero delta
                "100.0, 75.0, false",   // over
                "100.0, 100.0, true",   // exactly at 100%
        })
        void totalDeltaBoundary(double totalDelta, double limit, boolean expectedWithin) {
            budget.setMaxChangesPerRun(100);  // effectively disabled
            budget.setMaxTotalDeltaPct(limit);

            BudgetConsumption result = ledger.evaluate(
                    List.of(ProposedChangeDelta.of("jvm.concurrency", totalDelta)),
                    budget);

            assertEquals(expectedWithin, result.withinBudget(),
                    "Expected withinBudget=%s for delta=%.2f limit=%.2f"
                            .formatted(expectedWithin, totalDelta, limit));
        }

        @Test
        @DisplayName("two resources — total delta is sum of both")
        void multiResource_summedCorrectly() {
            budget.setMaxChangesPerRun(10);
            budget.setMaxTotalDeltaPct(60.0);

            BudgetConsumption result = ledger.evaluate(
                    List.of(ProposedChangeDelta.of("jvm.concurrency", 25.0),
                            ProposedChangeDelta.of("jvm.heap_size_mb", 30.0)),
                    budget);

            // 25 + 30 = 55% < 60% limit → allowed
            assertTrue(result.withinBudget());
            assertEquals(55.0, result.totalDeltaPctConsumed(), 0.001);
        }

        @Test
        @DisplayName("two resources summing over limit → denied")
        void multiResource_overLimit_denied() {
            budget.setMaxChangesPerRun(10);
            budget.setMaxTotalDeltaPct(50.0);

            BudgetConsumption result = ledger.evaluate(
                    List.of(ProposedChangeDelta.of("jvm.concurrency", 30.0),
                            ProposedChangeDelta.of("jvm.heap_size_mb", 30.0)),
                    budget);

            // 30 + 30 = 60% > 50% limit → denied
            assertFalse(result.withinBudget());
            assertEquals(60.0, result.totalDeltaPctConsumed(), 0.001);
            assertTrue(result.denialReason().contains("60") || result.denialReason().contains("60.0"),
                    "denial reason should mention 60: " + result.denialReason());
        }

        @Test
        @DisplayName("denied — denial reason references total delta")
        void denied_reasonMentionsDelta() {
            budget.setMaxChangesPerRun(100);
            budget.setMaxTotalDeltaPct(40.0);
            BudgetConsumption result = ledger.evaluate(
                    List.of(ProposedChangeDelta.of("jvm.concurrency", 50.0)), budget);
            assertFalse(result.withinBudget());
            assertNotNull(result.denialReason());
        }
    }

    // ── Rule precedence: count check runs first ────────────────────────────────

    @Nested
    @DisplayName("Rule precedence — count before delta")
    class PrecedenceTest {

        @Test
        @DisplayName("both count AND delta exceeded → count violation wins")
        void countExceeded_wins() {
            budget.setMaxChangesPerRun(1);
            budget.setMaxTotalDeltaPct(10.0);

            BudgetConsumption result = ledger.evaluate(
                    List.of(ProposedChangeDelta.of("jvm.concurrency", 50.0),
                            ProposedChangeDelta.of("jvm.heap_size_mb", 50.0)),
                    budget);

            assertFalse(result.withinBudget());
            // Denial reason comes from count rule, not delta rule
            assertTrue(result.denialReason().toLowerCase().contains("count")
                            || result.denialReason().toLowerCase().contains("exceed"),
                    "Unexpected denial reason: " + result.denialReason());
        }

        @Test
        @DisplayName("count ok but delta exceeded → delta violation fires")
        void deltaExceeded_afterCountPasses() {
            budget.setMaxChangesPerRun(5);
            budget.setMaxTotalDeltaPct(20.0);

            BudgetConsumption result = ledger.evaluate(
                    List.of(ProposedChangeDelta.of("jvm.concurrency", 50.0)),
                    budget);

            assertFalse(result.withinBudget());
            assertTrue(result.denialReason().toLowerCase().contains("delta")
                            || result.denialReason().toLowerCase().contains("exceed"),
                    "Unexpected denial reason: " + result.denialReason());
        }
    }

    // ── Consumption record fields ─────────────────────────────────────────────

    @Nested
    @DisplayName("BudgetConsumption record fields")
    class ConsumptionFieldsTest {

        @Test
        @DisplayName("allowed result carries correct census")
        void allowedResult_hasCorrectFields() {
            BudgetConsumption result = ledger.evaluate(
                    List.of(ProposedChangeDelta.of("jvm.concurrency", 25.0)), budget);

            assertTrue(result.withinBudget());
            assertEquals(1, result.changesAttempted());
            assertEquals(3, result.maxChangesPerRun());
            assertEquals(25.0, result.totalDeltaPctConsumed(), 0.001);
            assertEquals(75.0, result.maxTotalDeltaPct(), 0.001);
            assertEquals("single-service", result.blastRadiusScope());
            assertEquals(1, result.resourceDeltas().size());
            assertNull(result.denialReason());
        }

        @Test
        @DisplayName("denied result carries correct census")
        void deniedResult_hasCorrectFields() {
            budget.setMaxChangesPerRun(1);
            BudgetConsumption result = ledger.evaluate(
                    List.of(ProposedChangeDelta.of("a", 10.0),
                            ProposedChangeDelta.of("b", 10.0)),
                    budget);

            assertFalse(result.withinBudget());
            assertEquals(2, result.changesAttempted());
            assertEquals(1, result.maxChangesPerRun());
            assertEquals(2, result.resourceDeltas().size());
            assertNotNull(result.denialReason());
        }

        @Test
        @DisplayName("resource deltas list is immutable")
        void resourceDeltas_areImmutable() {
            BudgetConsumption result = ledger.evaluate(
                    List.of(ProposedChangeDelta.of("jvm.concurrency", 10.0)), budget);
            assertThrows(UnsupportedOperationException.class,
                    () -> result.resourceDeltas().add(ProposedChangeDelta.of("x", 5.0)));
        }
    }

    // ── ProposedChangeDelta.compute ────────────────────────────────────────────

    @Nested
    @DisplayName("ProposedChangeDelta.compute helper")
    class ComputeTest {

        @ParameterizedTest(name = "from={0} to={1} → {2}%")
        @CsvSource({
                "4.0,  6.0,  50.0",   //  +50%
                "4.0,  2.0,  50.0",   //  -50% → absolute = 50
                "1024.0, 512.0, 50.0",// -50%
                "100.0, 100.0, 0.0",  //  no change
                "0.0,   50.0,  0.0",  //  from=0 → defined as 0 (div-by-zero guard)
        })
        void computeDelta(double from, double to, double expectedPct) {
            ProposedChangeDelta delta = ProposedChangeDelta.compute("r", from, to);
            assertEquals(expectedPct, delta.absoluteDeltaPct(), 0.001);
        }

        @Test
        @DisplayName("negative absoluteDeltaPct → constructor throws")
        void negativeAbsoluteDelta_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> ProposedChangeDelta.of("r", -1.0));
        }

        @Test
        @DisplayName("blank resource → constructor throws")
        void blankResource_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> ProposedChangeDelta.of("  ", 10.0));
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Creates {@code n} deltas each with the given pct, on resources r0, r1, … */
    private static List<ProposedChangeDelta> buildDeltas(int n, double pctEach) {
        return java.util.stream.IntStream.range(0, n)
                .mapToObj(i -> ProposedChangeDelta.of("resource" + i, pctEach))
                .toList();
    }
}