package io.acra.core.active.model;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.common.Validation;
import io.acra.core.domain.endpoint.Endpoint;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.resource.Resource;
import io.acra.core.recon.SecurityContextFingerprint;
import java.util.List;

public record SecurityTest(
        String testId,
        String testVersion,
        TestContract category,
        HttpProtocol protocol,
        TargetDescriptor target,
        Endpoint endpoint,
        HttpMethod method,
        RequestDefinition baselineDefinition,
        RequestDefinition positiveControl,
        RequestDefinition negativeControl,
        Mutation mutation,
        SecurityContextFingerprint sourceContext,
        SecurityContextFingerprint targetContext,
        Resource sourceResource,
        Resource targetResource,
        AuthorizationDecision expectedDecision,
        List<String> expectedEvidence,
        SafetyPolicy safetyPolicy,
        int priority,
        String selectionReason,
        ConfigurationSnapshot configurationSnapshot,
        List<String> dependencies,
        ReproducibilityMetadata reproducibilityMetadata,
        int estimatedRequestCost,
        List<String> invariants) {
    public SecurityTest {
        testId = Validation.requireNonBlank(testId, "testId");
        testVersion = Validation.requireNonBlank(testVersion, "testVersion");
        if (category == null || protocol == null || target == null || endpoint == null || method == null) {
            throw new IllegalArgumentException("test category, protocol, target, endpoint and method are required");
        }
        if (endpoint.method() != method) throw new IllegalArgumentException("endpoint method mismatch");
        if (baselineDefinition == null || positiveControl == null || negativeControl == null || mutation == null) {
            throw new IllegalArgumentException("baseline and controls and mutation are required");
        }
        if (sourceContext == null || targetContext == null) throw new IllegalArgumentException("source and target context required");
        if (expectedDecision == null) expectedDecision = AuthorizationDecision.UNKNOWN;
        expectedEvidence = List.copyOf(expectedEvidence == null ? List.of() : expectedEvidence);
        if (safetyPolicy == null || configurationSnapshot == null || reproducibilityMetadata == null) {
            throw new IllegalArgumentException("safety, configuration and reproducibility metadata required");
        }
        if (priority < 0 || priority > 100 || estimatedRequestCost < 1) throw new IllegalArgumentException("priority/request cost");
        selectionReason = Validation.requireNonBlank(selectionReason, "selectionReason");
        dependencies = List.copyOf(dependencies == null ? List.of() : dependencies);
        invariants = List.copyOf(invariants == null ? List.of() : invariants);
    }

    public String signature() {
        return TestSignature.from(this);
    }
}
