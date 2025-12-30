package com.cloudoptimizer.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main entry point for the Agent Cloud Optimizer application.
 * 
 * This enterprise application provides AI-powered cloud resource optimization
 * capabilities using Spring Boot and Spring AI frameworks.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since 2025
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.cloudoptimizer.agent")
public class AgentCloudOptimizerApplication {

    /**
     * Main method to bootstrap the Spring Boot application.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AgentCloudOptimizerApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}
