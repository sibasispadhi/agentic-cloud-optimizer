package com.cloudoptimizer.agent.model;

/**
 * Enumeration of optimization strategies for cloud resource management.
 * 
 * Defines the strategic approach the AI agent should take when analyzing
 * cloud resources and generating optimization recommendations.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since 2025
 */
public enum AgentStrategy {
    
    /**
     * Simple rule-based deterministic optimization.
     * Uses threshold-based logic without AI/LLM.
     */
    RULE_BASED("Rule-Based Optimization",
            "Deterministic threshold-based optimization using simple rules"),
    
    /**
     * Focus on reducing cloud infrastructure costs.
     * Prioritizes cost savings over performance improvements.
     */
    COST_OPTIMIZATION("Cost Optimization", 
            "Minimize cloud spending while maintaining acceptable performance levels"),
    
    /**
     * Focus on improving application and resource performance.
     * Prioritizes performance gains over cost considerations.
     */
    PERFORMANCE_OPTIMIZATION("Performance Optimization",
            "Maximize resource performance and application responsiveness"),
    
    /**
     * Balance between cost reduction and performance improvement.
     * Seeks optimal trade-offs between spending and performance.
     */
    BALANCED("Balanced Optimization",
            "Achieve optimal balance between cost efficiency and performance"),
    
    /**
     * Focus on right-sizing resources to match actual usage patterns.
     * Eliminates over-provisioning and under-utilization.
     */
    RESOURCE_EFFICIENCY("Resource Efficiency",
            "Align resource allocation with actual utilization patterns"),
    
    /**
     * Focus on sustainability and reducing carbon footprint.
     * Prioritizes energy-efficient resource selection and scheduling.
     */
    SUSTAINABILITY("Sustainability Optimization",
            "Minimize environmental impact and carbon footprint"),
    
    /**
     * Focus on ensuring high availability and fault tolerance.
     * Prioritizes redundancy and resilience over cost.
     */
    RELIABILITY("Reliability Optimization",
            "Maximize uptime and fault tolerance of cloud resources"),
    
    /**
     * Focus on security posture and compliance requirements.
     * Prioritizes security controls and regulatory compliance.
     */
    SECURITY("Security Optimization",
            "Enhance security posture and ensure compliance requirements");

    private final String displayName;
    private final String description;

    /**
     * Constructs an AgentStrategy enum value.
     * 
     * @param displayName human-readable name
     * @param description detailed description of the strategy
     */
    AgentStrategy(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Gets the human-readable display name.
     * 
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the detailed description of the strategy.
     * 
     * @return strategy description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this strategy prioritizes cost savings.
     * 
     * @return true if cost-focused
     */
    public boolean isCostFocused() {
        return this == COST_OPTIMIZATION || this == BALANCED || this == RESOURCE_EFFICIENCY;
    }

    /**
     * Checks if this strategy prioritizes performance.
     * 
     * @return true if performance-focused
     */
    public boolean isPerformanceFocused() {
        return this == PERFORMANCE_OPTIMIZATION || this == BALANCED || this == RELIABILITY;
    }

    /**
     * Gets the priority weight for cost considerations (0.0 to 1.0).
     * 
     * @return cost priority weight
     */
    public double getCostWeight() {
        return switch (this) {
            case RULE_BASED -> 0.5;
            case COST_OPTIMIZATION -> 1.0;
            case RESOURCE_EFFICIENCY -> 0.8;
            case BALANCED -> 0.5;
            case SUSTAINABILITY -> 0.6;
            case PERFORMANCE_OPTIMIZATION -> 0.2;
            case RELIABILITY -> 0.3;
            case SECURITY -> 0.4;
        };
    }

    /**
     * Gets the priority weight for performance considerations (0.0 to 1.0).
     * 
     * @return performance priority weight
     */
    public double getPerformanceWeight() {
        return switch (this) {
            case RULE_BASED -> 0.5;
            case PERFORMANCE_OPTIMIZATION -> 1.0;
            case RELIABILITY -> 0.9;
            case BALANCED -> 0.5;
            case SECURITY -> 0.6;
            case RESOURCE_EFFICIENCY -> 0.4;
            case SUSTAINABILITY -> 0.3;
            case COST_OPTIMIZATION -> 0.2;
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
