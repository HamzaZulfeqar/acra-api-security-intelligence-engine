package io.acra.core.tests.sprint11;

import io.acra.core.active.research.AblationDimension;
import io.acra.core.active.research.AblationExecutionState;
import io.acra.core.active.research.AblationVariant;
import io.acra.core.active.research.ResearchMetricName;
import io.acra.core.active.research.S11AblationProtocol;
import io.acra.core.tests.TestSupport;
import java.util.List;

public final class Sprint11ResearchAblationFoundationTestSuite {
    private Sprint11ResearchAblationFoundationTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT11_RESEARCH_ABLATION_FOUNDATION PASS assertions=" + assertions);
    }

    public static int run() {
        S11AblationProtocol protocol = S11AblationProtocol.canonical();
        int assertions = 0;

        TestSupport.assertEquals("s11-ablation-protocol-v1", protocol.protocolVersion(),
                "Sprint 11 protocol version is explicit");
        assertions++;
        TestSupport.assertEquals(AblationExecutionState.NOT_RUN, protocol.executionState(),
                "defining the ablation protocol cannot claim experiment execution");
        assertions++;
        TestSupport.assertEquals(8, protocol.variants().size(),
                "A0 through A7 are all registered");
        assertions++;
        TestSupport.assertTrue(protocol.scope().contains("controlled registered ground truth only"),
                "protocol scope is restricted to controlled registered ground truth");
        assertions++;

        for (int index = 0; index < protocol.variants().size(); index++) {
            AblationVariant variant = protocol.variants().get(index);
            TestSupport.assertEquals("A" + index, variant.variantId(),
                    "ablation variant identifier follows methodology order");
            assertions++;
            TestSupport.assertEquals(index, variant.order(),
                    "ablation variant order is deterministic");
            assertions++;
            TestSupport.assertEquals(index, variant.enabledDimensions().size(),
                    "each ablation step adds exactly one contextual dimension");
            assertions++;
            if (index > 0) {
                List<AblationDimension> previous = protocol.variants().get(index - 1).enabledDimensions();
                TestSupport.assertEquals(previous,
                        variant.enabledDimensions().subList(0, previous.size()),
                        "ablation dimensions are cumulative");
                assertions++;
            }
        }

        TestSupport.assertTrue(protocol.variants().getFirst().enabledDimensions().isEmpty(),
                "A0 is the naive differential baseline");
        assertions++;
        TestSupport.assertEquals(List.of(AblationDimension.values()),
                protocol.variants().getLast().enabledDimensions(),
                "A7 enables the full registered correlation stack");
        assertions++;
        TestSupport.assertEquals(List.of(
                        ResearchMetricName.TRUE_POSITIVE,
                        ResearchMetricName.TRUE_NEGATIVE,
                        ResearchMetricName.FALSE_POSITIVE,
                        ResearchMetricName.FALSE_NEGATIVE,
                        ResearchMetricName.PRECISION,
                        ResearchMetricName.RECALL,
                        ResearchMetricName.F1,
                        ResearchMetricName.EVIDENCE_COMPLETENESS),
                protocol.requiredMetrics(),
                "required research metrics match the registered methodology");
        assertions++;

        S11AblationProtocol repeated = S11AblationProtocol.canonical();
        TestSupport.assertEquals(protocol.protocolId(), repeated.protocolId(),
                "protocol identity is deterministic");
        assertions++;
        TestSupport.assertEquals(protocol.fingerprint(), repeated.fingerprint(),
                "protocol fingerprint is deterministic");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new S11AblationProtocol(
                        "bad-order",
                        "s11-ablation-protocol-v1",
                        AblationExecutionState.NOT_RUN,
                        List.of(
                                protocol.variants().get(1),
                                protocol.variants().get(0),
                                protocol.variants().get(2),
                                protocol.variants().get(3),
                                protocol.variants().get(4),
                                protocol.variants().get(5),
                                protocol.variants().get(6),
                                protocol.variants().get(7)),
                        protocol.requiredMetrics(),
                        protocol.scope(),
                        ""),
                "out-of-order ablation protocol fails closed");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new S11AblationProtocol(
                        "missing-metric",
                        "s11-ablation-protocol-v1",
                        AblationExecutionState.NOT_RUN,
                        protocol.variants(),
                        protocol.requiredMetrics().subList(0, 7),
                        protocol.scope(),
                        ""),
                "incomplete metric contract fails closed");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new S11AblationProtocol(
                        "tampered-fingerprint",
                        "s11-ablation-protocol-v1",
                        AblationExecutionState.NOT_RUN,
                        protocol.variants(),
                        protocol.requiredMetrics(),
                        protocol.scope(),
                        "not-the-protocol-fingerprint"),
                "tampered protocol fingerprint fails closed");
        assertions++;

        return assertions;
    }
}
