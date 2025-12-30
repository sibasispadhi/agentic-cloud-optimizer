# 🚀 START HERE - New User Quick Start

**Welcome to Agent Cloud Optimizer!** This guide gets you from zero to running in 15 minutes.

---

## ⚠️ **Important: What This Project Is**

Before you start, understand what you're getting:

✅ **This IS:** A working demo showing autonomous optimization with AI agents  
❌ **This is NOT (yet):** A drop-in plugin for your existing APIs  

**What API does it test?** Its own built-in demo API (self-contained!)  
**Can I test my API?** Not directly yet - requires code modification  
**Should I still try it?** **YES!** Learn how autonomous optimization works  

📖 **Read:** [WHAT_THIS_IS.mdWHAT_THIS_IS.md) for complete details

**Ready to continue?** Let's go! 👇

---

## 🎯 What You'll Learn

By following this guide, you will:
1. ✅ See the project run on your machine
2. ✅ Understand what autonomous optimization means
3. ✅ Learn how AI agents make decisions
4. ✅ Get hands-on with real cloud optimization techniques

**Time:** 15-30 minutes  
**Difficulty:** Beginner (no prior experience needed)

---

## 📋 Prerequisites (Install These First)

### **Step 1: Install Java 17+**

**Why?** The project runs on Java

#### macOS:
```bash
brew install openjdk@17
```

#### Windows:
```powershell
winget install EclipseAdoptium.Temurin.17.JDK
```

#### Linux:
```bash
sudo apt install openjdk-17-jdk  # Ubuntu/Debian
```

**Verify:**
```bash
java -version
# Should show: openjdk version "17" or higher
```

---

### **Step 2: Install Maven**

**Why?** Builds the Java project

#### macOS:
```bash
brew install maven
```

#### Windows:
```powershell
winget install Apache.Maven
```

#### Linux:
```bash
sudo apt install maven  # Ubuntu/Debian
```

**Verify:**
```bash
mvn -version
# Should show: Apache Maven 3.8 or higher
```

---

### **Step 3 (Optional): Install Ollama**

**Why?** Only needed for AI-powered agent (you can start without this)

#### All Platforms:
1. Visit: https://ollama.com/download
2. Download for your OS
3. Install and run

**Verify:**
```bash
ollama --version
```

**Pull a model (only if you installed Ollama):**
```bash
ollama pull llama3.2:1b
```

---

## 🚀 Quick Start (5 Minutes)

### **Step 1: Download the Project**

```bash
# Clone from GitHub (once published)
git clone https://github.com/YOUR_USERNAME/agent-cloud-optimizer
cd agent-cloud-optimizer

# OR download ZIP from GitHub and extract
```

---

### **Step 2: Build the Project**

```bash
mvn clean package -DskipTests
```

**Expected output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: 30-60 seconds
```

**Troubleshooting:**
- If build fails, see [README.md](README.md#troubleshooting)
- Common issue: Java version too old (need 17+)

---

### **Step 3: Run Your First Test (Simple Agent)**

**macOS/Linux:**
```bash
./scripts/run-agent.sh
```

**Windows:**
```powershell
scripts\run-agent.bat
```

**What you'll see:**
```
======================================
Agent Cloud Optimizer v1.0.0
======================================
Configuration:
  Agent Strategy: simple
  Baseline Concurrency: 4
  Load Duration: 10s

Phase 1: Running baseline load test...
Phase 2: Analyzing metrics with simple agent...
Phase 3: Running post-optimization load test...
Phase 4: Generating comparison report...

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

======================================
Optimization Complete!
======================================
```

**🎉 Congratulations!** You just ran an autonomous optimization!

---

### **Step 4: View the Results**

#### **Option 1: Visual Dashboard (Recommended)**

```bash
# Start a local web server
python3 -m http.server 8000

# Open in browser:
http://localhost:8000/demo.html
```

You'll see:
- 📊 Before/After comparison charts
- 📈 Performance improvements
- 🤖 Agent decision and reasoning
- 📉 Detailed metrics breakdown

#### **Option 2: Check JSON Files**

```bash
# List generated artifacts
ls -la artifacts/

