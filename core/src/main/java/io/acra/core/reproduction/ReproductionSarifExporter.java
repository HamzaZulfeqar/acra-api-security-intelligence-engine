package io.acra.core.reproduction;

import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import io.acra.core.serialization.CanonicalJsonDocumentWriter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class ReproductionSarifExporter {
    public static final String SARIF_VERSION = "2.1.0";
    public static final String SARIF_SCHEMA =
            "https://docs.oasis-open.org/sarif/sarif/v2.1.0/errata01/os/schemas/sarif-schema-2.1.0.json";

    private final UniversalRedactor redactor = new UniversalRedactor();
    private final CanonicalJsonDocumentWriter writer = new CanonicalJsonDocumentWriter(redactor);

    public ReproductionExportArtifact sarif(ReproductionPackage value) {
        if (value == null) throw new IllegalArgumentException("reproduction package required");

        String ruleId = ruleId(value);
        Map<String, Object> driver = new TreeMap<>();
        driver.put("name", "ACRA");
        driver.put("semanticVersion", "0.3.0-rc1");
        driver.put("rules", List.of(rule(value, ruleId)));

        Map<String, Object> run = new TreeMap<>();
        run.put("tool", Map.of("driver", driver));
        run.put("results", List.of(result(value, ruleId)));

        Map<String, Object> log = new TreeMap<>();
        log.put("$schema", SARIF_SCHEMA);
        log.put("version", SARIF_VERSION);
        log.put("runs", List.of(run));

        String content = redactor.redactText(writer.write(log));
        return new ReproductionExportArtifact(
                "SARIF",
                "application/sarif+json",
                value.packageId() + ".sarif",
                TokenFingerprint.sha256(content),
                content);
    }

    private Map<String, Object> rule(ReproductionPackage value, String ruleId) {
        Map<String, Object> properties = new TreeMap<>();
        properties.put("acraDimensions", value.dimensions());
        properties.put("acraReviewOnly", true);

        Map<String, Object> rule = new TreeMap<>();
        rule.put("id", ruleId);
        rule.put("name", "AuthorizationReviewCandidate");
        rule.put("shortDescription", Map.of(
                "text", "Review-only API authorization finding candidate"));
        rule.put("properties", properties);
        return rule;
    }

    private Map<String, Object> result(ReproductionPackage value, String ruleId) {
        Map<String, Object> properties = new TreeMap<>();
        properties.put("acraAssessmentIds", value.assessmentIds());
        properties.put("acraCandidateId", value.candidateId());
        properties.put("acraCandidateState", value.candidateState().name());
        properties.put("acraConfidence", value.confidence().name());
        properties.put("acraEndpoint", value.endpoint());
        properties.put("acraEvidenceIds", value.evidenceIds());
        properties.put("acraExpectedDecision", value.expectedDecision().name());
        properties.put("acraIssueEligible", value.issueEligible());
        properties.put("acraLimitations", value.limitations());
        properties.put("acraObservedDecision", value.observedDecision().name());
        properties.put("acraPolicyReferences", value.policyReferences());
        properties.put("acraPrincipalFingerprint", value.principalFingerprint());
        properties.put("acraProjectId", value.projectId());
        properties.put("acraResourceId", value.resourceId());
        properties.put("acraReviewOnly", value.reviewOnly());
        properties.put("acraSeverity", value.severity().name());
        properties.put("acraSourceReportId", value.sourceReportId());
        properties.put("acraTenantRelationship", value.tenantRelationship());

        Map<String, Object> result = new TreeMap<>();
        result.put("ruleId", ruleId);
        result.put("kind", kind(value.candidateState()));
        result.put("level", "none");
        result.put("message", Map.of("text", message(value)));
        result.put("fingerprints", Map.of(
                "acraFinding/v1", value.findingFingerprint(),
                "acraReproduction/v1", value.fingerprint()));
        result.put("properties", properties);
        return result;
    }

    private String message(ReproductionPackage value) {
        return redactor.redactText(
                "ACRA review-only authorization result for endpoint "
                        + value.endpoint()
                        + ": expected " + value.expectedDecision()
                        + ", observed " + value.observedDecision()
                        + ". Human review is required; this export does not claim confirmed exploitation.");
    }

    private static String kind(FindingCandidateState state) {
        return switch (state) {
            case CANDIDATE -> "review";
            case REJECTED -> "pass";
            case INCONCLUSIVE -> "open";
        };
    }

    private static String ruleId(ReproductionPackage value) {
        String dimensions = value.dimensions().isEmpty()
                ? "GENERAL"
                : String.join("_", value.dimensions());
        String normalized = dimensions.toUpperCase(java.util.Locale.ROOT)
                .replaceAll("[^A-Z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (normalized.isBlank()) normalized = "GENERAL";
        return "ACRA.AUTHORIZATION." + normalized;
    }
}
