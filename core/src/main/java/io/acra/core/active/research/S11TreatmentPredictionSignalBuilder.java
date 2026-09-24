package io.acra.core.active.research;

import io.acra.core.analysis.PassiveDifferentialComparator;
import io.acra.core.analysis.ResponseComparisonMode;
import io.acra.core.analysis.semantic.ResponseSemanticAnalyzer;
import io.acra.core.domain.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class S11TreatmentPredictionSignalBuilder {
    private final PassiveDifferentialComparator differential = new PassiveDifferentialComparator();
    private final ResponseSemanticAnalyzer semantic = new ResponseSemanticAnalyzer();

    public AblationCaseEvidence build(
            AblationEvidenceBundle bundle,
            HttpResponse left,
            HttpResponse right) {
        if (bundle == null || left == null || right == null) {
            throw new IllegalArgumentException("bundle/response pair required");
        }

        ResearchPrediction baseline = differential.compare(left, right, ResponseComparisonMode.RAW).equivalent()
                ? ResearchPrediction.NEGATIVE : ResearchPrediction.POSITIVE;

        var leftSemantic = semantic.fingerprint(left);
        var rightSemantic = semantic.fingerprint(right);
        ResearchPrediction current = baseline;
        boolean contextualConflict = false;
        List<AblationDimensionEvidence> dimensions = new ArrayList<>();

        current = carry(bundle, AblationDimension.IDENTITY, current, dimensions);

        if (changed(leftSemantic.ownerIds(), rightSemantic.ownerIds())) {
            contextualConflict = true;
            current = ResearchPrediction.POSITIVE;
        }
        current = carry(bundle, AblationDimension.OWNERSHIP, current, dimensions);

        if (changed(leftSemantic.tenantIds(), rightSemantic.tenantIds())) {
            contextualConflict = true;
            current = ResearchPrediction.POSITIVE;
        }
        current = carry(bundle, AblationDimension.TENANT, current, dimensions);

        current = carry(bundle, AblationDimension.ROLE, current, dimensions);
        current = carry(bundle, AblationDimension.WORKFLOW, current, dimensions);

        boolean semanticallyEquivalent =
                differential.compare(left, right, ResponseComparisonMode.SEMANTIC).equivalent();
        current = semanticallyEquivalent && !contextualConflict
                ? ResearchPrediction.NEGATIVE : ResearchPrediction.POSITIVE;
        current = carry(bundle, AblationDimension.SEMANTIC_EVIDENCE, current, dimensions);

        current = carry(bundle, AblationDimension.EVIDENCE_CORRELATION, current, dimensions);

        return new AblationCaseEvidence(
                bundle.caseId(),
                baseline,
                bundle.baselineEvidenceIds(),
                dimensions);
    }

    private static ResearchPrediction carry(
            AblationEvidenceBundle bundle,
            AblationDimension dimension,
            ResearchPrediction prediction,
            List<AblationDimensionEvidence> out) {
        AblationDimensionEvidenceReference evidence = bundle.evidenceFor(dimension);
        if (evidence == null) throw new IllegalArgumentException("missing dimension evidence: " + dimension);
        out.add(new AblationDimensionEvidence(dimension, prediction, evidence.evidenceIds()));
        return prediction;
    }

    private static boolean changed(Set<String> left, Set<String> right) {
        if (left.isEmpty() && right.isEmpty()) return false;
        return !left.equals(right);
    }
}
