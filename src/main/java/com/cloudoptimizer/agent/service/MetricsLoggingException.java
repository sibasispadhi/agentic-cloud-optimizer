package com.cloudoptimizer.agent.service;

/**
 * Custom exception for metrics logging operations.
 * 
 * Thrown when metric data cannot be persisted to storage due to
 * I/O errors, validation failures, or system resource constraints.
 * 
 * @author Sibasis Padhi
 * @version 1.0
 * @since 2025
 */
public class MetricsLoggingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new metrics logging exception with the specified detail message.
     * 
     * @param message the detail message
     */
    public MetricsLoggingException(String message) {
        super(message);
    }

    /**
     * Constructs a new metrics logging exception with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public MetricsLoggingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new metrics logging exception with the specified cause.
     * 
     * @param cause the cause of the exception
     */
    public MetricsLoggingException(Throwable cause) {
        super(cause);
    }
}
