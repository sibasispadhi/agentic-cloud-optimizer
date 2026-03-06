# Milestone 1: OptimizationPlan Artifact

**Duration:** 2-3 weeks  
**Release:** v0.3.0-m1  
**Branch:** `feature/m1-optimization-plan`  
**Status:** PLANNING  

---

## Overview

M1 transforms ACO's output from in-memory recommendations + logs into a **first-class, portable artifact** that serves as the single source of truth for all downstream systems.

### Why This Matters

**Current State:**
```
ACO → logs (text)
   → metrics (JSON)
   → recommendations (in-memory objects)
```

Downstream systems must:
- Parse logs
- Integrate custom recommendation objects
- Track results ad-hoc

**M1 Target State:**
```
ACO → OptimizationPlan artifact (YAML/JSON)
   ↓
All downstream systems consume SINGLE artifact
```

**Benefits:**
- GitOps can read the plan
- Policy engine can validate the plan
- Cost tracking can extract cost deltas
- Audit systems can store the plan
- Teams can review the plan before execution

---

## The OptimizationPlan Schema

### Full Example

```yaml
apiVersion: agentic.cloud.io/v1
kind: OptimizationPlan
metadata:
  name: jvm-heap-optimization-prod-20260306
  namespace: default
  timestamp: 2026-03-06T15:50:19Z
  agent: spring-ai-llm-agent
  correlation_id: "opt-9f7c8e5d-4b2a-11ee-be56-0242ac120002"

spec:
  # SECTION 1: Intent
  # What problem are we solving?
  intent:
    objective: reduce_p99_latency_sla_breach
    target_service: payment-api
    slo_reference: payment-api-p99-slo
    description: "Payment API p99 latency breached for 3 consecutive intervals. SLA requires p99 < 100ms."
    
  # SECTION 2: Current State Snapshot
  # Metrics at the time of analysis
  current_state:
    timestamp: 2026-03-06T15:50:00Z
    metrics:
      p99_latency_ms: 150.3
      p95_latency_ms: 142.1
      median_latency_ms: 28.4
      heap_used_mb: 53
      heap_total_mb: 8192
      heap_used_percent: 0.6
      gc_pause_ms: 45.2
      gc_frequency_per_sec: 0.3
      error_rate_percent: 0.05
      throughput_rps: 850
    config:
      jvm:
        heap_mb: 8192
        heap_min_mb: 1024
        threads: 4
        gc_type: G1GC
        max_gc_pause_millis: 100
      kubernetes:
        cpu_request: "2"
        cpu_limit: "4"
        memory_request: "10Gi"
        memory_limit: "12Gi"
      container_runtime:
        image: java:21-openjdk
        jdk_version: "21.0.1"

  # SECTION 3: Proposed Changes
  # Each change is explicit with reasoning
  proposed_changes:
    - id: change-1
      type: jvm_heap
      parameter_name: heap_mb
      current_value: 8192
      proposed_value: 6144
      delta_absolute: -2048
      delta_percent: -25.0
      within_safety_bounds: true  # ±25% constraint
      reasoning: |
        Heap usage is 0.6% (53MB of 8192MB). Over-provisioning causes longer GC 
        pause times. Reducing to 6GB maintains safety margin while decreasing GC pause 
        from 45ms to ~15ms, which should restore p99 < 100ms.
      dependencies: []  # Other changes this depends on
      
    - id: change-2
      type: jvm_threads
      parameter_name: threads
      current_value: 4
      proposed_value: 3
      delta_absolute: -1
      delta_percent: -25.0
      within_safety_bounds: true  # ±2 thread constraint
      reasoning: |
        Thread pool utilization is 12%. Reducing from 4 to 3 threads is safe with 
        expected 10% throughput impact (still 765 RPS, well above required 100 RPS).
      dependencies: []
      
    - id: change-3
      type: k8s_memory_request
      parameter_name: memory_request
      current_value: "10Gi"
      proposed_value: "8Gi"
      delta_percent: -20.0
      within_safety_bounds: true
      reasoning: |
        Heap reduction from 8GB to 6GB allows proportional Kubernetes memory request 
        reduction. Maintains safety margin for memory fragmentation.
      dependencies: ["change-1"]  # Depends on heap reduction

  # SECTION 4: Policy Evaluation
  # Was this plan approved by governance policies?
  policy_evaluation:
    policies_checked:
      - id: production-safety-policy
        name: "Production Safety Policy"
        environment: production
        evaluations:
          - constraint_id: max_change_percent_25
            constraint_name: "Max ±25% parameter change"
            status: PASS
            values: {change_1: 25.0, change_2: 25.0, change_3: 20.0}
            
          - constraint_id: min_heap_floor
            constraint_name: "Heap must be >= 4GB"
            status: PASS
            value: 6144
            
          - constraint_id: max_heap_ceiling
            constraint_name: "Heap must be <= 12GB"
            status: PASS
            value: 6144
            
          - constraint_id: forbidden_gc_change
            constraint_name: "GC type changes forbidden"
            status: PASS
            reason: "No GC type change proposed"
            
    overall_allowed: true
    disallowed_reason: null
    
  # SECTION 5: Evidence
  # Why do we trust this plan?
  evidence:
    confidence_score: 0.92  # 0.0-1.0
    confidence_factors:
      - name: consecutive_breach_detection
        weight: 0.30
        value: "3_consecutive_intervals_breached"
        contribution: +0.25  # confidence contribution
        
      - name: heap_usage_pattern
        weight: 0.40
        value: "0.6_percent_utilization_over_extended_period"
        contribution: +0.35
        
      - name: gc_pause_correlation
        weight: 0.30
        value: "gc_pause_45ms_correlates_with_p99_spike"
        contribution: +0.32
        
    risk_level: LOW
    risk_assessment: |
      Bounded changes (±25%) + pattern observed across multiple intervals + 
      known JVM tuning patterns = low risk. Main risk is transient metric spike 
      (mitigated by consecutive window detection).
      
    alternative_explanations: [
      "Network latency increase (unlikely, latency consistent across services)",
      "Database response time (monitoring shows DB queries < 5ms)"
    ]
    
    before_metrics:
      p99_latency_ms: 150.3
      p95_latency_ms: 142.1
      gc_pause_ms: 45.2
      heap_used_percent: 0.6
      
    expected_after_metrics:
      p99_latency_ms: 62  # ~58% improvement
      p95_latency_ms: 58
      gc_pause_ms: 15
      heap_used_percent: 0.8  # Slightly higher% on smaller heap (expected)
      
  # SECTION 6: Validation Recipe
  # How do we know if this plan worked?
  validation_recipe:
    type: load_test_gate
    description: "Run load test with production-peak workload profile"
    
    workload:
      profile_name: production_peak_simulation
      duration_seconds: 300
      baseline_rps: 850
      peak_rps: 1200
      spike_pattern: "3 spikes of 30s each at 60s, 150s, 240s"
      request_distribution: "80% normal, 20% complex (5MB allocation)"
      
    success_criteria:
      - metric: p99_latency_ms
        operator: LESS_THAN
        threshold: 100
        required: true
        
      - metric: p95_latency_ms
        operator: LESS_THAN
        threshold: 90
        required: false
        
      - metric: error_rate_percent
        operator: LESS_THAN
        threshold: 0.5
        required: true
        
      - metric: gc_pause_ms
        operator: LESS_THAN
        threshold: 25
        required: false
        
    timeout_seconds: 600
    auto_rollback_on_failure: true
    
  # SECTION 7: Rollback Recipe
  # How do we undo this if it fails?
  rollback_recipe:
    type: helm_values_revert
    description: "Revert Helm values to previous version"
    
    previous_state_snapshot:
      jvm:
        heap_mb: 8192
        threads: 4
      kubernetes:
        memory_request: "10Gi"
        
    revert_commands:
      - command: "helm upgrade payment-api ./chart --values values.baseline.yaml"
        expected_duration_seconds: 45
        
    verification_steps:
      - metric: p99_latency_ms
        expected_range: [140, 160]
        wait_seconds: 60
        
      - metric: heap_total_mb
        expected_value: 8192
        wait_seconds: 30

status:
  # Status is OPTIONAL - filled in after plan is created
  phase: CREATED  # CREATED, APPROVED, EXECUTING, COMPLETED, ROLLED_BACK, FAILED
  created_at: 2026-03-06T15:50:19Z
  approved_at: null
  executed_at: null
  completed_at: null
  
  approval:
    approved_by: null  # GitHub username or automation
    approval_comment: null
    approval_pr_url: null
    
  execution:
    execution_duration_seconds: null
    actual_after_metrics: null
    rollback_triggered: false
    rollback_reason: null
    success: false
```

