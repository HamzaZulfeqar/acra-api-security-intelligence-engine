package io.acra.core.recon;
import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.uri.IdentifierCandidate;
public record SemanticIdentifier(IdentifierCandidate candidate, SemanticIdentifierType semanticType,
                                 String resourceType, Confidence confidence, String reason, String evidenceSource) {
    public SemanticIdentifier {
        if(candidate==null) throw new IllegalArgumentException("candidate required");
        if(semanticType==null) semanticType=SemanticIdentifierType.UNKNOWN;
        resourceType=resourceType==null?"":resourceType;
        if(confidence==null) confidence=candidate.confidence();
        reason=reason==null?"":reason;
        evidenceSource=evidenceSource==null?candidate.source():evidenceSource;
    }
}
