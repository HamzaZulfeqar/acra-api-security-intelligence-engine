package io.acra.core.recon;
import io.acra.core.domain.common.Confidence;
public record HeaderIntelligence(String name, HeaderRole role, Confidence confidence, String reason, boolean sensitive) {
    public HeaderIntelligence {
        name=name==null?"":name;
        if(role==null) role=HeaderRole.UNKNOWN;
        if(confidence==null) confidence=Confidence.unknown();
        reason=reason==null?"":reason;
    }
}
