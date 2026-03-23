package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.artifact.PlanChange;
import com.cloudoptimizer.agent.artifact.RollbackResult;
import com.cloudoptimizer.agent.artifact.ValidationResult;
import com.cloudoptimizer.agent.model.RunResult;
import com.cloudoptimizer.agent.simulator.WorkloadSimulator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RollbackExecutor}.
 *
 * <p>Uses hand-rolled stubs instead of Mockito because Mockito's ByteBuddy
 * instrumentation cannot reliably mock package-private or sealed interfaces
 * across JVM configurations.
 */
class RollbackExecutorTest {

    // ── Stubs ─────────────────────────────────────────────────────────────────

    private static final RunResult STUB_RESULT = RunResult.builder()
            .concurrency(4).p99LatencyMs(70).medianLatencyMs(35).requestsPerSecond(100)
            .build();

    /** Stub simulator that records the call arguments and returns STUB_RESULT. */
    private static class RecordingSimulator implements WorkloadSimulator {
        final AtomicInteger callCount = new AtomicInteger();
        final AtomicReference<int[]> lastArgs = new AtomicReference<>();

        @Override
        public RunResult executeLoad(int concurrency, int durationSeconds, double targetRps) {
            callCount.incrementAndGet();
            lastArgs.set(new int[]{concurrency, durationSeconds});
            return STUB_RESULT;
        }
        @Override public String getName() { return "recording"; }
        @Override public boolean isHealthy() { return true; }
    }

    /** Stub simulator that throws on executeLoad. */
    private static class FailingSimulator implements WorkloadSimulator {
        @Override
        public RunResult executeLoad(int concurrency, int durationSeconds, double targetRps) {
            throw new RuntimeException("simulator exploded");
        }
        @Override public String getName() { return "failing"; }
        @Override public boolean isHealthy() { return false; }
    }

    /** Stub simulator that asserts it's never called. */
    private static class NeverCalledSimulator implements WorkloadSimulator {
        @Override
        public RunResult executeLoad(int concurrency, int durationSeconds, double targetRps) {
            throw new AssertionError("executeLoad must not be called");
        }
        @Override public String getName() { return "never-called"; }
        @Override public boolean isHealthy() { return true; }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ValidationResult passed() { return ValidationResult.passed(80.0, 100.0); }
    private static ValidationResult failed() { return ValidationResult.failed(130.0, 100.0); }

    private static PlanChange reversible(String resource) {
        return PlanChange.builder().resource(resource)
                .fromValue("4").toValue("6").rationale("test").confidence(0.9)
                .reversible(true).build();
    }

    private static PlanChange nonReversible(String resource) {
        return PlanChange.builder().resource(resource)
                .fromValue("4").toValue("6").rationale("test").confidence(0.9)
                .reversible(false).build();
    }

    // ── Validation passed ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("When validation passes")
    class ValidationPassedTest {

        @Test
        @DisplayName("returns SKIPPED_VALIDATION_PASSED")
        void returns_skippedValidationPassed() {
            var sim = new NeverCalledSimulator();
            RollbackResult r = new RollbackExecutor(sim).maybeRollback(
                    passed(), List.of(reversible("jvm.concurrency")), 4, 10, 0);
            assertEquals(RollbackResult.RollbackStatus.SKIPPED_VALIDATION_PASSED, r.status());
            assertNull(r.triggeredAt());
        }

        @Test
        @DisplayName("does not invoke workloadSimulator")
        void doesNotInvokeSimulator() {
            var sim = new NeverCalledSimulator();
            // NeverCalledSimulator throws if called — pass means it was not called
            assertDoesNotThrow(() -> new RollbackExecutor(sim).maybeRollback(
                    passed(), List.of(reversible("jvm.concurrency")), 4, 10, 0));
        }
    }

    // ── Validation failed + all reversible ────────────────────────────────────

    @Nested
    @DisplayName("When validation fails and all changes are reversible")
    class ValidationFailedReversibleTest {

        @Test
        @DisplayName("returns EXECUTED")
        void returns_executed() {
            var sim = new RecordingSimulator();
            RollbackResult r = new RollbackExecutor(sim).maybeRollback(
                    failed(), List.of(reversible("jvm.concurrency")), 4, 10, 0);
            assertEquals(RollbackResult.RollbackStatus.EXECUTED, r.status());
            assertNotNull(r.triggeredAt());
        }

        @Test
        @DisplayName("invokes workloadSimulator with correct baseline concurrency")
        void invokesSimulatorWithBaselineConcurrency() {
            var sim = new RecordingSimulator();
            new RollbackExecutor(sim).maybeRollback(
                    failed(), List.of(reversible("jvm.concurrency")), 4, 10, 0);
            assertEquals(1, sim.callCount.get());
            assertArrayEquals(new int[]{4, 10}, sim.lastArgs.get());
        }

        @Test
        @DisplayName("empty change list → EXECUTED (no non-reversible blockers)")
        void emptyList_executed() {
            var sim = new RecordingSimulator();
            RollbackResult r = new RollbackExecutor(sim).maybeRollback(
                    failed(), List.of(), 4, 10, 0);
            assertEquals(RollbackResult.RollbackStatus.EXECUTED, r.status());
        }

        @Test
        @DisplayName("simulator failure still returns EXECUTED (rollback attempted)")
        void simulatorFailure_stillReturnsExecuted() {
            RollbackResult r = new RollbackExecutor(new FailingSimulator()).maybeRollback(
                    failed(), List.of(reversible("jvm.concurrency")), 4, 10, 0);
            assertEquals(RollbackResult.RollbackStatus.EXECUTED, r.status());
            assertTrue(r.reason().contains("simulator exploded") ||
                       r.reason().contains("failed") ||
                       r.reason().length() > 0);
        }
    }

    // ── Validation failed + non-reversible change ─────────────────────────────

    @Nested
    @DisplayName("When validation fails and at least one change is non-reversible")
    class ValidationFailedNonReversibleTest {

        @Test
        @DisplayName("returns NOT_APPLICABLE_NON_REVERSIBLE")
        void returns_notApplicable() {
            var sim = new NeverCalledSimulator();
            RollbackResult r = new RollbackExecutor(sim).maybeRollback(
                    failed(), List.of(nonReversible("schema.migration")), 4, 10, 0);
            assertEquals(RollbackResult.RollbackStatus.NOT_APPLICABLE_NON_REVERSIBLE, r.status());
            assertNotNull(r.reason());
        }

        @Test
        @DisplayName("does not invoke workloadSimulator")
        void doesNotInvokeSimulator() {
            var sim = new NeverCalledSimulator();
            assertDoesNotThrow(() -> new RollbackExecutor(sim).maybeRollback(
                    failed(), List.of(nonReversible("schema.migration")), 4, 10, 0));
        }

        @Test
        @DisplayName("mixed reversible+non-reversible → NOT_APPLICABLE (non-reversible wins)")
        void mixed_notApplicableWins() {
            var sim = new NeverCalledSimulator();
            List<PlanChange> mixed = List.of(
                    reversible("jvm.concurrency"),
                    nonReversible("schema.migration"));
            RollbackResult r = new RollbackExecutor(sim).maybeRollback(
                    failed(), mixed, 4, 10, 0);
            assertEquals(RollbackResult.RollbackStatus.NOT_APPLICABLE_NON_REVERSIBLE, r.status());
        }
    }
}