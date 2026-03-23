**[← Back to README](../README.md)** | **[Next: OLLAMA_SETUP →](OLLAMA_SETUP.md)**

---

# START HERE

Use this page if you want to get ACO running locally without guessing which script does what.

---

## 1) Prerequisites

You need:
- Java **21+**
- Maven **3.8+**
- Ollama (**only if you want LLM mode**)

Quick checks:

```bash
java -version
mvn -version
ollama --version
```

If you do **not** want to install Ollama right now, use the SimpleAgent Docker flow.

---

## 2) Clone the repository

```bash
git clone https://github.com/sibasispadhi/agentic-cloud-optimizer.git
cd agentic-cloud-optimizer
```

---

## 3) Choose how you want to run ACO

### Option A — Docker with local LLM
```bash
docker compose up --build
```

What you get:
- ACO web app
- local Ollama-backed reasoning
- dashboard + results pages

Open:
- http://localhost:8081/live-dashboard.html
- http://localhost:8081/results.html

### Option B — Docker with SimpleAgent only
```bash
docker compose -f docker-compose.simple.yml up --build
```

Use this if you want:
- no model download
- faster startup
- deterministic fallback behavior only

Open:
- http://localhost:8081/live-dashboard.html
- http://localhost:8081/results.html

### Option C — Local development

Start Ollama first:

```bash
ollama serve
ollama pull llama3.2:3b
```

Then run the app:

```bash
mvn spring-boot:run
```

Open:
- http://localhost:8081/live-dashboard.html
- http://localhost:8081/results.html

---

## 4) Build the project manually (optional)

If you want the packaged JAR first:

```bash
mvn clean package -DskipTests
```

Then run:

### macOS/Linux
```bash
./scripts/run-agent.sh
```

### Windows
```powershell
scripts\run-agent.bat
```

For the live UI:

### macOS/Linux
```bash
./scripts/run-web-ui.sh
```

### Windows
```powershell
scripts\run-web-ui.bat
```

---

## 5) What you should see

After a successful run, ACO should produce:

- live progress updates in the dashboard
- a results page at `/results.html`
- JSON outputs under `artifacts/`

Typical files in `artifacts/`:
- `baseline.json`
- `after.json`
- `report.json`
- optimization artifacts
- reasoning traces
- validation / rollback records

You can also inspect example outputs in **[examples/README.md](../examples/README.md)**.

---

## 6) Common issues

### Ollama connection refused
```bash
ollama serve
curl http://localhost:11434/api/tags
```

### Script permission denied (macOS/Linux)
```bash
chmod +x scripts/*.sh
```

### Java too old
Install Java 21+ (Temurin is a good default):
https://adoptium.net/

### Docker feels easier than local setup
Yes. That is a feature, not a personal failure.
Use:
```bash
docker compose up --build
```

---

## 7) Where to go next

- **[OLLAMA_SETUP.md](OLLAMA_SETUP.md)** — Ollama installation help
- **[WHAT_THIS_IS.md](WHAT_THIS_IS.md)** — scope, boundaries, and governance behavior
- **[INDEX.md](INDEX.md)** — full documentation map
- **[WINDOWS_SETUP.md](WINDOWS_SETUP.md)** — Windows-specific help
