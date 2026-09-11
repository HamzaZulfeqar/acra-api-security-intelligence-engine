package io.acra.burp.execution;
import io.acra.burp.scope.ScopeDecision;
import java.time.Duration;
public interface ActiveRequestExecutor {
    ExecutionHandle execute(ActiveRequest request); void cancel(String executionId); Duration timeout(); ScopeDecision scopeCheck(ActiveRequest request); RateLimitDecision rateLimit(); ActiveSafetyControls safetyControls();
}
