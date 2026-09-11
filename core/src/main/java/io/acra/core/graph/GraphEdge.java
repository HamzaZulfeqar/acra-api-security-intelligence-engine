package io.acra.core.graph;

import io.acra.core.domain.common.*;
import io.acra.core.security.TokenFingerprint;
import java.time.Instant;
import java.util.*;

public record GraphEdge(String edgeId, String source, String target, RelationType relation, Confidence confidence,
                        List<String> evidenceIds, Instant createdAt) {
    public GraphEdge {
        edgeId=Validation.requireNonBlank(edgeId,"edgeId"); source=Validation.requireNonBlank(source,"edge source"); target=Validation.requireNonBlank(target,"edge target");
        if(relation==null) throw new DomainValidationException("edge relation required"); if(confidence==null) confidence=Confidence.unknown();
        evidenceIds=List.copyOf(evidenceIds==null?List.of():evidenceIds); if(evidenceIds.isEmpty()) throw new DomainValidationException("graph edge requires evidence");
        if(createdAt==null) throw new DomainValidationException("edge createdAt required");
    }
    public static GraphEdge create(String source,String target,RelationType relation,Confidence confidence,List<String> evidenceIds,Instant createdAt){
        List<String> sorted=new ArrayList<>(evidenceIds); Collections.sort(sorted); String material=source+"|"+target+"|"+relation+"|"+String.join(",",sorted);
        return new GraphEdge("edge-"+TokenFingerprint.sha256(material).substring(0,20),source,target,relation,confidence,sorted,createdAt);
    }
}
