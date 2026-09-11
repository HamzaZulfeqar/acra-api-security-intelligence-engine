package io.acra.core.recon;
import java.time.Instant;
public record ConfirmedIdentityContext(String tokenFingerprint,String principalId,String role,String tenantId,IdentityConfidenceState state,String source,Instant confirmedAt) {
    public ConfirmedIdentityContext {tokenFingerprint=s(tokenFingerprint);principalId=s(principalId);role=u(role);tenantId=u(tenantId);if(state==null)state=IdentityConfidenceState.UNKNOWN;source=s(source);if(confirmedAt==null)throw new IllegalArgumentException("confirmedAt required");if(tokenFingerprint.isBlank())throw new IllegalArgumentException("tokenFingerprint required");}private static String s(String v){return v==null?"":v;}private static String u(String v){return v==null||v.isBlank()?"UNKNOWN":v;}
}
