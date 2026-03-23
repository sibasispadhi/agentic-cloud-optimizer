package com.cloudoptimizer.agent.artifact;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link OptimizationPlan} model hierarchy.
 *
 * <p>Validates default values, factory methods, and builder semantics.
 * No I/O — these are pure in-memory tests.
 */
class OptimizationPlanModelTest {

    // ── PlanMetadata ─────────────────────────────────────────────────────

    @Test
    void metadata_DefaultSchemaVersion() {
        PlanMetadata meta = PlanMetadata.builder()
                .planId("abc-123")
                .generatedAt(Instant.now())
                .agentStrategy("llm")
                .build();

        assertEquals("1.0", meta.getSchemaVersion(),
                "Schema version should default to '1.0'");
    }

    @Test
    void metadata_DefaultServiceLabel() {
        PlanMetadata meta = PlanMetadata.builder()
                .planId("abc-123")
                .generatedAt(Instant.now())
                .agentStrategy("simple")
                .build();

        assertEquals("cloud-optimizer-service", meta.getServiceLabel());
    }

    @Test
    void metadata_CustomServiceLabel() {
        PlanMetadata meta = PlanMetadata.builder()
                .planId("abc-123")
                .generatedAt(Instant.now())
                .agentStrategy("llm")
                .serviceLabel("payment-service")
                .build();

        assertEquals("payment-service", meta.getServiceLabel());
    }

    // ── ExecutionStatus ───────────────────────────────────────────────────

    @Test
    void optimizationPlan_DefaultStatusIsPending() {
        OptimizationPlan plan = OptimizationPlan.builder()
                .metadata(minimalMetadata())
                .build();

        assertEquals(ExecutionStatus.PENDING, plan.getStatus(),
                "A freshly-built plan should be PENDING");
    }

    @Test
    void optimizationPlan_ExplicitStatusOverridesDefault() {
        OptimizationPlan plan = OptimizationPlan.builder()
                .metadata(minimalMetadata())
                .status(ExecutionStatus.VALIDATED)
                .build();

        assertEquals(ExecutionStatus.VALIDATED, plan.getStatus());
    }

    // ── PolicyEvaluationResult ────────────────────────────────────────────

    @Test
    void policyResult_PendingFactoryMethod() {
        PolicyEvaluationResult result = PolicyEvaluationResult.pending();

        assertEquals(PolicyEvaluationResult.Status.PENDING_EVALUATION, result.getStatus());
        assertNull(result.getEvaluatedAt(), "evaluatedAt must be null when pending");
        assertTrue(result.getViolations().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void policyResult_DeniedWithViolations() {
        PolicyEvaluationResult result = PolicyEvaluationResult.builder()
                .status(PolicyEvaluationResult.Status.DENIED)
                .evaluatedAt(Instant.now())
                .violations(List.of("Concurrency increase exceeds safe limit of 50%"))
                .build();

        assertEquals(PolicyEvaluationResult.Status.DENIED, result.getStatus());
        assertEquals(1, result.getViolations().size());
        assertTrue(result.getWarnings().isEmpty(), "No warnings on a DENIED result");
    }

    @Test
    void policyResult_ApprovedWithWarnings() {
        PolicyEvaluationResult result = PolicyEvaluationResult.builder()
                .status(PolicyEvaluationResult.Status.APPROVED_WITH_WARNINGS)
                .evaluatedAt(Instant.now())
                .warnings(List.of("Heap change is large; monitor GC closely"))
                .build();

        assertEquals(PolicyEvaluationResult.Status.APPROVED_WITH_WARNINGS, result.getStatus());
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getViolations().isEmpty());
    }

    // ── ValidationRecipe ──────────────────────────────────────────────────

    @Test
    void validationRecipe_DefaultMethodIsLoadTest() {
        ValidationRecipe recipe = ValidationRecipe.builder()
                .durationSeconds(10)
                .threshold(120.0)
                .build();

        assertEquals("load_test", recipe.getMethod());
    }

    @Test
    void validationRecipe_DefaultTargetMetric() {
        ValidationRecipe recipe = ValidationRecipe.builder()
                .durationSeconds(10)
                .threshold(100.0)
                .build();

        assertEquals("p99_latency_ms", recipe.getTargetMetric());
    }

    @Test
    void validationRecipe_PassedIsNullBeforeValidation() {
        ValidationRecipe recipe = ValidationRecipe.builder()
                .durationSeconds(10)
                .threshold(100.0)
                .build();

        assertNull(recipe.getPassed(), "passed must be null until validation runs");
        assertNull(recipe.getValidatedAt(), "validatedAt must be null until validation runs");
    }

    // ── RollbackRecipe ────────────────────────────────────────────────────

    @Test
    void rollbackRecipe_DefaultIsReversible() {
        RollbackRecipe recipe = RollbackRecipe.builder()
                .restoreParams(Map.of("jvm.concurrency", 4))
                .triggerCondition("p99 > 120ms")
                .build();

        assertTrue(recipe.isReversible(),
                "Plans should default to reversible=true");
    }

    @Test
    void rollbackRecipe_ExecutedAtIsNullIfNotRolledBack() {
        RollbackRecipe recipe = RollbackRecipe.builder()
                .restoreParams(Map.of("jvm.concurrency", 4))
                .triggerCondition("p99 > 120ms")
                .build();

        assertNull(recipe.getExecutedAt(),
                "executedAt must be null until rollback occurs");
    }

    // ── PlanChange ─────────────────────────────────────────────────────────

    @Test
    void planChange_FieldsRoundTrip() {
        PlanChange change = PlanChange.builder()
                .resource("jvm.concurrency")
                .fromValue("4")
                .toValue("8")
                .rationale("High latency detected")
                .confidence(0.9)
                .build();

        assertEquals("jvm.concurrency", change.getResource());
        assertEquals("4", change.getFromValue());
        assertEquals("8", change.getToValue());
        assertEquals(0.9, change.getConfidence(), 0.001);
    }

    // ── PlanEvidence ───────────────────────────────────────────────────────

    @Test
    void planEvidence_NullableConfidenceFieldsAreOptional() {
        PlanEvidence ev = PlanEvidence.builder()
                .agentType("simple")
                .recommendation("Maintain concurrency")
                .reasoning("Latency within bounds")
                .confidenceScore(0.75)
                .impactLevel("LOW")
                .sloBreached(false)
                .build();

        assertNull(ev.getConcurrencyConfidence(),
                "concurrencyConfidence should be null when not set");
        assertNull(ev.getHeapConfidence(),
                "heapConfidence should be null when not set");
        assertNull(ev.getBreachReason(),
                "breachReason should be null when not set");
    }

    // ── PlanIntent.Trigger enum ─────────────────────────────────────────────

    @Test
    void planIntent_AllTriggersExist() {
        // enum completeness guard — adding a new Trigger requires updating this test
        PlanIntent.Trigger[] triggers = PlanIntent.Trigger.values();
        assertEquals(3, triggers.length,
                "Expected exactly 3 Trigger values: MANUAL, SCHEDULED, SLO_BREACH");
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private PlanMetadata minimalMetadata() {
        return PlanMetadata.builder()
                .planId("test-plan-id")
                .generatedAt(Instant.now())
                .agentStrategy("simple")
                .build();
    }
}