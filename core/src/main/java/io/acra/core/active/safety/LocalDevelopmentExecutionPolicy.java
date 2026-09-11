package io.acra.core.active.safety;

import io.acra.core.active.model.ExecutionEnvironment;
import io.acra.core.active.model.SafetyClass;
import io.acra.core.active.model.SecurityTest;
import java.util.Set;

public final class LocalDevelopmentExecutionPolicy {
    private static final Set<String> LOOPBACK = Set.of("localhost", "127.0.0.1", "::1", "[::1]");

    public ActiveConsent consentFor(SecurityTest test) {
        if (test == null) throw new IllegalArgumentException("test required");
        boolean safeLocal = test.target().authorized()
                && test.target().environment() == ExecutionEnvironment.LAB
                && LOOPBACK.contains(test.target().host())
                && test.safetyPolicy().safetyClass() != SafetyClass.DESTRUCTIVE
                && test.mutation().safetyClass() != SafetyClass.DESTRUCTIVE;
        return safeLocal ? new ActiveConsent(true, true, true, true, false) : ActiveConsent.disabled();
    }
}
