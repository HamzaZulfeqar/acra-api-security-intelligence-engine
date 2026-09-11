package io.acra.core.recon;
import io.acra.core.domain.authorization.AuthorizationDecision;
import java.util.List;
public record SecurityContextFingerprint(String principal, String role, String tenant, String resource, String owner,
                                         String action, String workflow, String uriClass, String uriForm, String tokenContext,
                                         AuthorizationDecision expected, AuthorizationDecision observed, List<String> evidenceIds) {
    public SecurityContextFingerprint {
        principal=unknown(principal);role=unknown(role);tenant=unknown(tenant);resource=unknown(resource);owner=unknown(owner);action=unknown(action);workflow=unknown(workflow);uriClass=unknown(uriClass);uriForm=unknown(uriForm);tokenContext=unknown(tokenContext);
        if(expected==null) expected=AuthorizationDecision.UNKNOWN; if(observed==null) observed=AuthorizationDecision.UNKNOWN; evidenceIds=List.copyOf(evidenceIds==null?List.of():evidenceIds);
    }
    private static String unknown(String s){return s==null||s.isBlank()?"UNKNOWN":s;}
}
