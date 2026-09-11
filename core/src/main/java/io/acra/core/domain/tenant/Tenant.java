package io.acra.core.domain.tenant;

import io.acra.core.domain.common.*;
import io.acra.core.domain.evidence.EvidenceSource;

public record Tenant(String tenantId, String name, EvidenceSource source, Confidence confidence) {
    public Tenant {
        tenantId = Validation.requireNonBlank(tenantId, "tenantId");
        name = name == null || name.isBlank() ? tenantId : name;
        if (source == null) source = EvidenceSource.UNKNOWN;
        if (confidence == null) confidence = Confidence.unknown();
    }
}
