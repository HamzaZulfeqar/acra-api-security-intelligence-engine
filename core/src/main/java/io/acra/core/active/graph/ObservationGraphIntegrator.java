package io.acra.core.active.graph;

import io.acra.core.active.analysis.AuthorizationOutcome;
import io.acra.core.active.evidence.EvidenceChainEntry;
import io.acra.core.active.evidence.EvidenceStage;
import io.acra.core.active.evidence.Observation;
import io.acra.core.active.evidence.ResponseSnapshot;
import io.acra.core.active.execution.TestExecutionResult;
import io.acra.core.active.model.SecurityTest;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.common.ConfidenceBasis;
import io.acra.core.domain.evidence.Evidence;
import io.acra.core.domain.evidence.EvidenceSource;
import io.acra.core.domain.resource.Resource;
import io.acra.core.domain.testing.TestState;
import io.acra.core.graph.GraphEdge;
import io.acra.core.graph.GraphNode;
import io.acra.core.graph.NodeType;
import io.acra.core.graph.RelationType;
import io.acra.core.graph.SecurityContextGraph;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.serialization.DomainSerializer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Maps a completed Sprint 4 observation into the existing SecurityContextGraph.
 * This is an adapter only; it deliberately does not introduce a second graph model.
 */
public final class ObservationGraphIntegrator {
    private static final String UNKNOWN = "UNKNOWN";

    private record VerifiedProvenance(List<EvidenceChainEntry> chain) {}

    private final String projectId;
    private final DomainSerializer serializer = new DomainSerializer();

    public ObservationGraphIntegrator(String projectId) {
        if (projectId == null || projectId.isBlank()) throw new IllegalArgumentException("graph projectId required");
        this.projectId = projectId;
    }

