package io.acra.core.active.replay;

import io.acra.core.active.analysis.ExpectedDecisionCandidate;
import io.acra.core.active.execution.TestExecutionResult;
import io.acra.core.active.execution.TestExecutor;
import io.acra.core.active.model.SecurityTest;
import io.acra.core.active.safety.ActiveConsent;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.serialization.DomainSerializer;
import java.time.Clock;
import java.util.List;

public final class ReplayService {
    private final Clock clock;

    public ReplayService(Clock clock) {
        if (clock == null) throw new IllegalArgumentException("clock required");
        this.clock = clock;
    }

    public ReplayDescriptor descriptor(TestExecutionResult execution, SecurityTest test) {
        if (execution == null || execution.observation() == null || test == null) {
            throw new IllegalArgumentException("completed execution and test required");
        }
        return new ReplayDescriptor(execution.executionId(), test, test.signature(),
                test.configurationSnapshot().fingerprint(),
                execution.observation().executionFingerprint().environmentFingerprint(), clock.instant());
    }

    public ReplayResult replay(ReplayDescriptor descriptor, TestExecutor executor, ActiveConsent consent,
                               List<ExpectedDecisionCandidate> candidates) {
        if (descriptor == null || executor == null) throw new IllegalArgumentException("descriptor and executor required");
        SecurityTest test = descriptor.test();
        if (!descriptor.testSignature().equals(test.signature())) return new ReplayResult(false, "test signature mismatch", null);
        if (!descriptor.configurationFingerprint().equals(test.configurationSnapshot().fingerprint())) {
            return new ReplayResult(false, "configuration fingerprint mismatch", null);
        }
        String environment = TokenFingerprint.sha256(new DomainSerializer().serialize(test.target()));
        if (!descriptor.environmentFingerprint().equals(environment)) return new ReplayResult(false, "environment fingerprint mismatch", null);
        TestExecutionResult fresh = executor.execute(test, consent, candidates);
        return fresh.observation() == null
                ? new ReplayResult(false, "fresh replay did not complete: " + fresh.state(), null)
                : new ReplayResult(true, "fresh execution created; prior evidence preserved", fresh);
    }
}
