# Agent Cloud Optimizer (ACO)

Autonomous, **LLM-powered** JVM performance optimizer for Java services.

Production-ready implementation that can run locally or in your infrastructure.

## Quick Start (recommended)

1) Follow **[docs/START_HERE.md](docs/START_HERE.md)**
2) Install Ollama via **[docs/OLLAMA_SETUP.md](docs/OLLAMA_SETUP.md)**
3) Verify setup: `./scripts/verify-ollama.sh`

## Run (CLI mode)

```bash
mvn clean package -DskipTests
./scripts/run-agent.sh
# Windows: scripts\run-agent.bat
```

## Run (Live Web Dashboard)

```bash
mvn clean package -DskipTests
./scripts/run-web-ui.sh
# Windows: scripts\run-web-ui.bat
```

Open: http://localhost:8080/live-dashboard.html

## View results

- Results page (served by the app): http://localhost:8080/results.html
- `artifacts/` directory (baseline/after/report + reasoning traces)

Legacy: `demo.html` now just points you to `/results.html`.

## Features

- ✅ **Concurrency Optimization**: Automatically tunes thread pool sizes
- ✅ **Heap Optimization**: Analyzes GC metrics and recommends optimal JVM heap size
- ✅ **LLM-Powered Analysis**: Uses local Ollama for intelligent reasoning
- ✅ **Rule-Based Fallback**: SimpleAgent provides fast, reliable decisions
- ✅ **Explainable AI**: Full reasoning traces for every decision
- ✅ **Real-time Dashboard**: WebSocket-powered live monitoring

## Optional docs

- What this is / isn't: **[docs/WHAT_THIS_IS.md](docs/WHAT_THIS_IS.md)**
- Windows setup: **[docs/WINDOWS_SETUP.md](docs/WINDOWS_SETUP.md)**
- Example outputs: **[examples/README.md](examples/README.md)**
- Heap optimization details: **[HEAP_OPTIMIZATION_STATUS.md](HEAP_OPTIMIZATION_STATUS.md)**

## License

MIT — see [LICENSE](LICENSE).