package io.acra.core.reporting.s12;

import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import java.util.List;

public final class S12SarifExporter {
    public static final String SARIF_VERSION = "2.1.0";
    private final UniversalRedactor redactor = new UniversalRedactor();

    public S12ReproductionExportArtifact export(S12ReproductionPackage reproduction) {
        if (reproduction == null) throw new IllegalArgumentException("reproduction package required");
        String content = sarif(reproduction);
        return new S12ReproductionExportArtifact(
                "SARIF",
                "application/sarif+json",
                reproduction.packageId() + ".sarif",
                TokenFingerprint.sha256(content),
                content);
    }

    private String sarif(S12ReproductionPackage value) {
        String kind = value.candidateState() == FindingCandidateState.REJECTED
                ? "informational" : "review";
        String message = "ACRA authorization candidate state=" + value.candidateState()
                + " expected=" + value.expectedDecision()
                + " observed=" + value.observedDecision()
                + "; human review required before vulnerability confirmation.";

        return "{"
                + "\"version\":\"2.1.0\","
                + "\"runs\":[{"
                + "\"tool\":{\"driver\":{"
                + "\"name\":\"ACRA\","
                + "\"version\":\"" + q(S12ReproductionPackage.VERSION) + "\","
                + "\"rules\":[{"
                + "\"id\":\"ACRA-AUTH-REVIEW\","
                + "\"shortDescription\":{\"text\":\"Authorization review candidate\"}"
                + "}]"
                + "}},"
                + "\"results\":[{"
                + "\"ruleId\":\"ACRA-AUTH-REVIEW\","
                + "\"kind\":\"" + kind + "\","
                + "\"level\":\"none\","
                + "\"message\":{\"text\":\"" + q(message) + "\"},"
                + "\"properties\":{"
                + "\"packageId\":\"" + q(value.packageId()) + "\","
                + "\"candidateId\":\"" + q(value.sourceCandidateId()) + "\","
                + "\"candidateState\":\"" + value.candidateState() + "\","
                + "\"endpoint\":\"" + q(value.endpoint()) + "\","
                + "\"resourceId\":\"" + q(value.resourceId()) + "\","
                + "\"confidence\":\"" + q(value.confidence()) + "\","
                + "\"dimensions\":" + array(value.dimensions()) + ","
                + "\"evidenceIds\":" + array(value.evidenceIds()) + ","
                + "\"policyReferences\":" + array(value.policyReferences()) + ","
                + "\"reviewOnly\":true"
                + "}"
                + "}]"
                + "}]"
                + "}";
    }

    private String array(List<String> values) {
        return "[" + String.join(",", values.stream()
                .map(value -> "\"" + q(value) + "\"")
                .toList()) + "]";
    }

    private String q(String value) {
        String safe = redactor.redactText(value == null ? "" : value);
        StringBuilder out = new StringBuilder();
        for (char ch : safe.toCharArray()) {
            switch (ch) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (ch < 0x20) out.append(String.format("\\u%04x", (int) ch));
                    else out.append(ch);
                }
            }
        }
        return out.toString();
    }
}
