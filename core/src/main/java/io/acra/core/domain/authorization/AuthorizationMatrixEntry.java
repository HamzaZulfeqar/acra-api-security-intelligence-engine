package io.acra.core.domain.authorization;
import java.util.List;
public record AuthorizationMatrixEntry(String principal,String role,String tenant,String resource,String owner,String action,String endpoint,
                                       AuthorizationDecision expected,AuthorizationDecision observed,List<String> evidenceIds,String status) {
    public AuthorizationMatrixEntry {principal=u(principal);role=u(role);tenant=u(tenant);resource=u(resource);owner=u(owner);action=u(action);endpoint=u(endpoint);if(expected==null)expected=AuthorizationDecision.UNKNOWN;if(observed==null)observed=AuthorizationDecision.UNKNOWN;evidenceIds=List.copyOf(evidenceIds==null?List.of():evidenceIds);status=u(status);}private static String u(String s){return s==null||s.isBlank()?"UNKNOWN":s;}
}
