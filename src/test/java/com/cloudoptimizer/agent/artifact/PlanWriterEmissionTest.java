package com.cloudoptimizer.agent.artifact;

import com.cloudoptimizer.agent.model.RunResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that {@link PlanWriter} correctly emits plan files to disk
 * and that the written content is structurally sound.
 *
 * <p>This is an "end of orchestration" slice test: it exercises the same
 * code path as the orchestrator's final persist step, without needing
 * a full Spring context or a live load simulator.
 */
class PlanWriterEmissionTest {

    @TempDir
    Path artifactsDir;

    private final PlanWriter writer = new PlanWriter();

    @Test
    void emit_JsonAndYamlAreBothCreated() throws Exception {
        writer.write(minimalPlan(), artifactsDir);

        assertTrue(Files.exists(artifactsDir.resolve(PlanWriter.JSON_FILENAME)),
                "optimization_plan.json must be created");
        assertTrue(Files.exists(artifactsDir.resolve(PlanWriter.YAML_FILENAME)),
                "optimization_plan.yaml must be created");
    }

    @Test
    void emit_JsonFileIsNonEmpty() throws Exception {
        writer.write(minimalPlan(), artifactsDir);

        long size = Files.size(artifactsDir.resolve(PlanWriter.JSON_FILENAME));
        assertTrue(size > 100, "JSON plan file should contain meaningful content (>100 bytes)");
    }

    @Test
    void emit_YamlFileIsNonEmpty() throws Exception {
        writer.write(minimalPlan(), artifactsDir);

        long size = Files.size(artifactsDir.resolve(PlanWriter.YAML_FILENAME));
        assertTrue(size > 50, "YAML plan file should contain meaningful content (>50 bytes)");
    }

    @Test
    void emit_PlanIdIsPresentInJson() throws Exception {
        OptimizationPlan plan = minimalPlan();
        writer.write(plan, artifactsDir);

        String json = Files.readString(artifactsDir.resolve(PlanWriter.JSON_FILENAME));
        assertTrue(json.contains(plan.getMetadata().getPlanId()),
                "plan_id must appear in emitted JSON");
    }

    @Test
    void emit_StatusPendingInJson() throws Exception {
        OptimizationPlan plan = minimalPlan(); // default status = PENDING
        writer.write(plan, artifactsDir);

        String json = Files.readString(artifactsDir.resolve(PlanWriter.JSON_FILENAME));
        assertTrue(json.contains("PENDING"), "Default status must be serialized as 'PENDING'");
    }

    @Test
    void emit_OverwritesPreviousPlanFile() throws Exception {
        OptimizationPlan first = minimalPlan();
        OptimizationPlan second = minimalPlan(); // different UUID

        writer.write(first, artifactsDir);
        writer.write(second, artifactsDir);

        // Second write overwrites the first
        String json = Files.readString(artifactsDir.resolve(PlanWriter.JSON_FILENAME));
        assertTrue(json.contains(second.getMetadata().getPlanId()),
                "Second write should replace the first plan_id");
    }

    @Test
    void emit_RoundTripIntegrity() throws Exception {
        OptimizationPlan original = minimalPlan();
        writer.write(original, artifactsDir);

        OptimizationPlan reloaded = writer.readJson(
                artifactsDir.resolve(PlanWriter.JSON_FILENAME));

        assertEquals(original.getMetadata().getPlanId(),
                reloaded.getMetadata().getPlanId());
        assertEquals(original.getStatus(), reloaded.getStatus());
        assertEquals(original.getIntent().getTrigger(), reloaded.getIntent().getTrigger());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private OptimizationPlan minimalPlan() {
        RunResult snapshot = RunResult.builder()
                .timestamp(Instant.now())
                .concurrency(4)
                .durationSeconds(10)
                .totalRequests(5000)
                .successfulRequests(5000)
                .failedRequests(0)
                .requestsPerSecond(500.0)
                .medianLatencyMs(80.0)
                .avgLatencyMs(82.0)
                .minLatencyMs(5.0)
                .maxLatencyMs(200.0)
                .p95LatencyMs(110.0)
                .p99LatencyMs(95.0)
                .costEstimateUsd(0.40)
                .build();

        return OptimizationPlan.builder()
                .metadata(PlanMetadata.builder()
                        .planId(UUID.randomUUID().toString())
                        .generatedAt(Instant.now())
                        .agentStrategy("simple")
                        .build())
                .intent(PlanIntent.builder()
                        .trigger(PlanIntent.Trigger.MANUAL)
                        .description("Emission test plan")
                        .targetLatencyMs(100.0)
                        .workloadDurationSeconds(10)
                        .baselineConcurrency(4)
                        .build())
                .baselineSnapshot(snapshot)
                .changes(List.of())
                .evidence(PlanEvidence.builder()
                        .agentType("simple")
                        .recommendation("Maintain concurrency at 4")
                        .reasoning("Within bounds")
                        .confidenceScore(0.75)
                        .impactLevel("LOW")
                        .sloBreached(false)
                        .build())
                .policyResult(PolicyEvaluationResult.pending())
                .validationRecipe(ValidationRecipe.builder()
                        .durationSeconds(10)
                        .threshold(120.0)
                        .build())
                .rollbackRecipe(RollbackRecipe.builder()
                        .restoreParams(Map.of("jvm.concurrency", 4))
                        .triggerCondition("p99 > 120ms")
                        .build())
                .optimizedSnapshot(snapshot)
                .build();
    }
}