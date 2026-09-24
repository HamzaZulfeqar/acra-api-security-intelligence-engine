package io.acra.core.reproduction;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record ReproductionPackage(
        String schemaVersion,
        String packageId,
        String candidateId,
        FindingCandidateState candidateState,
        boolean reviewOnly,
        String projectId,
        FindingSeverity severity,
        String confidence,
        String endpoint,
        String resourceId,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        List<String> dimensions,
        List<String> policyReferences,
        List<String> evidenceIds,
        String summary,
        List<ReproductionExportTarget> declaredTargets,
        String fingerprint) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public ReproductionPackage {
        schemaVersion = required(schemaVersion, "schemaVersion");
        candidateId = required(candidateId, "candidateId");
        candidateState = candidateState == null ? FindingCandidateState.INCONCLUSIVE : candidateState;
        if (!reviewOnly) {
            throw new IllegalArgumentException("reproduction packages are review-only");
        }
        projectId = required(projectId, "projectId");
        severity = severity == null ? FindingSeverity.INFO : severity;
        confidence = safe(confidence);
        endpoint = required(endpoint, "endpoint");
        resourceId = required(resourceId, "resourceId");
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        dimensions = safeList(dimensions);
        policyReferences = safeList(policyReferences);
        evidenceIds = safeList(evidenceIds);
        summary = safe(summary);
        declaredTargets = List.copyOf(declaredTargets == null ? List.of() : declaredTargets).stream()
                .distinct()
                .sorted()
                .toList();
        if (!declaredTargets.equals(List.of(
                ReproductionExportTarget.JSON,
                ReproductionExportTarget.SARIF,
                ReproductionExportTarget.BURP_ISSUE))) {
            throw new IllegalArgumentException("FR-013 requires JSON, SARIF and BURP_ISSUE target contracts");
        }

        String material = canonical(
                schemaVersion, candidateId, candidateState, projectId, severity, confidence,
                endpoint, resourceId, expectedDecision, observedDecision, dimensions,
                policyReferences, evidenceIds, summary, declaredTargets);
        String expectedId = "rp-" + TokenFingerprint.sha256(material).substring(0, 24);
        packageId = packageId == null || packageId.isBlank() ? expectedId : packageId.strip();
        if (!packageId.equals(expectedId)) throw new IllegalArgumentException("packageId mismatch");

        String calculated = TokenFingerprint.sha256(packageId + "|" + material);
        fingerprint = fingerprint == null || fingerprint.isBlank() ? calculated : fingerprint.strip();
        if (!fingerprint.equals(calculated)) throw new IllegalArgumentException("package fingerprint mismatch");
    }

    private static String canonical(
            String schemaVersion,
            String candidateId,
            FindingCandidateState state,
            String projectId,
            FindingSeverity severity,
            String confidence,
            String endpoint,
            String resourceId,
            AuthorizationDecision expectedDecision,
            AuthorizationDecision observedDecision,
            List<String> dimensions,
            List<String> policyReferences,
            List<String> evidenceIds,
            String summary,
            List<ReproductionExportTarget> targets) {
        return String.join("|",
                schemaVersion,
                candidateId,
                state.name(),
                projectId,
                severity.name(),
                confidence,
                endpoint,
                resourceId,
                expectedDecision.name(),
                observedDecision.name(),
                dimensions.toString(),
                policyReferences.toString(),
                evidenceIds.toString(),
                summary,
                targets.toString());
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
