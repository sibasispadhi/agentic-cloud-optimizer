# 🏛️ Agent Cloud Optimizer - Architecture

**Version:** 0.2.0  
**Last Updated:** December 30, 2025  
**For:** DevNexus Conference Presentation

---

## 📊 System Overview

```
┌───────────────────────────────────────────────────────────────┐
│          AGENT CLOUD OPTIMIZER v0.2.0                                  │
│     Autonomous AI Agents for Microservice Optimization                │
└───────────────────────────────────────────────────────────────┘
           │
           │  User selects workload simulator & agent strategy
           │
           │
┌──────────▼──────────────────────────────────────────────────┐
│                  ORCHESTRATOR                                        │
│                  (RunnerMain)                                        │
│                                                                      │
│  ● Health check workload simulator                                 │
│  ● Run baseline load test                                          │
│  ● Analyze metrics with selected agent                             │
│  ● Apply optimization decision                                      │
│  ● Run post-optimization test                                       │
│  ● Generate comparison report                                       │
└────────────────────────────────────────────────────────────┘
         │                               │
         │                               │
    ┌────▼─────────────────────────────▼───────────────────┐
    │                                                 │
┌───▼──────────────────────┐     ┌─────▼──────────────────────────┐
│  WORKLOAD SIMULATORS        │     │  DECISION AGENTS                │
│                              │     │                                │
│  🔌 WorkloadSimulator       │     │  🤖 Agent Strategy            │
│     Interface                │     │                                │
│                              │     │  ┌──────────────────────────┐ │
│  ┌──────────────────────┐  │     │  │ SimpleAgent            │ │
│  │ DemoWorkloadSimulator│  │     │  │ (Rule-Based)           │ │
│  │ (Built-in demo)      │  │     │  │                        │ │
│  │ - v0.1.0 compatible  │  │     │  │ ● Threshold analysis  │ │
│  │ - Always healthy     │  │     │  │ ● Deterministic      │ │
│  └──────────────────────┘  │     │  │ ● Fast decisions      │ │
│                              │     │  └──────────────────────────┘ │
│  ┌──────────────────────┐  │     │                                │
│  │ HttpRestWorkload     │  │     │  ┌──────────────────────────┐ │
│  │ Simulator (🆕 v0.2.0)  │  │     │  │ SpringAiLlmAgent       │ │
│  │                      │  │     │  │ (AI-Powered)           │ │
│  │ - External HTTP APIs │  │     │  │                        │ │
│  │ - GET/POST/PUT/DEL   │  │     │  │ ● Local LLM (Ollama)  │ │
│  │ - Headers & body     │  │     │  │ ● Context-aware       │ │
│  │ - Health checks      │  │     │  │ ● Explainable        │ │
│  └──────────────────────┘  │     │  └──────────────────────────┘ │
│                              │     │                                │
│  Future (v0.3.0+):            │     └────────────────────────────────┘
│  - Database queries           │                   │
│  - gRPC services              │                   │
│  - Message queues             │                   │
└──────────────────────────────┘                   │
                                             │
                                             │
                           ┌───────────────▼───────────────────┐
                           │  SUPPORTING SERVICES           │
                           │                                │
                           │  ● MetricsLogger (JSONL)      │
                           │  ● LoadRunner (demo workload) │
                           │  ● Config (YAML)              │
                           │  ● Artifact generation        │
                           └────────────────────────────────┘
```

---

## 🔄 Optimization Flow (Closed-Loop)