    public GraphHydrationResult hydrate(SecurityContextGraph graph, SecurityTest test, TestExecutionResult result) {
        if (graph == null || test == null || result == null) throw new IllegalArgumentException("graph, test and result required");
        Observation observation = result.observation();
        String observationId = observation == null ? "" : observation.observationId();

        List<String> blocked = verifyProjectAndProvenance(test, result);
        if (!blocked.isEmpty()) {
            return result(GraphHydrationStatus.BLOCKED, result, observationId, List.of(), List.of(), List.of(), blocked);
        }

        List<String> missing = missingContext(test, observation);
        if (!missing.isEmpty()) {
            return result(GraphHydrationStatus.INCONCLUSIVE, result, observationId, List.of(), List.of(), List.of(),
                    List.of("missing required context: " + String.join(", ", missing)));
        }

        VerifiedProvenance provenance = new VerifiedProvenance(result.evidenceChain());
        SecurityContextGraph delta = new SecurityContextGraph();
        List<String> provenanceEvidence = addProvenance(delta, result, observation, provenance);
        LinkedHashSet<String> conflictKinds = new LinkedHashSet<>();

        String endpointId = test.endpoint().endpointId();
        String action = observation.targetContext().action();
        String actionId = "action:" + action.toUpperCase(Locale.ROOT);
        String principalId = "principal:" + observation.targetContext().principal();
        String roleId = "role:" + observation.targetContext().role();
        String principalTenantId = "tenant:" + observation.targetContext().tenant();
        Resource expectedResource = test.targetResource();
        String resourceId = resourceId(expectedResource.resourceType(), expectedResource.resourceId());
        String resourceTenantId = "tenant:" + expectedResource.tenantId();
        String ownerId = "principal:" + expectedResource.ownerPrincipalId();

        addNode(delta, graph, new GraphNode(endpointId, NodeType.ENDPOINT,
                test.endpoint().method() + " " + test.endpoint().routeTemplate(),
                Map.of("routeTemplate", test.endpoint().routeTemplate())));
        addNode(delta, graph, new GraphNode(actionId, NodeType.ACTION, action.toUpperCase(Locale.ROOT), Map.of()));
        addNode(delta, graph, new GraphNode(principalId, NodeType.PRINCIPAL,
                observation.targetContext().principal(), Map.of()));
        addNode(delta, graph, new GraphNode(roleId, NodeType.ROLE, observation.targetContext().role(), Map.of()));
        addNode(delta, graph, new GraphNode(principalTenantId, NodeType.TENANT,
                observation.targetContext().tenant(), Map.of()));
        addNode(delta, graph, new GraphNode(resourceTenantId, NodeType.TENANT,
                expectedResource.tenantId(), Map.of()));
        addNode(delta, graph, resourceNode(expectedResource.resourceType(), expectedResource.resourceId()));
        addNode(delta, graph, new GraphNode(ownerId, NodeType.PRINCIPAL,
                expectedResource.ownerPrincipalId(), Map.of()));

        Confidence observed = Confidence.of(ConfidenceBasis.EXACT_OBSERVED);
        Confidence configured = Confidence.of(ConfidenceBasis.EXPLICIT_METADATA);
        addEdge(delta, endpointId, actionId, RelationType.PERFORMS, observed, provenanceEvidence, observation.createdAt());
        addEdge(delta, principalId, actionId, RelationType.PERFORMS, observed, provenanceEvidence, observation.createdAt());
        addEdge(delta, principalId, roleId, RelationType.HAS_ROLE, configured, provenanceEvidence, observation.createdAt());
        addEdge(delta, principalId, principalTenantId, RelationType.BELONGS_TO, configured, provenanceEvidence, observation.createdAt());
        addEdge(delta, principalId, resourceId, RelationType.ACCESSES, observed, provenanceEvidence, observation.createdAt());
        addEdge(delta, endpointId, resourceId, RelationType.REACHES, observed, provenanceEvidence, observation.createdAt());
        addEdge(delta, resourceId, resourceTenantId, RelationType.IN_TENANT, configured, provenanceEvidence, observation.createdAt());
        addEdge(delta, ownerId, resourceId, RelationType.OWNS, configured, provenanceEvidence, observation.createdAt());

        if (observation.observedDecision() == AuthorizationOutcome.ALLOW
                || observation.observedDecision() == AuthorizationOutcome.PARTIAL) {
            addObservedResponseContext(delta, graph, test, result, observation, principalId, endpointId,
                    expectedResource, provenanceEvidence, conflictKinds);
        }

        List<String> mergeFailure = preflight(graph, delta);
        if (!mergeFailure.isEmpty()) {
            return result(GraphHydrationStatus.BLOCKED, result, observationId, List.of(), List.of(), List.of(), mergeFailure);
        }
        graph.mergeFrom(delta);

        GraphHydrationStatus status = conflictKinds.isEmpty()
                ? GraphHydrationStatus.HYDRATED : GraphHydrationStatus.CONFLICTING_CONTEXT;
        List<String> reasons = conflictKinds.isEmpty()
                ? List.of() : List.of("observed response conflicts with configured context: " + String.join(", ", conflictKinds));
        return result(status, result, observationId,
                delta.nodes().stream().map(GraphNode::id).toList(),
                delta.edges().stream().map(GraphEdge::edgeId).toList(),
                delta.evidenceList().stream().map(Evidence::evidenceId).toList(), reasons);
    }

