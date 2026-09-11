package io.acra.core.analysis.semantic;

import java.util.Set;

public record ResponseSemanticFingerprint(
        int status,
        String contentType,
        int bodyLength,
        SemanticResponseClass responseClass,
        Set<String> fields,
        Set<String> resourceIds,
        Set<String> ownerIds,
        Set<String> tenantIds,
        Set<String> volatileFields,
        String structuralSignature,
        String semanticSignature) {
    public ResponseSemanticFingerprint {
        if (contentType == null || responseClass == null || fields == null || resourceIds == null
                || ownerIds == null || tenantIds == null || volatileFields == null
                || structuralSignature == null || semanticSignature == null) {
            throw new IllegalArgumentException("complete response semantic fingerprint required");
        }
        fields = Set.copyOf(fields);
        resourceIds = Set.copyOf(resourceIds);
        ownerIds = Set.copyOf(ownerIds);
        tenantIds = Set.copyOf(tenantIds);
        volatileFields = Set.copyOf(volatileFields);
    }
}