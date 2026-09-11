package io.acra.core.active.planning;

import io.acra.core.active.model.ConfigurationSnapshot;
import io.acra.core.active.model.SafetyPolicy;
import io.acra.core.active.model.SelectionMode;
import io.acra.core.active.model.TargetDescriptor;
import io.acra.core.active.model.TestContract;
import io.acra.core.active.model.TestProfileDefinition;
import io.acra.core.domain.authorization.AuthorizationMatrix;
import io.acra.core.domain.endpoint.ApiEndpointRecord;
import io.acra.core.graph.SecurityContextGraph;
import io.acra.core.openapi.OpenApiDocument;
import io.acra.core.recon.ContextCoverage;
import io.acra.core.recon.EndpointRiskAssessment;
import io.acra.core.recon.SecurityContextFingerprint;
import io.acra.core.route.RouteTemplateModel;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public record PlanningInput(
        String planId,
        List<ApiEndpointRecord> apiInventory,
        SecurityContextGraph securityContextGraph,
        AuthorizationMatrix authorizationMatrix,
        List<SecurityContextFingerprint> securityContexts,
        List<RouteTemplateModel> routes,
        OpenApiDocument openApi,
        TargetDescriptor target,
        Set<TestContract> enabledContracts,
        Map<String, EndpointRiskAssessment> riskPriority,
        Map<String, ContextCoverage> contextCoverage,
        int requestBudget,
        int mutationBudget,
        SafetyPolicy safetyPolicy,
        SelectionMode selectionMode,
        TestProfileDefinition profile,
        ConfigurationSnapshot configurationSnapshot,
        List<TestSeed> seeds,
        Instant createdAt) {
    public PlanningInput {
        if (planId == null || planId.isBlank() || securityContextGraph == null || authorizationMatrix == null
                || target == null || safetyPolicy == null || selectionMode == null || profile == null
                || configurationSnapshot == null || createdAt == null) {
            throw new IllegalArgumentException("planning metadata required");
        }
        apiInventory = List.copyOf(apiInventory == null ? List.of() : apiInventory);
        securityContexts = List.copyOf(securityContexts == null ? List.of() : securityContexts);
        routes = List.copyOf(routes == null ? List.of() : routes);
        enabledContracts = Set.copyOf(enabledContracts == null ? Set.of() : enabledContracts);
        riskPriority = immutableSorted(riskPriority);
        contextCoverage = immutableSorted(contextCoverage);
        if (requestBudget < 0 || mutationBudget < 0) throw new IllegalArgumentException("planning budgets");
        seeds = List.copyOf(seeds == null ? List.of() : seeds);
    }

    private static <T> Map<String, T> immutableSorted(Map<String, T> input) {
        TreeMap<String, T> copy = new TreeMap<>();
        if (input != null) copy.putAll(input);
        return Map.copyOf(copy);
    }
}
