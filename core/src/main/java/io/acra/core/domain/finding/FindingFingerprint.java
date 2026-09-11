package io.acra.core.domain.finding;
import io.acra.core.security.TokenFingerprint;
public record FindingFingerprint(String fingerprint,String rootEndpoint,String resource,String actor,String tenantRelationship,String authorizationViolation,String cause) {
    public FindingFingerprint {rootEndpoint=s(rootEndpoint);resource=s(resource);actor=s(actor);tenantRelationship=s(tenantRelationship);authorizationViolation=s(authorizationViolation);cause=s(cause);if(fingerprint==null||fingerprint.isBlank())fingerprint="ff-"+TokenFingerprint.sha256(String.join("|",rootEndpoint,resource,actor,tenantRelationship,authorizationViolation,cause)).substring(0,24);}public static FindingFingerprint of(String endpoint,String resource,String actor,String tenant,String violation,String cause){return new FindingFingerprint("",endpoint,resource,actor,tenant,violation,cause);}private static String s(String v){return v==null?"UNKNOWN":v;}
}
