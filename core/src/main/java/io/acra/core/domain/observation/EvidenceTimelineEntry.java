package io.acra.core.domain.observation;
import io.acra.core.domain.common.Validation;
import java.time.Instant;
import java.util.List;
public record EvidenceTimelineEntry(String transactionId, Instant timestamp, String stage, String summary, List<String> evidenceIds) {
    public EvidenceTimelineEntry {
        transactionId=Validation.requireNonBlank(transactionId,"transactionId");
        if(timestamp==null) throw new IllegalArgumentException("timestamp required");
        stage=Validation.requireNonBlank(stage,"stage");
        summary=summary==null?"":summary;
        evidenceIds=List.copyOf(evidenceIds==null?List.of():evidenceIds);
    }
}
