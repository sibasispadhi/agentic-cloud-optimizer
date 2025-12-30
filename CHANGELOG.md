# Changelog

All notable changes to Agent Cloud Optimizer will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

> 🗺️ See our [Product Roadmap](ROADMAP.md) for planned features beyond v0.2.0.

### Future Versions
See [ROADMAP.md](ROADMAP.md) for complete vision through v1.0.0 and beyond.

## [0.2.0] - 2025-12-30

### 🆕 Added - Plugin Architecture
- **WorkloadSimulator Interface** - Pluggable simulator architecture
  - Clean abstraction for different workload types
  - Health check validation before tests
  - Extensible for custom implementations
- **DemoWorkloadSimulator** - Refactored built-in simulator
  - Full backward compatibility with v0.1.0
  - Wraps existing LoadRunner functionality
  - Always-healthy demo mode
- **HttpRestWorkloadSimulator** - NEW! External HTTP API testing
  - Test any HTTP REST endpoint
  - Support for GET, POST, PUT, DELETE methods
  - Custom headers configuration (JSON format)
  - Request body support for POST/PUT
  - Multi-threaded load testing
  - Automatic health checks before testing
  - Full latency and throughput metrics
- **Simulator Selection** - YAML-based configuration
  - `workload.simulator` property ("demo" or "http")
  - System property overrides supported
  - Clear error messages for invalid simulators

### Changed
- **RunnerMain** refactored to use WorkloadSimulator plugins
  - No longer hardcoded to LoadRunner
  - Dynamic simulator selection at runtime
  - Added pre-flight health checks
- **LoadRunner** now wrapped by DemoWorkloadSimulator
  - Maintains all original functionality
  - Clean separation of concerns

### Technical Details
- Java 21
- Spring Boot 3.2.5
- Added Jackson for header JSON parsing
- RestTemplate for HTTP requests
- Full test coverage maintained (24 passing tests)

### Migration from v0.1.0
- **No breaking changes!** Default behavior unchanged
- To use HTTP simulator: Set `workload.simulator=http` in config
- See README for HTTP configuration examples

### Known Limitations (v0.2.0)
- HTTP simulator tested in controlled environments
- No ServiceLoader-based plugin discovery (manual registration)
- Simple header parsing (MVP implementation)
- No retry logic or circuit breakers
- Database/gRPC/MQ simulators deferred to v0.3.0+

### What This Enables
✅ **You can now optimize external HTTP APIs without code changes!**

Example:
```bash
java -Dworkload.simulator=http \
     -Dhttp.base-url=https://api.yourservice.com \
     -Dhttp.endpoint=/api/endpoint \
     -jar agent-cloud-optimizer-0.2.0.jar
```

## [0.1.0] - 2025-12-30

### Added
- Initial proof-of-concept release
- **SimpleAgent** - Rule-based autonomous optimization agent
  - Deterministic threshold-based decision making
  - Median latency analysis
  - Automatic concurrency adjustment recommendations
- **SpringAiLlmAgent** - AI-powered optimization agent
  - Local LLM integration via Ollama
  - Context-aware metric analysis
  - Natural language reasoning traces
  - 100% offline operation (no API keys required)
- **LoadRunner** - Self-contained load testing framework
  - Configurable concurrency and duration
  - Latency percentile calculations (p50, p95, p99)
  - Throughput measurement
  - Cost estimation
- **MetricsLogger** - Time-series metrics collection
  - JSONL format persistence
  - Real-time async logging
  - Metric aggregation and analysis
- **RunnerMain** - Complete optimization orchestration
  - Baseline performance measurement
  - Agent-based decision making
  - Post-optimization validation
  - Before/after comparison reports
- **Comprehensive Documentation**
  - Quick start guide (START_HERE.md)
  - Learning guide for students and engineers
  - Windows setup instructions
  - Ollama/LLM setup guide
  - Contributing guidelines
- **Test Coverage**
  - Unit tests for SimpleAgent (13 tests)
  - Unit tests for MetricRow (11 tests)
  - Test utilities and helpers
- **Demo Artifacts**
  - HTML visualization of results
  - JSON output for baseline and optimized runs
  - Reasoning trace files for both agent types
  - Example shell and batch scripts
- **GitHub Templates**
  - Bug report template
  - Feature request template
  - Pull request template

### Known Limitations
- Tests its own built-in demo API, not external services
- Requires code modification to test custom workloads
- Single-service optimization only (no multi-service orchestration)
- 7 LLM agent unit tests disabled due to Spring AI mocking complexity

### Technical Details
- Java 21
- Spring Boot 3.2.5
- Spring AI 0.8.0
- Maven build system
- MIT License

### Notes
This is a proof-of-concept demonstrating autonomous optimization technology.
It provides working code, full explainability, and educational value, but is
not yet a production-ready drop-in plugin.

---

## Release Types

- **Added** for new features
- **Changed** for changes in existing functionality
- **Deprecated** for soon-to-be removed features
- **Removed** for now removed features
- **Fixed** for any bug fixes
- **Security** in case of vulnerabilities

[Unreleased]: https://github.com/YOUR_USERNAME/agent-cloud-optimizer/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/YOUR_USERNAME/agent-cloud-optimizer/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/YOUR_USERNAME/agent-cloud-optimizer/releases/tag/v0.1.0
