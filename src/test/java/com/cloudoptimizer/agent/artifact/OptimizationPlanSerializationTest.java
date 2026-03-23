package com.cloudoptimizer.agent.artifact;

import com.cloudoptimizer.agent.model.HeapMetrics;
import com.cloudoptimizer.agent.model.RunResult;
import org.junit.jupiter.api.BeforeEach;
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
 * Verifies that {@link OptimizationPlan} and all its nested models
 * can be serialized to JSON + YAML and deserialized back without
 * losing any data (round-trip fidelity).
 */
class OptimizationPlanSerializationTest {

    @TempDir
    Path tempDir;

    private PlanWriter writer;
    private OptimizationPlan samplePlan;

    @BeforeEach
    void setUp() {
        writer = new PlanWriter();
        samplePlan = buildSamplePlan();
    }

    // ── JSON round-trip ───────────────────────────────────────────────────

    @Test
    void testJsonRoundTrip_MetadataPreserved() throws Exception {
        Path jsonFile = tempDir.resolve("plan.json");
        writer.writeJson(samplePlan, jsonFile);

        OptimizationPlan loaded = writer.readJson(jsonFile);

        assertEquals(samplePlan.getMetadata().getPlanId(), loaded.getMetadata().getPlanId());
        assertEquals(samplePlan.getMetadata().getAgentStrategy(), loaded.getMetadata().getAgentStrategy());
        assertEquals(samplePlan.getMetadata().getSchemaVersion(), loaded.getMetadata().getSchemaVersion());
        assertEquals(samplePlan.getMetadata().getServiceLabel(), loaded.getMetadata().getServiceLabel());
    }

    @Test
    void testJsonRoundTrip_IntentPreserved() throws Exception {
        Path jsonFile = tempDir.resolve("plan.json");
        writer.writeJson(samplePlan, jsonFile);

        OptimizationPlan loaded = writer.readJson(jsonFile);

        assertEquals(samplePlan.getIntent().getTrigger(), loaded.getIntent().getTrigger());
        assertEquals(samplePlan.getIntent().getTargetLatencyMs(), loaded.getIntent().getTargetLatencyMs(), 0.001);
        assertEquals(samplePlan.getIntent().getBaselineConcurrency(), loaded.getIntent().getBaselineConcurrency());
    }

    @Test
    void testJsonRoundTrip_BaselineSnapshotPreserved() throws Exception {
        Path jsonFile = tempDir.resolve("plan.json");
        writer.writeJson(samplePlan, jsonFile);

        OptimizationPlan loaded = writer.readJson(jsonFile);
        RunResult baseline = loaded.getBaselineSnapshot();

        assertNotNull(baseline);
        assertEquals(4, baseline.getConcurrency());
        assertEquals(80.0, baseline.getMedianLatencyMs(), 0.001);
        assertEquals(500.0, baseline.getRequestsPerSecond(), 0.001);
    }

    @Test
    void testJsonRoundTrip_ChangesPreserved() throws Exception {
        Path jsonFile = tempDir.resolve("plan.json");
        writer.writeJson(samplePlan, jsonFile);

        OptimizationPlan loaded = writer.readJson(jsonFile);

        assertEquals(1, loaded.getChanges().size());
        PlanChange change = loaded.getChanges().get(0);
        assertEquals("jvm.concurrency", change.getResource());
        assertEquals("4", change.getFromValue());
        assertEquals("6", change.getToValue());
        assertEquals(0.85, change.getConfidence(), 0.001);
    }

    @Test
    void testJsonRoundTrip_EvidencePreserved() throws Exception {
        Path jsonFile = tempDir.resolve("plan.json");
        writer.writeJson(samplePlan, jsonFile);

        OptimizationPlan loaded = writer.readJson(jsonFile);
        PlanEvidence ev = loaded.getEvidence();

        assertEquals("simple", ev.getAgentType());
        assertEquals(0.85, ev.getConfidenceScore(), 0.001);
        assertFalse(ev.isSloBreached());
        assertNull(ev.getBreachReason());
    }

    @Test
    void testJsonRoundTrip_PolicyResultPreserved() throws Exception {
        Path jsonFile = tempDir.resolve("plan.json");
        writer.writeJson(samplePlan, jsonFile);

        OptimizationPlan loaded = writer.readJson(jsonFile);

        assertEquals(PolicyEvaluationResult.Status.PENDING_EVALUATION,
                loaded.getPolicyResult().getStatus());
        assertTrue(loaded.getPolicyResult().getViolations().isEmpty());
        assertTrue(loaded.getPolicyResult().getWarnings().isEmpty());
    }

