package io.acra.core.resolution;

import io.acra.core.domain.common.*;
import java.util.List;

public final class EntityResolver {
    public EntityResolution compare(String left,String right,List<String> evidenceIds){
        if(left==null||right==null||left.isBlank()||right.isBlank()) return new EntityResolution(EntityResolutionStatus.UNKNOWN,Confidence.unknown(),"insufficient identifiers",evidenceIds);
        if(left.equals(right)) return new EntityResolution(EntityResolutionStatus.SAME,Confidence.of(ConfidenceBasis.EXACT_OBSERVED),"exact identifier equality",evidenceIds);
        if(left.equalsIgnoreCase(right)) return new EntityResolution(EntityResolutionStatus.POSSIBLE_SAME,Confidence.of(ConfidenceBasis.HEURISTIC),"case-insensitive identifier match; not auto-merged",evidenceIds);
        return new EntityResolution(EntityResolutionStatus.CONFLICT,Confidence.of(ConfidenceBasis.EXACT_OBSERVED),"distinct identifiers",evidenceIds);
    }
}
