package io.acra.core.reproduction;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record ReproductionPackage(
        String packageId,
        String packageVersion,
        String sourceReportId,
        String projectId,
        String candidateId,
        FindingCandidateState candidateState,
        boolean reviewOnly,
        boolean issueEligible,
        FindingSeverity severity,
        FindingConfidence confidence,
        String endpoint,
        String resourceId,
        String principalFingerprint,
        String tenantRelationship,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        List<String> dimensions,
        List<String> assessmentIds,
        List<String> evidenceIds,
        List<String> policyReferences,
        List<String> limitations,
        String findingFingerprint,
        String fingerprint) {

    public static final String VERSION = "acra-reproduction-package-v1";
    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public ReproductionPackage {
        packageId = safe(packageId);
        packageVersion = safe(packageVersion);
        sourceReportId = safe(sourceReportId);
        projectId = safe(projectId);
        candidateId = safe(candidateId);
        candidateState = candidateState == null ? FindingCandidateState.INCONCLUSIVE : candidateState;
        severity = severity == null ? FindingSeverity.INFO : severity;
        confidence = confidence == null ? FindingConfidence.INSUFFICIENT : confidence;
        endpoint = safe(endpoint);
        resourceId = safe(resourceId);
        principalFingerprint = safe(principalFingerprint);
        tenantRelationship = safe(tenantRelationship);
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        dimensions = safe(dimensions);
        assessmentIds = safe(assessmentIds);
        evidenceIds = safe(evidenceIds);
        policyReferences = safe(policyReferences);
        limitations = safe(limitations);
        findingFingerprint = safe(findingFingerprint);

        if (!VERSION.equals(packageVersion)) {
            throw new IllegalArgumentException("unsupported reproduction package version");
        }
        if (!reviewOnly) {
            throw new IllegalArgumentException("reproduction packages are review-only");
        }
        if (issueEligible != (candidateState == FindingCandidateState.CANDIDATE)) {
            throw new IllegalArgumentException("issueEligible must match CANDIDATE state");
        }

        String canonical = canonical(
                packageVersion, sourceReportId, projectId, candidateId, candidateState,
                reviewOnly, issueEligible, severity, confidence, endpoint, resourceId,
                principalFingerprint, tenantRelationship, expectedDecision, observedDecision,
                dimensions, assessmentIds, evidenceIds, policyReferences, limitations,
                findingFingerprint);
        String expectedPackageId = "rp-" + TokenFingerprint.sha256(canonical).substring(0, 24);
        packageId = packageId == null || packageId.isBlank() ? expectedPackageId : packageId;
        if (!packageId.equals(expectedPackageId)) {
            throw new IllegalArgumentException("reproduction package identity mismatch");
        }
        String calculated = TokenFingerprint.sha256(packageId + "|" + canonical);
        fingerprint = fingerprint == null || fingerprint.isBlank() ? calculated : fingerprint;
        if (!fingerprint.equals(calculated)) {
            throw new IllegalArgumentException("reproduction package fingerprint mismatch");
        }
    }

    private static String canonical(
            String packageVersion,
            String sourceReportId,
            String projectId,
            String candidateId,
            FindingCandidateState candidateState,
            boolean reviewOnly,
            boolean issueEligible,
            FindingSeverity severity,
            FindingConfidence confidence,
            String endpoint,
            String resourceId,
            String principalFingerprint,
            String tenantRelationship,
            AuthorizationDecision expectedDecision,
            AuthorizationDecision observedDecision,
            List<String> dimensions,
            List<String> assessmentIds,
            List<String> evidenceIds,
            List<String> policyReferences,
            List<String> limitations,
            String findingFingerprint) {
        return String.join("|",
                packageVersion,
                sourceReportId,
                projectId,
                candidateId,
                candidateState.name(),
                Boolean.toString(reviewOnly),
                Boolean.toString(issueEligible),
                severity.name(),
                confidence.name(),
                endpoint,
                resourceId,
                principalFingerprint,
                tenantRelationship,
                expectedDecision.name(),
                observedDecision.name(),
                dimensions.toString(),
                assessmentIds.toString(),
                evidenceIds.toString(),
                policyReferences.toString(),
                limitations.toString(),
                findingFingerprint);
    }

    private static String safe(String value) {
        return REDACTOR.redactText(value == null ? "" : value);
    }

    private static List<String> safe(List<String> values) {
        return List.copyOf(values == null ? List.<String>of() : values).stream()
                .map(ReproductionPackage::safe)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}