---

## Implementation Tasks

### Task 1: Create OptimizationPlan Model (Day 1-2)

```go
// src/artifacts/plan.go

package artifacts

import (
    "time"
)

type OptimizationPlan struct {
    ApiVersion string
    Kind       string
    Metadata   PlanMetadata
    Spec       PlanSpec
    Status     PlanStatus `yaml:"status,omitempty"`
}

type PlanMetadata struct {
    Name          string
    Namespace     string
    Timestamp     time.Time
    Agent         string
    CorrelationID string
}

type PlanSpec struct {
    Intent             Intent
    CurrentState       CurrentState
    ProposedChanges    []ProposedChange
    PolicyEvaluation   PolicyEvaluation
    Evidence           Evidence
    ValidationRecipe   ValidationRecipe
    RollbackRecipe     RollbackRecipe
}

type Intent struct {
    Objective   string
    TargetService string
    SloReference string
    Description string
}

type CurrentState struct {
    Timestamp time.Time
    Metrics   Metrics
    Config    Config
}

type Metrics struct {
    P99LatencyMs       float64
    P95LatencyMs       float64
    MedianLatencyMs    float64
    HeapUsedMb         float64
    HeapTotalMb        float64
    HeapUsedPercent    float64
    GcPauseMs          float64
    GcFrequencyPerSec  float64
    ErrorRatePercent   float64
    ThroughputRps      float64
}

type Config struct {
    Jvm        JvmConfig
    Kubernetes KubernetesConfig
    Runtime    RuntimeConfig
}

type ProposedChange struct {
    ID               string
    Type             string
    ParameterName    string
    CurrentValue     interface{}
    ProposedValue    interface{}
    DeltaAbsolute    interface{}
    DeltaPercent     float64
    WithinBounds     bool
    Reasoning        string
    Dependencies     []string
}

type PolicyEvaluation struct {
    PoliciesChecked  []PolicyCheck
    OverallAllowed   bool
    DisallowedReason string
}

type Evidence struct {
    ConfidenceScore         float64
    ConfidenceFactors       []ConfidenceFactor
    RiskLevel               string
    RiskAssessment          string
    AlternativeExplanations []string
    BeforeMetrics           Metrics
    ExpectedAfterMetrics    Metrics
}

type ValidationRecipe struct {
    Type                  string
    Description           string
    Workload              WorkloadProfile
    SuccessCriteria       []SuccessCriterion
    TimeoutSeconds        int64
    AutoRollbackOnFailure bool
}

type RollbackRecipe struct {
    Type                     string
    Description              string
    PreviousStateSnapshot    map[string]interface{}
    RevertCommands           []string
    VerificationSteps        []VerificationStep
}

type PlanStatus struct {
    Phase     string
    CreatedAt time.Time
    Approval  ApprovalStatus `yaml:"approval,omitempty"`
    Execution ExecutionStatus `yaml:"execution,omitempty"`
}
```

