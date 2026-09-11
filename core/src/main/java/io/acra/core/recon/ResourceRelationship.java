package io.acra.core.recon;
import io.acra.core.domain.common.Confidence;
import java.util.List;
public record ResourceRelationship(String sourceEntity, String targetEntity, ResourceRelationshipType relation,
                                   Confidence confidence, List<String> evidenceIds, String reason) {
    public ResourceRelationship {
        if(sourceEntity==null||sourceEntity.isBlank()||targetEntity==null||targetEntity.isBlank()) throw new IllegalArgumentException("relationship endpoints required");
        if(relation==null) relation=ResourceRelationshipType.UNKNOWN;
        if(confidence==null) confidence=Confidence.unknown();
        evidenceIds=List.copyOf(evidenceIds==null?List.of():evidenceIds);
        reason=reason==null?"":reason;
    }
}