```
  START
    │
    ▼
┌───────────────────────────────────────────┐
│ 1. HEALTH CHECK                              │
│    Validate workload simulator is ready      │
│    (HTTP connectivity, config, etc.)         │
└───────────────────────────────────────────┘
    │
    ▼
┌───────────────────────────────────────────┐
│ 2. BASELINE LOAD TEST (OBSERVE)              │
│    Run with current concurrency setting      │
│    Measure: latency, throughput, cost        │
│                                               │
│    Simulator.executeLoad(concurrency, ...)   │
└───────────────────────────────────────────┘
    │
    ▼
┌───────────────────────────────────────────┐
│ 3. METRICS COLLECTION                        │
│    Async logging to JSONL files              │
│    Time-series metric storage                │
└───────────────────────────────────────────┘
    │
    ▼
┌───────────────────────────────────────────┐
│ 4. AGENT ANALYSIS (REASON)                   │
│                                               │
│    SimpleAgent:                              │
│    - Calculate median latency                │
│    - Compare against 100ms target            │
│    - Apply threshold rules                   │
│                                               │
│    OR                                        │
│                                               │
│    LlmAgent:                                 │
│    - Build context prompt                    │
│    - Query local Ollama LLM                  │
│    - Parse structured decision               │
└───────────────────────────────────────────┘
    │
    ▼
┌───────────────────────────────────────────┐
│ 5. DECISION OUTPUT (DECIDE)                  │
│                                               │
│    AgentDecision:                            │
│    - recommendation: "Set concurrency to 6"  │
│    - reasoning: "Latency below target..."    │
│    - confidence: 95%                         │
│    - impact: MEDIUM                          │
└───────────────────────────────────────────┘
    │
    ▼
┌───────────────────────────────────────────┐
│ 6. APPLY OPTIMIZATION (ACT)                  │
│    Extract new concurrency from decision     │
│    Apply configuration change                │
└───────────────────────────────────────────┘
    │
    ▼
┌───────────────────────────────────────────┐
│ 7. POST-OPTIMIZATION TEST (VALIDATE)         │
│    Run with new concurrency                  │
│    Measure: latency, throughput, cost        │
└───────────────────────────────────────────┘
    │
    ▼
┌───────────────────────────────────────────┐
│ 8. GENERATE REPORT                           │
│                                               │
│    Outputs:                                  │
│    - baseline.json                           │
│    - after.json                              │
│    - report.json (comparison)                │
│    - reasoning_trace_{agent}.txt             │
│    - demo.html (visualization)               │
└───────────────────────────────────────────┘
    │
    ▼
   END
```

---

## 🔌 v0.2.0 Plugin Architecture

### WorkloadSimulator Interface

```java
public interface WorkloadSimulator {
    // Execute load test
    RunResult executeLoad(int concurrency, int durationSeconds, double targetRps);
    
    // Simulator identifier
    String getName();
    
    // Health check validation
    boolean isHealthy();
}
```

### Implementation Strategy

```
┌─────────────────────────────────────────────┐
│         WorkloadSimulator Interface                │
│  + executeLoad(concurrency, duration, rps)         │
│  + getName(): String                               │
│  + isHealthy(): boolean                            │
└─────────────────────────────────────────────┘
                   ▲                    ▲
                   │                    │
      implements   │                    │   implements
                   │                    │
┌──────────────────────┐  ┌──────────────────────────┐
│ DemoWorkloadSimulator│  │ HttpRestWorkload       │
│                      │  │ Simulator              │
│ @Component("demo")   │  │                        │
│                      │  │ @Component("http")     │
│ Fields:              │  │                        │
│ - LoadRunner         │  │ Fields:                │
│                      │  │ - String baseUrl       │
│ Methods:             │  │ - String endpoint      │
│ - executeLoad()      │  │ - String method        │
│   → delegates to    │  │ - String headersJson   │
│     LoadRunner       │  │ - String requestBody   │
│ - getName()          │  │ - RestTemplate         │
│   → returns "demo"  │  │                        │
│ - isHealthy()        │  │ Methods:               │
│   → always true    │  │ - executeLoad()        │
└──────────────────────┘  │   → HTTP requests      │
                         │ - getName()            │
                         │   → returns "http"     │
                         │ - isHealthy()          │
                         │   → ping endpoint      │
                         │ - executeHttpRequest() │
                         │ - parseHeaders()       │
                         └──────────────────────────┘
```

---

## 📦 Component Breakdown

### Core Components (src/main/java)

