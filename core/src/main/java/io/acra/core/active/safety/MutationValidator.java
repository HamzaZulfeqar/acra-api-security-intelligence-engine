package io.acra.core.active.safety;

import io.acra.core.active.evidence.SafetyAuditLog;
import io.acra.core.active.evidence.SafetyEventType;
import io.acra.core.active.execution.RequestSet;
import io.acra.core.active.model.ExecutionEnvironment;
import io.acra.core.active.model.SecurityTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MutationValidator {
    private final String projectId;
    private final Set<ExecutionEnvironment> enabledEnvironments;
    private final HardScopeGuard scopeGuard;
    private final EnvironmentGuard environmentGuard;
    private final ConsentGuard consentGuard;
    private final RequestEquivalenceGuard equivalenceGuard;
    private final KillSwitch killSwitch;
    private final HierarchicalBudgetManager budgets;
    private final HierarchicalConcurrencyController concurrency;
    private final ScopedRateLimiter rateLimiter;
    private final SafetyAuditLog audit;

    public MutationValidator(
            String projectId,
            Set<ExecutionEnvironment> enabledEnvironments,
            KillSwitch killSwitch,
            HierarchicalBudgetManager budgets,
            HierarchicalConcurrencyController concurrency,
            ScopedRateLimiter rateLimiter,
            SafetyAuditLog audit) {
        if (projectId == null || projectId.isBlank() || killSwitch == null || budgets == null
                || concurrency == null || rateLimiter == null || audit == null) {
            throw new IllegalArgumentException("validator dependencies required");
        }
        this.projectId = projectId;
        this.enabledEnvironments = Set.copyOf(enabledEnvironments == null ? Set.of() : enabledEnvironments);
        this.scopeGuard = new HardScopeGuard();
        this.environmentGuard = new EnvironmentGuard();
        this.consentGuard = new ConsentGuard();
        this.equivalenceGuard = new RequestEquivalenceGuard();
        this.killSwitch = killSwitch;
        this.budgets = budgets;
        this.concurrency = concurrency;
        this.rateLimiter = rateLimiter;
        this.audit = audit;
    }

    public ValidationDecision validate(
            String executionId,
            SecurityTest test,
            RequestSet requests,
            ActiveConsent consent,
            List<BudgetKey> budgetKeys,
            List<ConcurrencyKey> concurrencyKeys,
            List<String> rateKeys) {
        ValidationDecision result = validateInternal(test, requests, consent, budgetKeys, concurrencyKeys, rateKeys);
        SafetyEventType type = switch (result.status()) {
            case ALLOWED -> SafetyEventType.VALIDATION_ALLOWED;
            case BLOCKED -> SafetyEventType.VALIDATION_BLOCKED;
            case REQUIRES_CONFIRMATION -> SafetyEventType.VALIDATION_REQUIRES_CONFIRMATION;
            case INVALID -> SafetyEventType.VALIDATION_INVALID;
        };
        audit.append(type, executionId, test == null ? "" : test.testId(),
                Map.of("reasons", String.join("; ", result.reasons())));
        return result;
    }

    private ValidationDecision validateInternal(
            SecurityTest test,
            RequestSet requests,
            ActiveConsent consent,
            List<BudgetKey> budgetKeys,
            List<ConcurrencyKey> concurrencyKeys,
            List<String> rateKeys) {
        if (test == null || requests == null) return ValidationDecision.invalid("test and built requests are required");
        if (killSwitch.engaged()) return ValidationDecision.blocked("global kill switch is engaged");
        ValidationDecision environment = environmentGuard.evaluate(test.target(), enabledEnvironments);
        if (environment.status() != ValidationStatus.ALLOWED) return environment;
        ValidationDecision consentDecision = consentGuard.evaluate(test, consent);
        if (consentDecision.status() != ValidationStatus.ALLOWED) return consentDecision;
        ValidationDecision equivalence = equivalenceGuard.evaluate(test, requests);
        if (equivalence.status() != ValidationStatus.ALLOWED) return equivalence;
        if (test.estimatedRequestCost() > test.safetyPolicy().requestBudget()) {
            return ValidationDecision.blocked("test request cost exceeds test safety budget");
        }
        if (test.safetyPolicy().mutationBudget() < 1) return ValidationDecision.blocked("test mutation budget is zero");
        List<ValidationDecision> scopes = new ArrayList<>();
        scopes.add(scopeGuard.evaluate(projectId, test, requests.baseline().request()));
        scopes.add(scopeGuard.evaluate(projectId, test, requests.positiveControl().request()));
        scopes.add(scopeGuard.evaluate(projectId, test, requests.negativeControl().request()));
        scopes.add(scopeGuard.evaluate(projectId, test, requests.mutation().request()));
        ValidationDecision failedScope = scopes.stream().filter(decision -> decision.status() != ValidationStatus.ALLOWED).findFirst().orElse(null);
        if (failedScope != null) return failedScope;
        if (!budgets.canReserve(budgetKeys, test.estimatedRequestCost())) {
            return ValidationDecision.blocked("one or more request budgets cannot reserve the test cost");
        }
        if (!concurrency.canAcquire(concurrencyKeys)) return ValidationDecision.blocked("one or more concurrency scopes are full");
        if (!rateLimiter.isConfigured(rateKeys)) return ValidationDecision.invalid("one or more rate-limit scopes are not configured");
        return ValidationDecision.allowed();
    }
}
