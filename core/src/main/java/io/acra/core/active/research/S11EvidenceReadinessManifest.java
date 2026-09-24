package io.acra.core.active.research;

import io.acra.core.domain.common.Validation;
import io.acra.core.security.TokenFingerprint;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record S11EvidenceReadinessManifest(
        String manifestId,
        String version,
        String datasetId,
        AblationExecutionState executionState,
        List<ResearchFixtureReadiness> cases,
        String fingerprint) {

    public S11EvidenceReadinessManifest {
        manifestId = Validation.requireNonBlank(manifestId, "manifestId");
        version = Validation.requireNonBlank(version, "version");
        datasetId = Validation.requireNonBlank(datasetId, "datasetId");
        executionState = executionState == null ? AblationExecutionState.NOT_RUN : executionState;
        if (executionState != AblationExecutionState.NOT_RUN) {
            throw new IllegalArgumentException("readiness manifest cannot claim experiment execution");
        }
        cases = List.copyOf(cases == null ? List.of() : cases).stream()
                .sorted(Comparator.comparing(ResearchFixtureReadiness::caseId))
                .toList();
        validate(cases);
        String calculated = TokenFingerprint.sha256(canonical(version, datasetId, cases));
        fingerprint = fingerprint == null || fingerprint.isBlank() ? calculated : fingerprint;
        if (!fingerprint.equals(calculated)) {
            throw new IllegalArgumentException("readiness manifest fingerprint mismatch");
        }
    }

    public static S11EvidenceReadinessManifest canonical() {
        return new S11EvidenceReadinessManifest(
                "GT-S11-EVIDENCE-READINESS",
                "1",
                "GT-S11-ABLATION-DATASET",
                AblationExecutionState.NOT_RUN,
                List.of(
                        legacyReady("S11-EVAL-001", "S4-FP-PUBLIC",
                                List.of("/api/v1/s4/public"),
                                "public-resource endpoint exists and is exercised as a live false-positive control"),
                        legacyReady("S11-EVAL-002", "S4-FP-SOFT-DENY",
                                List.of("/api/v1/s4/application-denial"),
                                "live HTTP 200 application denial is explicitly normalized to DENY"),
                        researchReady("S11-EVAL-003", "S4-FP-TIMESTAMP",
                                List.of("/api/v1/s11/research/case-003?variant=a",
                                        "/api/v1/s11/research/case-003?variant=b"),
                                "dedicated localhost variants isolate timestamp while request_id remains fixed"),
                        researchReady("S11-EVAL-004", "S4-FP-REQUEST-ID",
                                List.of("/api/v1/s11/research/case-004?variant=a",
                                        "/api/v1/s11/research/case-004?variant=b"),
                                "dedicated localhost variants isolate request_id while timestamp remains fixed"),
                        researchReady("S11-EVAL-005", "S4-FP-ORDERING",
                                List.of("/api/v1/s11/research/case-005?variant=a",
                                        "/api/v1/s11/research/case-005?variant=b"),
                                "dedicated localhost variants isolate compact JSON field ordering"),
                        researchReady("S11-EVAL-006", "S4-FP-FORMATTING",
                                List.of("/api/v1/s11/research/case-006?variant=a",
                                        "/api/v1/s11/research/case-006?variant=b"),
                                "dedicated localhost variants isolate whitespace formatting with stable field order"),
                        legacyReady("S11-EVAL-007", "S4-FP-REPRESENTATION",
                                List.of("/api/v1/s4/public?variant=a", "/api/v1/s4/public?variant=b"),
                                "same public resource is verified semantically equivalent across raw representations"),
                        researchReady("S11-EVAL-008", "S4-FN-SAME-STATUS",
                                List.of("/api/v1/s11/research/case-008?variant=a"),
                                "same HTTP status carries secure DENY and vulnerable foreign-resource ALLOW"),
                        researchReady("S11-EVAL-009", "S4-FN-SOFT-DENIAL",
                                List.of("/api/v1/s11/research/case-009?variant=a"),
                                "HTTP-200 soft denial and vulnerable foreign-resource allowance are live verified"),
                        researchReady("S11-EVAL-010", "S4-FN-DYNAMIC-LENGTH",
                                List.of("/api/v1/s11/research/case-010?variant=a&pad=2",
                                        "/api/v1/s11/research/case-010?variant=a&pad=17"),
                                "controlled padding varies length while authorization meaning remains stable per mode"),
                        researchReady("S11-EVAL-011", "S4-FN-REORDERED-JSON",
                                List.of("/api/v1/s11/research/case-011?variant=a",
                                        "/api/v1/s11/research/case-011?variant=b"),
                                "foreign-resource JSON reordering is verified against secure denial"),
                        researchReady("S11-EVAL-012", "S4-FN-OPAQUE-ID",
                                List.of("/api/v1/s11/research/case-012?variant=a"),
                                "opaque foreign resource is verified against secure denial"),
                        researchReady("S11-EVAL-013", "S4-FN-NESTED",
                                List.of("/api/v1/s11/research/case-013?variant=a"),
                                "nested foreign resource and owner evidence are live verified"),
                        researchReady("S11-EVAL-014", "S4-FN-COLLECTION",
                                List.of("/api/v1/s11/research/case-014?variant=a"),
                                "secure collection excludes and vulnerable collection includes foreign member"),
                        researchReady("S11-EVAL-015", "S4-FN-NONSTANDARD-AUTH",
                                List.of("/api/v1/s11/research/case-015?variant=a"),
                                "synthetic nonstandard principal context is required and live verified")),
                "");
    }

    public long count(FixtureReadinessState state) {
        return cases.stream().filter(value -> value.state() == state).count();
    }

    private static ResearchFixtureReadiness legacyReady(
            String caseId, String sourceCaseId, List<String> routes, String reason) {
        return new ResearchFixtureReadiness(caseId, sourceCaseId, FixtureReadinessState.READY, routes,
                List.of("GT-EXEC-S4#false_positive_preparation",
                        "Sprint4LocalhostIntegrationTestSuite#testLiveFalsePositivePreparationCases"), reason);
    }

    private static ResearchFixtureReadiness researchReady(
            String caseId, String sourceCaseId, List<String> routes, String reason) {
        return new ResearchFixtureReadiness(caseId, sourceCaseId, FixtureReadinessState.READY, routes,
                List.of("GT-S11-RESEARCH-LAB-FIXTURES#" + caseId,
                        "Sprint11ControlledResearchLabFixtureTestSuite"), reason);
    }

    private static void validate(List<ResearchFixtureReadiness> cases) {
        S11EvaluationDatasetManifest dataset = S11EvaluationDatasetManifest.canonical();
        if (cases.size() != dataset.cases().size()) {
            throw new IllegalArgumentException("readiness manifest must cover every dataset case");
        }
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < cases.size(); index++) {
            ResearchFixtureReadiness value = cases.get(index);
            ResearchDatasetCase source = dataset.cases().get(index);
            if (!source.caseId().equals(value.caseId())
                    || !source.sourceCaseId().equals(value.sourceCaseId())) {
                throw new IllegalArgumentException("readiness case/source ordering mismatch");
            }
            if (!seen.add(value.caseId())) {
                throw new IllegalArgumentException("duplicate readiness case");
            }
        }
    }

    private static String canonical(
            String version, String datasetId, List<ResearchFixtureReadiness> cases) {
        StringBuilder out = new StringBuilder(version).append('|').append(datasetId).append('\n');
        for (ResearchFixtureReadiness value : cases) {
            out.append(value.caseId()).append('|')
                    .append(value.sourceCaseId()).append('|')
                    .append(value.state()).append('|')
                    .append(value.routes()).append('|')
                    .append(value.evidenceReferences()).append('|')
                    .append(value.reason()).append('\n');
        }
        return out.toString();
    }
}
