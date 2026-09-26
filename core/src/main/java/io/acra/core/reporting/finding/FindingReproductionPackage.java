package io.acra.core.reporting.finding;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.domain.finding.FindingLifecycleState;
import io.acra.core.domain.finding.FindingSeverity;
import java.time.Instant;
import java.util.List;

public record FindingReproductionPackage(
        String reproductionId,
        String schemaVersion,
        Instant generatedAt,
        String projectId,
        String findingId,
        String candidateId,
        FindingLifecycleState state,
        FindingSeverity severity,
        FindingConfidence confidence,
        boolean confirmedFinding,
        String findingFingerprint,
        String endpoint,
        String resourceId,
        String principalId,
        String tenantRelationship,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        List<String> dimensions,
        List<String> testIds,
        List<String> executionIds,
        List<String> observationIds,
        List<String> assessmentIds,
        List<String> evidenceIds,
        List<String> policyReferences,
        List<FindingReviewTrailEntry> reviewTrail,
        List<String> limitations) {

    public FindingReproductionPackage {
        reproductionId = required(reproductionId, "reproductionId");
        schemaVersion = required(schemaVersion, "schemaVersion");
        if (generatedAt == null) throw new IllegalArgumentException("generatedAt required");
        projectId = required(projectId, "projectId");
        findingId = required(findingId, "findingId");
        candidateId = required(candidateId, "candidateId");
        if (state == null) throw new IllegalArgumentException("state required");
        if (severity == null) throw new IllegalArgumentException("severity required");
        if (confidence == null) throw new IllegalArgumentException("confidence required");
        findingFingerprint = required(findingFingerprint, "findingFingerprint");
        endpoint = required(endpoint, "endpoint");
        resourceId = required(resourceId, "resourceId");
        principalId = required(principalId, "principalId");
        tenantRelationship = required(tenantRelationship, "tenantRelationship");
        if (expectedDecision == null) throw new IllegalArgumentException("expectedDecision required");
        if (observedDecision == null) throw new IllegalArgumentException("observedDecision required");
        dimensions = sorted(dimensions);
        testIds = sorted(testIds);
        executionIds = sorted(executionIds);
        observationIds = sorted(observationIds);
        assessmentIds = sorted(assessmentIds);
        evidenceIds = sorted(evidenceIds);
        policyReferences = sorted(policyReferences);
        reviewTrail = List.copyOf(reviewTrail == null ? List.of() : reviewTrail);
        if (reviewTrail.stream().anyMatch(value -> value == null)) {
            throw new IllegalArgumentException("reviewTrail contains null");
        }
        limitations = sorted(limitations);
        if (evidenceIds.isEmpty()) throw new IllegalArgumentException("evidenceIds required");
        if (limitations.isEmpty()) throw new IllegalArgumentException("limitations required");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " required");
        return value;
    }

    private static List<String> sorted(List<String> values) {
        return List.copyOf(values == null ? List.<String>of() : values).stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}
