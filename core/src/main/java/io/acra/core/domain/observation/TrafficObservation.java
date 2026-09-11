package io.acra.core.domain.observation;
import io.acra.core.domain.authorization.ActionType;
import io.acra.core.domain.authorization.ContextStatus;
import io.acra.core.domain.endpoint.Endpoint;
import io.acra.core.domain.identity.AuthenticationType;
import java.time.Instant;
import java.util.List;
public record TrafficObservation(String transactionId, Instant timestamp, Endpoint endpoint, int responseStatus,
                                 AuthenticationType authenticationType, String principalId, String tenantId,
                                 String resourceType, String resourceId, ActionType action, ContextStatus contextStatus,
                                 ObservationStatus status, List<String> evidenceIds) {
    public TrafficObservation {
        if(transactionId==null||transactionId.isBlank()) throw new IllegalArgumentException("transactionId required");
        if(timestamp==null||endpoint==null) throw new IllegalArgumentException("timestamp and endpoint required");
        if(responseStatus<0||responseStatus>599) throw new IllegalArgumentException("invalid response status");
        authenticationType=authenticationType==null?AuthenticationType.UNKNOWN:authenticationType;
        principalId=principalId==null?"":principalId; tenantId=tenantId==null?"":tenantId;
        resourceType=resourceType==null?"":resourceType; resourceId=resourceId==null?"":resourceId;
        action=action==null?ActionType.UNKNOWN:action;
        contextStatus=contextStatus==null?ContextStatus.PARTIAL:contextStatus;
        status=status==null?ObservationStatus.UNKNOWN:status;
        evidenceIds=List.copyOf(evidenceIds==null?List.of():evidenceIds);
    }
}
