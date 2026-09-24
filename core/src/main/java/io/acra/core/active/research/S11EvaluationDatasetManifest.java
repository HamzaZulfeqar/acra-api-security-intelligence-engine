package io.acra.core.active.research;

import io.acra.core.domain.common.Validation;
import io.acra.core.security.TokenFingerprint;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record S11EvaluationDatasetManifest(
        String datasetId,
        String version,
        AblationExecutionState executionState,
        String scope,
        List<ResearchDatasetCase> cases,
        String fingerprint) {

    public S11EvaluationDatasetManifest {
        datasetId = Validation.requireNonBlank(datasetId, "datasetId");
        version = Validation.requireNonBlank(version, "version");
        executionState = executionState == null ? AblationExecutionState.NOT_RUN : executionState;
        if (executionState != AblationExecutionState.NOT_RUN) {
            throw new IllegalArgumentException("dataset registration cannot claim experiment execution");
        }
        scope = Validation.requireNonBlank(scope, "scope");
        cases = List.copyOf(cases == null ? List.of() : cases).stream()
                .sorted(Comparator.comparing(ResearchDatasetCase::caseId))
                .toList();
        validateCases(cases);
        String calculated = TokenFingerprint.sha256(canonical(version, scope, cases));
        fingerprint = fingerprint == null || fingerprint.isBlank() ? calculated : fingerprint;
        if (!fingerprint.equals(calculated)) {
            throw new IllegalArgumentException("dataset manifest fingerprint mismatch");
        }
    }

    public static S11EvaluationDatasetManifest canonical() {
        List<ResearchDatasetCase> cases = List.of(
                negative("S11-EVAL-001", "S4-FP-PUBLIC", "FALSE_POSITIVE_CONTROL"),
                negative("S11-EVAL-002", "S4-FP-SOFT-DENY", "FALSE_POSITIVE_CONTROL"),
                negative("S11-EVAL-003", "S4-FP-TIMESTAMP", "FALSE_POSITIVE_CONTROL"),
                negative("S11-EVAL-004", "S4-FP-REQUEST-ID", "FALSE_POSITIVE_CONTROL"),
                negative("S11-EVAL-005", "S4-FP-ORDERING", "FALSE_POSITIVE_CONTROL"),
                negative("S11-EVAL-006", "S4-FP-FORMATTING", "FALSE_POSITIVE_CONTROL"),
                negative("S11-EVAL-007", "S4-FP-REPRESENTATION", "FALSE_POSITIVE_CONTROL"),
                positive("S11-EVAL-008", "S4-FN-SAME-STATUS", "FALSE_NEGATIVE_CONTROL"),
                positive("S11-EVAL-009", "S4-FN-SOFT-DENIAL", "FALSE_NEGATIVE_CONTROL"),
                positive("S11-EVAL-010", "S4-FN-DYNAMIC-LENGTH", "FALSE_NEGATIVE_CONTROL"),
                positive("S11-EVAL-011", "S4-FN-REORDERED-JSON", "FALSE_NEGATIVE_CONTROL"),
                positive("S11-EVAL-012", "S4-FN-OPAQUE-ID", "FALSE_NEGATIVE_CONTROL"),
                positive("S11-EVAL-013", "S4-FN-NESTED", "FALSE_NEGATIVE_CONTROL"),
                positive("S11-EVAL-014", "S4-FN-COLLECTION", "FALSE_NEGATIVE_CONTROL"),
                positive("S11-EVAL-015", "S4-FN-NONSTANDARD-AUTH", "FALSE_NEGATIVE_CONTROL"));
        String scope = "controlled registered ground truth only";
        String version = "1";
        String fingerprint = TokenFingerprint.sha256(canonical(version, scope, cases));
        return new S11EvaluationDatasetManifest(
                "GT-S11-ABLATION-DATASET",
                version,
                AblationExecutionState.NOT_RUN,
                scope,
                cases,
                fingerprint);
    }

    public long positiveCount() {
        return cases.stream().filter(value -> value.groundTruth() == ResearchGroundTruth.POSITIVE).count();
    }

    public long negativeCount() {
        return cases.stream().filter(value -> value.groundTruth() == ResearchGroundTruth.NEGATIVE).count();
    }

    private static ResearchDatasetCase positive(String id, String sourceCaseId, String family) {
        return item(id, sourceCaseId, family, ResearchGroundTruth.POSITIVE);
    }

    private static ResearchDatasetCase negative(String id, String sourceCaseId, String family) {
        return item(id, sourceCaseId, family, ResearchGroundTruth.NEGATIVE);
    }

    private static ResearchDatasetCase item(
            String id, String sourceCaseId, String family, ResearchGroundTruth truth) {
        return new ResearchDatasetCase(
                id, "GT-S4-RESEARCH-FIXTURES", sourceCaseId, family, truth);
    }

    private static void validateCases(List<ResearchDatasetCase> cases) {
        if (cases.isEmpty()) throw new IllegalArgumentException("dataset cases required");
        Set<String> ids = new HashSet<>();
        Set<String> sourceIds = new HashSet<>();
        int positive = 0;
        int negative = 0;
        for (int index = 0; index < cases.size(); index++) {
            ResearchDatasetCase value = cases.get(index);
            String expectedId = String.format("S11-EVAL-%03d", index + 1);
            if (!expectedId.equals(value.caseId())) {
                throw new IllegalArgumentException("dataset case ordering/identity mismatch");
            }
            if (!ids.add(value.caseId()) || !sourceIds.add(value.sourceCaseId())) {
                throw new IllegalArgumentException("duplicate dataset/source case");
            }
            if (!"GT-S4-RESEARCH-FIXTURES".equals(value.sourceGroundTruthId())) {
                throw new IllegalArgumentException("Phase 2 source must be the independently labelled S4 fixture");
            }
            if (value.groundTruth() == ResearchGroundTruth.POSITIVE) positive++; else negative++;
        }
        if (positive == 0 || negative == 0) {
            throw new IllegalArgumentException("dataset requires positive and negative controls");
        }
    }

    private static String canonical(
            String version, String scope, List<ResearchDatasetCase> cases) {
        StringBuilder out = new StringBuilder(version).append('|').append(scope).append('\n');
        for (ResearchDatasetCase value : cases) {
            out.append(value.caseId()).append('|')
                    .append(value.sourceGroundTruthId()).append('|')
                    .append(value.sourceCaseId()).append('|')
                    .append(value.family()).append('|')
                    .append(value.groundTruth()).append('\n');
        }
        return out.toString();
    }
}
