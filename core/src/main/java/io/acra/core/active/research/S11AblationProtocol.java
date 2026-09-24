package io.acra.core.active.research;

import io.acra.core.domain.common.Validation;
import io.acra.core.security.TokenFingerprint;
import java.util.ArrayList;
import java.util.List;

public record S11AblationProtocol(
        String protocolId,
        String protocolVersion,
        AblationExecutionState executionState,
        List<AblationVariant> variants,
        List<ResearchMetricName> requiredMetrics,
        String scope,
        String fingerprint) {

    private static final List<ResearchMetricName> REQUIRED_METRICS = List.of(
            ResearchMetricName.TRUE_POSITIVE,
            ResearchMetricName.TRUE_NEGATIVE,
            ResearchMetricName.FALSE_POSITIVE,
            ResearchMetricName.FALSE_NEGATIVE,
            ResearchMetricName.PRECISION,
            ResearchMetricName.RECALL,
            ResearchMetricName.F1,
            ResearchMetricName.EVIDENCE_COMPLETENESS);

    public S11AblationProtocol {
        protocolId = Validation.requireNonBlank(protocolId, "protocolId");
        protocolVersion = Validation.requireNonBlank(protocolVersion, "protocolVersion");
        executionState = executionState == null ? AblationExecutionState.NOT_RUN : executionState;
        variants = List.copyOf(variants == null ? List.of() : variants);
        requiredMetrics = List.copyOf(requiredMetrics == null ? List.of() : requiredMetrics);
        scope = Validation.requireNonBlank(scope, "scope");
        validateVariants(variants);
        if (!requiredMetrics.equals(REQUIRED_METRICS)) {
            throw new IllegalArgumentException("Sprint 11 metric contract mismatch");
        }
        String calculated = TokenFingerprint.sha256(canonical(
                protocolVersion, executionState, variants, requiredMetrics, scope));
        fingerprint = fingerprint == null || fingerprint.isBlank() ? calculated : fingerprint;
        if (!fingerprint.equals(calculated)) {
            throw new IllegalArgumentException("ablation protocol fingerprint mismatch");
        }
    }

    public static S11AblationProtocol canonical() {
        List<AblationVariant> variants = canonicalVariants();
        String version = "s11-ablation-protocol-v1";
        String scope = "controlled registered ground truth only; no external-target execution";
        String fingerprint = TokenFingerprint.sha256(canonical(
                version, AblationExecutionState.NOT_RUN, variants, REQUIRED_METRICS, scope));
        return new S11AblationProtocol(
                "s11-ablation-" + fingerprint.substring(0, 24),
                version,
                AblationExecutionState.NOT_RUN,
                variants,
                REQUIRED_METRICS,
                scope,
                fingerprint);
    }

    private static List<AblationVariant> canonicalVariants() {
        List<AblationVariant> values = new ArrayList<>();
        List<AblationDimension> cumulative = new ArrayList<>();
        values.add(variant("A0", 0, cumulative, "Naive differential baseline"));

        cumulative.add(AblationDimension.IDENTITY);
        values.add(variant("A1", 1, cumulative, "A0 plus identity context"));

        cumulative.add(AblationDimension.OWNERSHIP);
        values.add(variant("A2", 2, cumulative, "A1 plus resource ownership"));

        cumulative.add(AblationDimension.TENANT);
        values.add(variant("A3", 3, cumulative, "A2 plus tenant context"));

        cumulative.add(AblationDimension.ROLE);
        values.add(variant("A4", 4, cumulative, "A3 plus role context"));

        cumulative.add(AblationDimension.WORKFLOW);
        values.add(variant("A5", 5, cumulative, "A4 plus workflow context"));

        cumulative.add(AblationDimension.SEMANTIC_EVIDENCE);
        values.add(variant("A6", 6, cumulative, "A5 plus semantic response evidence"));

        cumulative.add(AblationDimension.EVIDENCE_CORRELATION);
        values.add(variant("A7", 7, cumulative, "Full ACRA evidence correlation"));
        return List.copyOf(values);
    }

    private static AblationVariant variant(
            String id, int order, List<AblationDimension> dimensions, String description) {
        return new AblationVariant(id, order, List.copyOf(dimensions), description);
    }

    private static void validateVariants(List<AblationVariant> variants) {
        if (variants.size() != 8) {
            throw new IllegalArgumentException("Sprint 11 requires exactly A0 through A7");
        }
        List<AblationDimension> previous = List.of();
        for (int index = 0; index < variants.size(); index++) {
            AblationVariant variant = variants.get(index);
            String expectedId = "A" + index;
            if (!expectedId.equals(variant.variantId()) || variant.order() != index) {
                throw new IllegalArgumentException("ablation variants must be ordered A0 through A7");
            }
            if (variant.enabledDimensions().size() != index) {
                throw new IllegalArgumentException("ablation variant dimension count mismatch");
            }
            if (!variant.enabledDimensions().subList(0, previous.size()).equals(previous)) {
                throw new IllegalArgumentException("ablation variants must be cumulative");
            }
            previous = variant.enabledDimensions();
        }
        if (!variants.getFirst().enabledDimensions().isEmpty()) {
            throw new IllegalArgumentException("A0 must be the naive baseline");
        }
        if (!variants.getLast().enabledDimensions().equals(List.of(AblationDimension.values()))) {
            throw new IllegalArgumentException("A7 must enable the full registered correlation stack");
        }
    }

    private static String canonical(
            String version,
            AblationExecutionState state,
            List<AblationVariant> variants,
            List<ResearchMetricName> metrics,
            String scope) {
        StringBuilder out = new StringBuilder();
        out.append(version).append('|').append(state.name()).append('|').append(scope).append('\n');
        for (AblationVariant variant : variants) {
            out.append(variant.variantId()).append('|')
                    .append(variant.order()).append('|')
                    .append(variant.enabledDimensions()).append('|')
                    .append(variant.description()).append('\n');
        }
        out.append(metrics);
        return out.toString();
    }
}