    private List<String> verifyProjectAndProvenance(SecurityTest test, TestExecutionResult result) {
        List<String> reasons = new ArrayList<>();
        Observation observation = result.observation();
        if (!projectId.equals(test.target().projectId())) reasons.add("cross-project graph hydration rejected");
        if (result.state() != TestState.COMPLETED || observation == null) {
            reasons.add("completed execution observation required");
            return reasons;
        }
        if (!result.testId().equals(test.testId()) || !observation.testId().equals(test.testId())) {
            reasons.add("test provenance mismatch");
        }
        if (!result.executionId().equals(observation.executionFingerprint().executionId())
                || !result.testId().equals(observation.executionFingerprint().testId())) {
            reasons.add("execution fingerprint identity mismatch");
        }
        if (!test.configurationSnapshot().fingerprint()
                .equals(observation.executionFingerprint().configurationFingerprint())) {
            reasons.add("configuration fingerprint mismatch");
        }
        String environment = TokenFingerprint.sha256(serializer.serialize(test.target()));
        if (!environment.equals(observation.executionFingerprint().environmentFingerprint())) {
            reasons.add("environment fingerprint mismatch");
        }
        String responseFingerprint = TokenFingerprint.sha256(List.of(
                        observation.baseline(), observation.positiveControl(), observation.negativeControl(), observation.mutation())
                .stream().map(ResponseSnapshot::fingerprint).reduce("", (left, right) -> left + '\n' + right));
        if (!responseFingerprint.equals(observation.executionFingerprint().responseFingerprint())) {
            reasons.add("response fingerprint mismatch");
        }

        List<EvidenceChainEntry> chain = result.evidenceChain();
        if (chain.isEmpty() || chain.stream().anyMatch(entry -> !entry.executionId().equals(result.executionId())
                || !entry.testId().equals(result.testId()))) {
            reasons.add("evidence chain identity mismatch");
            return reasons;
        }
        Set<String> chainIds = new HashSet<>();
        if (chain.stream().anyMatch(entry -> !chainIds.add(entry.evidenceId()))) reasons.add("duplicate evidence chain identity");

        List<String> preObservationIds = chain.stream().filter(entry -> entry.stage() != EvidenceStage.OBSERVATION)
                .map(EvidenceChainEntry::evidenceId).toList();
        if (!preObservationIds.equals(observation.evidenceIds())) reasons.add("observation evidence references do not match execution chain");

        verifyObject(reasons, chain, EvidenceStage.TEST, result.executionId() + ":test", test);
        verifyResponse(reasons, chain, EvidenceStage.BASELINE_REQUEST, EvidenceStage.BASELINE_RESPONSE, observation.baseline());
        verifyResponse(reasons, chain, EvidenceStage.POSITIVE_CONTROL_REQUEST, EvidenceStage.POSITIVE_CONTROL_RESPONSE,
                observation.positiveControl());
        verifyResponse(reasons, chain, EvidenceStage.NEGATIVE_CONTROL_REQUEST, EvidenceStage.NEGATIVE_CONTROL_RESPONSE,
                observation.negativeControl());
        verifyResponse(reasons, chain, EvidenceStage.MUTATION_REQUEST, EvidenceStage.MUTATION_RESPONSE, observation.mutation());
        verifyObject(reasons, chain, EvidenceStage.DIFFERENTIAL, result.executionId() + ":differential", observation.differences());
        verifyObject(reasons, chain, EvidenceStage.OBSERVATION, observation.observationId(), observation);
        return reasons;
    }

    private void verifyResponse(List<String> reasons, List<EvidenceChainEntry> chain, EvidenceStage requestStage,
                                EvidenceStage responseStage, ResponseSnapshot response) {
        boolean requestPresent = chain.stream().anyMatch(entry -> entry.stage() == requestStage
                && entry.objectId().equals(response.requestCorrelation()));
        if (!requestPresent) reasons.add(requestStage + " correlation is absent from evidence chain");
        verifyObject(reasons, chain, responseStage, response.responseId(), response);
    }

    private void verifyObject(List<String> reasons, List<EvidenceChainEntry> chain, EvidenceStage stage,
                              String objectId, Object value) {
        String fingerprint = TokenFingerprint.sha256(serializer.serialize(value));
        long matches = chain.stream().filter(entry -> entry.stage() == stage
                && entry.objectId().equals(objectId) && entry.fingerprint().equals(fingerprint)).count();
        if (matches != 1) reasons.add(stage + " provenance is absent, duplicated or forged");
    }

    private static List<String> missingContext(SecurityTest test, Observation observation) {
        List<String> missing = new ArrayList<>();
        if (!known(observation.targetContext().principal())) missing.add("identity");
        if (!known(observation.targetContext().role())) missing.add("role");
        if (!known(observation.targetContext().tenant())) missing.add("tenant");
        if (!known(observation.targetContext().resource()) || test.targetResource() == null
                || !known(test.targetResource().resourceId()) || !known(test.targetResource().resourceType())) {
            missing.add("resource");
        }
        if (!known(observation.targetContext().owner()) || test.targetResource() == null
                || !known(test.targetResource().ownerPrincipalId())) missing.add("owner");
        if (test.targetResource() == null || !known(test.targetResource().tenantId())) missing.add("resource tenant");
        if (!known(observation.targetContext().action())) missing.add("action");
        if (!known(observation.targetContext().uriClass())) missing.add("endpoint/route");
        if (observation.expectedDecision().conflict()
                || observation.expectedDecision().decision() == AuthorizationDecision.UNKNOWN
                || observation.expectedDecision().decision() == AuthorizationDecision.AMBIGUOUS) missing.add("policy");
        return List.copyOf(new LinkedHashSet<>(missing));
    }

