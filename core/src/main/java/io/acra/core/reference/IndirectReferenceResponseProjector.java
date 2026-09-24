package io.acra.core.reference;

import io.acra.core.active.evidence.ResponseSnapshot;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.security.TokenFingerprint;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IndirectReferenceResponseProjector {
    private static final Pattern RESOLVED =
            Pattern.compile("\\"resolved_resource_id\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    public IndirectReferenceResolution project(
            ResponseSnapshot response,
            String resolutionId,
            String sourceObservationId,
            String executionId,
            String testId,
            String endpoint,
            String transientRawReference,
            String referenceKind,
            String action,
            AuthorizationDecision observedDecision,
            List<String> evidenceIds) {

        if (response == null) throw new IllegalArgumentException("response required");
        if (transientRawReference == null || transientRawReference.isBlank()) {
            throw new IllegalArgumentException("transient raw reference required");
        }
        String resolved = resolvedResource(response.response().bodyUtf8());
        return new IndirectReferenceResolution(
                resolutionId,
                sourceObservationId,
                executionId,
                testId,
                endpoint,
                TokenFingerprint.sha256(transientRawReference),
                referenceKind,
                resolved,
                action,
                observedDecision,
                IndirectReferenceSource.OBSERVED,
                evidenceIds);
    }

    private static String resolvedResource(String body) {
        Matcher matcher = RESOLVED.matcher(body == null ? "" : body);
        if (!matcher.find()) throw new IllegalArgumentException("resolved_resource_id missing");
        String value = matcher.group(1);
        if (matcher.find()) throw new IllegalArgumentException("multiple resolved_resource_id values");
        return value;
    }
}
