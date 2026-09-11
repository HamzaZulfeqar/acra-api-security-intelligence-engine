package io.acra.core.domain.resource;

import io.acra.core.domain.common.*;

public record Resource(String resourceId, String resourceType, String parentResourceId, String ownerPrincipalId,
                       String tenantId, String state, Confidence confidence) {
    public Resource {
        resourceId = Validation.requireNonBlank(resourceId, "resourceId");
        resourceType = Validation.requireNonBlank(resourceType, "resourceType");
        parentResourceId = blankToNull(parentResourceId);
        ownerPrincipalId = blankToNull(ownerPrincipalId);
        tenantId = blankToNull(tenantId);
        state = blankToNull(state);
        if (confidence == null) confidence = Confidence.unknown();
    }
    private static String blankToNull(String v) { return v == null || v.isBlank() ? null : v; }
}
