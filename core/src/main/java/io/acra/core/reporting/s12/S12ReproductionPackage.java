package io.acra.core.reporting.s12;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record S12ReproductionPackage(
        String packageId,
        String packageVersion,
        String sourceCandidateId,
        FindingCandidateState candidateState,
        String projectId,
        String endpoint,
        String resourceId,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        List<String> dimensions,
        List<String> evidenceIds,
        List<String> policyReferences,
        String confidence,
        List<String> limitations,
        String fingerprint) {

    public static final String VERSION = "s12-reproduction-package-v1";
    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public S12ReproductionPackage {
        packageVersion = safeRequired(packageVersion, "packageVersion");
        sourceCandidateId = safeRequired(sourceCandidateId, "sourceCandidateId");
        candidateState = candidateState == null ? FindingCandidateState.INCONCLUSIVE : candidateState;
        projectId = safeRequired(projectId, "projectId");
        endpoint = safeRequired(endpoint, "endpoint");
        if (endpoint.contains("?") || endpoint.contains("#")) {
            throw new IllegalArgumentException("endpoint must exclude query and fragment material");
        }
        resourceId = safeRequired(resourceId, "resourceId");
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        dimensions = clean(dimensions);
        evidenceIds = clean(evidenceIds);
        policyReferences = clean(policyReferences);
        confidence = safeRequired(
                confidence == null || confidence.isBlank() ? "INSUFFICIENT" : confidence,
                "confidence");
        limitations = clean(limitations);
        if (candidateState == FindingCandidateState.CANDIDATE && evidenceIds.isEmpty()) {
            throw new IllegalArgumentException("review candidate requires supporting evidence");
        }
        String material = canonical(
                packageVersion, sourceCandidateId, candidateState, projectId, endpoint, resourceId,
                expectedDecision, observedDecision, dimensions, evidenceIds, policyReferences,
                confidence, limitations);
        String expectedPackageId = "s12-repro-" + TokenFingerprint.sha256(material).substring(0, 24);
        packageId = packageId == null || packageId.isBlank() ? expectedPackageId : packageId;
        if (!packageId.equals(expectedPackageId)) {
            throw new IllegalArgumentException("reproduction package identity mismatch");
        }
        String calculated = TokenFingerprint.sha256(packageId + "|" + material);
        fingerprint = fingerprint == null || fingerprint.isBlank() ? calculated : fingerprint;
        if (!fingerprint.equals(calculated)) {
            throw new IllegalArgumentException("reproduction package fingerprint mismatch");
        }
    }

    private static String canonical(
            String version,
            String candidateId,
            FindingCandidateState state,
            String projectId,
            String endpoint,
            String resourceId,
            AuthorizationDecision expected,
            AuthorizationDecision observed,
            List<String> dimensions,
            List<String> evidenceIds,
            List<String> policyReferences,
            String confidence,
            List<String> limitations) {
        return version + "|" + candidateId + "|" + state + "|" + projectId + "|"
                + endpoint + "|" + resourceId + "|" + expected + "|" + observed + "|"
                + dimensions + "|" + evidenceIds + "|" + policyReferences + "|"
                + confidence + "|" + limitations;
    }

    private static List<String> clean(List<String> values) {
        return List.copyOf(values == null ? List.<String>of() : values).stream()
                .map(value -> safeRequired(value, "list value"))
                .distinct()
                .sorted()
                .toList();
    }

    private static String safeRequired(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        String stripped = value.strip();
        String redacted = REDACTOR.redactText(stripped);
        if (!stripped.equals(redacted)) {
            throw new IllegalArgumentException(name + " contains secret-bearing material");
        }
        return stripped;
    }
}
