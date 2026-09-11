package io.acra.core.active.planning;

import io.acra.core.active.model.Mutation;
import io.acra.core.active.model.RequestDefinition;
import io.acra.core.active.model.TestContract;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.common.Validation;
import io.acra.core.domain.endpoint.Endpoint;
import io.acra.core.domain.resource.Resource;
import io.acra.core.recon.SecurityContextFingerprint;
import java.util.List;

public record TestSeed(
        String testId,
        String testVersion,
        TestContract contract,
        Endpoint endpoint,
        RequestDefinition baseline,
        RequestDefinition positiveControl,
        RequestDefinition negativeControl,
        Mutation mutation,
        SecurityContextFingerprint sourceContext,
        SecurityContextFingerprint targetContext,
        Resource sourceResource,
        Resource targetResource,
        AuthorizationDecision expectedDecision,
        List<String> expectedEvidence,
        List<String> dependencies,
        List<String> invariants,
        int estimatedRequestCost,
        boolean recommended,
        boolean userSelected,
        boolean userExcluded,
        String selectionReason) {
    public TestSeed {
        testId = Validation.requireNonBlank(testId, "testId");
        testVersion = Validation.requireNonBlank(testVersion, "testVersion");
        if (contract == null || endpoint == null || baseline == null || positiveControl == null
                || negativeControl == null || mutation == null || sourceContext == null || targetContext == null) {
            throw new IllegalArgumentException("complete test seed required");
        }
        if (expectedDecision == null) expectedDecision = AuthorizationDecision.UNKNOWN;
        expectedEvidence = List.copyOf(expectedEvidence == null ? List.of() : expectedEvidence);
        dependencies = List.copyOf(dependencies == null ? List.of() : dependencies);
        invariants = List.copyOf(invariants == null ? List.of() : invariants);
        if (estimatedRequestCost < 1) throw new IllegalArgumentException("estimatedRequestCost");
        selectionReason = Validation.requireNonBlank(selectionReason, "selectionReason");
    }
}
