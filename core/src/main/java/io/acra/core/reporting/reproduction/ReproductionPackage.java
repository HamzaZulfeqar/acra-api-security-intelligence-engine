package io.acra.core.reporting.reproduction;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record ReproductionPackage(
        String packageId,
        String packageVersion,
        String candidateId,
        FindingCandidateState state,
        String endpoint,
        String resourceId,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        List<String> dimensions,
        List<String> evidenceIds,
        List<String> policyReferences,
        String findingFingerprint,
        List<String> limitations) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public ReproductionPackage {
        packageVersion = required(packageVersion, "packageVersion");
        candidateId = required(candidateId, "candidateId");
        state = state == null ? FindingCandidateState.INCONCLUSIVE : state;
        endpoint = safe(endpoint);
        resourceId = safe(resourceId);
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        dimensions = safeList(dimensions);
        evidenceIds = safeList(evidenceIds);
        policyReferences = safeList(policyReferences);
        findingFingerprint = required(findingFingerprint, "findingFingerprint");
        limitations = safeList(limitations);

        String calculated = deterministicId(
                packageVersion,
                candidateId,
                state,
                endpoint,
                resourceId,
                expectedDecision,
                observedDecision,
                dimensions,
                evidenceIds,
                policyReferences,
                findingFingerprint,
                limitations);
        packageId = packageId == null || packageId.isBlank() ? calculated : packageId.strip();
        if (!packageId.equals(calculated)) {
            throw new IllegalArgumentException("reproduction package identity mismatch");
        }
    }

    public static String deterministicId(
            String version,
            String candidateId,
            FindingCandidateState state,
            String endpoint,
            String resourceId,
            AuthorizationDecision expectedDecision,
            AuthorizationDecision observedDecision,
            List<String> dimensions,
            List<String> evidenceIds,
            List<String> policyReferences,
            String findingFingerprint,
            List<String> limitations) {
        String material = version + "|" + candidateId + "|" + state + "|"
                + endpoint + "|" + resourceId + "|"
                + expectedDecision + "|" + observedDecision + "|"
                + dimensions + "|" + evidenceIds + "|" + policyReferences + "|"
                + findingFingerprint + "|" + limitations;
        return "acra-repro-" + TokenFingerprint.sha256(material).substring(0, 24);
    }

    private static String required(String value, String name) {
        String safe = safe(value);
        if (safe.isBlank()) throw new IllegalArgumentException(name + " required");
        return safe;
    }

    private static String safe(String value) {
        return REDACTOR.redactText(value == null ? "" : value).strip();
    }

    private static List<String> safeList(List<String> values) {
        return List.copyOf(values == null ? List.<String>of() : values).stream()
                .map(ReproductionPackage::safe)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}
