**[← Back to README](../README.md)** | **[📚 Reading Path](../NEW_USER_READING_PATH.md)** | **[Next: LEARNING_GUIDE →](../docs/LEARNING_GUIDE.md)**

---

# Example Artifacts

This directory contains example output artifacts from ACO optimization runs.

## Files

### baseline.json

Performance metrics **before** optimization:

- Current concurrency setting
- Baseline latency (median, avg, p95)
- Baseline throughput (requests/second)
- Total requests processed
- Estimated cost

### after.json

Performance metrics **after** optimization:

- New concurrency setting
- Optimized latency metrics
- Improved throughput
- Total requests processed
- Updated cost estimate

### report.json

Comprehensive before/after comparison:

- Agent strategy used (llm by default, simple as fallback)
- Decision details (recommendation, reasoning, confidence)
- Performance improvements:
  - Latency changes (ms and %)
  - Throughput changes (RPS and %)
  - Concurrency adjustments
- Full baseline and after metrics

### reasoning_trace_llm.txt (Primary)

Detailed reasoning trace for LLM-powered agent decisions:

- Input parameters (current concurrency, target latency)
- LLM prompt sent (what metrics were provided)
- LLM response (JSON with newConcurrency, expectedLatencyMs, explanation)
- Parsed decision with AI reasoning
- Action items
- Confidence score and impact level
- Risks identified

**This is the main output - shows AI reasoning!**

### reasoning_trace_rule.txt (Fallback Only)

Basic reasoning trace for rule-based agent (fallback mode):

- Input parameters
- Simple threshold analysis
- Rule matched (if-else logic)
- Decision rationale
- Action items

**Only created when using simple mode (not recommended)**

## Understanding the Results

### Good Optimization Example

```json
{
  "improvements": {
    "latency_change_ms": -64.2, // Latency DECREASED (good!)
    "latency_change_percent": -45.1, // 45% faster
    "throughput_change_rps": +13.6, // Throughput INCREASED (good!)
    "throughput_change_percent": +47.7, // 47% more requests/sec
    "concurrency_change": +2 // Needed more workers
  },
  "decision": {
    "confidence_score": 0.95, // 95% confident
    "impact_level": "MEDIUM" // Moderate change
  }
}
```

### What to Look For

#### Successful Optimization

- ✅ Latency decreased (negative change)
- ✅ Throughput increased (positive change)
- ✅ High confidence (>0.8)
- ✅ Cost efficiency improved

#### When to Investigate

- ⚠️ Latency increased
- ⚠️ Throughput decreased
- ⚠️ Low confidence (<0.6)
- ⚠️ CRITICAL impact level

## Using These Examples

### Copy to Your Project

```bash
cp examples/*.json artifacts/
```

### Compare with Your Results

```bash
# Run your optimization
./scripts/run-agent.sh

# Compare outputs
diff examples/report.json artifacts/report.json
```

### Visualize Results

```bash
# Start web server
python3 -m http.server 8000

# Open demo page
open http://localhost:8000/demo.html
```

## Metrics Explanation

### Latency Metrics

- **median_latency_ms**: 50th percentile - typical request time
- **avg_latency_ms**: Mean of all requests
- **p95_latency_ms**: 95th percentile - worst 5% threshold

**Goal**: Lower is better (faster responses)

### Throughput Metrics

- **requests_per_second**: Total RPS achieved
- **total_requests**: Count of successful requests

**Goal**: Higher is better (more capacity)

### Cost Metrics

- **cost_estimate_usd**: Estimated cloud cost based on:
  - Request count
  - Compute time
  - Resource utilization

**Formula**: `cost = total_requests * $0.0001` (simplified model)

### Concurrency

- Number of parallel workers
- Higher = more parallelism but more resources
- Lower = less resources but potential bottleneck

**Goal**: Find optimal balance for your latency target

## Decision Confidence

### High Confidence (0.8-1.0)

- Clear performance signal
- Metrics strongly indicate direction
- Safe to apply automatically

### Medium Confidence (0.6-0.8)

- Moderate performance signal
- Some uncertainty
- Review before production

### Low Confidence (0.4-0.6)

- Weak performance signal
- High uncertainty
- Manual review required

### Very Low (<0.4)

- No clear signal
- Don't apply changes
- Investigate system behavior

## Next Steps

1. **Run your own optimization**: `./scripts/run-agent.sh`
2. **Compare results**: Check if your metrics match expected patterns
3. **Tune parameters**: Adjust concurrency, duration, target latency
4. **Visualize**: Open `demo.html` to see charts and graphs
5. **View LLM reasoning**: Check `reasoning_trace_llm.txt` to see AI analysis

## Questions?

- See [README.md](../README.md) for full documentation
- Check [LEARNING_GUIDE.md](../LEARNING_GUIDE.md) for deep dive
- Review [PROBLEM_SOLUTION_ALIGNMENT.md](../PROBLEM_SOLUTION_ALIGNMENT.md) for use cases
