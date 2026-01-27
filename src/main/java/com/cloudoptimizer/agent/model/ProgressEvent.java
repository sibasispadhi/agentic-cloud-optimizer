package com.cloudoptimizer.agent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Real-time progress event for WebSocket streaming.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since December 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressEvent {
    private OptimizationPhase phase;
    private String message;
    private int progressPercent;
    private Map<String, Object> data;
    private Instant timestamp;
    private String eventType; // "phase", "metric", "reasoning", "complete"


    public static ProgressEvent phaseUpdate(OptimizationPhase phase, String message) {
        ProgressEvent event = new ProgressEvent();
        event.setPhase(phase);
        event.setMessage(message);
        event.setProgressPercent(phase.getProgressPercent());
        event.setEventType("phase");
        event.setTimestamp(Instant.now());
        event.setData(new HashMap<>());
        return event;
    }

    public static ProgressEvent metricUpdate(String message, Map<String, Object> metricData) {
        ProgressEvent event = new ProgressEvent();
        event.setMessage(message);
        event.setData(metricData);
        event.setEventType("metric");
        event.setTimestamp(Instant.now());
        return event;
    }

    public static ProgressEvent reasoningUpdate(String reasoning) {
        ProgressEvent event = new ProgressEvent();
        event.setMessage(reasoning);
        event.setEventType("reasoning");
        event.setTimestamp(Instant.now());
        event.setData(new HashMap<>());
        return event;
    }

    public static ProgressEvent completeEvent(Map<String, Object> reportData) {
        ProgressEvent event = new ProgressEvent();
        event.setPhase(OptimizationPhase.COMPLETE);
        event.setMessage("Optimization complete!");
        event.setProgressPercent(OptimizationPhase.COMPLETE.getProgressPercent());
        event.setData(reportData);
        event.setEventType("complete");
        event.setTimestamp(Instant.now());
        return event;
    }

    public static ProgressEvent errorEvent(String errorMessage) {
        ProgressEvent event = new ProgressEvent();
        event.setMessage(errorMessage);
        event.setEventType("error");
        event.setTimestamp(Instant.now());
        event.setData(new HashMap<>());
        return event;
    }

}
