# Contributing to Agent Cloud Optimizer

First off, thank you for considering contributing to ACO! 🎉

It's people like you that make open source such a great community.

---

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Setup](#development-setup)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Community](#community)

---

## 🤝 Code of Conduct

This project adheres to a simple code of conduct:

- **Be respectful** - Treat everyone with respect
- **Be collaborative** - Work together constructively
- **Be patient** - Remember everyone was a beginner once
- **Be inclusive** - Welcome diverse perspectives

By participating, you agree to uphold these values.

---

## 🎯 How Can I Contribute?

### **Reporting Bugs** 🐛

Before creating a bug report:
1. Check existing issues to avoid duplicates
2. Use the bug report template
3. Include as much detail as possible

**Good bug reports include:**
- Clear title and description
- Steps to reproduce
- Expected vs actual behavior
- System information (OS, Java version, etc.)
- Relevant logs or screenshots

### **Suggesting Enhancements** 💡

We welcome feature requests! Please:
1. Check if the feature was already requested
2. Explain the problem it solves
3. Describe the proposed solution
4. Consider if it fits the project scope

**Priority Features We Need:**
- Plugin architecture for external APIs
- Kubernetes integration
- Additional agent strategies
- Production deployment guides
- Performance improvements

### **Writing Documentation** 📝

Documentation improvements are always welcome!

**Areas that need help:**
- API integration examples
- Tutorial videos
- Blog posts
- Translation to other languages
- Architecture diagrams

### **Contributing Code** 💻

Ready to write code? Great!

**Good first issues:**
- Look for issues labeled `good first issue`
- Documentation improvements
- Test coverage
- Example integrations
- Bug fixes

---

## 🛠️ Development Setup

### **Prerequisites**

```bash
# Required
java --version  # Need Java 17+
mvn --version   # Need Maven 3.8+

# Optional (for LLM agent)
ollama --version
```

### **Fork and Clone**

```bash
# 1. Fork on GitHub (click "Fork" button)

# 2. Clone your fork
git clone https://github.com/YOUR_USERNAME/agent-cloud-optimizer
cd agent-cloud-optimizer

# 3. Add upstream remote
git remote add upstream https://github.com/ORIGINAL_AUTHOR/agent-cloud-optimizer
```

### **Build and Test**

```bash
# Build
mvn clean package

# Run tests
mvn test

# Run the application
./scripts/run-agent.sh
```

### **Development Workflow**

```bash
# 1. Create a branch
git checkout -b feature/my-awesome-feature

# 2. Make your changes
# ... code code code ...

# 3. Run tests
mvn test

# 4. Commit with clear message
git commit -m "Add plugin architecture for external APIs"

# 5. Push to your fork
git push origin feature/my-awesome-feature

# 6. Create Pull Request on GitHub
```

---

## 📬 Pull Request Process

### **Before Submitting**

- [ ] Code follows our style guide (see below)
- [ ] Tests pass (`mvn test`)
- [ ] New tests added for new features
- [ ] Documentation updated if needed
- [ ] Commit messages are clear
- [ ] Branch is up to date with main

### **PR Guidelines**

**Title Format:**
```
[Type] Brief description

Examples:
[Feature] Add WorkloadSimulator interface
[Fix] Resolve Ollama connection timeout
[Docs] Update Windows setup guide
[Test] Add unit tests for LLM agent
```

**Description Template:**
```markdown
## What does this PR do?
Brief summary of changes

## Why is this needed?
Problem this solves or feature it adds

## How was it tested?
Steps to verify the changes work

## Screenshots (if applicable)

## Checklist
- [ ] Tests pass
- [ ] Documentation updated
- [ ] No breaking changes (or documented)
```

### **Review Process**

1. Automated tests run on PR
2. Maintainer reviews code
3. Address feedback
4. Maintainer merges when ready

We aim to review PRs within 48 hours.

---

## 📐 Coding Standards

### **Java Code Style**

**Follow these guidelines:**

```java
// ✅ Good: Clear naming, proper JavaDoc
/**
 * Executes load test with specified concurrency.
 * 
 * @param concurrency number of parallel threads
 * @return test results with metrics
 */
public RunResult executeLoad(int concurrency) {
    // Implementation
}

// ❌ Bad: No docs, unclear naming
public RunResult exec(int c) {
    // Implementation
}
```

**Key Rules:**
- Max line length: 120 characters
- Max method length: 50 lines
- Max file length: 600 lines (hard limit!)
- Use meaningful variable names
- Add JavaDoc for public methods
- Follow DRY, SOLID, YAGNI principles

### **File Organization**

```
package structure:
com.cloudoptimizer.agent
├── config/          # Configuration classes
├── controller/      # REST endpoints
├── service/         # Business logic
├── model/           # Data models
└── util/            # Utility classes
```

### **Testing Standards**

```java
// Use descriptive test names
@Test
void testDecide_WhenLatencyHigh_ShouldIncreseConcurrency() {
    // Arrange
    int currentConcurrency = 4;
    List<MetricRow> highLatencyMetrics = createHighLatencyMetrics();
    
    // Act
    AgentDecision decision = agent.decide(highLatencyMetrics, currentConcurrency);
    
    // Assert
    assertTrue(decision.getRecommendation().contains("increase"));
}
```

### **Documentation Standards**

- Use Markdown for all docs
- Include code examples
- Add links to related docs
- Keep language clear and simple
- Test all code examples

---

## 🗂️ Project Structure

```
agent-cloud-optimizer/
├── .github/              # GitHub templates
├── docs/                 # Documentation
│   ├── guides/          # User guides
│   ├── reference/       # Reference docs
│   └── project/         # Project meta
├── examples/            # Example artifacts
├── scripts/             # Helper scripts
├── src/
│   ├── main/java/      # Source code
│   └── test/java/      # Tests
├── README.md           # Main documentation
├── CONTRIBUTING.md     # This file
└── LICENSE             # MIT License
```

---

## 🎯 Priority Contribution Areas

### **High Priority**

1. **Plugin Architecture** 🔌
   - Design WorkloadSimulator interface
   - Example implementations
   - Configuration system
   - Documentation

2. **Kubernetes Integration** ☸️
   - Deployment manifests
   - Helm charts
   - Auto-scaling integration
   - Monitoring setup

3. **Additional Agent Strategies** 🤖
   - Aggressive optimization
   - Conservative optimization
   - Cost-focused optimization
   - Hybrid strategies

### **Medium Priority**

4. **Production Features** 🏭
   - Circuit breakers
   - Retry logic
   - Health checks
   - Metrics export (Prometheus)

5. **Testing & Quality** ✅
   - Increase test coverage
   - Integration tests
   - Performance benchmarks
   - Load testing

6. **Documentation** 📚
   - Video tutorials
   - Blog posts
   - API integration guides
   - Case studies

---

## 🌟 Recognition

Contributors are recognized in:
- README.md (Contributors section)
- Release notes
- GitHub contributors page

**Top contributors may become maintainers!**

---

## 💬 Getting Help

**Stuck? Need guidance?**

- Open a GitHub Discussion
- Comment on the relevant issue
- Ask in PR comments
- Check existing documentation

**Response time:** Usually within 24-48 hours

---

## 📜 License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

## 🙏 Thank You!

Every contribution matters, whether it's:
- A bug report
- A typo fix
- A major feature
- Documentation improvement
- Answering questions

**You're helping build something valuable for the community!**

---

## 📞 Contact

- **GitHub Issues:** For bugs and features
- **GitHub Discussions:** For questions and ideas
- **Pull Requests:** For code contributions

---

**Happy coding! 🚀**
