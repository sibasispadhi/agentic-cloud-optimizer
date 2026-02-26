/* Dashboard controller for Agent Cloud Optimizer live dashboard */
/* Handles WebSocket connection, progress events, SLO status, and metrics display */

let stompClient = null;
let isOptimizing = false;
let baselineData = null;
let afterData = null;

// DOM elements
const startBtn = document.getElementById('startBtn');
const statusBadge = document.getElementById('statusBadge');
const progressBar = document.getElementById('progressBar');
const progressText = document.getElementById('progressText');
const phasesTimeline = document.getElementById('phasesTimeline');
const reasoningContent = document.getElementById('reasoningContent');
const comparisonCard = document.getElementById('comparisonCard');
const connectionDot = document.getElementById('connectionDot');
const connectionStatus = document.getElementById('connectionStatus');
const thinkingIcon = document.getElementById('thinkingIcon');
const reasoningThinking = document.getElementById('reasoningThinking');

// Initialize phase timeline
function initializePhases() {
    const phases = [
        { icon: '🔧', name: 'Initialize' },
        { icon: '📊', name: 'Baseline' },
        { icon: '🎯', name: 'SLO Check' },
        { icon: '🧠', name: 'AI Analysis' },
        { icon: '🚀', name: 'Optimize' },
        { icon: '✅', name: 'Complete' }
    ];

    phasesTimeline.innerHTML = phases.map(phase => `
        <div class="phase-item pending">
            <div class="phase-icon">${phase.icon}</div>
            <div class="phase-name">${phase.name}</div>
        </div>
    `).join('');
}

// Connect to WebSocket
function connect() {
    const socket = new SockJS('/ws-optimize');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function(frame) {
        console.log('Connected: ' + frame);
        connectionDot.classList.add('connected');
        connectionStatus.textContent = 'Connected';
        startBtn.disabled = false;

        stompClient.subscribe('/topic/optimization-progress', function(message) {
            handleProgressEvent(JSON.parse(message.body));
        });

        stompClient.subscribe('/topic/optimization-status', function(message) {
            console.log('Status update:', JSON.parse(message.body));
        });
    }, function(error) {
        console.error('WebSocket error:', error);
        connectionDot.classList.remove('connected');
        connectionStatus.textContent = 'Disconnected - Retrying...';
        setTimeout(connect, 3000);
    });
}

// Handle progress events
function handleProgressEvent(event) {
    console.log('Progress event:', event);

    switch(event.eventType) {
        case 'phase':     updatePhase(event);          break;
        case 'metric':    updateMetrics(event);        break;
        case 'reasoning': addReasoning(event.message); break;
        case 'complete':  handleCompletion(event);     break;
        case 'error':     handleError(event);          break;
    }

    if (event.progressPercent !== undefined) {
        updateProgressBar(event.progressPercent);
    }
}

// Update phase display
function updatePhase(event) {
    const phaseItems = phasesTimeline.querySelectorAll('.phase-item');
    const phaseIndex = Math.floor(event.progressPercent / 20);

    phaseItems.forEach((item, index) => {
        item.classList.remove('active', 'complete', 'pending');
        if (index < phaseIndex) item.classList.add('complete');
        else if (index === phaseIndex) item.classList.add('active');
        else item.classList.add('pending');
    });

    if (event.message && event.message.includes('AI')) {
        thinkingIcon.style.display = 'inline-block';
        reasoningThinking.style.display = 'inline-block';
    }
}

function updateProgressBar(percent) {
    progressBar.style.width = percent + '%';
    progressText.textContent = percent + '%';
}

// Update metrics display
function updateMetrics(event) {
    const data = event.data;
    const setVal = (id, val) => { document.getElementById(id).textContent = val; };

    if (data.concurrency !== undefined)        setVal('concurrencyValue', data.concurrency);
    if (data.median_latency_ms !== undefined)  setVal('latencyValue',     data.median_latency_ms.toFixed(2) + ' ms');
    if (data.avg_latency_ms !== undefined)     setVal('avgLatencyValue',  data.avg_latency_ms.toFixed(2) + ' ms');
    if (data.p95_latency_ms !== undefined)     setVal('p95LatencyValue',  data.p95_latency_ms.toFixed(2) + ' ms');
    if (data.p99_latency_ms !== undefined)     setVal('p99LatencyValue',  data.p99_latency_ms.toFixed(2) + ' ms');
    if (data.requests_per_second !== undefined) setVal('throughputValue', data.requests_per_second.toFixed(0));
    if (data.total_requests !== undefined)     setVal('requestsValue',    data.total_requests.toLocaleString());
    if (data.cost_estimate_usd !== undefined)  setVal('costValue',        '$' + data.cost_estimate_usd.toFixed(4));

    if (data.heap_metrics) {
        const heap = data.heap_metrics;
        setVal('heapSizeValue',  heap.heap_size_mb + ' MB');
        setVal('heapUsageValue', heap.heap_usage_percent.toFixed(1) + '%');
        setVal('gcFreqValue',    heap.gc_frequency_per_sec.toFixed(2) + '/sec');
        setVal('gcPauseValue',   heap.gc_pause_time_avg_ms.toFixed(1) + ' ms');
    }

    if (event.message && event.message.includes('Baseline'))      baselineData = data;
    else if (event.message && event.message.includes('Optimization')) afterData = data;
}