### Task 2: Create Serialization (Day 2-3)

```go
// src/artifacts/serializer.go

package artifacts

import (
    "fmt"
    "gopkg.in/yaml.v3"
)

func (p *OptimizationPlan) ToYAML() (string, error) {
    data, err := yaml.Marshal(p)
    if err != nil {
        return "", fmt.Errorf("YAML marshaling failed: %w", err)
    }
    return string(data), nil
}

func (p *OptimizationPlan) ToJSON() (string, error) {
    // Similar for JSON
}

func (p *OptimizationPlan) SaveToFile(path string) error {
    yaml, err := p.ToYAML()
    if err != nil {
        return err
    }
    return ioutil.WriteFile(path, []byte(yaml), 0644)
}
```

### Task 3: Refactor Orchestrator (Day 3-5)

```go
// src/core/orchestrator.go (refactored)

func (o *OptimizationOrchestrator) Optimize(
    config WorkloadConfig,
    policy SloPolicy,
) (*artifacts.OptimizationPlan, error) {
    
    // PHASE 1: OBSERVE
    baseline, err := o.runner.Execute(config)
    if err != nil {
        return nil, fmt.Errorf("baseline execution failed: %w", err)
    }
    
    o.detector.RecordMeasurement(baseline)
    
    if !o.detector.IsBreached(policy) {
        // Return a "no action needed" plan
        return createNoActionPlan(baseline, config), nil
    }
    
    // PHASE 2: REASON
    agent := o.selectAgent(baseline, config)
    recommendation, err := agent.Analyze(baseline, config)
    if err != nil {
        return nil, fmt.Errorf("agent analysis failed: %w", err)
    }
    
    if recommendation.IsNoAction() {
        return createNoActionPlan(baseline, config), nil
    }
    
    // PHASE 3: BUILD OPTIMIZATION PLAN
    plan := o.buildPlan(
        baseline,
        config,
        recommendation,
        policy,
        agent.Name(),
    )
    
    // PHASE 4: ENFORCE (experimental - would be done by GitOps in M2)
    optimized := recommendation.Apply(config)
    afterOptimization, err := o.runner.Execute(optimized)
    if err != nil {
        plan.Status.Phase = "FAILED"
        return plan, fmt.Errorf("optimization execution failed: %w", err)
    }
    
    // PHASE 5: VALIDATE
    if o.enforcer.ShouldRollback(baseline, afterOptimization) {
        plan.Status.Phase = "ROLLED_BACK"
        plan.Status.Execution.RollbackTriggered = true
        plan.Status.Execution.RollbackReason = "Performance degraded"
        return plan, nil
    }
    
    // Success
    plan.Status.Phase = "COMPLETED"
    plan.Status.Execution.ActualAfterMetrics = afterOptimization.Metrics
    plan.Status.Execution.Success = true
    
    return plan, nil
}

func (o *OptimizationOrchestrator) buildPlan(
    baseline *RunResult,
    config WorkloadConfig,
    recommendation *Recommendation,
    policy SloPolicy,
    agentName string,
) *artifacts.OptimizationPlan {
    
    plan := &artifacts.OptimizationPlan{
        ApiVersion: "agentic.cloud.io/v1",
        Kind:       "OptimizationPlan",
        Metadata: artifacts.PlanMetadata{
            Name:          generatePlanName(),
            Namespace:     "default",
            Timestamp:     time.Now(),
            Agent:         agentName,
            CorrelationID: uuid.New().String(),
        },
        Spec: artifacts.PlanSpec{
            Intent: artifacts.Intent{
                Objective:     "reduce_p99_latency_sla_breach",
                TargetService: config.ServiceName,
                SloReference:  policy.Name,
                Description:   fmt.Sprintf("%d consecutive SLO breaches detected", 3),
            },
            CurrentState: artifacts.CurrentState{
                Timestamp: time.Now(),
                Metrics:   baseline.Metrics,
                Config:    config.ToConfigArtifact(),
            },
            ProposedChanges: recommendation.ToProposedChanges(),
            PolicyEvaluation: o.policyEngine.Evaluate(
                recommendation,
                config,
            ),
            Evidence: o.generateEvidence(baseline, recommendation),
            ValidationRecipe: artifacts.ValidationRecipe{
                Type:                  "load_test_gate",
                Description:           "Production peak simulation",
                Workload:              defaultWorkloadProfile(),
                SuccessCriteria:       defaultSuccessCriteria(policy),
                TimeoutSeconds:        600,
                AutoRollbackOnFailure: true,
            },
            RollbackRecipe: o.generateRollbackRecipe(config),
        },
        Status: artifacts.PlanStatus{
            Phase:     "CREATED",
            CreatedAt: time.Now(),
        },
    }
    
    return plan
}
```

