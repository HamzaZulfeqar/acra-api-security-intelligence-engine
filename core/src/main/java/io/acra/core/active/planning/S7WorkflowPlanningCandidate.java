package io.acra.core.active.planning;

import io.acra.core.active.model.RequestDefinition;
import io.acra.core.active.model.TestContract;
import io.acra.core.domain.endpoint.Endpoint;
import io.acra.core.domain.resource.Resource;
import io.acra.core.domain.workflow.WorkflowAuthorizationResolution;
import io.acra.core.recon.SecurityContextFingerprint;
import java.util.List;
import java.util.Set;

public record S7WorkflowPlanningCandidate(
        String candidateId,
        Endpoint endpoint,
        RequestDefinition baseline,
        RequestDefinition positiveControl,
        RequestDefinition negativeControl,
        SecurityContextFingerprint sourceContext,
        SecurityContextFingerprint targetContext,
        Resource sourceResource,
        Resource targetResource,
        WorkflowAuthorizationResolution baselineResolution,
        WorkflowAuthorizationResolution targetResolution,
        Set<TestContract> allowedContracts,
        List<String> dependencies,
        List<String> invariants,
        boolean userSelected,
        boolean userExcluded) {

    public S7WorkflowPlanningCandidate {
        if (candidateId == null || candidateId.isBlank()) throw new IllegalArgumentException("candidateId required");
        if (endpoint == null || baseline == null || positiveControl == null || negativeControl == null
                || sourceContext == null || targetContext == null || baselineResolution == null
                || targetResolution == null) {
            throw new IllegalArgumentException("complete S7 workflow planning candidate required");
        }
        allowedContracts = Set.copyOf(allowedContracts == null ? Set.of() : allowedContracts);
        dependencies = List.copyOf(dependencies == null ? List.of() : dependencies);
        invariants = List.copyOf(invariants == null ? List.of() : invariants);
    }
}
