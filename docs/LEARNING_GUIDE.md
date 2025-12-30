# ACO Learning Guide

**For Students, Engineers, and Conference Attendees**

---

## 🎯 Who This Is For

### **Job aspirants**
Learn real-world skills universities don't teach:
- Closed-loop autonomous systems
- Agentic AI vs traditional programming
- Performance optimization and tuning
- Production observability patterns
- Explainable AI systems

### **Software Engineers**
Master production-ready patterns:
- Spring Boot enterprise architecture
- AI integration with Spring AI + Ollama
- Load testing and performance engineering
- Metrics collection and analysis
- Autonomous decision-making systems

### **Conference Attendees**
See the first autonomous, explainable optimization engine for Java microservices in action.

---

## 📚 Learning Path

### **Level 1: Quick Start (15 minutes)**

#### **Goal:** Understand what ACO does

**Steps:**
```bash
# 1. Build
mvn clean package -DskipTests

# 2. Run with Simple Agent
./scripts/run-agent.sh

# 3. View results
python3 -m http.server 8000
# Open http://localhost:8000/demo.html
```

**What You'll See:**
- Baseline performance: 4 threads, 142ms latency
- Agent decision: "Increase to 5 threads"
- After optimization: 5 threads, 95ms latency
- Improvement: 33% faster

**Key Learning:**
> "ACO automatically finds better configurations by testing, analyzing, and validating changes."

---

### **Level 2: Simple Agent Deep Dive (30 minutes)**

#### **Goal:** Understand deterministic optimization logic

**Files to Study:**
1. `src/main/java/com/cloudoptimizer/agent/service/SimpleAgent.java`
2. `artifacts/reasoning_trace_simple.txt`

**Key Code:**
```java
// SimpleAgent.java - Lines 70-102
if (medianLatency > targetLatencyMs) {
    // Too slow → increase concurrency
    newConcurrency = (int) Math.ceil(currentConcurrency * 1.25);
    reasoning = "Median latency exceeds target. Increasing concurrency...";
    
} else if (medianLatency < targetLatencyMs * 0.7) {
    // Too fast → decrease concurrency (save resources)
    newConcurrency = (int) Math.floor(currentConcurrency * 0.75);
    reasoning = "Latency well below target. Decreasing concurrency...";
    
} else {
    // Just right → maintain
    newConcurrency = currentConcurrency;
    reasoning = "Latency within acceptable range. Maintaining...";
}
```

**Experiment:**
```bash
# Try different target latencies
java -jar target/agent-cloud-optimizer-1.0-SNAPSHOT.jar \
  -Dagent.target-latency-ms=50

# Try different baseline concurrency
java -jar target/agent-cloud-optimizer-1.0-SNAPSHOT.jar \
  -Dbaseline.concurrency=8
```

**Key Learning:**
> "SimpleAgent uses hard-coded if-else rules. It's fast and predictable, but not intelligent."

---

### **Level 3: LLM Agent Deep Dive (45 minutes)**

#### **Goal:** Understand AI-powered reasoning

**Setup:**
```bash
# Install Ollama
brew install ollama  # macOS
# or visit https://ollama.com/download

# Start Ollama service
ollama serve

# Pull model (in another terminal)
ollama pull llama2
```

**Run LLM Agent:**
```bash
AGENT_STRATEGY=llm ./scripts/run-agent.sh
```

**Files to Study:**
1. `src/main/java/com/cloudoptimizer/agent/service/SpringAiLlmAgent.java`
2. `src/main/java/com/cloudoptimizer/agent/service/LlmPromptBuilder.java`
3. `artifacts/reasoning_trace_llm.txt`

**Key Code:**
```java
// SpringAiLlmAgent.java - Lines 78-89
String prompt = promptBuilder.buildPrompt(recentMetrics, currentConcurrency, targetLatencyMs);

// Ask LLM to analyze and decide
ChatResponse response = chatClient.call(new Prompt(prompt));
String llmOutput = response.getResult().getOutput().getContent();

// Parse LLM's JSON response
AgentDecision decision = parseJsonResponse(llmOutput, currentConcurrency);
```

**Compare Reasoning:**

**SimpleAgent:**
```
Median latency (142.50ms) exceeds target (100.00ms).
Increasing concurrency from 4 to 5 to improve parallelism.
```

