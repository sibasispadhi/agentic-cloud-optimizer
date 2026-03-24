package com.cloudoptimizer.agent.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Default policy engine — enforces seven governance rules.
 *
 * <h2>Rule summary</h2>
 * <ol>
 *   <li><b>MAX_CONCURRENCY_DELTA</b>  — % change from baseline concurrency</li>
 *   <li><b>CONCURRENCY_BOUNDS</b>     — absolute min/max concurrency</li>
 *   <li><b>MAX_HEAP_DELTA</b>         — % change from current heap size</li>
 *   <li><b>HEAP_BOUNDS</b>            — absolute min/max heap size</li>
 *   <li><b>MIN_CONFIDENCE</b>         — agent confidence below threshold → REQUIRES_APPROVAL</li>
 *   <li><b>FORBIDDEN_RESOURCE</b>     — proposed resource is on the deny-list → DENIED</li>
 *   <li><b>IMPACT_LEVEL_APPROVAL</b>  — impact level on the approval-required list</li>
 * </ol>
 *
 * <p>Cooldown checking ({@code COOLDOWN_WINDOW}) is implemented as a no-op
 * placeholder. The orchestrator does not currently persist {@code lastAppliedAt}
 * across JVM restarts; real cooldown enforcement is deferred to Phase 3
 * (Actuation Budgets).
 */