```
com.cloudoptimizer.agent/
├── AgentCloudOptimizerApplication.java    # Spring Boot entry point
├── RunnerMain.java                        # Orchestrator (CommandLineRunner)
│
├── config/
│   └── AppConfig.java                     # Spring configuration
│
├── controller/
│   └── WorkController.java                # Demo /work endpoint
│
├── model/
│   ├── AgentDecision.java                 # Agent decision output
│   ├── AgentStrategy.java                 # Agent type enum
│   ├── MetricRow.java                     # Metric data model
│   └── RunResult.java                     # Load test result
│
├── service/
│   ├── LoadRunner.java                    # Built-in demo workload
│   ├── MetricsLogger.java                 # Async JSONL logging
│   ├── MetricsLoggingException.java       # Custom exception
│   ├── SimpleAgent.java                   # Rule-based agent
│   ├── SpringAiLlmAgent.java              # LLM-powered agent
│   └── LlmPromptBuilder.java              # LLM prompt construction
│
└── simulator/  (🆕 v0.2.0)
    ├── WorkloadSimulator.java             # Plugin interface
    ├── DemoWorkloadSimulator.java         # Built-in demo plugin
    └── HttpRestWorkloadSimulator.java     # HTTP REST plugin
```

---

## 📊 Data Flow

### Metrics Pipeline

```
Load Test     MetricsLogger      JSONL File        Agent
(RunResult)   (async queue)      (persistent)      (analysis)
    │              │                │              │
    │── log ──►    │                │              │
    │         queue.offer()      │              │
    │              │                │              │
    │              │── flush ──►        │              │
    │              │         Files.write()    │
    │              │                │              │
    │              │                │── read ──►     │
    │              │                │    readRecent(20)
    │              │                │              │
    │              │                │              ▼
    │              │                │        decide(metrics)
    │              │                │              │
    │              │                │              ▼
    │              │                │        AgentDecision


Format: JSONL (JSON Lines)

{
  "timestamp": "2025-12-30T14:00:00Z",
  "resourceId": "load-test",
  "resourceType": "WorkEndpoint",
  "metricName": "latencyMs",
  "metricValue": 29.45,
  "unit": "ms",
  "tags": {"concurrency": "4"}
}
```

---

## 🔐 Key Design Principles

### 1. **Plugin Architecture** (v0.2.0)
- **Extensibility**: Add new workload types without modifying core
- **Testability**: Mock simulators for unit testing
- **Flexibility**: Switch simulators via configuration

### 2. **Explainability First**
- Every decision includes reasoning trace
- Human-readable explanations
- Full audit trail for compliance

### 3. **Offline Operation**
- No external API dependencies (except target service)
- Local LLM via Ollama (optional)
- Works in air-gapped environments

### 4. **Safety & Validation**
- Health checks before tests
- Gradual concurrency changes
- Before/after comparison
- Conservative by default

### 5. **Observable**
- Structured JSON outputs
- Time-series metrics
- HTML visualization
- Prometheus-ready metrics endpoint

---

## 🔧 Technology Stack

| Component           | Technology          | Purpose                          |
| ------------------- | ------------------- | -------------------------------- |
| **Runtime**         | Java 21             | Modern JVM features              |
| **Framework**       | Spring Boot 3.2.5   | Dependency injection, REST       |
| **AI/LLM**          | Spring AI 0.8.0     | Local LLM integration            |
| **LLM Provider**    | Ollama (optional)   | Offline AI inference             |
| **HTTP Client**     | RestTemplate        | External API testing             |
| **Build**           | Maven 3.8+          | Dependency management            |
| **Metrics Storage** | JSONL files         | Append-only time-series          |
| **Configuration**   | YAML                | Human-readable config            |
| **Testing**         | JUnit 5, Mockito    | Unit & integration tests         |

---

## 📝 Configuration

### application.yml

```yaml
# Workload Simulator Selection
workload:
  simulator: demo  # or 'http' for external APIs

# HTTP REST Simulator (v0.2.0)
http:
  base-url: https://api.yourservice.com
  endpoint: /api/endpoint
  method: GET
  headers: '{"Authorization":"Bearer TOKEN"}'
  body: ''

# Agent Configuration
agent:
  target-latency-ms: 100.0
  artifacts-dir: artifacts

# Metrics
metrics:
  directory: data/metrics
  application-name: agent-cloud-optimizer
```

### System Properties Override

