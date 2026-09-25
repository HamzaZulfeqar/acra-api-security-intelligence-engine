package io.acra.core.tests.sprint6;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.authorization.AuthorizationScope;
import io.acra.core.domain.authorization.Permission;
import io.acra.core.domain.authorization.PolicyGraphIntegrationState;
import io.acra.core.domain.authorization.RoleAssignment;
import io.acra.core.domain.authorization.RoleInheritance;
import io.acra.core.domain.authorization.RolePermissionAssignment;
import io.acra.core.domain.authorization.TenantMembership;
import io.acra.core.domain.authorization.TenantMembershipType;
import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.common.ConfidenceBasis;
import io.acra.core.domain.evidence.Evidence;
import io.acra.core.domain.evidence.EvidenceSource;
import io.acra.core.domain.finding.AuthorizationRootCauseCluster;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.engine.AuthorizationPolicyGraphIntegrator;
import io.acra.core.engine.AuthorizationRootCauseGrouper;
import io.acra.core.graph.RelationType;
import io.acra.core.graph.SecurityContextGraph;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Sprint6GraphAndGroupingTestSuite {
    private static final Instant NOW = Instant.parse("2026-09-23T14:45:00Z");

    private Sprint6GraphAndGroupingTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT6_GRAPH_GROUPING PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        SecurityContextGraph graph = new SecurityContextGraph();
        AuthorizationPolicyGraphIntegrator integrator = new AuthorizationPolicyGraphIntegrator();
        AuthorizationPolicySnapshot snapshot = policy();

        Evidence e1 = evidence("e1");
        Evidence e2 = evidence("e2");
        Evidence e3 = evidence("e3");
        Evidence e4 = evidence("e4");
        Evidence e5 = evidence("e5");
        var applied = integrator.integrate(graph, snapshot, Map.of(
                "e1", e1, "e2", e2, "e3", e3, "e4", e4, "e5", e5));
        TestSupport.assertEquals(PolicyGraphIntegrationState.APPLIED, applied.state(),
                "complete policy evidence should hydrate the existing graph");
        assertions++;
        TestSupport.assertTrue(graph.edges().stream().anyMatch(e -> e.relation() == RelationType.ROLE_ASSIGNMENT),
                "role assignment relationship should be present");
        assertions++;
        TestSupport.assertTrue(graph.edges().stream().anyMatch(e -> e.relation() == RelationType.INHERITS_ROLE),
                "role inheritance relationship should be present");
        assertions++;
        TestSupport.assertTrue(graph.edges().stream().anyMatch(e -> e.relation() == RelationType.HAS_PERMISSION),
                "role-permission relationship should be present");
        assertions++;

        int nodes = graph.nodeCount();
        int edges = graph.edgeCount();
        var replay = integrator.integrate(graph, snapshot, Map.of(
                "e1", e1, "e2", e2, "e3", e3, "e4", e4, "e5", e5));
        TestSupport.assertEquals(PolicyGraphIntegrationState.APPLIED, replay.state(),
                "identical graph replay should remain idempotent");
        assertions++;
        TestSupport.assertEquals(nodes, graph.nodeCount(), "replay should not duplicate nodes");
        assertions++;
        TestSupport.assertEquals(edges, graph.edgeCount(), "replay should not duplicate edges");
        assertions++;

        SecurityContextGraph blockedGraph = new SecurityContextGraph();
        var blocked = integrator.integrate(blockedGraph, snapshot, Map.of("e1", e1));
        TestSupport.assertEquals(PolicyGraphIntegrationState.BLOCKED, blocked.state(),
                "missing referenced evidence must fail closed");
        assertions++;
        TestSupport.assertEquals(0, blockedGraph.nodeCount(), "blocked preflight must remain atomic");
        assertions++;

        FindingCandidate first = candidate("c1", "/a", "r1");
        FindingCandidate second = candidate("c2", "/b", "r2");
        List<AuthorizationRootCauseCluster> clusters = new AuthorizationRootCauseGrouper().group(List.of(first, second));
        TestSupport.assertEquals(1, clusters.size(), "same policy/dimension cause should group");
        assertions++;
        TestSupport.assertEquals(2, clusters.get(0).candidateIds().size(), "cluster should retain both candidates");
        assertions++;
        return assertions;
    }

    private static AuthorizationPolicySnapshot policy() {
        return AuthorizationPolicySnapshot.create("policy-graph", "1", "lab",
                List.of(new TenantMembership("tm1", "user-a", "tenant-a", TenantMembershipType.DIRECT, true, List.of("e1"))),
                List.of(new RoleAssignment("ra1", "user-a", "manager", "tenant-a",
                        AuthorizationScope.tenant("tenant-a"), true, List.of("e2"))),
                List.of(new RoleInheritance("rh1", "manager", "viewer", "tenant-a", List.of("e3"))),
                List.of(new Permission("perm1", "READ", "document", "", "",
                        AuthorizationScope.tenant("tenant-a"), List.of("e4"))),
                List.of(new RolePermissionAssignment("rpa1", "viewer", "perm1", "tenant-a", List.of("e5"))),
                List.of(), List.of(), List.of("e1", "e2", "e3", "e4", "e5"), AuthorizationDecision.DENY, NOW);
    }

    private static Evidence evidence(String id) {
        return new Evidence(id, EvidenceSource.USER_POLICY, "s6-policy", "policy", id,
                "s6-explicit-policy", Confidence.of(ConfidenceBasis.EXPLICIT_METADATA), NOW);
    }

    private static FindingCandidate candidate(String id, String endpoint, String resource) {
        return new FindingCandidate(id, FindingCandidateState.CANDIDATE, "project", List.of("test"),
                List.of("exec"), List.of("obs"), List.of("assessment"), List.of("RBAC"),
                endpoint, resource, "user-a", "CROSS_TENANT", AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW, List.of("e1"), List.of(), List.of("policy-fp"),
                "HIGH", "candidate", FindingFingerprint.of(endpoint, resource, "user-a",
                        "CROSS_TENANT", "RBAC", "POLICY_MISMATCH"));
    }
}