@Service
public class DefaultPolicyEngine implements PolicyEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultPolicyEngine.class);

    @Override
    public PolicyDecision evaluate(PolicyContext ctx, ActuationPolicy policy) {
        List<PolicyViolation> violations = new ArrayList<>();
        List<PolicyWarning>   warnings   = new ArrayList<>();
        List<PolicyWarning>   approvals  = new ArrayList<>();

        checkConcurrencyDelta(ctx, policy, violations);
        checkConcurrencyBounds(ctx, policy, violations);
        checkHeapDelta(ctx, policy, violations);
        checkHeapBounds(ctx, policy, violations);
        checkConfidence(ctx, policy, approvals);
        checkForbiddenResources(ctx, policy, violations);
        checkImpactLevel(ctx, policy, approvals, warnings);
        checkCooldown(ctx, policy, violations);

        PolicyDecision decision = buildDecision(violations, approvals, warnings);
        log.info("Policy evaluation: {} | violations={} approvals={} warnings={}",
                decision.outcome(), violations.size(), approvals.size(), warnings.size());
        return decision;
    }

    // ── Rules ─────────────────────────────────────────────────────────────────

    /** Rule 1: concurrency may not change by more than maxConcurrencyDeltaPct. */
    private void checkConcurrencyDelta(PolicyContext ctx, ActuationPolicy policy,
                                       List<PolicyViolation> violations) {
        int baseline = ctx.getBaseline().getConcurrency();
        int proposed = ctx.getProposedConcurrency();
        if (baseline == 0) return;

        double deltaPct = Math.abs((proposed - baseline) / (double) baseline) * 100.0;
        double limit    = policy.getMaxConcurrencyDeltaPct();

        if (deltaPct > limit) {
            violations.add(PolicyViolation.of("MAX_CONCURRENCY_DELTA",
                    String.format("Concurrency change %.1f%% exceeds limit of %.1f%% " +
                                  "(baseline=%d, proposed=%d)",
                                  deltaPct, limit, baseline, proposed)));
        }
    }

    /** Rule 2: proposed concurrency must stay within absolute bounds. */
    private void checkConcurrencyBounds(PolicyContext ctx, ActuationPolicy policy,
                                        List<PolicyViolation> violations) {
        int proposed = ctx.getProposedConcurrency();
        if (proposed < policy.getMinConcurrency()) {
            violations.add(PolicyViolation.of("CONCURRENCY_BOUNDS",
                    String.format("Proposed concurrency %d is below minimum %d",
                                  proposed, policy.getMinConcurrency())));
        }
        if (proposed > policy.getMaxConcurrency()) {
            violations.add(PolicyViolation.of("CONCURRENCY_BOUNDS",
                    String.format("Proposed concurrency %d exceeds maximum %d",
                                  proposed, policy.getMaxConcurrency())));
        }
    }

    /** Rule 3: heap may not change by more than maxHeapDeltaPct (when proposed). */
    private void checkHeapDelta(PolicyContext ctx, ActuationPolicy policy,
                                List<PolicyViolation> violations) {
        if (ctx.getProposedHeapSizeMb() == null) return;
        if (ctx.getBaseline().getHeapMetrics() == null) return;

        double current  = ctx.getBaseline().getHeapMetrics().getHeapSizeMb();
        double proposed = ctx.getProposedHeapSizeMb();
        if (current == 0) return;

        double deltaPct = Math.abs((proposed - current) / current) * 100.0;
        double limit    = policy.getMaxHeapDeltaPct();

        if (deltaPct > limit) {
            violations.add(PolicyViolation.of("MAX_HEAP_DELTA",
                    String.format("Heap change %.1f%% exceeds limit of %.1f%% " +
                                  "(current=%.0fMB, proposed=%.0fMB)",
                                  deltaPct, limit, current, proposed)));
        }
    }

    /** Rule 4: proposed heap must stay within absolute bounds (when proposed). */
    private void checkHeapBounds(PolicyContext ctx, ActuationPolicy policy,
                                 List<PolicyViolation> violations) {
        if (ctx.getProposedHeapSizeMb() == null) return;

        int proposed = ctx.getProposedHeapSizeMb();
        if (proposed < policy.getMinHeapMb()) {
            violations.add(PolicyViolation.of("HEAP_BOUNDS",
                    String.format("Proposed heap %dMB is below minimum %dMB",
                                  proposed, policy.getMinHeapMb())));
        }
        if (proposed > policy.getMaxHeapMb()) {
            violations.add(PolicyViolation.of("HEAP_BOUNDS",
                    String.format("Proposed heap %dMB exceeds maximum %dMB",
                                  proposed, policy.getMaxHeapMb())));
        }
    }

    /** Rule 5: low-confidence decisions require approval rather than auto-execute. */
    private void checkConfidence(PolicyContext ctx, ActuationPolicy policy,
                                 List<PolicyWarning> approvals) {
        double confidence = ctx.getConfidenceScore();
        double minRequired = policy.getMinConfidenceToAct();

        if (confidence < minRequired) {
            approvals.add(PolicyWarning.of("MIN_CONFIDENCE",
                    String.format("Agent confidence %.2f is below threshold %.2f — human approval required",
                                  confidence, minRequired)));
        }
    }

    /** Rule 6: any change to a forbidden resource is an immediate denial. */
    private void checkForbiddenResources(PolicyContext ctx, ActuationPolicy policy,
                                         List<PolicyViolation> violations) {
        for (String resource : ctx.getProposedResources()) {
            if (policy.getForbiddenResources().contains(resource)) {
                violations.add(PolicyViolation.of("FORBIDDEN_RESOURCE",
                        String.format("Resource '%s' is forbidden by policy", resource)));
            }
        }
    }

    /** Rule 7: high-impact changes may require approval (configurable per impact level). */
    private void checkImpactLevel(PolicyContext ctx, ActuationPolicy policy,
                                  List<PolicyWarning> approvals,
                                  List<PolicyWarning> warnings) {
        String impact = ctx.getImpactLevel();
        if (impact == null) return;

        if (policy.getImpactLevelsRequiringApproval().contains(impact)) {
            approvals.add(PolicyWarning.of("IMPACT_LEVEL_APPROVAL",
                    String.format("Impact level '%s' requires human approval per policy", impact)));
        } else if ("HIGH".equals(impact)) {
            warnings.add(PolicyWarning.of("HIGH_IMPACT_CHANGE",
                    String.format("Change has HIGH impact level — monitor closely after application")));
        }
    }

    /**
     * Rule 8 (placeholder): cooldown window enforcement.
     *
     * <p>Raises a violation if {@code lastAppliedAt} is set and the required
     * cooldown has not yet elapsed.  When {@code cooldownSeconds == 0},
     * the rule is disabled entirely.
     */
    private void checkCooldown(PolicyContext ctx, ActuationPolicy policy,
                               List<PolicyViolation> violations) {
        if (policy.getCooldownSeconds() <= 0) return;
        if (ctx.getLastAppliedAt() == null) return;

        long secondsSinceLast = Duration.between(ctx.getLastAppliedAt(), Instant.now()).toSeconds();
        if (secondsSinceLast < policy.getCooldownSeconds()) {
            violations.add(PolicyViolation.of("COOLDOWN_WINDOW",
                    String.format("Cooldown of %ds not yet elapsed (%.0fs remaining)",
                                  policy.getCooldownSeconds(),
                                  (double)(policy.getCooldownSeconds() - secondsSinceLast))));
        }
    }

    // ── Decision assembly ─────────────────────────────────────────────────────

    /**
     * Applies outcome precedence: DENIED > REQUIRES_APPROVAL > ALLOWED_WITH_WARNINGS > ALLOWED.
     */
    private PolicyDecision buildDecision(List<PolicyViolation> violations,
                                         List<PolicyWarning> approvals,
                                         List<PolicyWarning> warnings) {
        if (!violations.isEmpty()) {
            return PolicyDecision.denied(violations, concat(approvals, warnings));
        }
        if (!approvals.isEmpty()) {
            return PolicyDecision.requiresApproval(concat(approvals, warnings));
        }
        if (!warnings.isEmpty()) {
            return PolicyDecision.allowedWithWarnings(warnings);
        }
        return PolicyDecision.allowed();
    }

    private static List<PolicyWarning> concat(List<PolicyWarning> a, List<PolicyWarning> b) {
        List<PolicyWarning> result = new ArrayList<>(a);
        result.addAll(b);
        return result;
    }
}