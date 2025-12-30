# Agent Cloud Optimizer (ACO)

**🔬 Proof-of-Concept: Autonomous AI agents for Java microservice optimization**

> **⚠️ PROOF OF CONCEPT** - This is a working demo that showcases autonomous optimization technology. It can optimize both its built-in demo API and external HTTP REST APIs. See [Current Limitations](#current-limitations) for details.

> **👉 New to this project? [Start Here →](docs/START_HERE.md)** (15-minute quick start)

> Modern Java microservices still depend on manual tuning for concurrency, scaling, and performance configuration. This creates instability, latency spikes, higher cloud cost, and slow incident response. **This project demonstrates how autonomous agents** (rule-based + AI-powered) can analyze metrics, make intelligent decisions, and optimize runtime configuration - with full explainability.

---

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot 3.2.5](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Version](https://img.shields.io/badge/version-0.2.0-blue.svg)](CHANGELOG.md)
[![Status](https://img.shields.io/badge/status-proof--of--concept-orange.svg)](#)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)

---

## 🎓 **New User? Start Here!**

### **👉 [Quick Start Guide (START_HERE.md)](docs/START_HERE.md)**

Get the project running in 15 minutes with step-by-step instructions.

⚠️ **Important:** This is a proof-of-concept with a self-contained demo API. See [Current Limitations](#current-limitations) below.

**Already familiar?** Continue reading for full documentation.

---

## 📚 Documentation

- 🚀 **[Quick Start](docs/START_HERE.md)** - Get running in 15 minutes
- 💭 **[Product Roadmap](ROADMAP.md)** - Vision and future features
- 📖 **[Learning Guide](docs/LEARNING_GUIDE.md)** - Deep dive tutorials  
- 🪟 **[Windows Setup](docs/WINDOWS_SETUP.md)** - Windows users start here
- 🤖 **[LLM Setup](docs/OLLAMA_SETUP.md)** - Enable AI-powered optimization
- 🤝 **[Contributing](CONTRIBUTING.md)** - Help build this project

---

## 🚨 The Problem

Java microservice teams today face critical challenges:

### 1. **Manual Performance Tuning Is Slow & Error-Prone**

- Thread pool sizes, executor concurrency, and JVM parameters require constant manual adjustment
- Teams rely on trial-and-error, guesswork, and reactive fire drills
- Results in **slower incident response**, **overprovisioning** (wasted cloud cost), and **latency spikes**

### 2. **Traditional Observability Tools Don't Act**

- Tools like Datadog, NewRelic, and Prometheus **observe and alarm** but **don't fix**
- No closed-loop optimization exists in the Java ecosystem
- SREs manually interpret dashboards and apply changes hours or days later

### 3. **Scaling Decisions Are Reactive, Not Predictive**

- Teams "chase the problem" by scaling up after latency spikes occur
- Causes **high latency**, **downtime during spikes**, and **expensive over-scaling**
- No proactive, intelligent optimization layer exists

### 4. **Cloud Cost Is Out of Control**

- 30-60% of cloud spend is wasted due to:
  - Concurrency too high
  - CPU underutilized
  - Pods mis-sized
  - Wrong autoscaling rules
- Companies massively overspend to stay safe

### 5. **No Explainable Autonomy Exists for Java Microservices**

- No framework provides:
  - ✅ Explainable agentic decisions
  - ✅ Runtime configuration tuning
  - ✅ Before/after performance validation
  - ✅ Local LLM reasoning for optimization
  - ✅ Zero-cloud-API autonomous control

**ACO is the first system to combine all of these capabilities.**

---

## ✅ The Solution

ACO provides **autonomous, explainable, self-optimizing microservices** through:

### **Closed-Loop Optimization**

1. **Observe** - Continuously monitor performance metrics (latency, throughput, concurrency)
2. **Reason** - Analyze patterns using rule-based logic or local LLM
3. **Decide** - Generate explainable optimization recommendations
4. **Act** - Apply configuration changes safely
5. **Validate** - Measure before/after impact with full traceability

### **Dual-Agent Architecture**

Choose your autonomous agent:

- **SimpleAgent** 🛠️ - Rule-based autonomous optimization (deterministic, fast, no extra setup)
- **LLM Agent** 🤖 - AI-powered autonomous optimization (intelligent, context-aware, requires Ollama)

**Both agents are autonomous** - they observe, reason, decide, and optimize without human intervention.

### **100% Offline & Explainable**

- No external API calls or cloud dependencies
- Every decision includes detailed reasoning traces
- Full audit trail for compliance and debugging

---

## 🎯 The Impact

| **Before ACO**                   | **After ACO**                            |
| -------------------------------- | ---------------------------------------- |
| Manual tuning takes hours/days   | Autonomous tuning in seconds             |
| Reactive scaling after incidents | Proactive optimization before issues     |
| 30-60% cloud cost waste          | Optimized resource utilization           |
| No explainability                | Full reasoning traces for every decision |
| Observability without action     | Closed-loop observe → reason → act       |

### **Real-World Benefits**

- ⚡ **Faster incident response** - Autonomous tuning eliminates manual intervention
- 💰 **Lower cloud costs** - Prevent overprovisioning and waste
- 📈 **Better performance** - Proactive optimization prevents latency spikes
- 🔍 **Full explainability** - Understand exactly why and how decisions are made
- 🎓 **Educational value** - Learn real-world performance tuning and agentic systems

---

## ⚠️ Current Limitations

**This is a v0.1.0 proof-of-concept.** It demonstrates the technology works but has these limitations:

❌ **Not yet a drop-in plugin** - Tests its own built-in demo API, not external services  
❌ **Requires code modification** - To test your API, you need to modify `LoadRunner.java`  
❌ **No multi-service orchestration** - Single-service optimization only  

✅ **What it DOES provide:**
- Working autonomous optimization with real agents
- Full explainability and reasoning traces
- Proof that the architecture works
- Educational value for learning AI agents
- Foundation for production features

**What's next?** See our [Product Roadmap](ROADMAP.md) for v0.2.0+ features:
- 🔌 Plugin architecture for external APIs (v0.2.0)
- 🎯 Multi-parameter optimization (v0.3.0)
- ☸️ Kubernetes integration (v0.4.0)
- 🔄 Continuous optimization mode (v0.5.0)
- ...and much more! [See full roadmap →](ROADMAP.md)

---

## 🚀 Quick Start

### 1. Prerequisites

**Required:**
- **JDK 17** or higher ([Download](https://adoptium.net/))
- **Maven 3.8+** ([Download](https://maven.apache.org/download.cgi))

**Agent Choice:**
- **Simple Agent** (rule-based) - No additional setup ✅
- **LLM Agent** (AI-powered) - Requires Ollama ([Setup Guide](docs/OLLAMA_SETUP.md)) 🤖

Verify installation:

```bash
java -version    # Should show 17 or higher
mvn -version     # Should show 3.8 or higher
```

### 2. Build

```bash
mvn clean package -DskipTests
```

**Expected output**: `BUILD SUCCESS` and JAR file in `target/` directory

### 3. Run with Simple Agent (Rule-Based)

```bash
./scripts/run-agent.sh
# or on Windows
scripts\run-agent.bat
```

**Expected output**:

```
======================================
OPTIMIZATION SUMMARY
======================================

Concurrency:  4 → 6 (+2)

Latency (median):
  Before:  142.50 ms
  After:   78.30 ms
  Change:  -64.20 ms (-45.1%)

Throughput (RPS):
  Before:  28.50 req/s
  After:   42.10 req/s
  Change:  +13.60 req/s (+47.7%)

Decision Confidence: 95%
Impact Level: MEDIUM
```

### 4. View Results

```bash
# Option 1: Start local web server
python3 -m http.server 8000

# Open http://localhost:8000/demo.html in your browser

# Option 2: Check JSON artifacts
ls -la artifacts/
cat artifacts/report.json

# Option 3: Review reasoning traces
cat artifacts/reasoning_trace_simple.txt
```

### 5. 🆕 Optimize External HTTP APIs (v0.2.0+)

**NEW!** You can now optimize external HTTP REST APIs:

```bash
java -Dworkload.simulator=http \
     -Dhttp.base-url=https://api.yourservice.com \
     -Dhttp.endpoint=/api/endpoint \
     -Dhttp.method=GET \
     -Dbaseline.concurrency=4 \
     -Dload.duration=10 \
     -jar target/agent-cloud-optimizer-0.2.0.jar
```

**With headers and body:**

```bash
java -Dworkload.simulator=http \
     -Dhttp.base-url=https://api.yourservice.com \
     -Dhttp.endpoint=/v1/users \
     -Dhttp.method=POST \
     -Dhttp.headers='{"Authorization":"Bearer YOUR_TOKEN","Content-Type":"application/json"}' \
     -Dhttp.body='{"query":"test"}' \
     -Dbaseline.concurrency=4 \
     -jar target/agent-cloud-optimizer-0.2.0.jar
```

**Or configure in `application.yml`:**

```yaml
workload:
  simulator: http  # Use 'demo' for built-in, 'http' for external APIs

http:
  base-url: https://api.yourservice.com
  endpoint: /api/endpoint
  method: GET
  headers: '{"Authorization":"Bearer TOKEN"}'
  body: ''
```

---

## 📚 Learning Resources

- **[LEARNING_GUIDE.md](docs/LEARNING_GUIDE.md)** - Complete learning path for students and engineers
- **[WINDOWS_SETUP.md](docs/WINDOWS_SETUP.md)** - Windows-specific setup guide
- **[OLLAMA_SETUP.md](docs/OLLAMA_SETUP.md)** - LLM setup instructions

## 📋 What ACO Does

1. **Baseline Load Test** - Measures current performance with existing concurrency
2. **AI Analysis** - Analyzes metrics using selected agent strategy
3. **Optimization** - Applies recommended concurrency changes
4. **Post-Test** - Measures improved performance
5. **Report** - Generates before/after comparison with reasoning

## 🤖 Agent Strategies

### Simple Agent (Rule-Based)

Deterministic threshold-based optimization:

- If median latency > target → INCREASE concurrency
- If median latency < 70% of target → DECREASE concurrency
- Otherwise → MAINTAIN current setting

```bash
AGENT_STRATEGY=simple ./scripts/run-agent.sh
```

### LLM Agent (AI-Powered) 🤖

**Autonomous AI agent** using local Ollama for intelligent optimization:

- 🧠 Analyzes complex metric patterns
- 🎯 Considers multiple factors simultaneously  
- 📝 Provides detailed AI reasoning traces
- 🔒 100% offline (no API keys, no cloud calls)

```bash
# Setup Ollama first
ollama serve
ollama pull llama2

# Run with LLM agent
AGENT_STRATEGY=llm ./scripts/run-agent.sh
```

## 🛠️ Technology Stack

- **Java 17** - LTS version for enterprise stability
- **Spring Boot 3.2.5** - Modern enterprise application framework
- **Spring AI 0.8.0** - AI integration framework
- **Ollama** - Local LLM inference (100% offline, no API keys)
- **Maven** - Dependency management and build automation

## 📁 Project Structure

```
com.cloudoptimizer.agent
├── config/          # Application configuration
├── controller/      # REST API endpoints
├── service/         # Business logic & agents
│   ├── SimpleAgent.java       # Rule-based agent
│   ├── SpringAiLlmAgent.java  # LLM-based agent
│   ├── LoadRunner.java        # Load testing
│   └── MetricsLogger.java     # Metrics persistence
├── model/           # Domain models
│   ├── AgentDecision.java     # Optimization decisions
│   ├── AgentStrategy.java     # Strategy enum
│   ├── MetricRow.java         # Metric data
│   └── RunResult.java         # Load test results
└── RunnerMain.java  # Main orchestrator
```

## 📊 Output Artifacts

All results are written to the `artifacts/` directory:

- **baseline.json** - Baseline performance metrics
- **after.json** - Post-optimization metrics
- **report.json** - Complete comparison report
- **reasoning_trace_simple.txt** - Rule-based agent reasoning
- **reasoning_trace_llm.txt** - LLM agent reasoning

## 🎯 Configuration

### Environment Variables

```bash
# Agent selection
export AGENT_STRATEGY=simple    # or 'llm'

# Load test parameters
export CONCURRENCY=4            # Starting concurrency
export DURATION=10              # Test duration in seconds
export TARGET_RPS=0             # Target requests/sec (0=unlimited)
```

### System Properties

```bash
java -jar target/agent-cloud-optimizer-1.0-SNAPSHOT.jar \
  -Dagent.strategy=llm \
  -Dbaseline.concurrency=4 \
  -Dload.duration=10 \
  -Dtarget.rps=0
```

## 📖 Example Output

```
======================================
OPTIMIZATION SUMMARY
======================================

Concurrency:  4 → 6 (+2)

Latency (median):
  Before:  142.50 ms
  After:   78.30 ms
  Change:  -64.20 ms (-45.1%)

Throughput (RPS):
  Before:  28.50 req/s
  After:   42.10 req/s
  Change:  +13.60 req/s (+47.7%)

Decision Confidence: 95%
Impact Level: MEDIUM
```

## 🔧 Prerequisites

- **JDK 17** or higher
- **Maven 3.8+**
- **Ollama** (optional, for LLM agent only)

### Ollama Setup (for LLM Agent)

```bash
# Install Ollama
brew install ollama  # macOS
# or visit https://ollama.com/download

# Start service
ollama serve

# Pull model
ollama pull llama2
```

See [OLLAMA_SETUP.md](docs/OLLAMA_SETUP.md) for detailed instructions.

## 🧪 Running Tests

```bash
mvn test
```

## 📜 Scripts

### Unix/Linux/macOS

- `scripts/run-baseline.sh` - Run baseline test only
- `scripts/run-agent.sh` - Run full optimization cycle

### Windows

- `scripts\run-baseline.bat` - Run baseline test only
- `scripts\run-agent.bat` - Run full optimization cycle

## 🎨 Demo UI

The `demo.html` file provides a beautiful web interface to visualize optimization results:

- Before/after comparison
- Key metrics improvements
- Agent reasoning and confidence
- Detailed performance breakdown

Simply open `demo.html` in any modern browser after running the optimization.

## 🔒 Security & Privacy

- **100% Offline** - No external API calls
- **No API Keys** - Ollama runs locally
- **Data Privacy** - All data stays on your machine
- **Safe for Production** - No sensitive data leaves your infrastructure

## 📈 Performance

- **Baseline Test**: ~10 seconds
- **Agent Analysis**: < 1 second (simple) or 2-5 seconds (LLM)
- **Post-Optimization Test**: ~10 seconds
- **Total Runtime**: ~20-30 seconds

## 🔧 Troubleshooting

### Build Issues

#### Maven Dependency Errors

```bash
# Clear Maven cache and retry
rm -rf ~/.m2/repository
mvn clean package -U
```

#### JDK Version Mismatch

```bash
# Check Java version
java -version

# Set JAVA_HOME (macOS/Linux)
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# Set JAVA_HOME (Windows)
set JAVA_HOME=C:\Program Files\Java\jdk-17
```

### Runtime Issues

#### No Metrics Generated

**Problem**: `artifacts/` directory is empty

**Solution**:

- Check console for errors
- Verify `artifacts/` directory exists (created automatically)
- Run with increased logging: `java -jar target/agent-cloud-optimizer-1.0-SNAPSHOT.jar --logging.level.com.cloudoptimizer=DEBUG`

#### LLM Agent Fails

**Error**: `Connection refused to Ollama`

**Solution**:

1. Start Ollama: `ollama serve`
2. Pull model: `ollama pull llama2`
3. Verify: `curl http://localhost:11434/api/tags`
4. See [OLLAMA_SETUP.md](docs/OLLAMA_SETUP.md) for detailed guide

#### Permission Denied on Scripts

```bash
# Make scripts executable (Unix/Linux/macOS)
chmod +x scripts/*.sh

# Run with explicit shell
bash scripts/run-agent.sh
```

### Configuration Issues

#### Changing Test Parameters

```bash
# Environment variables
export CONCURRENCY=8
export DURATION=15
export TARGET_RPS=100
./scripts/run-agent.sh

# Or system properties
java -jar target/agent-cloud-optimizer-1.0-SNAPSHOT.jar \
  -Dbaseline.concurrency=8 \
  -Dload.duration=15 \
  -Dtarget.rps=100
```

#### Changing Target Latency

Edit `src/main/resources/application.yml`:

```yaml
agent:
  target-latency-ms: 150.0 # Change from default 100ms
```

### Demo Page Issues

#### demo.html Not Loading Data

```bash
# Ensure artifacts exist
ls artifacts/*.json

# Start web server in project root
python3 -m http.server 8000

# Access via localhost (not file://)
# ✅ http://localhost:8000/demo.html
# ❌ file:///path/to/demo.html
```

### Getting Help

1. **Check Logs**: Look at console output for detailed error messages
2. **Enable Debug Logging**: Add `--logging.level.root=DEBUG` to command
3. **Verify Prerequisites**: Java 17+, Maven 3.8+, Ollama (for LLM)
4. **Check Examples**: Review `examples/` directory for expected output format
5. **Review Docs**: [LEARNING_GUIDE.md](docs/LEARNING_GUIDE.md), [OLLAMA_SETUP.md](docs/OLLAMA_SETUP.md)

## 🤝 Contributing

This is an enterprise MVP. For production deployment:

1. Add authentication/authorization
2. Implement persistent storage
3. Add monitoring and alerting
4. Configure production-grade thread pools
5. Add circuit breakers and retry logic

## 📄 License

MIT License - See LICENSE file for details

Copyright (c) 2025 Sibasis Padhi

## 👥 Author

**Sibasis Padhi**

Created in December 2025

## 🔗 References

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Ollama Documentation](https://ollama.com/docs)

---

**Built with ❤️ for autonomous cloud optimization**
