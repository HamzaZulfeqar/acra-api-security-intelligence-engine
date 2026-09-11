package io.acra.burp.scope;
import java.util.List;
public record ScopeConfiguration(ScopeMode mode,List<ScopeRule> rules,int maxBodyBytes,int maxTransactions,
                                 int maxConcurrentActiveRequests,int requestsPerSecond,int timeoutMillis,boolean activeExecutionEnabled) {
    public ScopeConfiguration {
        mode=mode==null?ScopeMode.IN_SCOPE_ONLY:mode; rules=List.copyOf(rules==null?List.of():rules);
        if(maxBodyBytes<1||maxTransactions<1||maxConcurrentActiveRequests<1||requestsPerSecond<1||timeoutMillis<1) throw new IllegalArgumentException("safety limits must be positive");
    }
    public static ScopeConfiguration safeDefaults(){ return new ScopeConfiguration(ScopeMode.IN_SCOPE_ONLY,List.of(),2*1024*1024,50_000,2,2,10_000,false); }
}