# View the report
cat artifacts/report.json
```

#### **Option 3: Read the Reasoning**

```bash
# See how the agent made its decision
cat artifacts/reasoning_trace_rule.txt
```

This shows you:
- What metrics were analyzed
- What rules were applied
- Why the decision was made
- What actions to take

---

## 📚 Understanding What Just Happened

### **The 5-Phase Optimization Cycle:**

```
1. OBSERVE  → Measure baseline performance (4 threads)
   ↓
2. REASON   → Analyze metrics ("latency too high")
   ↓
3. DECIDE   → Recommend change ("increase to 6 threads")
   ↓  
4. ACT      → Apply the change
   ↓
5. VALIDATE → Measure improvement (-45% latency!)
```

### **What Makes This Special:**

✅ **Autonomous** - No human intervention needed  
✅ **Explainable** - You can see WHY it decided  
✅ **Validated** - Proves it actually helped  
✅ **Fast** - Complete cycle in ~20 seconds  

---

## 🎓 Learning Path (Choose Your Level)

### **Level 1: Beginner (You Are Here!)**
✅ You just completed this!  
📖 **Next:** Read [README.md](../../README.md) to understand the project better

### **Level 2: Understanding Simple Agent**
⏱️ **Time:** 30 minutes  
📖 **Read:** [LEARNING_GUIDE.md - Level 2](LEARNING_GUIDE.md#level-2-simple-agent-deep-dive-30-minutes)

**You'll learn:**
- How rule-based optimization works
- When to increase/decrease concurrency
- How to tune the target latency

### **Level 3: Understanding LLM Agent**
⏱️ **Time:** 45 minutes  
📖 **Read:** [LEARNING_GUIDE.md - Level 3](LEARNING_GUIDE.md#level-3-llm-agent-deep-dive-45-minutes)

**You'll learn:**
- How AI analyzes performance patterns
- Difference between rules and AI reasoning
- How to use local LLMs (Ollama)

**Run it:**
```bash
# First install Ollama (see Step 3 above)
# Then:
export AGENT_STRATEGY=llm  # macOS/Linux
set AGENT_STRATEGY=llm     # Windows

./scripts/run-agent.sh     # macOS/Linux
scripts\run-agent.bat      # Windows
```

### **Level 4: Architecture Deep Dive**
⏱️ **Time:** 1 hour  
📖 **Read:** [LEARNING_GUIDE.md - Level 4](LEARNING_GUIDE.md#level-4-architecture-deep-dive-1-hour)

**You'll learn:**
- How the components work together
- Spring Boot architecture patterns
- How to extend the system

### **Level 5: Hands-On Experiments**
⏱️ **Time:** 2+ hours  
📖 **Read:** [LEARNING_GUIDE.md - Level 5](LEARNING_GUIDE.md#level-5-hands-on-experiments-2-hours)

**You'll:**
- Modify agent logic
- Add new metrics
- Tune parameters
- Build your own features

---

## 🔧 Experiments You Can Try Now

### **Experiment 1: Change Target Latency**

```bash
# Try a more aggressive target
java -Dagent.target-latency-ms=50 -jar target/agent-cloud-optimizer-1.0-SNAPSHOT.jar
```

**Question:** How does this change the agent's decision?

### **Experiment 2: Different Starting Concurrency**

```bash
# Start with higher concurrency
java -Dbaseline.concurrency=8 -jar target/agent-cloud-optimizer-1.0-SNAPSHOT.jar
```

**Question:** Does the agent increase or decrease?

### **Experiment 3: Longer Load Test**

```bash
# Run for 30 seconds instead of 10
java -Dload.duration=30 -jar target/agent-cloud-optimizer-1.0-SNAPSHOT.jar
```

**Question:** Are the results more stable?

---

## 🆘 Common Issues & Solutions

### **Issue 1: "java: command not found"**

**Solution:** Java not installed or not in PATH
```bash
# Install Java 17+ (see Step 1 above)
# Then verify:
java -version
```

### **Issue 2: "mvn: command not found"**

**Solution:** Maven not installed
```bash
# Install Maven (see Step 2 above)
# Then verify:
mvn -version
```

### **Issue 3: Build fails with "compiler error"**

**Solution:** Java version too old
```bash
# Check version:
java -version

