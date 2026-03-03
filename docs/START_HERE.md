**[← Back to README](../README.md)** | **[Next: OLLAMA_SETUP →](OLLAMA_SETUP.md)**

---

# START HERE (run it locally)

## 1) Prereqs

- Java **21+**
- Maven **3.8+**
- Ollama (**required** for LLM mode)

Quick checks:

```bash
java -version
mvn -version
ollama --version
```

## 2) Get the code

```bash
git clone https://github.com/sibasispadhi/agent-cloud-optimizer.git
cd agent-cloud-optimizer
```

**Note:** Replace `sibasispadhi` with the actual GitHub username/organization where the repo is hosted.

## 3) Build

```bash
mvn clean package -DskipTests
```

## 4) Run (CLI mode)

### macOS/Linux

```bash
./scripts/run-agent.sh
```

### Windows

```powershell
scripts\run-agent.bat
```

## 5) Run (Live Web Dashboard)

### macOS/Linux

```bash
./scripts/run-web-ui.sh
```

### Windows

```powershell
scripts\run-web-ui.bat
```

Open: http://localhost:8081/live-dashboard.html

## 6) View results

- `demo.html` (recommended - requires Python 3):

  ```bash
  python3 -m http.server 8000
  # Then open in browser: http://localhost:8000/demo.html
  ```

  If Python 3 is not installed, alternatives:
  - Install Python 3 from https://python.org
  - OR use Node.js: `npm install -g http-server && http-server -p 8000`

- `artifacts/` directory: baseline/after/report JSON + reasoning traces

## Common issues

### Ollama connection refused

```bash
ollama serve
# then:
curl http://localhost:11434/api/tags
```

### Script permission denied (macOS/Linux)

```bash
chmod +x scripts/*.sh
```

### Java too old

Install Java 21+ (Temurin recommended): https://adoptium.net/
