package io.acra.core.engine;

import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.authorization.AuthorizationRule;
import io.acra.core.domain.authorization.AuthorizationRuleEffect;
import io.acra.core.domain.authorization.Delegation;
import io.acra.core.domain.authorization.PolicyGraphIntegrationResult;
import io.acra.core.domain.authorization.PolicyGraphIntegrationState;
import io.acra.core.domain.authorization.RoleAssignment;
import io.acra.core.domain.authorization.RoleInheritance;
import io.acra.core.domain.authorization.RolePermissionAssignment;
import io.acra.core.domain.authorization.TenantMembership;
import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.common.ConfidenceBasis;
import io.acra.core.domain.evidence.Evidence;
import io.acra.core.graph.GraphEdge;
import io.acra.core.graph.GraphNode;
import io.acra.core.graph.NodeType;
import io.acra.core.graph.RelationType;
import io.acra.core.graph.SecurityContextGraph;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class AuthorizationPolicyGraphIntegrator {

    public PolicyGraphIntegrationResult integrate(SecurityContextGraph graph, AuthorizationPolicySnapshot snapshot,
                                                  Map<String, Evidence> evidenceById) {
        if (graph == null) throw new IllegalArgumentException("graph required");
        if (snapshot == null) throw new IllegalArgumentException("snapshot required");
        Map<String, Evidence> evidence = evidenceById == null ? Map.of() : Map.copyOf(evidenceById);
        SecurityContextGraph delta = new SecurityContextGraph();
        List<String> reasons = new ArrayList<>();

        Set<String> referenced = referencedEvidence(snapshot);
        for (String id : referenced) {
            Evidence item = evidence.get(id);
            if (item == null) {
                reasons.add("MISSING_POLICY_EVIDENCE:" + id);
            } else {
                delta.addEvidence(item);
            }
        }
        if (!reasons.isEmpty()) {
            return new PolicyGraphIntegrationResult(PolicyGraphIntegrationState.BLOCKED, 0, 0, 0, reasons);
        }

        GraphNode policy = node("policy:" + snapshot.policyId(), NodeType.POLICY, snapshot.policyId(),
                Map.of("version", snapshot.version(), "fingerprint", snapshot.fingerprint()));
        delta.addNode(policy);

        for (TenantMembership membership : snapshot.memberships()) {
            GraphNode principal = node("principal:" + membership.principalId(), NodeType.PRINCIPAL,
                    membership.principalId(), Map.of());
            GraphNode tenant = node("tenant:" + membership.tenantId(), NodeType.TENANT,
                    membership.tenantId(), Map.of("membershipType", membership.type().name()));
            delta.addNode(principal);
            delta.addNode(tenant);
            addEdge(delta, principal.id(), tenant.id(), RelationType.BELONGS_TO, membership.evidenceIds(),
                    snapshot);
        }

        for (RoleAssignment assignment : snapshot.roleAssignments()) {
            GraphNode principal = node("principal:" + assignment.principalId(), NodeType.PRINCIPAL,
                    assignment.principalId(), Map.of());
            GraphNode role = node("role:" + assignment.roleId(), NodeType.ROLE,
                    assignment.roleId(), Map.of("scope", assignment.scope().type().name()));
            delta.addNode(principal);
            delta.addNode(role);
            addEdge(delta, principal.id(), role.id(), RelationType.ROLE_ASSIGNMENT, assignment.evidenceIds(),
                    snapshot);
            if (!assignment.tenantId().isBlank()) {
                GraphNode tenant = node("tenant:" + assignment.tenantId(), NodeType.TENANT,
                        assignment.tenantId(), Map.of());
                delta.addNode(tenant);
                addEdge(delta, role.id(), tenant.id(), RelationType.SCOPED_TO, assignment.evidenceIds(), snapshot);
            }
        }

        for (RoleInheritance inheritance : snapshot.roleInheritances()) {
            GraphNode child = node("role:" + inheritance.childRoleId(), NodeType.ROLE,
                    inheritance.childRoleId(), Map.of());
            GraphNode parent = node("role:" + inheritance.parentRoleId(), NodeType.ROLE,
                    inheritance.parentRoleId(), Map.of());
            delta.addNode(child);
            delta.addNode(parent);
            addEdge(delta, child.id(), parent.id(), RelationType.INHERITS_ROLE, inheritance.evidenceIds(), snapshot);
        }

        snapshot.permissions().forEach(permission -> delta.addNode(
                node("permission:" + permission.permissionId(), NodeType.PERMISSION, permission.permissionId(),
                        Map.of("action", permission.action(), "scope", permission.scope().type().name()))));

        for (RolePermissionAssignment assignment : snapshot.rolePermissionAssignments()) {
            GraphNode role = node("role:" + assignment.roleId(), NodeType.ROLE, assignment.roleId(), Map.of());
            GraphNode permission = node("permission:" + assignment.permissionId(), NodeType.PERMISSION,
                    assignment.permissionId(), Map.of());
            delta.addNode(role);
            delta.addNode(permission);
            addEdge(delta, role.id(), permission.id(), RelationType.HAS_PERMISSION,
                    assignment.evidenceIds(), snapshot);
        }

        for (AuthorizationRule rule : snapshot.rules()) {
            GraphNode permission = node("permission:" + rule.permissionId(), NodeType.PERMISSION,
                    rule.permissionId(), Map.of());
            delta.addNode(permission);
            addEdge(delta, policy.id(), permission.id(),
                    rule.effect() == AuthorizationRuleEffect.ALLOW ? RelationType.ALLOWS : RelationType.DENIES,
                    rule.evidenceIds(), snapshot);
        }

        for (Delegation delegation : snapshot.delegations()) {
            GraphNode delegate = node("principal:" + delegation.delegatePrincipalId(), NodeType.PRINCIPAL,
                    delegation.delegatePrincipalId(), Map.of());
            GraphNode delegator = node("principal:" + delegation.delegatorPrincipalId(), NodeType.PRINCIPAL,
                    delegation.delegatorPrincipalId(), Map.of());
            GraphNode delegationNode = node("delegation:" + delegation.delegationId(), NodeType.DELEGATION,
                    delegation.delegationId(), Map.of("targetTenant", delegation.targetTenantId()));
            delta.addNode(delegate);
            delta.addNode(delegator);
            delta.addNode(delegationNode);
            addEdge(delta, delegate.id(), delegator.id(), RelationType.DELEGATED_BY,
                    delegation.evidenceIds(), snapshot);
            if (!delegation.targetTenantId().isBlank()) {
                GraphNode tenant = node("tenant:" + delegation.targetTenantId(), NodeType.TENANT,
                        delegation.targetTenantId(), Map.of());
                delta.addNode(tenant);
                addEdge(delta, delegationNode.id(), tenant.id(), RelationType.SCOPED_TO,
                        delegation.evidenceIds(), snapshot);
            }
        }

        if (!preflight(graph, delta, reasons)) {
            return new PolicyGraphIntegrationResult(PolicyGraphIntegrationState.BLOCKED, 0, 0, 0, reasons);
        }

        int evidenceBefore = graph.evidenceCount();
        int nodesBefore = graph.nodeCount();
        int edgesBefore = graph.edgeCount();
        graph.mergeFrom(delta);
        return new PolicyGraphIntegrationResult(PolicyGraphIntegrationState.APPLIED,
                graph.evidenceCount() - evidenceBefore, graph.nodeCount() - nodesBefore,
                graph.edgeCount() - edgesBefore, reasons);
    }

    private Set<String> referencedEvidence(AuthorizationPolicySnapshot snapshot) {
        Set<String> ids = new TreeSet<>();
        snapshot.memberships().forEach(x -> ids.addAll(x.evidenceIds()));
        snapshot.roleAssignments().forEach(x -> ids.addAll(x.evidenceIds()));
        snapshot.roleInheritances().forEach(x -> ids.addAll(x.evidenceIds()));
        snapshot.permissions().forEach(x -> ids.addAll(x.evidenceIds()));
        snapshot.rolePermissionAssignments().forEach(x -> ids.addAll(x.evidenceIds()));
        snapshot.rules().forEach(x -> ids.addAll(x.evidenceIds()));
        snapshot.delegations().forEach(x -> ids.addAll(x.evidenceIds()));
        return ids;
    }

    private boolean preflight(SecurityContextGraph graph, SecurityContextGraph delta, List<String> reasons) {
        Map<String, Evidence> existingEvidence = new HashMap<>();
        graph.evidenceList().forEach(e -> existingEvidence.put(e.evidenceId(), e));
        for (Evidence e : delta.evidenceList()) {
            Evidence existing = existingEvidence.get(e.evidenceId());
            if (existing != null && !existing.equals(e)) reasons.add("EVIDENCE_ID_CONFLICT:" + e.evidenceId());
        }

        Map<String, GraphNode> existingNodes = new HashMap<>();
        graph.nodes().forEach(n -> existingNodes.put(n.id(), n));
        for (GraphNode n : delta.nodes()) {
            GraphNode existing = existingNodes.get(n.id());
            if (existing != null && !existing.equals(n)) reasons.add("GRAPH_NODE_CONFLICT:" + n.id());
        }

        Map<String, GraphEdge> existingEdges = new HashMap<>();
        graph.edges().forEach(e -> existingEdges.put(e.edgeId(), e));
        for (GraphEdge e : delta.edges()) {
            GraphEdge existing = existingEdges.get(e.edgeId());
            if (existing != null && !existing.equals(e)) reasons.add("GRAPH_EDGE_CONFLICT:" + e.edgeId());
        }
        return reasons.isEmpty();
    }

    private GraphNode node(String id, NodeType type, String label, Map<String, String> attributes) {
        return new GraphNode(id, type, label, attributes);
    }

    private void addEdge(SecurityContextGraph graph, String source, String target, RelationType relation,
                         List<String> evidenceIds, AuthorizationPolicySnapshot snapshot) {
        if (evidenceIds == null || evidenceIds.isEmpty()) return;
        graph.addEdge(GraphEdge.create(source, target, relation,
                Confidence.of(ConfidenceBasis.EXPLICIT_METADATA), evidenceIds, snapshot.capturedAt()));
    }
}