// Add reasoning entry
function addReasoning(message) {
    const entry = document.createElement('div');
    entry.className = 'reasoning-item';
    entry.textContent = message;

    if (message.includes('SLO BREACH') || message.includes('SLO compliant')) {
        showSloStatus(message);
    }

    if (reasoningContent.querySelector('p')) reasoningContent.innerHTML = '';
    reasoningContent.appendChild(entry);
    reasoningContent.scrollTop = reasoningContent.scrollHeight;
}

// Show SLO status card
function showSloStatus(message) {
    const card = document.getElementById('sloStatusCard');
    const content = document.getElementById('sloStatusContent');
    const isBreach = message.includes('BREACH');

    content.style.borderLeftColor = isBreach ? 'var(--error, #ea1100)' : 'var(--success, #2a8703)';
    content.style.background = isBreach ? 'rgba(234, 17, 0, 0.06)' : 'rgba(42, 135, 3, 0.06)';

    content.innerHTML = `
        <div style="display: flex; align-items: center; gap: 12px;">
            <span style="font-size: 2rem;">${isBreach ? '❌' : '✅'}</span>
            <div>
                <div style="font-size: 1.1rem; font-weight: 700; color: ${isBreach ? 'var(--error, #ea1100)' : 'var(--success, #2a8703)'};">
                    ${isBreach ? 'SLO BREACH DETECTED' : 'SLO COMPLIANT'}
                </div>
                <div style="font-size: 0.85rem; color: var(--muted); margin-top: 4px;">${message}</div>
            </div>
        </div>
    `;
    card.style.display = 'block';
}

// Handle completion
function handleCompletion(event) {
    isOptimizing = false;
    statusBadge.className = 'status-badge success';
    statusBadge.textContent = 'Complete ';
    startBtn.disabled = false;
    thinkingIcon.style.display = 'none';
    reasoningThinking.style.display = 'none';

    if (event.data && event.data.decision) showAISummary(event.data.decision);

    if (event.data && event.data.slo_compliance) {
        const slo = event.data.slo_compliance;
        if (slo.breached && slo.slo_restored_after_optimization) {
            const card = document.getElementById('sloStatusCard');
            const content = document.getElementById('sloStatusContent');
            content.style.borderLeftColor = 'var(--success, #2a8703)';
            content.style.background = 'rgba(42, 135, 3, 0.06)';
            content.innerHTML = `
                <div style="display: flex; align-items: center; gap: 12px;">
                    <span style="font-size: 2rem;">🎉</span>
                    <div>
                        <div style="font-size: 1.1rem; font-weight: 700; color: var(--success, #2a8703);">SLO RESTORED AFTER OPTIMIZATION</div>
                        <div style="font-size: 0.85rem; color: var(--muted); margin-top: 4px;">
                            p99: ${slo.baseline_p99_ms.toFixed(1)}ms → ${slo.after_p99_ms.toFixed(1)}ms (target: &lt;${slo.breach_threshold_ms.toFixed(0)}ms)
                        </div>
                    </div>
                </div>
            `;
            card.style.display = 'block';
        }
    }

    if (event.data && event.data.baseline && event.data.after) showComparison(event.data);
    console.log('🎉 Optimization complete!');
}

// Handle errors
function handleError(event) {
    isOptimizing = false;
    statusBadge.className = 'status-badge error';
    statusBadge.textContent = 'Error ❌';
    startBtn.disabled = false;
    thinkingIcon.style.display = 'none';
    reasoningThinking.style.display = 'none';
    addReasoning('❌ Error: ' + event.message);
}

