package io.acra.core.active.research;

import io.acra.core.analysis.PassiveDifferentialComparator;
import io.acra.core.analysis.ResponseNormalizer;
import io.acra.core.analysis.ResponseComparisonMode;
import io.acra.core.analysis.semantic.ResponseSemanticAnalyzer;
import io.acra.core.domain.common.Validation;
import io.acra.core.domain.http.HttpResponse;
import io.acra.core.security.TokenFingerprint;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public final class S11TreatmentEvidenceCollector {
    private final ResponseSemanticAnalyzer semantic = new ResponseSemanticAnalyzer();
    private final PassiveDifferentialComparator differential = new PassiveDifferentialComparator();
    private final ResponseNormalizer normalizer = new ResponseNormalizer();

    public AblationEvidenceBundle collect(
            String caseId,
            String route,
            String principal,
            String tenant,
            String role,
            HttpResponse left,
            HttpResponse right) {

        caseId = Validation.requireNonBlank(caseId, "caseId");
        route = Validation.requireNonBlank(route, "route");
        if (left == null || right == null) throw new IllegalArgumentException("response pair required");

        var leftSemantic = semantic.fingerprint(left);
        var rightSemantic = semantic.fingerprint(right);
        var leftNormalized = normalizer.normalize(left);
        var rightNormalized = normalizer.normalize(right);
        var semanticDiff = differential.compare(left, right, ResponseComparisonMode.SEMANTIC);

        List<String> baseline = List.of(
                evidenceId("baseline-left", caseId, route, stableResponseMaterial(leftNormalized)),
                evidenceId("baseline-right", caseId, route, stableResponseMaterial(rightNormalized)),
                evidenceId("baseline-differential", caseId, route,
                        semanticDiff.equivalent() + "|" + semanticDiff.changedSignals()));

        List<AblationDimensionEvidenceReference> dimensions = new ArrayList<>();
        dimensions.add(contextEvidence(
                AblationDimension.IDENTITY, "identity", caseId, route, principal));
        dimensions.add(resourceEvidence(
                AblationDimension.OWNERSHIP, "ownership", caseId, route,
                leftSemantic.ownerIds(), rightSemantic.ownerIds()));
        dimensions.add(tenantEvidence(
                caseId, route, tenant, leftSemantic.tenantIds(), rightSemantic.tenantIds()));
        dimensions.add(contextEvidence(
                AblationDimension.ROLE, "role", caseId, route, role));
        dimensions.add(new AblationDimensionEvidenceReference(
                AblationDimension.WORKFLOW,
                route.contains("/workflows/")
                        ? AblationEvidenceDisposition.OBSERVED
                        : AblationEvidenceDisposition.NOT_APPLICABLE,
                List.of(evidenceId("workflow", caseId, route,
                        route.contains("/workflows/") ? "workflow-route" : "non-workflow-route"))));
        dimensions.add(new AblationDimensionEvidenceReference(
                AblationDimension.SEMANTIC_EVIDENCE,
                AblationEvidenceDisposition.OBSERVED,
                List.of(evidenceId("semantic", caseId, route,
                        stableResponseMaterial(leftNormalized) + "||"
                                + stableResponseMaterial(rightNormalized) + "|"
                                + leftSemantic.ownerIds() + "|" + leftSemantic.tenantIds() + "||"
                                + rightSemantic.ownerIds() + "|" + rightSemantic.tenantIds()))));

        TreeSet<String> prior = new TreeSet<>(baseline);
        for (AblationDimensionEvidenceReference item : dimensions) {
            for (String evidenceId : item.evidenceIds()) prior.add(evidenceId);
        }
        dimensions.add(new AblationDimensionEvidenceReference(
                AblationDimension.EVIDENCE_CORRELATION,
                AblationEvidenceDisposition.OBSERVED,
                List.of(evidenceId("correlation", caseId, route, String.join("|", prior)))));

        return new AblationEvidenceBundle(caseId, baseline, dimensions, "");
    }

    private static AblationDimensionEvidenceReference contextEvidence(
            AblationDimension dimension,
            String kind,
            String caseId,
            String route,
            String value) {
        boolean present = value != null && !value.isBlank();
        String material = present ? TokenFingerprint.sha256(value.strip()) : "not-applicable";
        return new AblationDimensionEvidenceReference(
                dimension,
                present ? AblationEvidenceDisposition.OBSERVED : AblationEvidenceDisposition.NOT_APPLICABLE,
                List.of(evidenceId(kind, caseId, route, material)));
    }

    private static AblationDimensionEvidenceReference resourceEvidence(
            AblationDimension dimension,
            String kind,
            String caseId,
            String route,
            java.util.Set<String> left,
            java.util.Set<String> right) {
        TreeSet<String> values = new TreeSet<>();
        values.addAll(left);
        values.addAll(right);
        boolean present = !values.isEmpty();
        return new AblationDimensionEvidenceReference(
                dimension,
                present ? AblationEvidenceDisposition.OBSERVED : AblationEvidenceDisposition.NOT_APPLICABLE,
                List.of(evidenceId(kind, caseId, route,
                        present ? TokenFingerprint.sha256(String.join("|", values)) : "not-applicable")));
    }

    private static AblationDimensionEvidenceReference tenantEvidence(
            String caseId,
            String route,
            String tenant,
            java.util.Set<String> left,
            java.util.Set<String> right) {
        TreeSet<String> values = new TreeSet<>();
        if (tenant != null && !tenant.isBlank()) values.add(tenant.strip());
        values.addAll(left);
        values.addAll(right);
        boolean present = !values.isEmpty();
        return new AblationDimensionEvidenceReference(
                AblationDimension.TENANT,
                present ? AblationEvidenceDisposition.OBSERVED : AblationEvidenceDisposition.NOT_APPLICABLE,
                List.of(evidenceId("tenant", caseId, route,
                        present ? TokenFingerprint.sha256(String.join("|", values)) : "not-applicable")));
    }

    private static String stableResponseMaterial(io.acra.core.analysis.NormalizedResponse response) {
        return response.status() + "|" + response.contentType() + "|"
                + response.structuralSignature() + "|" + response.semanticSignature() + "|"
                + TokenFingerprint.sha256(response.body());
    }

    private static String evidenceId(String kind, String caseId, String route, String material) {
        String digest = TokenFingerprint.sha256(kind + "|" + caseId + "|" + route + "|" + material);
        return "s11-evidence-" + digest.substring(0, 32);
    }
}
