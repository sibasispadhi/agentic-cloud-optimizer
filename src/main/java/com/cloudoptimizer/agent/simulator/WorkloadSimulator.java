package com.cloudoptimizer.agent.simulator;

import com.cloudoptimizer.agent.model.RunResult;

/**
 * Interface for workload simulators that execute load tests.
 * 
 * Implementations can test different types of services:
 * - Built-in demo (DemoWorkloadSimulator)
 * - HTTP REST APIs (HttpRestWorkloadSimulator)
 * - Database queries (future)
 * - gRPC services (future)
 * 
 * @author Sibasis Padhi
 * @version 0.2.0
 * @since December 2025
 */
public interface WorkloadSimulator {
    
    /**
     * Executes a load test with specified parameters.
     * 
     * @param concurrency number of concurrent threads/requests
     * @param durationSeconds how long to run the test
     * @param targetRps target requests per second (0 = unlimited)
     * @return RunResult containing performance metrics
     */
    RunResult executeLoad(int concurrency, int durationSeconds, double targetRps);
    
    /**
     * Returns the name/identifier of this simulator.
     * Used for configuration and logging.
     * 
     * @return simulator name (e.g., "demo", "http", "grpc")
     */
    String getName();
}