    @Test
    void testJsonRoundTrip_ValidationRecipePreserved() throws Exception {
        Path jsonFile = tempDir.resolve("plan.json");
        writer.writeJson(samplePlan, jsonFile);

        OptimizationPlan loaded = writer.readJson(jsonFile);
        ValidationRecipe vr = loaded.getValidationRecipe();

        assertEquals("load_test", vr.getMethod());
        assertEquals(10, vr.getDurationSeconds());
        assertEquals("p99_latency_ms", vr.getTargetMetric());
        assertEquals(120.0, vr.getThreshold(), 0.001);
        assertTrue(vr.getPassed());
    }

    @Test
    void testJsonRoundTrip_RollbackRecipePreserved() throws Exception {
        Path jsonFile = tempDir.resolve("plan.json");
        writer.writeJson(samplePlan, jsonFile);

        OptimizationPlan loaded = writer.readJson(jsonFile);
        RollbackRecipe rb = loaded.getRollbackRecipe();

        assertTrue(rb.isReversible());
        assertNotNull(rb.getRestoreParams());
        assertEquals("4", rb.getRestoreParams().get("jvm.concurrency").toString());
    }

    @Test
    void testJsonRoundTrip_StatusPreserved() throws Exception {
        Path jsonFile = tempDir.resolve("plan.json");
        writer.writeJson(samplePlan, jsonFile);

        OptimizationPlan loaded = writer.readJson(jsonFile);

        assertEquals(ExecutionStatus.VALIDATED, loaded.getStatus());
    }

    // ── YAML round-trip ───────────────────────────────────────────────────

    @Test
    void testYamlRoundTrip_MetadataPreserved() throws Exception {
        Path yamlFile = tempDir.resolve("plan.yaml");
        writer.writeYaml(samplePlan, yamlFile);

        OptimizationPlan loaded = writer.readYaml(yamlFile);

        assertEquals(samplePlan.getMetadata().getPlanId(), loaded.getMetadata().getPlanId());
        assertEquals(samplePlan.getMetadata().getSchemaVersion(), loaded.getMetadata().getSchemaVersion());
    }

    @Test
    void testYamlRoundTrip_ChangesPreserved() throws Exception {
        Path yamlFile = tempDir.resolve("plan.yaml");
        writer.writeYaml(samplePlan, yamlFile);

        OptimizationPlan loaded = writer.readYaml(yamlFile);

        assertEquals(1, loaded.getChanges().size());
        assertEquals("jvm.concurrency", loaded.getChanges().get(0).getResource());
    }

    @Test
    void testYamlRoundTrip_StatusPreserved() throws Exception {
        Path yamlFile = tempDir.resolve("plan.yaml");
        writer.writeYaml(samplePlan, yamlFile);

        OptimizationPlan loaded = writer.readYaml(yamlFile);

        assertEquals(ExecutionStatus.VALIDATED, loaded.getStatus());
    }

    // ── PlanWriter.write() (both formats) ────────────────────────────────────

    @Test
    void testWrite_CreatesBothFiles() throws Exception {
        writer.write(samplePlan, tempDir);

        assertTrue(Files.exists(tempDir.resolve(PlanWriter.JSON_FILENAME)),
                "JSON plan file should be created");
        assertTrue(Files.exists(tempDir.resolve(PlanWriter.YAML_FILENAME)),
                "YAML plan file should be created");
    }

    @Test
    void testWrite_CreatesDirectoryIfMissing() throws Exception {
        Path nested = tempDir.resolve("deep").resolve("nested");
        assertFalse(Files.exists(nested));

        writer.write(samplePlan, nested);

        assertTrue(Files.exists(nested));
        assertTrue(Files.exists(nested.resolve(PlanWriter.JSON_FILENAME)));
    }

    @Test
    void testWrite_JsonFileIsReadableAfterWrite() throws Exception {
        writer.write(samplePlan, tempDir);

        OptimizationPlan loaded = writer.readJson(tempDir.resolve(PlanWriter.JSON_FILENAME));
        assertEquals(samplePlan.getMetadata().getPlanId(), loaded.getMetadata().getPlanId());
    }

    @Test
    void testWrite_YamlFileIsReadableAfterWrite() throws Exception {
        writer.write(samplePlan, tempDir);

        OptimizationPlan loaded = writer.readYaml(tempDir.resolve(PlanWriter.YAML_FILENAME));
        assertEquals(samplePlan.getMetadata().getPlanId(), loaded.getMetadata().getPlanId());
    }

