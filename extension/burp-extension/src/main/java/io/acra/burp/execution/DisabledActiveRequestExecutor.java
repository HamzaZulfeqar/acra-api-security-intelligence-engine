package io.acra.burp.execution;
import io.acra.burp.scope.*;
import java.time.Duration;
public final class DisabledActiveRequestExecutor implements ActiveRequestExecutor {
    private final ScopeController scope; public DisabledActiveRequestExecutor(ScopeController scope){this.scope=scope;}
    @Override public ExecutionHandle execute(ActiveRequest request){ ScopeDecision d=scopeCheck(request); return new ExecutionHandle("",ExecutionHandle.State.BLOCKED,d.accepted()?"active execution disabled for Sprint 2 default":"scope rejected: "+d.reason()); }
    @Override public void cancel(String executionId){} @Override public Duration timeout(){return Duration.ofMillis(scope.configuration().timeoutMillis());}
    @Override public ScopeDecision scopeCheck(ActiveRequest r){return scope.evaluate(false,r.scheme(),r.host(),r.port(),r.path(),true);}
    @Override public RateLimitDecision rateLimit(){return new RateLimitDecision(false,"active execution disabled by default");}
    @Override public ActiveSafetyControls safetyControls(){return ActiveSafetyControls.sprint2Disabled(scope.configuration());}
}
