package com.cloudoptimizer.agent.service;

import com.cloudoptimizer.agent.model.AgentDecision;
import com.cloudoptimizer.agent.model.MetricRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SpringAiLlmAgent}.
 *
 * <p>Uses mocked {@link ChatClient} to verify LLM agent logic without
 * requiring a live Ollama instance. {@code mockito-inline} on the classpath
 * enables mocking of final Spring-AI classes ({@link ChatResponse},
 * {@link Generation}).
 *
 * @author Sibasis Padhi
 */
class SpringAiLlmAgentTest {

    private SpringAiLlmAgent agent;
    private ChatClient mockChatClient;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        mockChatClient = mock(ChatClient.class);
        agent = new SpringAiLlmAgent(mockChatClient);

        // Inject @Value fields — Spring context is not available in unit tests
        setField("targetLatencyMs",          100.0);
        setField("artifactsDir",             tempDir.toString());
        setField("minConcurrency",           1);
        setField("maxConcurrency",           100);
        setField("impactCriticalChangePct",  50.0);
        setField("impactHighChangePct",      25.0);
        setField("impactMediumChangePct",    10.0);
        setField("confidenceLowChange",      0.90);
        setField("confidenceMediumChange",   0.80);
        setField("confidenceLargeChange",    0.70);
        setField("confidenceVeryLargeChange",0.60);
        setField("fallbackConfidence",       0.50);
    }

    // ── Happy-path tests ──────────────────────────────────────────────────────

    @Test
    void testDecide_SuccessfulLlmResponse() {
        List<MetricRow> metrics = createTestMetrics(150.0, 20);
        String json = "{'newConcurrency': 6, 'expectedLatencyMs': 85.0, 'explanation': 'Latency is high, increasing concurrency.'}"
                .replace("'", "\"");
        when(mockChatClient.call(any(Prompt.class))).thenReturn(mockResponse(json));

        AgentDecision decision = agent.decide(metrics, 4);

        assertNotNull(decision);
        assertEquals("Set concurrency to 6", decision.getRecommendation());
        assertTrue(decision.getReasoning().contains("Latency is high"));
        assertTrue(decision.getConfidenceScore() > 0.0);
        assertNotNull(decision.getImpactLevel());
    }

    @Test
    void testDecide_LlmResponseWithMarkdownCodeBlock() {
        List<MetricRow> metrics = createTestMetrics(50.0, 20);
        String json = ("```json\n{'newConcurrency': 5, 'expectedLatencyMs': 55.0, "
                + "'explanation': 'Latency below target, reducing concurrency.'}\n```")
                .replace("'", "\"");
        when(mockChatClient.call(any(Prompt.class))).thenReturn(mockResponse(json));

        AgentDecision decision = agent.decide(metrics, 8);

        assertNotNull(decision);
        assertEquals("Set concurrency to 5", decision.getRecommendation());
        assertTrue(decision.getReasoning().contains("reducing concurrency"));
    }

    // ── Error / fallback tests ────────────────────────────────────────────────

    @Test
    void testDecide_InvalidJsonReturnsFallback() {
        // Invalid JSON cannot be parsed → exception is caught → fallback returned
        when(mockChatClient.call(any(Prompt.class))).thenReturn(mockResponse("Not JSON at all!"));

        AgentDecision decision = agent.decide(createTestMetrics(100.0, 20), 4);

        assertNotNull(decision);
        assertTrue(decision.getRecommendation().contains("Maintain concurrency"),
                "Expected fallback recommendation but got: " + decision.getRecommendation());
        assertEquals(0.50, decision.getConfidenceScore(), 0.01);
    }

    @Test
    void testDecide_LlmExceptionReturnsFallback() {
        when(mockChatClient.call(any(Prompt.class)))
                .thenThrow(new RuntimeException("Connection refused to Ollama"));

        AgentDecision decision = agent.decide(createTestMetrics(100.0, 20), 4);

        assertNotNull(decision);
        assertTrue(decision.getRecommendation().contains("Maintain concurrency"));
        assertTrue(decision.getReasoning().contains("LLM agent encountered error"));
        assertEquals(0.50, decision.getConfidenceScore(), 0.01);
    }

    // ── Guard tests ───────────────────────────────────────────────────────────

    @Test
    void testDecide_EmptyMetricsListThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> agent.decide(new ArrayList<>(), 4));
    }

    @Test
    void testDecide_NullMetricsListThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> agent.decide(null, 4));
    }

    // ── Bounds / policy tests ─────────────────────────────────────────────────

    @Test
    void testDecide_ConcurrencyClampedToMax() {
        // LLM returns 150 - should be clamped to maxConcurrency (100)
        String json = "{'newConcurrency': 150, 'expectedLatencyMs': 50.0, 'explanation': 'Testing bounds'}"
                .replace("'", "\"");
        when(mockChatClient.call(any(Prompt.class))).thenReturn(mockResponse(json));

        AgentDecision decision = agent.decide(createTestMetrics(100.0, 20), 4);

        assertNotNull(decision);
        assertTrue(decision.getRecommendation().contains("100"),
                "Expected concurrency clamped to 100 but got: " + decision.getRecommendation());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<MetricRow> createTestMetrics(double latencyMs, int count) {
        List<MetricRow> metrics = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            metrics.add(MetricRow.builder()
                    .resourceId("test-resource")
                    .resourceType("TestService")
                    .metricName("latencyMs")
                    .metricValue(latencyMs + (Math.random() * 20 - 10))
                    .unit("ms")
                    .timestamp(Instant.now().minusSeconds(count - i))
                    .build());
        }
        return metrics;
    }

    /** Returns a real ChatResponse built from the given content string. */
    private ChatResponse mockResponse(String content) {
        return new ChatResponse(List.of(new Generation(content)));
    }

    /** Sets a private field on {@code agent} via reflection. */
    private void setField(String name, Object value) {
        try {
            Field f = SpringAiLlmAgent.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(agent, value);
        } catch (Exception e) {
            fail("Failed to inject field '" + name + "': " + e.getMessage());
        }
    }
}