# Need 17 or higher!
# Upgrade Java (see Step 1 above)
```

### **Issue 4: "Permission denied" on scripts**

**macOS/Linux:**
```bash
chmod +x scripts/*.sh
./scripts/run-agent.sh
```

**Windows:**
```powershell
# Just run directly:
java -jar target\agent-cloud-optimizer-1.0-SNAPSHOT.jar
```

### **Issue 5: "Artifacts directory not found"**

**Solution:** Created automatically on first run
```bash
# Just run the agent, it will create it:
./scripts/run-agent.sh
```

---

## 📖 Key Concepts to Understand

### **1. Concurrency**
**What:** Number of parallel threads processing requests  
**Why it matters:** More threads = higher throughput, but uses more resources

### **2. Latency**
**What:** Time to process one request  
**Why it matters:** Lower latency = faster response times

### **3. Throughput**
**What:** Requests processed per second  
**Why it matters:** Higher throughput = more work done

### **4. Optimization**
**What:** Adjusting concurrency to balance latency and throughput  
**Why it matters:** Find the sweet spot - fast AND efficient

### **5. Autonomous**
**What:** System makes decisions without human input  
**Why it matters:** Faster response, no manual work

### **6. Explainable**
**What:** You can see WHY a decision was made  
**Why it matters:** Trust, debugging, learning

---

## 🎯 What to Do Next

### **If You Want to Learn:**
1. ✅ Read [LEARNING_GUIDE.md](LEARNING_GUIDE.md)
2. ✅ Try the experiments above
3. ✅ Read the reasoning traces
4. ✅ Modify the code and see what happens

### **If You Want to Use It:**
1. ✅ Read [README.md](../../README.md) for full documentation
2. ✅ Integrate with your own Java services
3. ✅ Customize for your specific needs
4. ✅ Deploy to production (with testing!)

### **If You Want to Contribute:**
1. ✅ Read [CONTRIBUTING.md](../../CONTRIBUTING.md) (if it exists)
2. ✅ Check GitHub issues for "Good First Issue"
3. ✅ Fork the repo and make improvements
4. ✅ Submit pull requests

### **If You're a Student:**
1. ✅ Use this for your portfolio
2. ✅ Write a blog post about what you learned
3. ✅ Present it in class or hackathon
4. ✅ Mention in job interviews

---

## 📚 Complete Documentation Map

```
START_HERE.md ← YOU ARE HERE!
    |
    ├── README.md               (Project overview)
    ├── LEARNING_GUIDE.md       (5-level learning path)
    ├── PROBLEM_SOLUTION_ALIGNMENT.md (Why this matters)
    ├── OLLAMA_SETUP.md         (LLM installation)
    ├── WINDOWS_SETUP.md        (Windows users)
    └── examples/README.md      (Understanding outputs)
```

### **Quick Reference:**
- **New user?** Read START_HERE.md (this file)
- **Want to learn?** Read LEARNING_GUIDE.md
- **On Windows?** Read WINDOWS_SETUP.md  
- **Need LLM help?** Read OLLAMA_SETUP.md
- **Troubleshooting?** See README.md
- **Understanding results?** See examples/README.md

---

## ✅ Quick Checklist

**Before you start:**
- [ ] Java 17+ installed
- [ ] Maven 3.8+ installed
- [ ] Project downloaded/cloned

**First run:**
- [ ] Build successful (`mvn clean package`)
- [ ] Simple agent runs (`./scripts/run-agent.sh`)
- [ ] Artifacts generated (check `artifacts/` folder)
- [ ] Viewed results (demo.html or JSON files)

**Understanding:**
- [ ] Read the optimization summary
- [ ] Checked the reasoning trace
- [ ] Understand what happened

**Next steps:**
- [ ] Read LEARNING_GUIDE.md
- [ ] Try an experiment
- [ ] (Optional) Install Ollama and try LLM agent

---

## 🎊 You Did It!

If you got this far, you:
1. ✅ Installed prerequisites
2. ✅ Built the project  
3. ✅ Ran your first autonomous optimization
4. ✅ Saw the results
5. ✅ Understand the basics

**Next:** Choose a learning path above and keep going! 🚀

---

## 💬 Questions?

- **Documentation:** See [README.md](../../README.md)
- **Learning:** See [LEARNING_GUIDE.md](LEARNING_GUIDE.md)
- **Issues:** Check GitHub Issues
- **Community:** Join Discord/Slack (if available)

---

**Welcome to Agent Cloud Optimizer! Happy learning! 🐶**
