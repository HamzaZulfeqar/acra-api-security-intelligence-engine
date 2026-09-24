package io.acra.core.reproduction;

import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.security.UniversalRedactor;
import io.acra.core.serialization.CanonicalJsonSerializer;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class ReproductionSarifExporter {
    public static final String SARIF_VERSION = "2.1.0";
    public static final String SARIF_SCHEMA =
            "https://docs.oasis-open.org/sarif/sarif/v2.1.0/errata01/os/schemas/sarif-schema-2.1.0.json";
    public static final String RULE_ID = "ACRA-AUTHORIZATION-REVIEW";

    private final CanonicalJsonSerializer serializer = new CanonicalJsonSerializer();
    private final UniversalRedactor redactor = new UniversalRedactor();

    public ReproductionExportArtifact export(ReproductionPackage reproductionPackage) {
        if (reproductionPackage == null) throw new IllegalArgumentException("reproductionPackage required");

        Map<String, Object> properties = new TreeMap<>();
        properties.put("acraCandidateId", reproductionPackage.candidateId());
        properties.put("acraCandidateState", reproductionPackage.candidateState().name());
        properties.put("acraConfidence", reproductionPackage.confidence());
        properties.put("acraDimensions", reproductionPackage.dimensions());
        properties.put("acraEvidenceIds", reproductionPackage.evidenceIds());
        properties.put("acraExpectedDecision", reproductionPackage.expectedDecision().name());
        properties.put("acraObservedDecision", reproductionPackage.observedDecision().name());
        properties.put("acraPackageFingerprint", reproductionPackage.fingerprint());
        properties.put("acraPackageId", reproductionPackage.packageId());
        properties.put("acraPolicyReferences", reproductionPackage.policyReferences());
        properties.put("acraProjectId", reproductionPackage.projectId());
        properties.put("acraReproductionSchema", reproductionPackage.schemaVersion());
        properties.put("acraResourceId", reproductionPackage.resourceId());
        properties.put("acraReviewOnly", true);
        properties.put("acraSeverity", reproductionPackage.severity().name());
        properties.put("endpoint", reproductionPackage.endpoint());

        Map<String, Object> result = new TreeMap<>();
        result.put("fingerprints", Map.of("acra/v1", reproductionPackage.fingerprint()));
        result.put("kind", "review");
        result.put("level", level(reproductionPackage.severity()));
        result.put("message", Map.of("text", message(reproductionPackage)));
        result.put("properties", properties);
        result.put("ruleId", RULE_ID);

        Map<String, Object> rule = new TreeMap<>();
        rule.put("defaultConfiguration", Map.of("level", level(reproductionPackage.severity())));
        rule.put("fullDescription", Map.of(
                "text", "Evidence-backed ACRA authorization candidate exported for human review."));
        rule.put("id", RULE_ID);
        rule.put("name", "AuthorizationReviewCandidate");
        rule.put("properties", Map.of(
                "tags", List.of("security", "authorization", "review-only")));
        rule.put("shortDescription", Map.of(
                "text", "Review-only authorization finding candidate"));

        Map<String, Object> driver = new TreeMap<>();
        driver.put("name", "ACRA");
        driver.put("rules", List.of(rule));

        Map<String, Object> run = new TreeMap<>();
        run.put("results", List.of(result));
        run.put("tool", Map.of("driver", driver));

        Map<String, Object> root = new TreeMap<>();
        root.put("$schema", SARIF_SCHEMA);
        root.put("runs", List.of(run));
        root.put("version", SARIF_VERSION);

        String content = redactor.redactText(serializer.serialize(root));
        return new ReproductionExportArtifact(
                ReproductionExportTarget.SARIF,
                "application/sarif+json",
                reproductionPackage.packageId() + ".sarif",
                "",
                content);
    }

    private String message(ReproductionPackage reproductionPackage) {
        String summary = redactor.redactText(reproductionPackage.summary());
        if (summary.isBlank()) {
            return "Review-only ACRA authorization finding candidate.";
        }
        return summary;
    }

    private static String level(FindingSeverity severity) {
        return switch (severity == null ? FindingSeverity.INFO : severity) {
            case INFO, LOW -> "note";
            case MEDIUM -> "warning";
            case HIGH, CRITICAL -> "error";
        };
    }
}
