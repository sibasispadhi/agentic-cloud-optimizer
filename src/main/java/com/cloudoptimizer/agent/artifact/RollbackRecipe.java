package com.cloudoptimizer.agent.artifact;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Map;

/**
 * Describes how to reverse the changes applied by an {@link OptimizationPlan}.
 *
 * <p>Rollback is triggered when {@link ValidationRecipe#isPassed()} returns
 * {@code false} after the post-optimization load test, or when an operator
 * manually requests a rollback.
 *
 * <p>{@link #executedAt} is {@code null} until a rollback actually occurs.
 */
@Getter
@Builder
@ToString
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RollbackRecipe {

    /**
     * Whether the changes made by this plan are reversible at all.
     * Some changes (e.g., data migrations) may be irreversible.
     */
    @JsonProperty("reversible")
    @Builder.Default
    private final boolean reversible = true;

    /**
     * The exact parameter values to restore when rolling back.
     * Key: resource identifier (e.g., {@code "jvm.concurrency"}).
     * Value: the original value to restore (e.g., {@code 4}).
     */
    @JsonProperty("restore_params")
    private final Map<String, Object> restoreParams;

    /**
     * Human-readable description of the condition that should trigger rollback.
     * Example: {@code "p99 latency exceeds 120ms after optimization"}.
     */
    @JsonProperty("trigger_condition")
    private final String triggerCondition;

    /** When rollback was executed.  {@code null} if no rollback has occurred. */
    @JsonProperty("executed_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private final Instant executedAt;
}