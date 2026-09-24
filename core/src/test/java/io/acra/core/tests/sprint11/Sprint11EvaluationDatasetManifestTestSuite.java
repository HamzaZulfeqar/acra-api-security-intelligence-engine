package io.acra.core.tests.sprint11;

import io.acra.core.active.research.AblationExecutionState;
import io.acra.core.active.research.ResearchDatasetCase;
import io.acra.core.active.research.ResearchGroundTruth;
import io.acra.core.active.research.S11EvaluationDatasetManifest;
import io.acra.core.tests.TestSupport;
import java.util.ArrayList;
import java.util.List;

public final class Sprint11EvaluationDatasetManifestTestSuite {
    private Sprint11EvaluationDatasetManifestTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT11_EVALUATION_DATASET_MANIFEST PASS assertions=" + assertions);
    }

    public static int run() {
        S11EvaluationDatasetManifest manifest = S11EvaluationDatasetManifest.canonical();
        int assertions = 0;

        TestSupport.assertEquals("GT-S11-ABLATION-DATASET", manifest.datasetId(),
                "dataset identity is explicit");
        assertions++;
        TestSupport.assertEquals(AblationExecutionState.NOT_RUN, manifest.executionState(),
                "dataset registration cannot claim experiment execution");
        assertions++;
        TestSupport.assertEquals(15, manifest.cases().size(),
                "all independently labelled S4 FP/FN controls are registered");
        assertions++;
        TestSupport.assertEquals(8L, manifest.positiveCount(),
                "dataset contains eight positive controls");
        assertions++;
        TestSupport.assertEquals(7L, manifest.negativeCount(),
                "dataset contains seven negative controls");
        assertions++;
        TestSupport.assertTrue(manifest.scope().contains("controlled registered ground truth"),
                "dataset remains controlled-ground-truth only");
        assertions++;

        for (int index = 0; index < manifest.cases().size(); index++) {
            ResearchDatasetCase item = manifest.cases().get(index);
            TestSupport.assertEquals(String.format("S11-EVAL-%03d", index + 1), item.caseId(),
                    "dataset case order is deterministic");
            assertions++;
            TestSupport.assertEquals("GT-S4-RESEARCH-FIXTURES", item.sourceGroundTruthId(),
                    "every case preserves source-ground-truth provenance");
            assertions++;
        }

        S11EvaluationDatasetManifest repeated = S11EvaluationDatasetManifest.canonical();
        TestSupport.assertEquals(manifest.fingerprint(), repeated.fingerprint(),
                "dataset fingerprint is deterministic");
        assertions++;

        List<ResearchDatasetCase> duplicate = new ArrayList<>(manifest.cases());
        duplicate.set(1, new ResearchDatasetCase(
                "S11-EVAL-002",
                "GT-S4-RESEARCH-FIXTURES",
                duplicate.getFirst().sourceCaseId(),
                "FALSE_POSITIVE_CONTROL",
                ResearchGroundTruth.NEGATIVE));
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new S11EvaluationDatasetManifest(
                        "duplicate-source",
                        "1",
                        AblationExecutionState.NOT_RUN,
                        manifest.scope(),
                        duplicate,
                        ""),
                "duplicate source case fails closed");
        assertions++;

        List<ResearchDatasetCase> noPositives = manifest.cases().stream()
                .map(value -> new ResearchDatasetCase(
                        value.caseId(),
                        value.sourceGroundTruthId(),
                        value.sourceCaseId(),
                        value.family(),
                        ResearchGroundTruth.NEGATIVE))
                .toList();
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new S11EvaluationDatasetManifest(
                        "single-class",
                        "1",
                        AblationExecutionState.NOT_RUN,
                        manifest.scope(),
                        noPositives,
                        ""),
                "single-class dataset fails closed");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new S11EvaluationDatasetManifest(
                        "executed-without-results",
                        "1",
                        AblationExecutionState.COMPLETE,
                        manifest.scope(),
                        manifest.cases(),
                        ""),
                "dataset manifest cannot self-promote experiment completion");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new S11EvaluationDatasetManifest(
                        "tampered",
                        "1",
                        AblationExecutionState.NOT_RUN,
                        manifest.scope(),
                        manifest.cases(),
                        "bad-fingerprint"),
                "tampered dataset fingerprint fails closed");
        assertions++;

        return assertions;
    }
}
