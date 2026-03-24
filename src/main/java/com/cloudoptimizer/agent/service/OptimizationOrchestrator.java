package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.artifact.OptimizationPlan;
import com.cloudoptimizer.agent.artifact.PlanChange;
import com.cloudoptimizer.agent.artifact.PlanWriter;
import com.cloudoptimizer.agent.artifact.PolicyEvaluationResult;
import com.cloudoptimizer.agent.artifact.RollbackResult;
import com.cloudoptimizer.agent.artifact.ValidationResult;
import com.cloudoptimizer.agent.autonomy.AutonomyConfig;
import com.cloudoptimizer.agent.autonomy.AutonomyGate;
import com.cloudoptimizer.agent.autonomy.AutonomyGateResult;
import com.cloudoptimizer.agent.budget.ActuationBudget;
import com.cloudoptimizer.agent.budget.ActuationBudgetLedger;
import com.cloudoptimizer.agent.budget.BudgetConsumption;
import com.cloudoptimizer.agent.budget.ProposedChangeDelta;
import com.cloudoptimizer.agent.model.*;
import com.cloudoptimizer.agent.policy.*;
import com.cloudoptimizer.agent.simulator.WorkloadSimulator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Service that orchestrates the optimization workflow and emits real-time progress events.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since December 2025
 */
