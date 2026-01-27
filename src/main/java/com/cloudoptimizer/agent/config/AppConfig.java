package com.cloudoptimizer.agent.config;

import com.cloudoptimizer.agent.simulator.DemoWorkloadSimulator;
import com.cloudoptimizer.agent.simulator.HttpRestWorkloadSimulator;
import com.cloudoptimizer.agent.simulator.WorkloadSimulator;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Main application configuration for Agent Cloud Optimizer.
 * 
 * Configures core beans including AI chat clients, thread pools,
 * and async execution capabilities for enterprise-grade performance.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since 2025
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AppConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${executor.core-pool-size:10}")
    private int corePoolSize;

    @Value("${executor.max-pool-size:50}")
    private int maxPoolSize;

    @Value("${executor.queue-capacity:100}")
    private int queueCapacity;

    /**
     * Configures the AI chat client for cloud optimization recommendations.
     * 
     * Uses @ConditionalOnProperty to only create this bean when LLM strategy is active.
     * This prevents startup failures when Ollama is not available.
     * 
     * @param ollamaChatClient the Ollama chat client instance
     * @return configured ChatClient instance
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "agent.strategy", 
        havingValue = "llm"
    )
    public ChatClient chatClient(OllamaChatClient ollamaChatClient) {
        return ollamaChatClient;
    }

    /**
     * Configures the async task executor for non-blocking operations.
     * 
     * Provides a thread pool for handling asynchronous tasks such as
     * metric collection, AI inference, and background processing.
     * 
     * @return configured ThreadPoolTaskExecutor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(applicationName + "-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Configures the metrics processing executor for high-throughput metric ingestion.
     * 
     * @return configured ThreadPoolTaskExecutor for metrics processing
     */
    @Bean(name = "metricsExecutor")
    public Executor metricsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix(applicationName + "-metrics-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Configures the workload simulator based on application properties.
     * 
     * @param demoSimulator demo workload simulator bean
     * @param httpSimulator HTTP REST workload simulator bean
     * @param simulatorName name of the simulator to use (demo or http)
     * @return configured WorkloadSimulator instance
     */
    @Bean
    public WorkloadSimulator workloadSimulator(
            DemoWorkloadSimulator demoSimulator,
            HttpRestWorkloadSimulator httpSimulator,
            @Value("${workload.simulator:demo}") String simulatorName) {
        
        if ("http".equalsIgnoreCase(simulatorName)) {
            return httpSimulator;
        }
        return demoSimulator;
    }
}
