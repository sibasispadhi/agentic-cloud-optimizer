**[← Back to README](../README.md)** | **[← Previous: START_HERE](START_HERE.md)**

---

# Ollama Setup for Agent Cloud Optimizer

## Overview

ACO uses Ollama for 100% offline LLM-based optimization. No API keys or external services required.

**Current Tested Version**: Ollama 0.13.5 with Llama 3.2 models

## Quick Setup (TL;DR)

```bash
# 1. Install Ollama
brew install ollama  # macOS

# 2. Start service
ollama serve

# 3. Pull recommended model (1.3GB - fast!)
ollama pull llama3.2:1b

# 4. Verify
ollama list
curl http://localhost:11434/api/tags

# 5. Run your app
java -jar target/agent-cloud-optimizer-0.2.0.jar
```

## Installation

### macOS

```bash
brew install ollama
```

### Linux

```bash
curl -fsSL https://ollama.com/install.sh | sh
```

### Windows

Download from: https://ollama.com/download

## Starting Ollama Service

```bash
ollama serve
```

This starts the Ollama service on `http://localhost:11434`

## Pulling Models

### Recommended: Llama 3.2 (Default - Optimized for Speed)

```bash
# Fastest - 1.3GB (Recommended for demos and development)
ollama pull llama3.2:1b

# Balanced - 2.0GB (Better quality, still fast)
ollama pull llama3.2:3b
```

### Alternative: Older Models

```bash
# Legacy models (larger, slower)
ollama pull llama2      # 3.8GB
ollama pull mistral     # 4.1GB
ollama pull llama3      # 7.4GB
```

## Configuration

### application-dev.yml

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: llama3.2:1b  # Recommended: fast and efficient
          # Alternatives: llama3.2:3b, llama2, mistral
          temperature: 0.7
          num-predict: 500
          top-p: 0.9
```

### Environment Variables

```bash
# Override Ollama URL
export OLLAMA_BASE_URL=http://localhost:11434

# Override model
export OLLAMA_MODEL=llama3.2:1b  # or llama3.2:3b, llama2, mistral

# Select agent strategy
export AGENT_STRATEGY=llm  # This is the default now! Only use 'simple' for testing/fallback
```

## Verifying Installation

### Test Ollama Service

```bash
curl http://localhost:11434/api/tags
```

### Test Model

```bash
ollama run llama3.2:1b "Hello, how are you?"
```

## Agent Selection

### Use Rule-Based Agent (Fallback Mode - Not Recommended)

**Note**: This is NOT the intended use case for ACO. The LLM-powered mode is what makes ACO valuable!

```bash
java -jar target/agent-cloud-optimizer-0.2.0.jar \
  -Dagent.strategy=simple
```

### Use LLM Agent (Requires Ollama)

```bash
java -jar target/agent-cloud-optimizer-0.2.0.jar \
  -Dagent.strategy=llm
```

## Troubleshooting

### Ollama Not Running

**Error**: `Connection refused to localhost:11434`

**Solution**:

```bash
# Start Ollama service
ollama serve

# Or run in background (macOS/Linux)
nohup ollama serve > /dev/null 2>&1 &

# Check if running
curl http://localhost:11434/api/tags
```

### Model Not Found

**Error**: `model 'llama3.2:1b' not found`

**Solution**:

```bash
# Pull the model
ollama pull llama3.2:1b

# Verify it's available
ollama list
```

### Port Already in Use

**Error**: `bind: address already in use`

**Solution**:

```bash
# Find process using port 11434
lsof -i :11434

# Kill existing Ollama process
pkill ollama

# Restart
ollama serve
```

### Slow Response Times

- **First request**: Model loading takes 2-10 seconds (normal for llama3.2:1b)
- **Subsequent requests**: Should be 1-3 seconds
- **Solutions**:
  - Keep Ollama service running between requests
  - Use smaller models (llama3.2:1b is fastest)
  - Reduce `num-predict` tokens in configuration
  - Close other memory-intensive applications

### Memory Issues

**Symptoms**: System slowdown, OOM errors

**Requirements**:

- Llama3.2:1b: ~1.5GB RAM (Recommended)
- Llama3.2:3b: ~2.5GB RAM
- Llama2: ~4GB RAM
- Mistral: ~4GB RAM
- Llama3: ~8GB RAM
- Recommended: 4GB+ total system RAM (8GB+ for larger models)

**Solutions**:

```bash
# Use smallest model
ollama pull llama3.2:1b

# Check memory usage
ollama ps

# Unload models to free memory
ollama stop llama3.2:1b
```

### Spring AI Connection Errors

**Error**: `Unable to connect to Ollama`

**Checklist**:

1. ✅ Ollama service is running: `curl http://localhost:11434`
2. ✅ Model is pulled: `ollama list`
3. ✅ Base URL is correct in `application.yml`
4. ✅ No firewall blocking localhost:11434
5. ✅ Check application logs for detailed error

### macOS Specific Issues

#### Ollama Not Starting

```bash
# Check if installed
which ollama

# Reinstall if needed
brew reinstall ollama

# Start as service
brew services start ollama
```

#### Permission Denied

```bash
# Fix permissions
sudo chmod +x /usr/local/bin/ollama
```

### Linux Specific Issues

#### Service Not Running

```bash
# Check systemd status
systemctl status ollama

# Enable on boot
sudo systemctl enable ollama

# Start service
sudo systemctl start ollama
```

### Windows Specific Issues

#### Service Not Starting

1. Check Windows Services for "Ollama"
2. Restart the service
3. Run as Administrator if needed
4. Check Windows Firewall settings

### Performance Benchmarking

Test your Ollama installation:

```bash
# Simple response time test
time ollama run llama3.2:1b "What is 2+2?"

# Expected results:
# - First run: 2-10 seconds (cold start)
# - Second run: 1-3 seconds (warm)
```

## Model Comparison

| Model        | Size  | Speed      | Quality   | RAM Usage | Recommended For          |
| ------------ | ----- | ---------- | --------- | --------- | ------------------------ |
| llama3.2:1b  | 1.3GB | Very Fast  | Good      | ~1.5GB    | **Demos, Development**   |
| llama3.2:3b  | 2.0GB | Fast       | Very Good | ~2.5GB    | **Production (Balanced)**|
| mistral      | 4.1GB | Medium     | Good      | ~4GB      | Legacy production        |
| llama2       | 3.8GB | Medium     | Good      | ~4GB      | Legacy general use       |
| llama3       | 7.4GB | Slow       | Excellent | ~8GB      | High accuracy needs      |

## Performance Tips

1. **Keep Ollama running**: Don't restart between requests
2. **Warm up**: First request loads model into memory
3. **Adjust temperature**: Lower (0.3-0.5) for deterministic, higher (0.7-0.9) for creative
4. **Limit tokens**: `num-predict: 500` balances speed vs detail

## Security Notes

- Ollama runs locally - no data leaves your machine
- No API keys or authentication required
- Models are downloaded once and cached locally
- Safe for sensitive production data

## References

- Ollama Documentation: https://ollama.com/docs
- Model Library: https://ollama.com/library
- Spring AI Ollama: https://docs.spring.io/spring-ai/reference/api/clients/ollama-chat.html
