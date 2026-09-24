package io.acra.core.tests.sprint11;

import io.acra.core.active.research.AblationCaseEvidence;
import io.acra.core.active.research.AblationDimension;
import io.acra.core.active.research.AblationDimensionEvidence;
import io.acra.core.active.research.AblationPredictionState;
import io.acra.core.active.research.ResearchPrediction;
import io.acra.core.active.research.S11AblationPredictionAdapter;
import io.acra.core.active.research.S11AblationProtocol;
import io.acra.core.tests.TestSupport;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;

public final class Sprint11AblationPredictionAdapterTestSuite {
    private Sprint11AblationPredictionAdapterTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT11_ABLATION_PREDICTION_ADAPTER PASS assertions=" + assertions);
    }

    public static int run() {
        var protocol = S11AblationProtocol.canonical();
        var adapter = new S11AblationPredictionAdapter();
        AblationCaseEvidence evidence = completeEvidence();
        int assertions = 0;

        List<ResearchPrediction> expected = List.of(
                ResearchPrediction.POSITIVE,
                ResearchPrediction.NEGATIVE,
                ResearchPrediction.POSITIVE,
                ResearchPrediction.NEGATIVE,
                ResearchPrediction.POSITIVE,
                ResearchPrediction.NEGATIVE,
                ResearchPrediction.POSITIVE,
                ResearchPrediction.NEGATIVE);

        for (int index = 0; index < protocol.variants().size(); index++) {
            var variant = protocol.variants().get(index);
            var result = adapter.predict(variant, evidence);
            TestSupport.assertEquals(AblationPredictionState.RESOLVED, result.state(),
                    "complete evidence resolves every A0-A7 treatment");
            assertions++;
            TestSupport.assertEquals(expected.get(index), result.prediction(),
                    "variant prediction uses only the latest enabled treatment refinement");
            assertions++;
            TestSupport.assertEquals(index, result.usedDimensions().size(),
                    "variant uses exactly its enabled dimension count");
            assertions++;
            TestSupport.assertEquals(1 + index, result.evidenceIds().size(),
                    "variant evidence excludes disabled later dimensions");
            assertions++;
            TestSupport.assertEquals(variant.enabledDimensions(), result.usedDimensions(),
                    "result records exactly the enabled dimensions");
            assertions++;
        }

        var a3 = adapter.predict(protocol.variants().get(3), evidence);
        TestSupport.assertTrue(!a3.evidenceIds().contains("e-role"),
                "A3 cannot see role evidence");
        assertions++;
        TestSupport.assertTrue(!a3.evidenceIds().contains("e-workflow"),
                "A3 cannot see workflow evidence");
        assertions++;
        TestSupport.assertTrue(!a3.evidenceIds().contains("e-semantic"),
                "A3 cannot see semantic evidence");
        assertions++;
        TestSupport.assertTrue(!a3.evidenceIds().contains("e-correlation"),
                "A3 cannot see correlation evidence");
        assertions++;

        AblationCaseEvidence missingRole = evidenceWithout(AblationDimension.ROLE);
        var a4Missing = adapter.predict(protocol.variants().get(4), missingRole);
        TestSupport.assertEquals(AblationPredictionState.INCONCLUSIVE, a4Missing.state(),
                "missing enabled dimension fails closed as inconclusive");
        assertions++;
        TestSupport.assertEquals(null, a4Missing.prediction(),
                "inconclusive treatment carries no binary prediction");
        assertions++;
        TestSupport.assertContains(String.join(",", a4Missing.reasons()), "MISSING_DIMENSION_EVIDENCE:ROLE",
                "missing-dimension reason remains explicit");
        assertions++;
        TestSupport.assertTrue(!a4Missing.evidenceIds().contains("e-workflow"),
                "inconclusive A4 cannot consume later disabled evidence");
        assertions++;

        var repeated = adapter.predict(protocol.variants().get(7), evidence);
        var repeatedAgain = adapter.predict(protocol.variants().get(7), evidence);
        TestSupport.assertEquals(repeated.resultId(), repeatedAgain.resultId(),
                "prediction identity is deterministic");
        assertions++;
        TestSupport.assertEquals(repeated.fingerprint(), repeatedAgain.fingerprint(),
                "prediction fingerprint is deterministic");
        assertions++;

        List<AblationDimensionEvidence> duplicate = new ArrayList<>(evidence.dimensionEvidence());
        duplicate.add(new AblationDimensionEvidence(
                AblationDimension.IDENTITY, ResearchPrediction.POSITIVE, List.of("e-identity-duplicate")));
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new AblationCaseEvidence(
                        "duplicate-dimension",
                        ResearchPrediction.POSITIVE,
                        List.of("e-baseline"),
                        duplicate),
                "duplicate dimension evidence fails closed");
        assertions++;

        for (RecordComponent component : AblationCaseEvidence.class.getRecordComponents()) {
            TestSupport.assertTrue(!component.getName().toLowerCase().contains("groundtruth"),
                    "prediction input schema contains no groundTruth component");
            assertions++;
            TestSupport.assertTrue(!component.getType().getSimpleName().contains("GroundTruth"),
                    "prediction input type cannot expose ResearchGroundTruth");
            assertions++;
        }

        return assertions;
    }

    private static AblationCaseEvidence completeEvidence() {
        return new AblationCaseEvidence(
                "S11-EVAL-001",
                ResearchPrediction.POSITIVE,
                List.of("e-baseline"),
                List.of(
                        dim(AblationDimension.IDENTITY, ResearchPrediction.NEGATIVE, "e-identity"),
                        dim(AblationDimension.OWNERSHIP, ResearchPrediction.POSITIVE, "e-ownership"),
                        dim(AblationDimension.TENANT, ResearchPrediction.NEGATIVE, "e-tenant"),
                        dim(AblationDimension.ROLE, ResearchPrediction.POSITIVE, "e-role"),
                        dim(AblationDimension.WORKFLOW, ResearchPrediction.NEGATIVE, "e-workflow"),
                        dim(AblationDimension.SEMANTIC_EVIDENCE, ResearchPrediction.POSITIVE, "e-semantic"),
                        dim(AblationDimension.EVIDENCE_CORRELATION, ResearchPrediction.NEGATIVE, "e-correlation")));
    }

    private static AblationCaseEvidence evidenceWithout(AblationDimension missing) {
        AblationCaseEvidence complete = completeEvidence();
        return new AblationCaseEvidence(
                complete.caseId(),
                complete.baselinePrediction(),
                complete.baselineEvidenceIds(),
                complete.dimensionEvidence().stream()
                        .filter(value -> value.dimension() != missing)
                        .toList());
    }

    private static AblationDimensionEvidence dim(
            AblationDimension dimension,
            ResearchPrediction prediction,
            String evidenceId) {
        return new AblationDimensionEvidence(dimension, prediction, List.of(evidenceId));
    }
}
