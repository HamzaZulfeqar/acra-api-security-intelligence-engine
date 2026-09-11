package io.acra.core.domain.authorization;

import io.acra.core.domain.common.*;
import io.acra.core.domain.evidence.EvidenceSource;

public record Action(ActionType actionType, EvidenceSource source, Confidence confidence, String applicationAction) {
    public Action {
        if (actionType == null) actionType = ActionType.UNKNOWN;
        if (source == null) source = EvidenceSource.UNKNOWN;
        if (confidence == null) confidence = Confidence.unknown();
        applicationAction = applicationAction == null ? "" : applicationAction;
    }
}