    private List<String> addProvenance(SecurityContextGraph delta, TestExecutionResult result,
                                       Observation observation, VerifiedProvenance provenance) {
        List<String> ids = new ArrayList<>();
        for (EvidenceChainEntry entry : provenance.chain()) {
            EvidenceSource source = switch (entry.stage()) {
                case TEST -> EvidenceSource.CONFIGURATION;
                case BASELINE_REQUEST, POSITIVE_CONTROL_REQUEST, NEGATIVE_CONTROL_REQUEST, MUTATION_REQUEST ->
                        EvidenceSource.TRANSACTION;
                default -> EvidenceSource.RESPONSE;
            };
            String value = "testId=" + result.testId()
                    + ";observationId=" + observation.observationId()
                    + ";chainEvidenceId=" + entry.evidenceId()
                    + ";objectId=" + entry.objectId()
                    + ";fingerprint=" + entry.fingerprint();
            Evidence graphEvidence = Evidence.create(source, result.executionId(), "s4/" + entry.stage().name(), value,
                    "s4-active-observation-provenance", Confidence.of(ConfidenceBasis.EXACT_OBSERVED), entry.timestamp());
            delta.addEvidence(graphEvidence);
            ids.add(graphEvidence.evidenceId());
        }
        return List.copyOf(ids);
    }

    private void addObservedResponseContext(SecurityContextGraph delta, SecurityContextGraph graph, SecurityTest test,
                                            TestExecutionResult result, Observation observation, String principalId,
                                            String endpointId, Resource expectedResource, List<String> evidenceIds,
                                            Set<String> conflictKinds) {
        Set<String> observedResources = observation.mutation().semanticFingerprint().resourceIds();
        Set<String> observedOwners = observation.mutation().semanticFingerprint().ownerIds();
        Set<String> observedTenants = observation.mutation().semanticFingerprint().tenantIds();
        Confidence observed = Confidence.of(ConfidenceBasis.EXACT_OBSERVED);

        Set<String> resourceValues = observedResources.isEmpty()
                ? Set.of(expectedResource.resourceId()) : observedResources;
        for (String observedResource : resourceValues) {
            String observedResourceId = resourceId(expectedResource.resourceType(), observedResource);
            addNode(delta, graph, resourceNode(expectedResource.resourceType(), observedResource));
            addEdge(delta, principalId, observedResourceId, RelationType.ACCESSES, observed, evidenceIds, observation.createdAt());
            addEdge(delta, endpointId, observedResourceId, RelationType.REACHES, observed, evidenceIds, observation.createdAt());
            for (String tenant : observedTenants) {
                String tenantId = "tenant:" + tenant;
                addNode(delta, graph, new GraphNode(tenantId, NodeType.TENANT, tenant, Map.of()));
                addEdge(delta, observedResourceId, tenantId, RelationType.IN_TENANT, observed, evidenceIds, observation.createdAt());
            }
            for (String owner : observedOwners) {
                String observedOwnerId = "principal:" + owner;
                addNode(delta, graph, new GraphNode(observedOwnerId, NodeType.PRINCIPAL, owner, Map.of()));
                addEdge(delta, observedOwnerId, observedResourceId, RelationType.OWNS, observed, evidenceIds, observation.createdAt());
            }
        }

        if (!observedResources.isEmpty() && !observedResources.contains(expectedResource.resourceId())) {
            conflictKinds.add("resource");
            addConflict(delta, graph, result, observation, "resource",
                    resourceId(expectedResource.resourceType(), expectedResource.resourceId()),
                    resourceId(expectedResource.resourceType(), observedResources.iterator().next()), evidenceIds);
        }
        if (!observedOwners.isEmpty() && !observedOwners.contains(expectedResource.ownerPrincipalId())) {
            conflictKinds.add("owner");
            addConflict(delta, graph, result, observation, "owner", "principal:" + expectedResource.ownerPrincipalId(),
                    "principal:" + observedOwners.iterator().next(), evidenceIds);
        }
        if (!observedTenants.isEmpty() && !observedTenants.contains(expectedResource.tenantId())) {
            conflictKinds.add("tenant");
            addConflict(delta, graph, result, observation, "tenant", "tenant:" + expectedResource.tenantId(),
                    "tenant:" + observedTenants.iterator().next(), evidenceIds);
        }
    }

