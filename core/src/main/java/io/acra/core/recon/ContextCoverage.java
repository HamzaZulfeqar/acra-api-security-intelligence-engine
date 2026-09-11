package io.acra.core.recon;
import java.util.LinkedHashMap;
import java.util.Map;
public record ContextCoverage(boolean principal, boolean role, boolean tenant, boolean resource, boolean owner,
                              boolean action, boolean route, boolean schema, boolean policy) {
    public int knownCount(){int n=0; if(principal)n++;if(role)n++;if(tenant)n++;if(resource)n++;if(owner)n++;if(action)n++;if(route)n++;if(schema)n++;if(policy)n++;return n;}
    public int total(){return 9;}
    public double ratio(){return knownCount()/9.0;}
    public Map<String,Boolean> fields(){Map<String,Boolean> m=new LinkedHashMap<>();m.put("principal",principal);m.put("role",role);m.put("tenant",tenant);m.put("resource",resource);m.put("owner",owner);m.put("action",action);m.put("route",route);m.put("schema",schema);m.put("policy",policy);return Map.copyOf(m);}
}
