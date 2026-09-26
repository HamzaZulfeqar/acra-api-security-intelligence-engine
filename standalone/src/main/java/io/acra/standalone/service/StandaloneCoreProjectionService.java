package io.acra.standalone.service;

import io.acra.core.domain.authorization.Action;
import io.acra.core.domain.authorization.ActionType;
import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationRule;
import io.acra.core.domain.authorization.AuthorizationRuleEffect;
import io.acra.core.domain.authorization.AuthorizationScope;
import io.acra.core.domain.authorization.AuthorizationScopeType;
import io.acra.core.domain.authorization.ContextStatus;
import io.acra.core.domain.authorization.EffectiveAuthorizationRequest;
import io.acra.core.domain.authorization.Permission;
import io.acra.core.domain.authorization.RoleAssignment;
import io.acra.core.domain.authorization.RolePermissionAssignment;
import io.acra.core.domain.authorization.TenantMembership;
import io.acra.core.domain.authorization.TenantMembershipType;
import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.common.ConfidenceBasis;
import io.acra.core.domain.evidence.EvidenceSource;
import io.acra.core.domain.identity.Principal;
import io.acra.core.domain.identity.Role;
import io.acra.core.domain.resource.Resource;
import io.acra.core.domain.tenant.Tenant;
import io.acra.core.engine.BflaAssessmentEvaluator;
import io.acra.core.engine.BolaAssessmentEvaluator;
import io.acra.core.engine.EffectiveAuthorizationResolver;
import io.acra.core.engine.ManualAuthorizationPolicyBuilder;
import io.acra.core.product.authorization.S6AuthorizationWorkspace;
import io.acra.core.product.property.S9PropertyWorkspace;
import io.acra.core.product.routing.S8RoutingWorkspace;
import io.acra.core.product.workflow.S7WorkflowWorkspace;
import io.acra.core.security.TokenFingerprint;
import io.acra.standalone.model.AuthorizationExpectationRecord;
import io.acra.standalone.model.CoreAuthorizationProjectionRecord;
import io.acra.standalone.model.CoreProjectionSnapshot;
import io.acra.standalone.model.PrincipalContextRecord;
import io.acra.standalone.model.ResourceContextRecord;
import io.acra.standalone.model.RoleContextRecord;
import io.acra.standalone.model.TenantContextRecord;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StandaloneCoreProjectionService {
    private final LocalWorkspaceStore workspace;
    private final SecurityContextService contextService;
    private final EffectiveAuthorizationResolver resolver = new EffectiveAuthorizationResolver();
    private final BolaAssessmentEvaluator bolaEvaluator = new BolaAssessmentEvaluator();
    private final BflaAssessmentEvaluator bflaEvaluator = new BflaAssessmentEvaluator();

    public StandaloneCoreProjectionService(LocalWorkspaceStore workspace) {
        this.workspace = java.util.Objects.requireNonNull(workspace);
        this.contextService = new SecurityContextService(workspace);
    }

    public CoreProjectionSnapshot project(UUID projectId) throws IOException {
        // Validate project existence through the authoritative workspace boundary.
        workspace.listInventory(projectId);

        List<PrincipalContextRecord> principals = contextService.principals(projectId);
        List<RoleContextRecord> roles = contextService.roles(projectId);
        List<TenantContextRecord> tenants = contextService.tenants(projectId);
        List<ResourceContextRecord> resources = contextService.resources(projectId);
        List<AuthorizationExpectationRecord> expectations = contextService.expectations(projectId);

        Map<String, PrincipalContextRecord> principalById = indexPrincipals(principals);
        Map<String, RoleContextRecord> roleById = indexRoles(roles);
        Map<String, TenantContextRecord> tenantById = indexTenants(tenants);
        Map<String, ResourceContextRecord> resourceById = indexResources(resources);

        ManualAuthorizationPolicyBuilder builder = new ManualAuthorizationPolicyBuilder("standalone-" + projectId)
                .version("s10-phase4")
                .source("standalone-security-context")
                .defaultDecision(AuthorizationDecision.UNKNOWN);

        Set<String> membershipIds = new HashSet<>();
        Set<String> roleAssignmentIds = new HashSet<>();
        Set<String> permissionIds = new HashSet<>();
        Set<String> rolePermissionIds = new HashSet<>();
        Set<String> ruleIds = new HashSet<>();

        for (AuthorizationExpectationRecord expectation : expectations) {
            String tenantId = expectation.tenantId();
            String roleId = expectation.roleId();
            String resourceId = expectation.resourceId();

            if (!tenantId.isBlank()) {
                String id = "membership-" + shortHash(expectation.principalId() + "|" + tenantId);
                if (membershipIds.add(id)) {
                    builder.membership(new TenantMembership(
                            id,
                            expectation.principalId(),
                            tenantId,
                            TenantMembershipType.DIRECT,
                            true,
                            List.of()));
                }
            }

            if (!roleId.isBlank()) {
                String id = "role-assignment-" + shortHash(expectation.principalId() + "|" + roleId + "|" + tenantId);
                if (roleAssignmentIds.add(id)) {
                    AuthorizationScope roleScope = tenantId.isBlank()
                            ? AuthorizationScope.global()
                            : AuthorizationScope.tenant(tenantId);
                    builder.role(new RoleAssignment(
                            id,
                            expectation.principalId(),
                            roleId,
                            tenantId,
                            roleScope,
                            true,
                            List.of()));
                }
            }

            if (binary(expectation.expectedDecision())) {
                ResourceContextRecord resource = resourceId.isBlank() ? null : resourceById.get(resourceId);
                String resourceType = resource == null ? "" : resource.resourceType();
                AuthorizationScope scope = new AuthorizationScope(
                        AuthorizationScopeType.ENDPOINT,
                        tenantId,
                        resourceId,
                        expectation.endpoint(),
                        expectation.action().name(),
                        "");

                String permissionId = "permission-" + shortHash(expectation.id().toString());
                if (permissionIds.add(permissionId)) {
                    builder.permission(new Permission(
                            permissionId,
                            expectation.action().name(),
                            resourceType,
                            expectation.endpoint(),
                            "",
                            scope,
                            List.of()));
                }

                if (!roleId.isBlank()) {
                    String assignmentId = "role-permission-" + shortHash(roleId + "|" + permissionId + "|" + tenantId);
                    if (rolePermissionIds.add(assignmentId)) {
                        builder.rolePermission(new RolePermissionAssignment(
                                assignmentId,
                                roleId,
                                permissionId,
                                tenantId,
                                List.of()));
                    }
                }

                String ruleId = "rule-" + shortHash(expectation.id().toString());
                if (ruleIds.add(ruleId)) {
                    builder.rule(new AuthorizationRule(
                            ruleId,
                            expectation.expectedDecision() == AuthorizationDecision.ALLOW
                                    ? AuthorizationRuleEffect.ALLOW
                                    : AuthorizationRuleEffect.DENY,
                            expectation.principalId(),
                            roleId,
                            "*",
                            tenantId,
                            scope,
                            null,
                            "",
                            List.of()));
                }
            }
        }

        var policy = builder.build(Instant.now());
        S6AuthorizationWorkspace authorizationWorkspace = new S6AuthorizationWorkspace();
        authorizationWorkspace.loadPolicy(policy);

        List<CoreAuthorizationProjectionRecord> projections = new ArrayList<>();
        for (AuthorizationExpectationRecord expectation : expectations) {
            ResourceContextRecord resourceRecord = expectation.resourceId().isBlank()
                    ? null : resourceById.get(expectation.resourceId());
            String resourceTenant = resourceRecord != null && !resourceRecord.tenantId().isBlank()
                    ? resourceRecord.tenantId()
                    : expectation.tenantId();
            String resourceType = resourceRecord == null ? "" : resourceRecord.resourceType();

            EffectiveAuthorizationRequest request = new EffectiveAuthorizationRequest(
                    expectation.principalId(),
                    expectation.tenantId(),
                    resourceTenant,
                    expectation.resourceId(),
                    resourceType,
                    expectation.endpoint(),
                    "",
                    expectation.action().name(),
                    AuthorizationDecision.UNKNOWN,
                    false,
                    Instant.now());

            var resolution = resolver.resolve(policy, request);
            AuthorizationContext context = authorizationContext(
                    expectation,
                    principalById,
                    roleById,
                    tenantById,
                    resourceById);

            var bola = bolaEvaluator.evaluate(context, "", "", "", projectId.toString());
            var bfla = bflaEvaluator.evaluate(
                    context,
                    "",
                    "",
                    "",
                    projectId.toString(),
                    expectation.endpoint());

            projections.add(new CoreAuthorizationProjectionRecord(
                    expectation.id(),
                    expectation.endpoint(),
                    expectation.action().name(),
                    expectation.principalId(),
                    expectation.expectedDecision(),
                    resolution.expectedDecision(),
                    resolution.state(),
                    resolution.effectiveRoleIds(),
                    resolution.reasons(),
                    bola.status(),
                    bfla.status()));
        }

        projections.sort(java.util.Comparator
                .comparing(CoreAuthorizationProjectionRecord::endpoint)
                .thenComparing(CoreAuthorizationProjectionRecord::action)
                .thenComparing(CoreAuthorizationProjectionRecord::principalId));

        var authSnapshot = authorizationWorkspace.snapshot();
        var workflowSnapshot = new S7WorkflowWorkspace().snapshot();
        var routingSnapshot = new S8RoutingWorkspace().snapshot();
        var propertySnapshot = new S9PropertyWorkspace().snapshot();

        return new CoreProjectionSnapshot(
                projectId,
                authSnapshot.policy().fingerprint(),
                authSnapshot.policy().memberships().size(),
                authSnapshot.policy().roleAssignments().size(),
                authSnapshot.policy().permissions().size(),
                authSnapshot.policy().rules().size(),
                projections,
                true,
                workflowSnapshot.resolutions().size(),
                true,
                routingSnapshot.assessments().size(),
                true,
                propertySnapshot.assessments().size());
    }

    private static AuthorizationContext authorizationContext(
            AuthorizationExpectationRecord expectation,
            Map<String, PrincipalContextRecord> principals,
            Map<String, RoleContextRecord> roles,
            Map<String, TenantContextRecord> tenants,
            Map<String, ResourceContextRecord> resources) {

        Confidence confidence = Confidence.of(ConfidenceBasis.EXPLICIT_METADATA);

        PrincipalContextRecord principalRecord = principals.get(expectation.principalId());
        Principal principal = principalRecord == null ? null : new Principal(
                principalRecord.principalId(),
                principalRecord.displayName(),
                principalRecord.authenticationType(),
                confidence);

        RoleContextRecord roleRecord = expectation.roleId().isBlank() ? null : roles.get(expectation.roleId());
        Role role = roleRecord == null ? null : new Role(
                roleRecord.roleId(),
                roleRecord.name(),
                EvidenceSource.USER_POLICY,
                confidence);

        TenantContextRecord tenantRecord = expectation.tenantId().isBlank() ? null : tenants.get(expectation.tenantId());
        Tenant tenant = tenantRecord == null ? null : new Tenant(
                tenantRecord.tenantId(),
                tenantRecord.name(),
                EvidenceSource.USER_POLICY,
                confidence);

        ResourceContextRecord resourceRecord = expectation.resourceId().isBlank()
                ? null : resources.get(expectation.resourceId());
        Resource resource = resourceRecord == null ? null : new Resource(
                resourceRecord.resourceId(),
                resourceRecord.resourceType(),
                "",
                resourceRecord.ownerPrincipalId(),
                resourceRecord.tenantId(),
                resourceRecord.state(),
                confidence);

        Action action = new Action(
                expectation.action(),
                EvidenceSource.USER_POLICY,
                confidence,
                expectation.action().name());

        ContextStatus status = principal != null && action.actionType() != ActionType.UNKNOWN
                ? ContextStatus.RESOLVED
                : ContextStatus.PARTIAL;

        return new AuthorizationContext(
                principal,
                role,
                tenant,
                resource,
                resourceRecord == null ? "" : resourceRecord.ownerPrincipalId(),
                action,
                null,
                expectation.expectedDecision(),
                AuthorizationDecision.UNKNOWN,
                List.of(),
                status);
    }

    private static Map<String, PrincipalContextRecord> indexPrincipals(List<PrincipalContextRecord> values) {
        Map<String, PrincipalContextRecord> out = new HashMap<>();
        values.forEach(value -> out.put(value.principalId(), value));
        return out;
    }

    private static Map<String, RoleContextRecord> indexRoles(List<RoleContextRecord> values) {
        Map<String, RoleContextRecord> out = new HashMap<>();
        values.forEach(value -> out.put(value.roleId(), value));
        return out;
    }

    private static Map<String, TenantContextRecord> indexTenants(List<TenantContextRecord> values) {
        Map<String, TenantContextRecord> out = new HashMap<>();
        values.forEach(value -> out.put(value.tenantId(), value));
        return out;
    }

    private static Map<String, ResourceContextRecord> indexResources(List<ResourceContextRecord> values) {
        Map<String, ResourceContextRecord> out = new HashMap<>();
        values.forEach(value -> out.put(value.resourceId(), value));
        return out;
    }

    private static boolean binary(AuthorizationDecision decision) {
        return decision == AuthorizationDecision.ALLOW || decision == AuthorizationDecision.DENY;
    }

    private static String shortHash(String value) {
        return TokenFingerprint.sha256(value).substring(0, 20);
    }
}
