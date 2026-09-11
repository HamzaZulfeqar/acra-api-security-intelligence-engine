package io.acra.core.active.safety;

import io.acra.core.active.model.ExecutionEnvironment;
import io.acra.core.active.model.TargetDescriptor;
import java.util.Set;

public final class EnvironmentGuard {
    public ValidationDecision evaluate(TargetDescriptor target, Set<ExecutionEnvironment> enabledEnvironments) {
        if (target == null) return ValidationDecision.invalid("target is required");
        Set<ExecutionEnvironment> enabled = Set.copyOf(enabledEnvironments == null ? Set.of() : enabledEnvironments);
        if (target.environment() == ExecutionEnvironment.UNKNOWN) return ValidationDecision.blocked("unknown environment");
        if (target.environment() == ExecutionEnvironment.OUT_OF_SCOPE) return ValidationDecision.blocked("out-of-scope environment");
        if (!enabled.contains(target.environment())) return ValidationDecision.blocked("environment is not enabled by policy");
        if (target.environment() == ExecutionEnvironment.LAB && !isLoopback(target.host())) {
            return ValidationDecision.blocked("LAB targets must use a loopback host");
        }
        if (target.environment().requiresExplicitAuthorization() && !target.authorized()) {
            return ValidationDecision.blocked("authorized environment lacks target authorization");
        }
        return ValidationDecision.allowed();
    }

    private static boolean isLoopback(String host) {
        return "127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host) || "::1".equals(host);
    }
}