    private static void addConflict(SecurityContextGraph delta, SecurityContextGraph graph, TestExecutionResult result,
                                    Observation observation, String kind, String configuredId, String observedId,
                                    List<String> evidenceIds) {
        String conflictId = "conflict:s4:" + result.executionId() + ':' + kind;
        addNode(delta, graph, new GraphNode(conflictId, NodeType.CONFLICT, "CONFLICTING_EVIDENCE", Map.of(
                "kind", kind,
                "state", "CONFLICTING_EVIDENCE",
                "executionId", result.executionId(),
                "testId", result.testId(),
                "observationId", observation.observationId())));
        addEdge(delta, conflictId, configuredId, RelationType.CONFLICTS_WITH,
                Confidence.of(ConfidenceBasis.EXPLICIT_METADATA), evidenceIds, observation.createdAt());
        addEdge(delta, conflictId, observedId, RelationType.CONFLICTS_WITH,
                Confidence.of(ConfidenceBasis.EXACT_OBSERVED), evidenceIds, observation.createdAt());
    }

    private static void addNode(SecurityContextGraph delta, SecurityContextGraph existing, GraphNode desired) {
        GraphNode node = existing.node(desired.id()).orElse(null);
        if (node == null || node.type() != desired.type() || !node.label().equals(desired.label())) {
            delta.addNode(desired);
            return;
        }
        delta.addNode(node);
    }

    private static void addEdge(SecurityContextGraph graph, String source, String target, RelationType relation,
                                Confidence confidence, List<String> evidenceIds, java.time.Instant createdAt) {
        graph.addEdge(GraphEdge.create(source, target, relation, confidence, evidenceIds, createdAt));
    }

    private static GraphNode resourceNode(String type, String id) {
        return new GraphNode(resourceId(type, id), NodeType.RESOURCE, type + ':' + id, Map.of("resourceType", type));
    }

    private static String resourceId(String type, String id) {
        return "resource:" + type + ':' + id;
    }

    private static List<String> preflight(SecurityContextGraph graph, SecurityContextGraph delta) {
        List<String> reasons = new ArrayList<>();
        Map<String, Evidence> evidence = new HashMap<>();
        graph.evidenceList().forEach(value -> evidence.put(value.evidenceId(), value));
        for (Evidence value : delta.evidenceList()) {
            Evidence prior = evidence.get(value.evidenceId());
            if (prior != null && !prior.equals(value)) reasons.add("conflicting graph evidence identity: " + value.evidenceId());
        }
        Map<String, GraphNode> nodes = new HashMap<>();
        graph.nodes().forEach(value -> nodes.put(value.id(), value));
        for (GraphNode value : delta.nodes()) {
            GraphNode prior = nodes.get(value.id());
            if (prior != null && !prior.equals(value)) reasons.add("conflicting graph node identity: " + value.id());
        }
        Map<String, GraphEdge> edges = new HashMap<>();
        graph.edges().forEach(value -> edges.put(value.edgeId(), value));
        for (GraphEdge value : delta.edges()) {
            GraphEdge prior = edges.get(value.edgeId());
            if (prior != null && !prior.equals(value)) reasons.add("conflicting graph edge identity: " + value.edgeId());
        }
        return List.copyOf(reasons);
    }

    private static boolean known(String value) {
        return value != null && !value.isBlank() && !UNKNOWN.equalsIgnoreCase(value);
    }

    private static GraphHydrationResult result(GraphHydrationStatus status, TestExecutionResult execution,
                                                String observationId, List<String> nodes, List<String> edges,
                                                List<String> evidence, List<String> reasons) {
        return new GraphHydrationResult(status, execution.executionId(), execution.testId(), observationId,
                nodes, edges, evidence, reasons);
    }
}
