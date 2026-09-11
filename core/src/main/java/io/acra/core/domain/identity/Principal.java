package io.acra.core.domain.identity;

import io.acra.core.domain.common.*;

public record Principal(String principalId, String displayName, AuthenticationType authenticationType, Confidence confidence) {
    public Principal {
        principalId = Validation.requireNonBlank(principalId, "principalId");
        displayName = displayName == null ? "" : displayName;
        if (authenticationType == null) authenticationType = AuthenticationType.UNKNOWN;
        if (confidence == null) confidence = Confidence.unknown();
    }
}
