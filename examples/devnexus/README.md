# DevNexus 2026 — Pre-generated Demo Artifacts

These files are **pre-generated backup artifacts** for the DevNexus 2026 live demo.
If the live demo fails for any reason (network, Ollama timeout, etc.), reference these.

## Files

| File | Description |
|------|-------------|
| `baseline.json` | Metrics snapshot before optimization |
| `after.json` | Metrics snapshot after ACO ran |
| `report.json` | Full comparison report with SLO data |
| `reasoning_trace.txt` | LLM agent step-by-step reasoning |

## Key Numbers to Know (for slides)

| Metric | Before | After | Change |
|--------|--------|-------|--------|
| p99 latency | 150.3ms | 62.4ms | **-58.5%** |
| p95 latency | 95.4ms | 58.7ms | **-38.5%** |
| Heap size | 8192 MB | 6144 MB | **-25%** |
| GC frequency | 2.1/sec | 0.8/sec | **-61.9%** |
| Cost estimate | $0.0054 | $0.0041 | **-24.1%** |
| SLO status | 🔴 BREACH | ✅ RESTORED | — |

## Demo Scenario

- **SLO target**: p99 < 100ms
- **SLO breach trigger**: p99 > 120ms for 3 consecutive intervals
- **Root cause ACO identified**: 8GB heap with <1% usage causing excessive GC scan time → p99 spikes
- **Fix**: Reduce heap (8192→6144 MB) + reduce threads (4→3)
- **Result**: p99 drops from 150ms to 62ms, SLO restored, 24% cost reduction

## How to Load Pre-generated Artifacts

If the live demo doesn't run, copy these to the `artifacts/` directory:

```bash
cp examples/devnexus/baseline.json artifacts/baseline.json
cp examples/devnexus/after.json artifacts/after.json
cp examples/devnexus/report.json artifacts/report.json
```

Then open: http://localhost:8080/results.html
