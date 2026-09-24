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
                        ready("S11-EVAL-001", "S4-FP-PUBLIC",
                                List.of("/api/v1/s4/public"),
                                "public-resource endpoint exists and is exercised as a live false-positive control"),
                        ready("S11-EVAL-002", "S4-FP-SOFT-DENY",
                                List.of("/api/v1/s4/application-denial"),
                                "live HTTP 200 application denial is explicitly normalized to DENY"),
                        partial("S11-EVAL-003", "S4-FP-TIMESTAMP",
                                List.of("/api/v1/s4/documents/Document-A"),
                                "dynamic timestamp is present but timestamp and request_id currently vary together"),
                        partial("S11-EVAL-004", "S4-FP-REQUEST-ID",
                                List.of("/api/v1/s4/documents/Document-A"),
                                "dynamic request_id is present but request_id and timestamp currently vary together"),
                        partial("S11-EVAL-005", "S4-FP-ORDERING",
                                List.of("/api/v1/s4/public?variant=a", "/api/v1/s4/public?variant=b"),
                                "current pair changes ordering and formatting together"),
                        partial("S11-EVAL-006", "S4-FP-FORMATTING",
                                List.of("/api/v1/s4/public?variant=a", "/api/v1/s4/public?variant=b"),
                                "current pair changes formatting and ordering together"),
                        ready("S11-EVAL-007", "S4-FP-REPRESENTATION",
                                List.of("/api/v1/s4/public?variant=a", "/api/v1/s4/public?variant=b"),
                                "same public resource is verified semantically equivalent across raw representations"),
                        missing("S11-EVAL-008", "S4-FN-SAME-STATUS"),
                        missing("S11-EVAL-009", "S4-FN-SOFT-DENIAL"),
                        missing("S11-EVAL-010", "S4-FN-DYNAMIC-LENGTH"),
                        missing("S11-EVAL-011", "S4-FN-REORDERED-JSON"),
                        missing("S11-EVAL-012", "S4-FN-OPAQUE-ID"),
                        missing("S11-EVAL-013", "S4-FN-NESTED"),
                        missing("S11-EVAL-014", "S4-FN-COLLECTION"),
                        missing("S11-EVAL-015", "S4-FN-NONSTANDARD-AUTH")),
                "");
    }

    public long count(FixtureReadinessState state) {
        return cases.stream().filter(value -> value.state() == state).count();
    }

    private static ResearchFixtureReadiness ready(
            String caseId, String sourceCaseId, List<String> routes, String reason) {
        return new ResearchFixtureReadiness(caseId, sourceCaseId, FixtureReadinessState.READY, routes,
                List.of("GT-EXEC-S4#false_positive_preparation",
                        "Sprint4LocalhostIntegrationTestSuite#testLiveFalsePositivePreparationCases"), reason);
    }

    private static ResearchFixtureReadiness partial(
            String caseId, String sourceCaseId, List<String> routes, String reason) {
        return new ResearchFixtureReadiness(caseId, sourceCaseId, FixtureReadinessState.PARTIAL, routes,
                List.of("GT-EXEC-S4#false_positive_preparation",
                        "Sprint4LocalhostIntegrationTestSuite#testLiveFalsePositivePreparationCases"), reason);
    }

    private static ResearchFixtureReadiness missing(String caseId, String sourceCaseId) {
        return new ResearchFixtureReadiness(caseId, sourceCaseId, FixtureReadinessState.MISSING_FIXTURE,
                List.of(), List.of(), "no dedicated executable fixture currently maps this positive research case");
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
