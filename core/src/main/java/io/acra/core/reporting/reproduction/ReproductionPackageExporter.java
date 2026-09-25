package io.acra.core.reporting.reproduction;

import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import io.acra.core.serialization.DomainSerializer;
import java.util.List;

public final class ReproductionPackageExporter {
    private final DomainSerializer serializer = new DomainSerializer();
    private final UniversalRedactor redactor = new UniversalRedactor();

    public ReproductionExportArtifact json(ReproductionPackage reproduction) {
        require(reproduction);
        String content = redactor.redactText(serializer.serialize(reproduction));
        return artifact("JSON", "application/json", reproduction.packageId() + ".json", content);
    }

    public ReproductionExportArtifact sarif(ReproductionPackage reproduction) {
        require(reproduction);
        String content = sarifContent(reproduction);
        return artifact("SARIF", "application/sarif+json", reproduction.packageId() + ".sarif", content);
    }

    private String sarifContent(ReproductionPackage reproduction) {
        String level = reproduction.state() == FindingCandidateState.CANDIDATE ? "warning" : "note";
        String message = "ACRA review artifact: expected " + reproduction.expectedDecision()
                + ", observed " + reproduction.observedDecision()
                + "; state=" + reproduction.state();

        StringBuilder out = new StringBuilder();
        out.append("{\"version\":\"2.1.0\",\"runs\":[{")
                .append("\"tool\":{\"driver\":{")
                .append("\"name\":\"ACRA\",")
                .append("\"semanticVersion\":\"12.0.0-phase1\",")
                .append("\"rules\":[{")
                .append("\"id\":\"ACRA-AUTHORIZATION-REVIEW\",")
                .append("\"name\":\"AuthorizationReviewArtifact\",")
                .append("\"shortDescription\":{\"text\":\"Authorization review artifact\"}")
                .append("}]}}},")
                .append("\"results\":[{")
                .append("\"ruleId\":\"ACRA-AUTHORIZATION-REVIEW\",")
                .append("\"level\":").append(q(level)).append(",")
                .append("\"message\":{\"text\":").append(q(message)).append("},")
                .append("\"locations\":[{\"logicalLocations\":[{")
                .append("\"fullyQualifiedName\":").append(q(reproduction.endpoint()))
                .append("}]}],")
                .append("\"fingerprints\":{\"acraFinding\":")
                .append(q(reproduction.findingFingerprint())).append("},")
                .append("\"properties\":{")
                .append("\"candidateId\":").append(q(reproduction.candidateId())).append(",")
                .append("\"reproductionPackageId\":").append(q(reproduction.packageId())).append(",")
                .append("\"state\":").append(q(reproduction.state().name())).append(",")
                .append("\"resourceId\":").append(q(reproduction.resourceId())).append(",")
                .append("\"expectedDecision\":").append(q(reproduction.expectedDecision().name())).append(",")
                .append("\"observedDecision\":").append(q(reproduction.observedDecision().name())).append(",")
                .append("\"dimensions\":").append(array(reproduction.dimensions())).append(",")
                .append("\"evidenceIds\":").append(array(reproduction.evidenceIds())).append(",")
                .append("\"policyReferences\":").append(array(reproduction.policyReferences())).append(",")
                .append("\"confirmed\":false")
                .append("}}]}]}");
        return redactor.redactText(out.toString());
    }

    private ReproductionExportArtifact artifact(
            String format,
            String mediaType,
            String fileName,
            String content) {
        String safe = redactor.redactText(content);
        return new ReproductionExportArtifact(
                format,
                mediaType,
                fileName,
                TokenFingerprint.sha256(safe),
                safe);
    }

    private static String array(List<String> values) {
        StringBuilder out = new StringBuilder("[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) out.append(',');
            out.append(q(values.get(index)));
        }
        return out.append(']').toString();
    }

    private static String q(String value) {
        String safe = value == null ? "" : value;
        StringBuilder out = new StringBuilder("\"");
        for (char c : safe.toCharArray()) {
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '\"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('\"').toString();
    }

    private static void require(ReproductionPackage reproduction) {
        if (reproduction == null) throw new IllegalArgumentException("reproduction package required");
    }
}
