package com.cloudoptimizer.agent.simulator;

import com.cloudoptimizer.agent.model.RunResult;
import com.cloudoptimizer.agent.service.LoadRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Demo workload simulator using the built-in /work endpoint.
 * 
 * This is the original v0.1.0 behavior - maintains backward compatibility.
 * Wraps the existing LoadRunner service.
 * 
 * @author Sibasis Padhi
 * @version 0.2.0
 * @since December 2025
 */
@Component("demo")
public class DemoWorkloadSimulator implements WorkloadSimulator {
    
    private static final Logger log = LoggerFactory.getLogger(DemoWorkloadSimulator.class);
    
    private final LoadRunner loadRunner;
    
    @Autowired
    public DemoWorkloadSimulator(LoadRunner loadRunner) {
        this.loadRunner = loadRunner;
        log.info("DemoWorkloadSimulator initialized");
    }
    
    @Override
    public RunResult executeLoad(int concurrency, int durationSeconds, double targetRps) {
        log.debug("Executing demo load test: concurrency={}, duration={}s, targetRps={}",
                concurrency, durationSeconds, targetRps);
        return loadRunner.runLoad(durationSeconds, concurrency, targetRps);
    }
    
    @Override
    public String getName() {
        return "demo";
    }
}