    @Test
    void testWrite_YamlFileContainsNoDocumentStartMarker() throws Exception {
        writer.write(samplePlan, tempDir);

        String yamlContent = Files.readString(tempDir.resolve(PlanWriter.YAML_FILENAME));
        assertFalse(yamlContent.startsWith("---"),
                "YAML should not begin with document-start marker");
    }

    // ── Field content spot-checks ──────────────────────────────────────────────

    @Test
    void testJsonContent_ContainsPlanId() throws Exception {
        Path jsonFile = tempDir.resolve("plan.json");
        writer.writeJson(samplePlan, jsonFile);

        String content = Files.readString(jsonFile);
        assertTrue(content.contains(samplePlan.getMetadata().getPlanId()),
                "JSON file must contain the plan_id string");
    }

    @Test
    void testJsonContent_ContainsStatusField() throws Exception {
        Path jsonFile = tempDir.resolve("plan.json");
        writer.writeJson(samplePlan, jsonFile);

        String content = Files.readString(jsonFile);
        assertTrue(content.contains("VALIDATED"), "JSON must contain status=VALIDATED");
    }

    // ── Factory helpers ─────────────────────────────────────────────────────────

    private OptimizationPlan buildSamplePlan() {
        String planId = UUID.randomUUID().toString();

        PlanMetadata metadata = PlanMetadata.builder()
                .planId(planId)
                .generatedAt(Instant.parse("2026-03-23T00:00:00.000Z"))
                .agentStrategy("simple")
                .build();

        PlanIntent intent = PlanIntent.builder()
                .trigger(PlanIntent.Trigger.MANUAL)
                .description("Test optimization run")
                .targetLatencyMs(100.0)
                .workloadDurationSeconds(10)
                .baselineConcurrency(4)
                .build();

        HeapMetrics heap = HeapMetrics.builder()
                .heapSizeMb(512)
                .heapUsedMb(256)
                .heapUsagePercent(50.0)
                .gcCount(3)
                .gcTimeMs(12)
                .gcPauseTimeAvgMs(4.0)
                .gcFrequencyPerSec(0.3)
                .build();

        RunResult baseline = RunResult.builder()
                .timestamp(Instant.parse("2026-03-23T00:00:00.000Z"))
                .concurrency(4)
                .durationSeconds(10)
                .totalRequests(5000)
                .successfulRequests(4990)
                .failedRequests(10)
                .requestsPerSecond(500.0)
                .medianLatencyMs(80.0)
                .avgLatencyMs(85.0)
                .minLatencyMs(10.0)
                .maxLatencyMs(200.0)
                .p95LatencyMs(110.0)
                .p99LatencyMs(95.0)
                .costEstimateUsd(0.42)
                .heapMetrics(heap)
                .build();

        RunResult after = RunResult.builder()
                .timestamp(Instant.parse("2026-03-23T00:01:00.000Z"))
                .concurrency(6)
                .durationSeconds(10)
                .totalRequests(7000)
                .successfulRequests(7000)
                .failedRequests(0)
                .requestsPerSecond(700.0)
                .medianLatencyMs(65.0)
                .avgLatencyMs(68.0)
                .minLatencyMs(8.0)
                .maxLatencyMs(180.0)
                .p95LatencyMs(90.0)
                .p99LatencyMs(98.0)
                .costEstimateUsd(0.55)
                .heapMetrics(heap)
                .build();

        PlanChange change = PlanChange.builder()
                .resource("jvm.concurrency")
                .fromValue("4")
                .toValue("6")
                .rationale("Latency high, increasing concurrency.")
                .confidence(0.85)
                .build();

        PlanEvidence evidence = PlanEvidence.builder()
                .agentType("simple")
                .recommendation("Set concurrency to 6")
                .reasoning("Latency high, increasing concurrency.")
                .confidenceScore(0.85)
                .impactLevel("MEDIUM")
                .sloBreached(false)
                .build();

        ValidationRecipe validation = ValidationRecipe.builder()
                .durationSeconds(10)
                .threshold(120.0)
                .passed(true)
                .validatedAt(Instant.parse("2026-03-23T00:01:30.000Z"))
                .build();

        RollbackRecipe rollback = RollbackRecipe.builder()
                .restoreParams(Map.of("jvm.concurrency", 4))
                .triggerCondition("p99 latency exceeds 120ms after optimization")
                .build();

        return OptimizationPlan.builder()
                .metadata(metadata)
                .intent(intent)
                .baselineSnapshot(baseline)
                .changes(List.of(change))
                .evidence(evidence)
                .policyResult(PolicyEvaluationResult.pending())
                .validationRecipe(validation)
                .rollbackRecipe(rollback)
                .optimizedSnapshot(after)
                .status(ExecutionStatus.VALIDATED)
                .build();
    }
}