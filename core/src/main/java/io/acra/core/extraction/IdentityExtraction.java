package io.acra.core.extraction;

import io.acra.core.domain.evidence.Evidence;
import io.acra.core.domain.identity.*;
import java.util.*;

public record IdentityExtraction(ExtractionResult<Principal> principal, ExtractionResult<Role> role,
                                 String tokenFingerprint, AuthenticationType authenticationType,
                                 List<Evidence> evidence) {
    public IdentityExtraction {
        if (principal == null) principal = ExtractionResult.unknown("identity not resolved");
        if (role == null) role = ExtractionResult.unknown("role not resolved");
        tokenFingerprint = tokenFingerprint == null ? "" : tokenFingerprint;
        if (authenticationType == null) authenticationType = AuthenticationType.UNKNOWN;
        evidence = List.copyOf(evidence == null ? List.of() : evidence);
    }
}