**LLM Agent:**
```
The median latency of 142.5ms exceeds the target by 42.5%.
However, examining the distribution reveals high variance
(min: 28ms, max: 187ms, p95: 178ms). This pattern suggests
thread pool starvation during peak load rather than sustained
overload. The p95 being 25% higher than median indicates
bursty traffic.

Recommendation: Increase concurrency from 4 to 7 threads (75%).
This is more aggressive than a simple proportional increase
because the variance pattern suggests we need headroom for
bursts. However, we avoid going to 8+ to prevent excessive
context switching overhead.

Expected outcome: Median latency ~78ms, p95 ~95ms.
Confidence: 85% (moderate due to variance in baseline data).
```

**Key Learning:**
> "LLM Agent analyzes patterns (variance, p95, distribution), reasons about root causes (thread starvation), and makes smarter decisions with rich explanations."

---

### **Level 4: Architecture Deep Dive (1 hour)**

#### **Goal:** Understand the complete system architecture

**Study These Components:**

#### **1. Orchestrator**
- **File:** `src/main/java/com/cloudoptimizer/agent/RunnerMain.java`
- **Purpose:** Coordinates the entire optimization workflow
- **Pattern:** Closed-loop control (Observe → Reason → Act → Validate)

#### **2. Load Testing**
- **File:** `src/main/java/com/cloudoptimizer/agent/service/LoadRunner.java`
- **Purpose:** Measures performance under load
- **Pattern:** Concurrent execution with latency tracking

#### **3. Metrics Collection**
- **File:** `src/main/java/com/cloudoptimizer/agent/service/MetricsLogger.java`
- **Purpose:** Stores performance metrics for analysis
- **Pattern:** JSONL format with daily rotation

#### **4. Decision Making**
- **Files:** 
  - `SimpleAgent.java` - Rule-based logic
  - `SpringAiLlmAgent.java` - AI-powered reasoning
- **Purpose:** Analyze metrics and recommend optimizations
- **Pattern:** Strategy pattern (swap agents easily)

#### **5. REST API**
- **File:** `src/main/java/com/cloudoptimizer/agent/controller/WorkController.java`
- **Purpose:** Simulate workload and expose metrics
- **Pattern:** Spring REST with async processing

**Architecture Diagram:**
```
┌─────────────────────────────────────────────────────────┐
│                    RunnerMain                           │
│                  (Orchestrator)                         │
└─────────────────────────────────────────────────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        ▼                 ▼                 ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ LoadRunner   │  │ Agent        │  │ MetricsLogger│
│ (Observe)    │  │ (Reason)     │  │ (Store)      │
└──────────────┘  └──────────────┘  └──────────────┘
        │                 │                 │
        └─────────────────┴─────────────────┘
                          │
                          ▼
                  ┌──────────────┐
                  │ Artifacts    │
                  │ (Report)     │
                  └──────────────┘
```

**Key Learning:**
> "ACO uses a modular architecture where each component has a single responsibility. This makes it easy to understand, test, and extend."

---

### **Level 5: Hands-On Experiments (2 hours)**

#### **Experiment 1: Threshold Tuning**
```bash
# Modify SimpleAgent.java line 82
LOW_THRESHOLD_FACTOR = 0.5;  // Changed from 0.7

# Rebuild and test
mvn clean package -DskipTests
./scripts/run-agent.sh

# Question: How does this affect decisions?
```

#### **Experiment 2: Load Test Parameters**
```bash
# Test with different durations
DURATION=5 ./scripts/run-agent.sh
DURATION=30 ./scripts/run-agent.sh

# Test with different concurrency
CONCURRENCY=2 ./scripts/run-agent.sh
CONCURRENCY=16 ./scripts/run-agent.sh

# Question: How does baseline affect recommendations?
```

#### **Experiment 3: LLM Prompt Engineering**
```bash
# Modify LlmPromptBuilder.java lines 45-50
# Add more analysis guidelines
# Rebuild and test with LLM agent

AGENT_STRATEGY=llm ./scripts/run-agent.sh

# Question: How do prompts affect LLM decisions?
```

#### **Experiment 4: Custom Metrics**
```bash
# Add new metric in LoadRunner.java
MetricRow cpuMetric = MetricRow.builder()
    .metricName("cpuUsage")
    .metricValue(getCpuUsage())
    .build();

# Modify agent to consider CPU
# Question: How do multiple metrics affect decisions?
```

