package io.acra.burp.model;
import io.acra.core.domain.identity.AuthenticationType;
public record AuthenticationFingerprint(AuthenticationType type,String fingerprint,String source) {
    public AuthenticationFingerprint { type=type==null?AuthenticationType.UNKNOWN:type; fingerprint=fingerprint==null?"":fingerprint; source=source==null?"UNKNOWN":source; }
}