### Task 4: Save Plans to Disk (Day 5)

```go
// src/core/plan_storage.go

package core

import (
    "fmt"
    "os"
    "path/filepath"
    "time"
)

type PlanStorage struct {
    baseDir string
}

func NewPlanStorage(baseDir string) *PlanStorage {
    return &PlanStorage{baseDir: baseDir}
}

func (ps *PlanStorage) Save(plan *artifacts.OptimizationPlan) error {
    // Create timestamp-based filename
    timestamp := time.Now().Format("20060102-150405")
    filename := fmt.Sprintf("%s-%s-optimization-plan.yaml",
        timestamp,
        plan.Spec.Intent.TargetService,
    )
    
    path := filepath.Join(ps.baseDir, "plans", filename)
    
    // Create directory if not exists
    os.MkdirAll(filepath.Dir(path), 0755)
    
    // Serialize to YAML
    yaml, err := plan.ToYAML()
    if err != nil {
        return fmt.Errorf("serialization failed: %w", err)
    }
    
    // Write to file
    return os.WriteFile(path, []byte(yaml), 0644)
}
```

### Task 5: Unit Tests (Day 5-6)

```go
// src/artifacts/plan_test.go

package artifacts

import (
    "testing"
    "assert"
)

func TestOptimizationPlanYAMLSerialization(t *testing.T) {
    plan := createMockPlan()
    
    yaml, err := plan.ToYAML()
    assert.NoError(t, err)
    assert.NotEmpty(t, yaml)
    assert.Contains(t, yaml, "apiVersion: agentic.cloud.io/v1")
    assert.Contains(t, yaml, "kind: OptimizationPlan")
}

func TestProposedChangeWithinBounds(t *testing.T) {
    change := ProposedChange{
        CurrentValue: 8192,
        ProposedValue: 6144,
        DeltaPercent: -25.0,
    }
    
    assert.True(t, change.DeltaPercent >= -25.0)
    assert.True(t, change.DeltaPercent <= 25.0)
}

func TestPolicyEvaluationPass(t *testing.T) {
    plan := createMockPlan()
    assert.True(t, plan.Spec.PolicyEvaluation.OverallAllowed)
}
```

