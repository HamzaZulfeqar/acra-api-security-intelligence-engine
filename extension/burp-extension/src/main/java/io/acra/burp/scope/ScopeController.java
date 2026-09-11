package io.acra.burp.scope;
import java.util.concurrent.atomic.AtomicReference;
public final class ScopeController {
    private final AtomicReference<ScopeConfiguration> configuration=new AtomicReference<>(ScopeConfiguration.safeDefaults());
    public ScopeConfiguration configuration(){return configuration.get();}
    public void update(ScopeConfiguration next){ if(next==null) throw new IllegalArgumentException("configuration required"); configuration.set(next); }
    public ScopeDecision evaluate(boolean burpInScope,String scheme,String host,int port,String path,boolean manual){
        ScopeConfiguration c=configuration.get();
        return switch(c.mode()){
            case ALL_TRAFFIC -> new ScopeDecision(true,"all-traffic mode");
            case IN_SCOPE_ONLY -> new ScopeDecision(burpInScope,"Burp suite scope");
            case MANUAL_ONLY -> new ScopeDecision(manual,"manual-only mode");
            case SELECTED_HOSTS,SELECTED_ENDPOINTS -> c.rules().stream().anyMatch(r->r.matches(scheme,host,port,path))?new ScopeDecision(true,"matched configured scope rule"):new ScopeDecision(false,"no configured scope rule matched");
        };
    }
}
