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
        
        // Determine web application type based on run.mode property
        // Default is web mode (servlet) for live dashboard
        // CLI mode uses NONE for quick execution and exit
        String runMode = getRunMode(args);
        
        if ("cli".equalsIgnoreCase(runMode)) {
            app.setWebApplicationType(WebApplicationType.NONE);
        } else {
            // Web mode - start embedded Tomcat for WebSocket dashboard
            app.setWebApplicationType(WebApplicationType.SERVLET);
        }
        
        app.run(args);
    }
    
    /**
     * Extract run mode from command line arguments or use default.
     * 
     * @param args command line arguments
     * @return run mode ("web" or "cli")
     */
    private static String getRunMode(String[] args) {
        // Check command line args for --run.mode=xxx
        for (String arg : args) {
            if (arg.startsWith("--run.mode=")) {
                return arg.substring("--run.mode=".length());
            }
        }
        
        // Check system property
        String sysProperty = System.getProperty("run.mode");
        if (sysProperty != null) {
            return sysProperty;
        }
        
        // Default to web mode
        return "web";
    }
}