### Task 6: Example Plans (Day 6)

Create example plans in `examples/plans/`:
- `jvm-heap-optimization.yaml` - heap tuning
- `k8s-request-tuning.yaml` - K8s memory/CPU
- `cost-optimization.yaml` - cost-focused
- `no-action-needed.yaml` - when SLO is healthy

---

## Deliverables

### Code
- [ ] `src/artifacts/plan.go` - OptimizationPlan model
- [ ] `src/artifacts/plan_builder.go` - Builder pattern (optional)
- [ ] `src/artifacts/serializer.go` - YAML/JSON serialization
- [ ] `src/core/plan_storage.go` - File persistence
- [ ] `src/core/orchestrator.go` (refactored) - Emit OptimizationPlan
- [ ] `src/artifacts/plan_test.go` - Unit tests

### Documentation
- [ ] `docs/MILESTONES/M1_IMPLEMENTATION.md` - This file
- [ ] `docs/OptimizationPlan.md` - Schema documentation
- [ ] `docs/PLAN_EXAMPLES.md` - Example plans with explanations

### Examples
- [ ] `examples/plans/jvm-heap-optimization.yaml`
- [ ] `examples/plans/k8s-request-tuning.yaml`
- [ ] `examples/plans/cost-optimization.yaml`
- [ ] `examples/plans/no-action-needed.yaml`

### Git
- [ ] Create branch: `feature/m1-optimization-plan`
- [ ] Commit model + serialization
- [ ] Commit orchestrator refactor
- [ ] Commit tests + examples
- [ ] Create PR, review, merge
- [ ] Tag: `v0.3.0-m1`

### Blog Post
- [ ] Title: "ACO as a Control Plane Component: Introducing OptimizationPlan"
- [ ] Length: ~1,200 words
- [ ] Topics:
  - Why artifacts matter in autonomous operations
  - OptimizationPlan schema overview
  - Example plan walkthrough
  - Integration with downstream systems (teaser for M2)
- [ ] Publish on your blog/Medium
- [ ] Announce on Twitter + LinkedIn

---

## Success Criteria

✅ OptimizationPlan can be serialized to YAML/JSON without errors  
✅ Plan includes all required sections (intent, current state, proposed changes, evidence, etc.)  
✅ Orchestrator refactored to emit plans for all optimization scenarios  
✅ Plans saved to disk with timestamp-based filenames  
✅ Example plans cover different optimization scenarios  
✅ Unit tests verify serialization and structure  
✅ Blog post published with good engagement (50+ reads)  
✅ Feedback from early readers integrated  
✅ v0.3.0-m1 tagged and released  

---

## Timeline

| Day | Task |
|-----|------|
| 1-2 | Model definition + structure |
| 2-3 | Serialization (YAML/JSON) |
| 3-5 | Orchestrator refactor |
| 5 | File storage + examples |
| 6 | Unit tests |
| 7 | Documentation + blog |
| 8-10 | Review, revisions, publish |
| 10 | v0.3.0-m1 release + announce |

---

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Schema design isn't flexible enough | Use map[string]interface{} for extensibility |
| Serialization has edge cases | Test with complex nested configs |
| File storage grows unbounded | Add retention policy (keep last 100 plans) |
| Existing code tightly coupled | Refactor with builder pattern |

---

## What M2 Will Do

Once M1 OptimizationPlan is solid, M2 will:
1. Read the OptimizationPlan artifact
2. Render it to Helm/Kustomize patch
3. Open GitHub PR with plan + evidence
4. Wait for approval (or auto-merge based on policy)

**This is where governance-first becomes reality.**
