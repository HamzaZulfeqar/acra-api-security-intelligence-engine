package io.acra.core.domain.identity;

import io.acra.core.domain.common.*;
import io.acra.core.domain.evidence.EvidenceSource;

public record Role(String roleId, String name, EvidenceSource source, Confidence confidence) {
    public Role {
        roleId = Validation.requireNonBlank(roleId, "roleId");
        name = Validation.requireNonBlank(name, "role name");
        if (source == null) source = EvidenceSource.UNKNOWN;
        if (confidence == null) confidence = Confidence.unknown();
    }
    public static Role unknown() { return new Role("unknown", "UNKNOWN", EvidenceSource.UNKNOWN, Confidence.unknown()); }
}
