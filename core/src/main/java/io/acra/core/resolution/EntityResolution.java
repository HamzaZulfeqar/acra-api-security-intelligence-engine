package io.acra.core.resolution;

import io.acra.core.domain.common.Confidence;
import java.util.List;

public record EntityResolution(EntityResolutionStatus status, Confidence confidence, String reason, List<String> evidenceIds) {
    public EntityResolution {
        if(status==null) status=EntityResolutionStatus.UNKNOWN;
        if(confidence==null) confidence=Confidence.unknown();
        reason=reason==null?"":reason; evidenceIds=List.copyOf(evidenceIds==null?List.of():evidenceIds);
    }
}