@Service
public class OptimizationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OptimizationOrchestrator.class);

    private final WorkloadSimulator workloadSimulator;
    private final MetricsLogger metricsLogger;
    private final SimpleAgent simpleAgent;
    private final SpringAiLlmAgent llmAgent;
    private final SloBreachDetector sloBreachDetector;
    private final SimpMessagingTemplate messagingTemplate;
    private final PlanWriter planWriter;
    private final PlanAssembler planAssembler;
    private final PolicyEngine policyEngine;
    private final ActuationPolicy actuationPolicy;
    private final ActuationBudgetLedger actuationBudgetLedger;
    private final ActuationBudget actuationBudget;
    private final AutonomyGate autonomyGate;
    private final AutonomyConfig autonomyConfig;
    private final ValidationExecutor validationExecutor;
    private final RollbackExecutor rollbackExecutor;
    private final ReportGenerator reportGenerator;
    private final ObjectMapper objectMapper;
    
    @Value("${baseline.concurrency:4}")
    private int baselineConcurrency;
    
    @Value("${load.duration:10}")
    private int loadDuration;
    
    @Value("${target.rps:0}")
    private double targetRps;
    
    @Value("${agent.strategy:llm}")
    private String agentStrategy;

    @Value("${slo.target-p99-ms:100.0}")
    private double sloTargetP99Ms;

    @Value("${slo.breach-threshold:1.2}")
    private double sloBreachThreshold;

    @Value("${slo.breach-window-intervals:3}")
    private int sloBreachWindowIntervals;

    @Value("${slo.enabled:true}")
    private boolean sloEnabled;

    public OptimizationOrchestrator(WorkloadSimulator workloadSimulator,
                                   MetricsLogger metricsLogger,
                                   SimpleAgent simpleAgent,
                                   SpringAiLlmAgent llmAgent,
                                   SloBreachDetector sloBreachDetector,
                                   SimpMessagingTemplate messagingTemplate,
                                   PlanWriter planWriter,
                                   PlanAssembler planAssembler,
                                   PolicyEngine policyEngine,
                                   ActuationPolicy actuationPolicy,
                                   ActuationBudgetLedger actuationBudgetLedger,
                                   ActuationBudget actuationBudget,
                                   AutonomyGate autonomyGate,
                                   AutonomyConfig autonomyConfig,
                                   ValidationExecutor validationExecutor,
                                   RollbackExecutor rollbackExecutor,
                                   ReportGenerator reportGenerator) {
        this.workloadSimulator = workloadSimulator;
        this.metricsLogger = metricsLogger;
        this.simpleAgent = simpleAgent;
        this.llmAgent = llmAgent;
        this.sloBreachDetector = sloBreachDetector;
        this.messagingTemplate = messagingTemplate;
        this.planWriter = planWriter;
        this.planAssembler = planAssembler;
        this.policyEngine = policyEngine;
        this.actuationPolicy = actuationPolicy;
        this.actuationBudgetLedger = actuationBudgetLedger;
        this.actuationBudget = actuationBudget;
        this.autonomyGate = autonomyGate;
        this.autonomyConfig = autonomyConfig;
        this.validationExecutor = validationExecutor;
        this.rollbackExecutor = rollbackExecutor;
        this.reportGenerator = reportGenerator;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Runs the complete optimization workflow asynchronously with real-time progress updates.
     */
    public CompletableFuture<Map<String, Object>> runOptimization() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeOptimization();
            } catch (Exception e) {
                log.error("Optimization failed", e);
                emitEvent(ProgressEvent.errorEvent("Optimization failed: " + e.getMessage()));
                throw new RuntimeException("Optimization failed", e);
            }
        });
    }

    private Map<String, Object> executeOptimization() throws Exception {
        // Phase 0: Initialize
        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.INITIALIZING, "Initializing optimization workflow..."));
        
        Path artifactsDir = Paths.get("artifacts");
        if (!Files.exists(artifactsDir)) {
            Files.createDirectories(artifactsDir);
        }
        
        // Health check
        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.INITIALIZING, "Performing health check..."));
        if (!workloadSimulator.isHealthy()) {
            throw new IllegalStateException("Simulator health check failed");
        }
        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.INITIALIZING, "Health check passed!"));

        // Phase 1: Baseline Load Test
        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.BASELINE_RUNNING, 
                String.format("Running baseline test with %d threads for %d seconds...", baselineConcurrency, loadDuration)));
        
        RunResult baseline = workloadSimulator.executeLoad(baselineConcurrency, loadDuration, targetRps);
        writeJson(artifactsDir.resolve("baseline.json"), baseline);
        
        // Emit baseline metrics
        emitEvent(ProgressEvent.metricUpdate("Baseline complete",
                reportGenerator.buildRunSnapshot(baseline)));
        
        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.BASELINE_COMPLETE, 
                String.format("Baseline complete! Median latency: %.2fms, RPS: %.0f", 
                        baseline.getMedianLatencyMs(), baseline.getRequestsPerSecond())));

        // Phase 1.5: SLO Breach Detection
        boolean sloBreached = false;
        String breachReason = null;

        if (sloEnabled) {
            emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.SLO_CHECKING,
                    String.format("Checking SLO compliance (p99 target: %.0fms, breach at: %.0fms)...",
                            sloTargetP99Ms, sloTargetP99Ms * sloBreachThreshold)));

            SloPolicy sloPolicy = SloPolicy.builder()
                    .targetP99Ms(sloTargetP99Ms)
                    .breachThreshold(sloBreachThreshold)
                    .breachWindowIntervals(sloBreachWindowIntervals)
                    .description(String.format("p99 < %.0fms (breach at %.0fms over %d intervals)",
                            sloTargetP99Ms, sloTargetP99Ms * sloBreachThreshold, sloBreachWindowIntervals))
                    .build();

            sloBreachDetector.recordMetric(baseline);
            sloBreached = sloBreachDetector.isBreached(sloPolicy);
            breachReason = sloBreachDetector.getBreachReason();

            if (sloBreached) {
                emitEvent(ProgressEvent.reasoningUpdate(
                        String.format("🚨 SLO BREACH DETECTED! p99=%.2fms exceeds %.0fms threshold",
                                baseline.getP99LatencyMs(), sloTargetP99Ms * sloBreachThreshold)));
                emitEvent(ProgressEvent.reasoningUpdate("→ Triggering autonomous agent for root cause analysis"));
                log.warn("SLO breach detected: {}", breachReason);
            } else {
                emitEvent(ProgressEvent.reasoningUpdate(
                        String.format("✅ SLO compliant: p99=%.2fms (target: <%.0fms)",
                                baseline.getP99LatencyMs(), sloTargetP99Ms * sloBreachThreshold)));
                log.info("SLO compliant: p99={}ms", baseline.getP99LatencyMs());
            }
        } else {
            emitEvent(ProgressEvent.reasoningUpdate("ℹ️ SLO breach detection disabled"));
        }

        // Capture SLO state for inclusion in decision
        final boolean finalSloBreached = sloBreached;
        final String finalBreachReason = breachReason;

        // Phase 2: LLM Analysis
        Thread.sleep(1000); // Wait for metrics to flush
        
        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.LLM_ANALYZING, "AI is analyzing performance metrics..."));
        emitEvent(ProgressEvent.reasoningUpdate("🧠 Reading recent metrics..."));
        
        List<MetricRow> recentMetrics = metricsLogger.readRecent(20);
        emitEvent(ProgressEvent.reasoningUpdate(String.format("📊 Analyzing %d metric samples", recentMetrics.size())));
        
        AgentDecision decision;
        if ("llm".equalsIgnoreCase(agentStrategy)) {
            emitEvent(ProgressEvent.reasoningUpdate("🤖 LLM Agent evaluating patterns (including heap metrics)..."));
            decision = llmAgent.decideWithHeapAnalysis(baseline, baselineConcurrency);
        } else {
            emitEvent(ProgressEvent.reasoningUpdate("⚙️ Simple Agent applying rules (including heap analysis)..."));
            decision = simpleAgent.decideWithHeapAnalysis(baseline, baselineConcurrency);
        }

        // Emit reasoning
        emitEvent(ProgressEvent.reasoningUpdate("💡 " + decision.getReasoning()));
        emitEvent(ProgressEvent.reasoningUpdate(String.format("✨ Confidence: %.0f%%", decision.getConfidenceScore() * 100)));
        
        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.LLM_COMPLETE, 
                "AI Decision: " + decision.getRecommendation()));

        // Extract new concurrency
        int newConcurrency = extractConcurrency(decision.getRecommendation(), baselineConcurrency);
        emitEvent(ProgressEvent.reasoningUpdate(
                String.format("🎯 Applying recommendation: %d → %d threads", baselineConcurrency, newConcurrency)));

        // Phase 2.4: Autonomy Gate
        AutonomyGateResult autonomyDecision = autonomyGate.evaluate(decision, autonomyConfig);
        emitEvent(ProgressEvent.reasoningUpdate(
                String.format("🤖 Autonomy gate: mode=%s → %s",
                        autonomyDecision.mode(), autonomyDecision.outcome())));

        if (!autonomyDecision.actuationPermitted()) {
            emitEvent(ProgressEvent.reasoningUpdate(
                    "🔵 Autonomy gate blocked actuation: " + autonomyDecision.reason()));
            log.info("Autonomy gate blocked actuation: mode={} outcome={}",
                    autonomyDecision.mode(), autonomyDecision.outcome());

            OptimizationPlan advisoryPlan = planAssembler.buildAdvisoryPlan(
                    baseline, decision, agentStrategy,
                    finalSloBreached, finalBreachReason, autonomyDecision);
            planWriter.write(advisoryPlan, artifactsDir);

            Map<String, Object> advisoryReport = new HashMap<>();
            advisoryReport.put("status", "advisory");
            advisoryReport.put("autonomy_mode", autonomyDecision.mode().name());
            advisoryReport.put("autonomy_outcome", autonomyDecision.outcome().name());
            advisoryReport.put("reason", autonomyDecision.reason());
            advisoryReport.put("recommendation", decision.getRecommendation());
            advisoryReport.put("confidence", decision.getConfidenceScore());
            advisoryReport.put("agent_strategy", agentStrategy);
            emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.COMPLETE,
                    "Run completed in advisory mode: " + autonomyDecision.mode()));
            emitEvent(ProgressEvent.completeEvent(advisoryReport));
            return advisoryReport;
        }

        // Phase 2.5: Policy Evaluation
        Set<String> proposedResources = buildProposedResources(decision);
        PolicyContext policyContext = PolicyContext.builder()
                .baseline(baseline)
                .proposedConcurrency(newConcurrency)
                .proposedHeapSizeMb(decision.getRecommendedHeapSizeMb())
                .confidenceScore(decision.getConfidenceScore())
                .impactLevel(decision.getImpactLevel() != null ? decision.getImpactLevel().name() : null)
                .proposedResources(proposedResources)
                .build();

        PolicyDecision policyDecision = policyEngine.evaluate(policyContext, actuationPolicy);
        PolicyEvaluationResult policyResult = policyDecision.toPolicyEvaluationResult();

        if (policyDecision.isDenied() || policyDecision.requiresApproval()) {
            String reason = policyDecision.isDenied() ? "DENIED" : "REQUIRES_APPROVAL";
            emitEvent(ProgressEvent.reasoningUpdate(
                    String.format("🚫 Policy gate: %s — skipping actuation", reason)));
            policyDecision.violations().forEach(v ->
                    emitEvent(ProgressEvent.reasoningUpdate("  ✗ " + v.message())));
            policyDecision.warnings().forEach(w ->
                    emitEvent(ProgressEvent.reasoningUpdate("  ⚠ " + w.message())));
            log.warn("Policy gate blocked execution: outcome={}", policyDecision.outcome());

            OptimizationPlan blockedPlan = planAssembler.buildBlockedPlan(
                    baseline, decision, agentStrategy,
                    finalSloBreached, finalBreachReason, policyResult, null, autonomyDecision);
            planWriter.write(blockedPlan, artifactsDir);

            Map<String, Object> blockedReport = new HashMap<>();
            blockedReport.put("status", "blocked");
            blockedReport.put("policy_outcome", reason);
            blockedReport.put("violations", policyDecision.violations().stream()
                    .map(PolicyViolation::message).toList());
            blockedReport.put("warnings", policyDecision.warnings().stream()
                    .map(PolicyWarning::message).toList());
            blockedReport.put("agent_strategy", agentStrategy);
            emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.COMPLETE,
                    "Run blocked by policy: " + reason));
            emitEvent(ProgressEvent.completeEvent(blockedReport));
            return blockedReport;
        }

        if (!policyDecision.warnings().isEmpty()) {
            emitEvent(ProgressEvent.reasoningUpdate("⚠️  Policy warnings (proceeding with caution):"));
            policyDecision.warnings().forEach(w ->
                    emitEvent(ProgressEvent.reasoningUpdate("  ⚠ " + w.message())));
        }

        // Phase 2.6: Actuation Budget Gate
        List<ProposedChangeDelta> proposedDeltas = buildProposedDeltas(baseline, newConcurrency, decision);
        BudgetConsumption budgetConsumption = actuationBudgetLedger.evaluate(proposedDeltas, actuationBudget);

        if (!budgetConsumption.withinBudget()) {
            emitEvent(ProgressEvent.reasoningUpdate(
                    "💰 Budget gate: EXCEEDED — " + budgetConsumption.denialReason()));
            emitEvent(ProgressEvent.reasoningUpdate(
                    String.format("   changes: %d/%d  totalDelta: %.1f%%/%.1f%%",
                            budgetConsumption.changesAttempted(), budgetConsumption.maxChangesPerRun(),
                            budgetConsumption.totalDeltaPctConsumed(), budgetConsumption.maxTotalDeltaPct())));
            log.warn("Budget gate blocked execution: {}", budgetConsumption.denialReason());

            OptimizationPlan budgetBlockedPlan = planAssembler.buildBlockedPlan(
                    baseline, decision, agentStrategy,
                    finalSloBreached, finalBreachReason, policyResult, budgetConsumption, autonomyDecision);
            planWriter.write(budgetBlockedPlan, artifactsDir);

            Map<String, Object> budgetReport = new HashMap<>();
            budgetReport.put("status", "blocked");
            budgetReport.put("policy_outcome", "BUDGET_EXCEEDED");
            budgetReport.put("budget_denial_reason", budgetConsumption.denialReason());
            budgetReport.put("changes_attempted", budgetConsumption.changesAttempted());
            budgetReport.put("max_changes_per_run", budgetConsumption.maxChangesPerRun());
            budgetReport.put("total_delta_pct", budgetConsumption.totalDeltaPctConsumed());
            budgetReport.put("max_total_delta_pct", budgetConsumption.maxTotalDeltaPct());
            budgetReport.put("agent_strategy", agentStrategy);
            emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.COMPLETE,
                    "Run blocked by actuation budget"));
            emitEvent(ProgressEvent.completeEvent(budgetReport));
            return budgetReport;
        }

        emitEvent(ProgressEvent.reasoningUpdate(
                String.format("💰 Budget gate: OK — %d change(s), %.1f%% total delta (limit: %.1f%%)",
                        budgetConsumption.changesAttempted(),
                        budgetConsumption.totalDeltaPctConsumed(),
                        budgetConsumption.maxTotalDeltaPct())));

        // Phase 3: Post-Optimization Load Test
        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.OPTIMIZATION_RUNNING, 
                String.format("Running optimized test with %d threads...", newConcurrency)));
        
        RunResult after = workloadSimulator.executeLoad(newConcurrency, loadDuration, targetRps);
        writeJson(artifactsDir.resolve("after.json"), after);
        
        emitEvent(ProgressEvent.metricUpdate("Optimization complete",
                reportGenerator.buildRunSnapshot(after)));
        
        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.OPTIMIZATION_COMPLETE, 
                String.format("Optimized test complete! Median latency: %.2fms, RPS: %.0f", 
                        after.getMedianLatencyMs(), after.getRequestsPerSecond())));

        // Record post-optimization metric for ongoing SLO monitoring
        if (sloEnabled) {
            sloBreachDetector.recordMetric(after);
        }

        // Phase 4.5: Validation + Rollback (Phase 5 of the implementation plan)
        double sloThresholdMs = sloTargetP99Ms * sloBreachThreshold;
        ValidationResult validationResult = validationExecutor.validate(after, sloThresholdMs);
        emitEvent(ProgressEvent.reasoningUpdate(
                String.format("✅ Validation: %s — p99=%.1fms threshold=%.1fms",
                        validationResult.status(), validationResult.measuredP99Ms(), sloThresholdMs)));

        List<PlanChange> proposedChanges = planAssembler.buildProposedChanges(baseline, decision);
        RollbackResult rollbackResult = rollbackExecutor.maybeRollback(
                validationResult, proposedChanges,
                baselineConcurrency, loadDuration, targetRps);
        if (rollbackResult.status() != RollbackResult.RollbackStatus.SKIPPED_VALIDATION_PASSED) {
            emitEvent(ProgressEvent.reasoningUpdate(
                    String.format("🔄 Rollback: %s — %s",
                            rollbackResult.status(), rollbackResult.reason())));
        }

        // Phase 4: Generate Report
        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.GENERATING_REPORT, "Generating comparison report..."));
        
        Map<String, Object> report = reportGenerator.generateReport(baseline, after, decision,
                agentStrategy, finalSloBreached, finalBreachReason);
        writeJson(artifactsDir.resolve("report.json"), report);

        // Emit OptimizationPlan artifact (single source of truth for this run)
        OptimizationPlan plan = planAssembler.buildPlan(baseline, after, decision, agentStrategy,
                finalSloBreached, finalBreachReason, policyResult, budgetConsumption, autonomyDecision,
                validationResult, rollbackResult);
        planWriter.write(plan, artifactsDir);

        emitEvent(ProgressEvent.phaseUpdate(OptimizationPhase.COMPLETE, "Optimization complete!"));
        emitEvent(ProgressEvent.completeEvent(report));
        
        return report;
    }

    private void emitEvent(ProgressEvent event) {
        messagingTemplate.convertAndSend("/topic/optimization-progress", event);
        log.debug("Emitted event: {} - {}", event.getEventType(), event.getMessage());
    }

    private int extractConcurrency(String recommendation, int defaultValue) {
        try {
            String[] parts = recommendation.toLowerCase().split("concurrency to ");
            if (parts.length > 1) {
                String numberPart = parts[1].split("\\D")[0];
                return Integer.parseInt(numberPart);
            }
        } catch (Exception e) {
            log.warn("Could not extract concurrency from recommendation, using default");
        }
        return defaultValue;
    }


    private void writeJson(Path path, Object obj) throws IOException {
        objectMapper.writeValue(path.toFile(), obj);
        log.debug("Written: {}", path);
    }

    /** Builds the set of resource identifiers the agent proposes to change. */
    /**
     * Computes the per-resource absolute delta percentages for the proposed changes.
     * Used as input to the actuation budget gate.
     */
    private List<ProposedChangeDelta> buildProposedDeltas(RunResult baseline,
                                                          int proposedConcurrency,
                                                          AgentDecision decision) {
        List<ProposedChangeDelta> deltas = new java.util.ArrayList<>();

        if (proposedConcurrency != baseline.getConcurrency()) {
            deltas.add(ProposedChangeDelta.compute(
                    "jvm.concurrency",
                    baseline.getConcurrency(),
                    proposedConcurrency));
        }

        if (decision.getRecommendedHeapSizeMb() != null && baseline.getHeapMetrics() != null) {
            deltas.add(ProposedChangeDelta.compute(
                    "jvm.heap_size_mb",
                    baseline.getHeapMetrics().getHeapSizeMb(),
                    decision.getRecommendedHeapSizeMb()));
        }

        return deltas;
    }

    private Set<String> buildProposedResources(AgentDecision decision) {
        Set<String> resources = new HashSet<>();
        int proposed = extractConcurrency(decision.getRecommendation(), baselineConcurrency);
        if (proposed != baselineConcurrency) {
            resources.add("jvm.concurrency");
        }
        if (decision.getRecommendedHeapSizeMb() != null) {
            resources.add("jvm.heap_size_mb");
        }
        return resources;
    }
}
