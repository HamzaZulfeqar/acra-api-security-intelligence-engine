package io.acra.core.domain.endpoint;

import io.acra.core.domain.identity.AuthenticationType;
import java.time.Instant;

public record ApiEndpointRecord(Endpoint endpoint, String version, String environment, AuthenticationType authMethod,
                                String owner, Instant firstObserved, Instant lastObserved,
                                DocumentationStatus documentationStatus, RiskTier riskTier) {
    public ApiEndpointRecord {
        if (endpoint == null) throw new IllegalArgumentException("endpoint required");
        version = version == null ? "" : version;
        environment = environment == null ? "UNKNOWN" : environment;
        if (authMethod == null) authMethod = AuthenticationType.UNKNOWN;
        owner = owner == null ? "" : owner;
        if (firstObserved == null || lastObserved == null) throw new IllegalArgumentException("observation timestamps required");
        if (documentationStatus == null) documentationStatus = DocumentationStatus.UNKNOWN;
        if (riskTier == null) riskTier = RiskTier.UNKNOWN;
    }
}