---

## 🎓 Skills You'll Learn

### **For Students**

#### **1. Autonomous Systems**
- Closed-loop control patterns
- Observe → Reason → Act → Validate cycle
- Self-optimizing systems

#### **2. AI Integration**
- How to use LLMs in Java applications
- Prompt engineering for structured outputs
- Parsing and validating AI responses

#### **3. Performance Engineering**
- Load testing methodologies
- Latency vs throughput trade-offs
- Concurrency and thread pool tuning

#### **4. Production Patterns**
- Spring Boot architecture
- Dependency injection
- Configuration management
- Error handling and logging

#### **5. Explainable AI**
- Reasoning trace generation
- Audit trails for decisions
- Confidence scoring

---

### **For Engineers**

#### **1. Spring Boot Best Practices**
- Service layer architecture
- REST API design
- Async processing with `@Async`
- Configuration with `application.yml`

#### **2. Spring AI Framework**
- ChatClient integration
- Ollama local LLM setup
- Prompt building and response parsing
- Error handling and fallbacks

#### **3. Observability**
- Metrics collection and storage
- JSONL format for time-series data
- Daily rotation patterns
- Efficient metric queries

#### **4. Load Testing**
- Concurrent execution patterns
- Latency measurement (median, p95, p99)
- Throughput calculation
- Statistical analysis

#### **5. Autonomous Decision-Making**
- Strategy pattern for swappable agents
- Confidence scoring
- Before/after validation
- Rollback strategies

---

## 🎤 Conference Demo Script

### **Opening (1 minute)**
> "Today I'll show you ACO - the first autonomous, explainable optimization engine for Java microservices. It combines closed-loop control with AI reasoning to automatically tune performance."

### **Demo Part 1: The Problem (1 minute)**
> "Imagine your microservice is slow. Today, an SRE manually adjusts thread pools, waits hours for results, and repeats. This is slow, error-prone, and expensive."

### **Demo Part 2: Simple Agent (2 minutes)**
```bash
./scripts/run-agent.sh
```
> "We automated that manual logic. Now it takes 30 seconds. But it's still just if-else rules. Watch what happens when we add AI..."

### **Demo Part 3: LLM Agent (4 minutes)**
```bash
AGENT_STRATEGY=llm ./scripts/run-agent.sh
```
> "The LLM analyzes variance, p95, patterns - things we'd never hard-code. It reasons about root causes and explains its thinking. This is the innovation."

### **Demo Part 4: Results (2 minutes)**
> "Both agents improved performance. But look at the reasoning traces. SimpleAgent: basic template. LLM: rich analysis. That's the difference between automation and intelligence."

### **Closing (1 minute)**
> "ACO demonstrates three innovations:
> 1. Closed-loop autonomous optimization
> 2. Dual-agent architecture (rules + AI)
> 3. Full explainability with reasoning traces
> 
> This combination doesn't exist anywhere else. Questions?"

---

## 📖 Further Reading

### **Code Deep Dives**
- [SimpleAgent Implementation](../src/main/java/com/cloudoptimizer/agent/service/SimpleAgent.java)
- [LLM Agent Implementation](../src/main/java/com/cloudoptimizer/agent/service/SpringAiLlmAgent.java)
- [Load Testing Implementation](../src/main/java/com/cloudoptimizer/agent/service/LoadRunner.java)

### **Documentation**
- [README.md](../README.md) - Quick start guide
- [PROBLEM_SOLUTION_ALIGNMENT.md](../PROBLEM_SOLUTION_ALIGNMENT.md) - Industry gap analysis
- [OLLAMA_SETUP.md](../OLLAMA_SETUP.md) - LLM setup guide

### **External Resources**
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Ollama Documentation](https://ollama.com/docs)

---

## 🚀 Next Steps

### **For Students**
1. Complete Levels 1-3
2. Try experiments in Level 5
3. Add to your portfolio/GitHub
4. Practice explaining it in interviews

### **For Engineers**
1. Complete all 5 levels
2. Integrate with your own services
3. Deploy to Kubernetes
4. Present at your company's tech talk

### **For Conference Attendees**
1. Clone the repo
2. Run the demo
3. Study the reasoning traces
4. Ask questions!

---

**Built with ❤️ for learning and innovation**