// Show AI Summary
function showAISummary(decision) {
    const aiSummaryCard = document.getElementById('aiSummaryCard');
    const aiSummaryContent = document.getElementById('aiSummaryContent');
    const concurrencyMatch = decision.recommendation.match(/(\d+)/);
    const recommendedConcurrency = concurrencyMatch ? concurrencyMatch[1] : 'N/A';
    const fullReasoning = decision.reasoning || '';
    const reasoningParts = fullReasoning.split(/\.\s+(?=[A-Z])/);

    let concurrencySummary = decision.recommendation || `Set concurrency to ${recommendedConcurrency} threads`;
    let heapSummary = decision.recommended_heap_size_mb
        ? `Recommended heap size: ${decision.recommended_heap_size_mb} MB`
        : 'No heap adjustment needed';

    if (reasoningParts.length >= 2) {
        concurrencySummary = reasoningParts[0] + '.';
        heapSummary = reasoningParts.slice(1).join('. ');
    } else if (fullReasoning) {
        concurrencySummary = fullReasoning;
        heapSummary = decision.recommended_heap_size_mb
            ? `Set heap to ${decision.recommended_heap_size_mb} MB for optimal memory management and GC performance`
            : 'Maintain current heap size';
    }

    aiSummaryContent.innerHTML = buildAISummaryHTML(decision, recommendedConcurrency, concurrencySummary, heapSummary);
    aiSummaryCard.style.display = 'block';
    aiSummaryCard.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function buildAISummaryHTML(d, concurrency, concSummary, heapSummary) {
    return `
    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-4); margin-bottom: var(--space-4);">
        <div style="padding: var(--space-4); background: linear-gradient(135deg, rgba(59,130,246,0.1) 0%, rgba(37,99,235,0.05) 100%); border-radius: 8px; border: 1px solid rgba(59,130,246,0.3);">
            <div style="display: flex; align-items: center; margin-bottom: var(--space-3);">
                <span style="font-size: 1.5rem; margin-right: var(--space-2);">⚡</span>
                <h3 style="color: var(--primary); margin: 0; font-size: 0.95rem; font-weight: 600; text-transform: uppercase;">Concurrency</h3>
            </div>
            <div style="font-size: 0.75rem; color: var(--muted);">RECOMMENDED</div>
            <div style="font-size: 2rem; font-weight: 700; color: var(--primary);">${concurrency} <span style="font-size: 0.9rem; font-weight: 400; color: var(--muted);">threads</span></div>
            <div style="font-size: 0.85rem; color: var(--muted); margin-top: var(--space-2);"><strong>Summary:</strong> ${concSummary}</div>
            ${d.concurrency_confidence ? `<div style="margin-top: var(--space-3); padding: var(--space-2); background: rgba(59,130,246,0.1); border-radius: 4px; text-align: center;"><div style="font-size: 0.7rem; color: var(--muted);">Confidence</div><div style="font-size: 1.5rem; font-weight: 700; color: var(--primary);">${(d.concurrency_confidence*100).toFixed(0)}%</div></div>` : ''}
        </div>
        <div style="padding: var(--space-4); background: linear-gradient(135deg, rgba(16,185,129,0.1) 0%, rgba(5,150,105,0.05) 100%); border-radius: 8px; border: 1px solid rgba(16,185,129,0.3);">
            <div style="display: flex; align-items: center; margin-bottom: var(--space-3);">
                <span style="font-size: 1.5rem; margin-right: var(--space-2);">💾</span>
                <h3 style="color: var(--success); margin: 0; font-size: 0.95rem; font-weight: 600; text-transform: uppercase;">Heap Memory</h3>
            </div>
            <div style="font-size: 0.75rem; color: var(--muted);">RECOMMENDED</div>
            <div style="font-size: 2rem; font-weight: 700; color: var(--success);">${d.recommended_heap_size_mb || 'N/A'} <span style="font-size: 0.9rem; font-weight: 400; color: var(--muted);">MB</span></div>
            <div style="font-size: 0.85rem; color: var(--muted); margin-top: var(--space-2);"><strong>Summary:</strong> ${heapSummary}</div>
            ${d.heap_confidence ? `<div style="margin-top: var(--space-3); padding: var(--space-2); background: rgba(16,185,129,0.1); border-radius: 4px; text-align: center;"><div style="font-size: 0.7rem; color: var(--muted);">Confidence</div><div style="font-size: 1.5rem; font-weight: 700; color: var(--success);">${(d.heap_confidence*100).toFixed(0)}%</div></div>` : ''}
        </div>
    </div>
    <div style="margin-bottom: var(--space-4); padding: var(--space-4); background: rgba(139,92,246,0.05); border-radius: 8px; border-left: 4px solid var(--primary);">
        <h3 style="color: var(--primary); margin: 0 0 var(--space-3) 0; font-size: 0.95rem; font-weight: 600;">🧠 AI Analysis & Reasoning</h3>
        <p style="font-size: 0.95rem; margin: 0; line-height: 1.7;">${d.reasoning}</p>
    </div>
    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-3);">
        <div style="padding: var(--space-3); background: rgba(16,185,129,0.08); border-radius: 6px; text-align: center;">
            <div style="font-size: 0.75rem; color: var(--muted); text-transform: uppercase;">AI Confidence</div>
            <div style="font-size: 1.8rem; font-weight: 700; color: var(--success);">${(d.confidence_score*100).toFixed(0)}%</div>
        </div>
        <div style="padding: var(--space-3); background: rgba(251,191,36,0.08); border-radius: 6px; text-align: center;">
            <div style="font-size: 0.75rem; color: var(--muted); text-transform: uppercase;">Impact Level</div>
            <div style="font-size: 1.5rem; font-weight: 700; color: var(--warning);">${d.impact_level}</div>
        </div>
    </div>`;
}

// Show comparison
function showComparison(report) {
    comparisonCard.style.display = 'block';
    const baseline = report.baseline;
    const after = report.after;

    document.getElementById('beforeMetrics').innerHTML = buildMetricsHTML(baseline, 'info');
    document.getElementById('afterMetrics').innerHTML = buildMetricsHTML(after, 'success', report.decision);
}

function buildMetricsHTML(data, cls, decision) {
    let html = `
        <div class="log-entry ${cls}"><strong>Concurrency:</strong> ${data.concurrency}</div>
        <div class="log-entry ${cls}"><strong>Median Latency:</strong> ${data.median_latency_ms.toFixed(2)} ms</div>
        <div class="log-entry ${cls}"><strong>Avg Latency:</strong> ${data.avg_latency_ms.toFixed(2)} ms</div>
        <div class="log-entry ${cls}"><strong>P95 Latency:</strong> ${data.p95_latency_ms.toFixed(2)} ms</div>
        <div class="log-entry ${cls}"><strong>Throughput:</strong> ${data.requests_per_second.toFixed(0)} req/s</div>
        <div class="log-entry ${cls}"><strong>Total Requests:</strong> ${data.total_requests.toLocaleString()}</div>
        <div class="log-entry ${cls}"><strong>Cost Estimate:</strong> $${data.cost_estimate_usd.toFixed(4)}</div>
    `;
    if (data.heap_metrics) {
        const h = data.heap_metrics;
        html += `
            <div style="margin-top: 10px; padding-top: 10px; border-top: 1px solid var(--border);"></div>
            <div class="log-entry ${cls}"><strong>Heap Size:</strong> ${h.heap_size_mb} MB</div>
            <div class="log-entry ${cls}"><strong>Heap Usage:</strong> ${h.heap_usage_percent.toFixed(1)}%</div>
            <div class="log-entry ${cls}"><strong>GC Frequency:</strong> ${h.gc_frequency_per_sec.toFixed(2)}/sec</div>
            <div class="log-entry ${cls}"><strong>GC Pause Avg:</strong> ${h.gc_pause_time_avg_ms.toFixed(1)} ms</div>
        `;
    }
    if (decision && decision.recommended_heap_size_mb) {
        html += `<div style="margin-top: 10px; padding: 10px; background: var(--success-bg); border-radius: 6px;"><strong>💡 Recommended Heap:</strong> ${decision.recommended_heap_size_mb} MB</div>`;
    }
    return html;
}

// Start optimization
function startOptimization() {
    if (isOptimizing || !stompClient) return;

    isOptimizing = true;
    statusBadge.className = 'status-badge running';
    statusBadge.textContent = 'Running...';
    startBtn.disabled = true;
    comparisonCard.style.display = 'none';
    document.getElementById('sloStatusCard').style.display = 'none';
    document.getElementById('aiSummaryCard').style.display = 'none';
    reasoningContent.innerHTML = '';
    baselineData = null;
    afterData = null;
    updateProgressBar(0);
    stompClient.send('/app/start-optimization', {}, JSON.stringify({}));
}

// Event listeners
startBtn.addEventListener('click', startOptimization);

// Initialize
initializePhases();
connect();
