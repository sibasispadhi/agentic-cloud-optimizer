package com.cloudoptimizer.agent.controller;

import com.cloudoptimizer.agent.service.OptimizationOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket controller for real-time optimization workflow.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since December 2025
 */
@Controller
public class OptimizationWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(OptimizationWebSocketController.class);
    
    private final OptimizationOrchestrator orchestrator;

    public OptimizationWebSocketController(OptimizationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Handles optimization start request from WebSocket client.
     * Runs optimization asynchronously and streams progress to /topic/optimization-progress
     */
    @MessageMapping("/start-optimization")
    @SendTo("/topic/optimization-status")
    public Map<String, String> startOptimization() {
        log.info("Received optimization start request via WebSocket");
        
        // Start optimization asynchronously
        orchestrator.runOptimization()
                .thenAccept(report -> {
                    log.info("Optimization completed successfully");
                })
                .exceptionally(ex -> {
                    log.error("Optimization failed", ex);
                    return null;
                });
        
        return Map.of(
                "status", "started",
                "message", "Optimization workflow started. Watch /topic/optimization-progress for updates."
        );
    }
}