```bash
java -Dworkload.simulator=http \
     -Dhttp.base-url=https://api.example.com \
     -Dhttp.endpoint=/v1/users \
     -Dhttp.method=POST \
     -Dhttp.headers='{"Content-Type":"application/json"}' \
     -Dhttp.body='{"query":"test"}' \
     -Dagent.strategy=simple \
     -Dbaseline.concurrency=4 \
     -Dload.duration=10 \
     -jar agent-cloud-optimizer-0.2.0.jar
```

---

## 🚀 Evolution: v0.1.0 → v0.2.0

### v0.1.0 (Proof-of-Concept)
```
┌───────────────────────────┐
│     RunnerMain          │
│           │             │
│           ▼             │
│     LoadRunner          │  ← Hardcoded dependency
│  (built-in demo only)   │
└───────────────────────────┘

❌ Can only test built-in demo
❌ Requires code modification for external APIs
✅ Works as POC
```

### v0.2.0 (Plugin Architecture)
```
┌────────────────────────────────────────────────┐
│           RunnerMain                          │
│                 │                           │
│                 ▼                           │
│     Map<String, WorkloadSimulator>          │  ← Plugin registry
│                 │                           │
│       ┌─────────┴──────────┐              │
│       │                     │              │
│       ▼                     ▼              │
│  DemoWorkload        HttpRestWorkload        │
│  Simulator           Simulator               │
└────────────────────────────────────────────────┘

✅ Tests external HTTP APIs
✅ Pure YAML configuration
✅ Extensible for future simulators
✅ Backward compatible with v0.1.0
```

---

## 📊 Performance Characteristics

### Metrics
- **Load test overhead**: ~2-5% (multi-threading)
- **Metrics logging**: Async, non-blocking
- **Agent decision time**: 
  - SimpleAgent: <10ms
  - LlmAgent: 1-3 seconds (local Ollama)
- **Memory footprint**: ~50-100MB (Spring Boot)

### Scalability
- **Concurrency range**: 1-100 threads (configurable)
- **Test duration**: 5-300 seconds typical
- **Metrics storage**: JSONL (append-only, scalable)
- **HTTP requests**: Thousands per second capable

---

## 🔮 Future Architecture (v0.3.0+)

### Planned Simulators
```
WorkloadSimulator (interface)
    │
    ├── DemoWorkloadSimulator       (✅ v0.2.0)
    ├── HttpRestWorkloadSimulator   (✅ v0.2.0)
    ├── DatabaseWorkloadSimulator   (📋 v0.3.0)
    ├── GrpcWorkloadSimulator       (📋 v0.3.0)
    ├── MessageQueueSimulator       (📋 v0.3.0)
    └── CustomWorkloadSimulator     (📋 user-defined)
```

### ServiceLoader Discovery (Planned)
```java
// Future: Automatic plugin discovery
ServiceLoader<WorkloadSimulator> loader = 
    ServiceLoader.load(WorkloadSimulator.class);

for (WorkloadSimulator simulator : loader) {
    simulators.put(simulator.getName(), simulator);
}
```

---

## 🎯 Use Cases

### Current (v0.2.0)
1. **Optimize internal HTTP microservices**
2. **Test third-party API integrations**
3. **Learn autonomous agent architecture**
4. **Demonstrate LLM integration patterns**
5. **Benchmark concurrency settings**

### Future (v0.3.0+)
1. **Multi-service orchestration**
2. **Database query optimization**
3. **Kubernetes HPA tuning**
4. **Continuous optimization mode**
5. **Production deployment**

---

## 📚 References

- **Source Code**: [GitHub Repository](#)
- **Documentation**: `docs/` directory
- **Examples**: `examples/` directory
- **Roadmap**: `ROADMAP.md`
- **Changelog**: `CHANGELOG.md`

---

**For DevNexus Presentation Questions:**
- Architecture decisions and trade-offs
- Plugin pattern implementation
- LLM integration strategies
- Production readiness considerations
- Extensibility and future roadmap

---

*Last updated: December 30, 2025*  
*Version: 0.2.0*  
*Prepared for: DevNexus Conference Atlanta*
