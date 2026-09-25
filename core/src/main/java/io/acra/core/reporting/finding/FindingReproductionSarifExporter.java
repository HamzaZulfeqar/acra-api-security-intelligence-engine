package io.acra.core.reporting.finding;

import io.acra.core.domain.finding.FindingLifecycleState;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import io.acra.core.serialization.DomainSerializer;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class FindingReproductionSarifExporter {
    public static final String SARIF_VERSION = "2.1.0";
    public static final String SARIF_SCHEMA =
            "https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/schemas/sarif-schema-2.1.0.json";

    private final DomainSerializer serializer = new DomainSerializer();
    private final UniversalRedactor redactor = new UniversalRedactor();

    public FindingReproductionExportArtifact sarif(FindingReproductionPackage reproduction) {
        if (reproduction == null) throw new IllegalArgumentException("reproduction required");

        Map<String, Object> log = new TreeMap<>();
        log.put("$schema", SARIF_SCHEMA);
        log.put("version", SARIF_VERSION);
        log.put("runs", List.of(run(reproduction)));

        String content = redactor.redactText(serializer.serializeCanonicalValue(log));
        return new FindingReproductionExportArtifact(
                "SARIF",
                "application/sarif+json",
                reproduction.reproductionId() + ".sarif",
                TokenFingerprint.sha256(content),
                content);
    }

    private static Map<String, Object> run(FindingReproductionPackage reproduction) {
        String ruleId = ruleId(reproduction);

        Map<String, Object> driver = new TreeMap<>();
        driver.put("name", "ACRA");
        driver.put(
                "informationUri",
                "https://github.com/HamzaZulfeqar/acra-api-security-intelligence-engine");
        driver.put("rules", List.of(rule(ruleId, reproduction)));

        Map<String, Object> tool = new TreeMap<>();
        tool.put("driver", driver);

        Map<String, Object> run = new TreeMap<>();
        run.put("tool", tool);
        run.put("results", List.of(result(ruleId, reproduction)));
        return run;
    }

    private static Map<String, Object> rule(
            String ruleId,
            FindingReproductionPackage reproduction) {
        Map<String, Object> rule = new TreeMap<>();
        rule.put("id", ruleId);
        rule.put("name", "ACRA API authorization review");
        rule.put(
                "shortDescription",
                Map.of("text", "Evidence-backed API authorization review result"));
        rule.put(
                "fullDescription",
                Map.of(
                        "text",
                        "ACRA correlates authorization context and evidence before human review."));
        rule.put(
                "properties",
                Map.of(
                        "dimensions", reproduction.dimensions(),
                        "reproductionSchemaVersion", reproduction.schemaVersion()));
        return rule;
    }

    private static Map<String, Object> result(
            String ruleId,
            FindingReproductionPackage reproduction) {
        Map<String, Object> result = new TreeMap<>();
        result.put("ruleId", ruleId);
        result.put("ruleIndex", 0);
        result.put("kind", kind(reproduction.state()));
        result.put("level", level(reproduction.state(), reproduction.severity()));
        result.put("message", Map.of("text", message(reproduction)));
        result.put(
                "locations",
                List.of(
                        Map.of(
                                "logicalLocations",
                                List.of(
                                        Map.of(
                                                "fullyQualifiedName", reproduction.endpoint(),
                                                "name", reproduction.endpoint(),
                                                "kind", "apiEndpoint")))));
        result.put(
                "partialFingerprints",
                Map.of("acraFindingFingerprint/v1", reproduction.findingFingerprint()));
        result.put("properties", properties(reproduction));

        if (reproduction.state() == FindingLifecycleState.ACCEPTED_RISK) {
            result.put(
                    "suppressions",
                    List.of(
                            Map.of(
                                    "kind", "external",
                                    "status", "accepted",
                                    "justification",
                                            "Risk acceptance recorded in the ACRA finding lifecycle.")));
        }
        return result;
    }

    private static Map<String, Object> properties(FindingReproductionPackage reproduction) {
        Map<String, Object> properties = new TreeMap<>();
        properties.put("acraCandidateId", reproduction.candidateId());
        properties.put("acraFindingId", reproduction.findingId());
        properties.put("acraLifecycleState", reproduction.state().name());
        properties.put("acraReproductionId", reproduction.reproductionId());
        properties.put("acraSeverity", reproduction.severity().name());
        properties.put("acraConfidence", reproduction.confidence().name());
        properties.put("assessmentIds", reproduction.assessmentIds());
        properties.put("confirmedFinding", reproduction.confirmedFinding());
        properties.put("dimensions", reproduction.dimensions());
        properties.put("evidenceIds", reproduction.evidenceIds());
        properties.put("executionIds", reproduction.executionIds());
        properties.put("expectedDecision", reproduction.expectedDecision().name());
        properties.put("limitations", reproduction.limitations());
        properties.put("observationIds", reproduction.observationIds());
        properties.put("observedDecision", reproduction.observedDecision().name());
        properties.put("policyReferences", reproduction.policyReferences());
        properties.put("principalId", reproduction.principalId());
        properties.put("projectId", reproduction.projectId());
        properties.put("resourceId", reproduction.resourceId());
        properties.put("reviewTrail", reproduction.reviewTrail());
        properties.put("tenantRelationship", reproduction.tenantRelationship());
        properties.put("testIds", reproduction.testIds());
        return properties;
    }

    private static String kind(FindingLifecycleState state) {
        return switch (state) {
            case NEEDS_REVIEW, VALIDATED -> "review";
            case CONFIRMED, ACCEPTED_RISK -> "fail";
            case FALSE_POSITIVE -> "pass";
        };
    }

    private static String level(
            FindingLifecycleState state,
            FindingSeverity severity) {
        if (state == FindingLifecycleState.NEEDS_REVIEW
                || state == FindingLifecycleState.VALIDATED
                || state == FindingLifecycleState.FALSE_POSITIVE) {
            return "none";
        }
        return switch (severity) {
            case CRITICAL, HIGH -> "error";
            case MEDIUM -> "warning";
            case LOW, INFO -> "note";
        };
    }

    private static String message(FindingReproductionPackage reproduction) {
        return "ACRA API authorization finding state="
                + reproduction.state().name()
                + ", expected="
                + reproduction.expectedDecision().name()
                + ", observed="
                + reproduction.observedDecision().name()
                + ", endpoint="
                + reproduction.endpoint()
                + ", resource="
                + reproduction.resourceId()
                + ".";
    }

    private static String ruleId(FindingReproductionPackage reproduction) {
        if (reproduction.dimensions().size() == 1) {
            return "ACRA.AUTHORIZATION." + normalized(reproduction.dimensions().get(0));
        }
        return "ACRA.AUTHORIZATION.CONTEXT";
    }

    private static String normalized(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase();
        normalized = normalized.replaceAll("[^A-Z0-9_]+", "_");
        return normalized.isBlank() ? "CONTEXT" : normalized;
    }
}
