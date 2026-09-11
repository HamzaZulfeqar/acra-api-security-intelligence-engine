package io.acra.core.recon;
import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.uri.IdentifierLocation;
import java.util.List;
public record ParameterIntelligence(String name, List<String> values, IdentifierLocation location,
                                    ParameterRole role, Confidence confidence, String reason) {
    public ParameterIntelligence {
        name=name==null?"":name;
        values=List.copyOf(values==null?List.of():values);
        if(location==null) location=IdentifierLocation.UNKNOWN;
        if(role==null) role=ParameterRole.UNKNOWN;
        if(confidence==null) confidence=Confidence.unknown();
        reason=reason==null?"":reason;
    }
}
