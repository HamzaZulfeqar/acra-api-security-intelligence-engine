package io.acra.core.tests.sprint6;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.authorization.AuthorizationRule;
import io.acra.core.domain.authorization.AuthorizationRuleEffect;
import io.acra.core.domain.authorization.AuthorizationScope;
import io.acra.core.domain.authorization.Delegation;
import io.acra.core.domain.authorization.EffectiveAuthorizationRequest;
import io.acra.core.domain.authorization.EffectiveAuthorizationResolution;
import io.acra.core.domain.authorization.Permission;
import io.acra.core.domain.authorization.PolicyResolutionState;
import io.acra.core.domain.authorization.RbacAssessmentState;
import io.acra.core.domain.authorization.RoleAssignment;
import io.acra.core.domain.authorization.RoleInheritance;
import io.acra.core.domain.authorization.RolePermissionAssignment;
import io.acra.core.domain.authorization.TenantIsolationAssessmentState;
import io.acra.core.domain.authorization.TenantMembership;
import io.acra.core.domain.authorization.TenantMembershipType;
import io.acra.core.domain.authorization.TenantRelationship;
import io.acra.core.engine.EffectiveAuthorizationResolver;
import io.acra.core.engine.S6AssessmentEvaluator;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;

public final class Sprint6PolicyResolutionTestSuite {
    private static final Instant NOW = Instant.parse("2026-09-23T14:35:00Z");

    private Sprint6PolicyResolutionTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT6_POLICY_RESOLUTION PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        EffectiveAuthorizationResolver resolver = new EffectiveAuthorizationResolver();

        EffectiveAuthorizationResolution sameTenant = resolver.resolve(basePolicy(),
                request("user-a", "tenant-a", "tenant-a", "READ_DOCUMENT", AuthorizationDecision.ALLOW, false));
        TestSupport.assertEquals(AuthorizationDecision.ALLOW, sameTenant.expectedDecision(),
                "same-tenant inherited viewer permission should resolve allow");
        assertions++;
        TestSupport.assertTrue(sameTenant.effectiveRoleIds().contains("viewer"),
                "role inheritance must feed effective permission resolution");
        assertions++;

        EffectiveAuthorizationResolution crossTenant = resolver.resolve(basePolicy(),
                request("user-a", "tenant-a", "tenant-b", "READ_DOCUMENT", AuthorizationDecision.ALLOW, false));
        TestSupport.assertEquals(TenantRelationship.CROSS_TENANT, crossTenant.tenantRelationship(),
                "ordinary cross-tenant relationship should be explicit");
        assertions++;
        TestSupport.assertEquals(AuthorizationDecision.DENY, crossTenant.expectedDecision(),
                "explicit default deny should cover unauthorized cross-tenant access");
        assertions++;
        TestSupport.assertEquals(TenantIsolationAssessmentState.CANDIDATE,
                new S6AssessmentEvaluator().tenant(crossTenant).state(),
                "cross-tenant observed allow against expected deny should be a candidate");
        assertions++;

        EffectiveAuthorizationResolution global = resolver.resolve(globalPolicy(),
                request("global-admin", "tenant-a", "tenant-b", "READ_DOCUMENT", AuthorizationDecision.ALLOW, false));
        TestSupport.assertEquals(TenantRelationship.GLOBAL, global.tenantRelationship(),
                "explicit global role should classify global relationship");
        assertions++;
        TestSupport.assertEquals(AuthorizationDecision.ALLOW, global.expectedDecision(),
                "global role permission should resolve allow");
        assertions++;

        EffectiveAuthorizationResolution delegated = resolver.resolve(delegatedPolicy(),
                request("delegate-a", "tenant-a", "tenant-b", "READ_DOCUMENT", AuthorizationDecision.ALLOW, false));
        TestSupport.assertEquals(TenantRelationship.DELEGATED, delegated.tenantRelationship(),
                "validated delegation should classify delegated relationship");
        assertions++;
        TestSupport.assertEquals(AuthorizationDecision.ALLOW, delegated.expectedDecision(),
                "delegated role permission should resolve allow");
        assertions++;

        EffectiveAuthorizationResolution conflict = resolver.resolve(conflictPolicy(false),
                request("user-a", "tenant-a", "tenant-a", "READ_DOCUMENT", AuthorizationDecision.ALLOW, false));
        TestSupport.assertEquals(PolicyResolutionState.CONFLICTING, conflict.state(),
                "allow and deny without proven precedence must remain conflicting");
        assertions++;
        TestSupport.assertEquals(RbacAssessmentState.CONFLICTING,
                new S6AssessmentEvaluator().rbac(conflict).state(),
                "RBAC assessment preserves policy conflict");
        assertions++;

        EffectiveAuthorizationResolution precedence = resolver.resolve(conflictPolicy(true),
                request("user-a", "tenant-a", "tenant-a", "READ_DOCUMENT", AuthorizationDecision.ALLOW, false));
        TestSupport.assertEquals(AuthorizationDecision.DENY, precedence.expectedDecision(),
                "higher explicit deny precedence should deterministically resolve deny");
        assertions++;

        EffectiveAuthorizationResolution multi = resolver.resolve(multiRolePolicy(),
                request("user-multi", "tenant-a", "tenant-a", "EXPORT_REPORT", AuthorizationDecision.ALLOW, false));
        TestSupport.assertEquals(AuthorizationDecision.ALLOW, multi.expectedDecision(),
                "multi-role permission union should authorize an explicitly granted action");
        assertions++;
        TestSupport.assertTrue(multi.effectiveRoleIds().containsAll(List.of("viewer", "auditor")),
                "multi-role context must retain both roles");
        assertions++;

        EffectiveAuthorizationResolution privateCrossTenant = resolver.resolve(sharedScopePolicy(),
                new EffectiveAuthorizationRequest("user-shared", "tenant-a", "tenant-b", "report-b", "report",
                        "/reports/report-b", "", "READ_REPORT", AuthorizationDecision.ALLOW, false, NOW));
        TestSupport.assertEquals(AuthorizationDecision.DENY, privateCrossTenant.expectedDecision(),
                "shared-scope permission must not authorize a private cross-tenant resource");
        assertions++;

        EffectiveAuthorizationResolution sharedResource = resolver.resolve(sharedScopePolicy(),
                new EffectiveAuthorizationRequest("user-shared", "tenant-a", "shared", "shared-report", "report",
                        "/reports/shared-report", "", "READ_REPORT", AuthorizationDecision.ALLOW, true, NOW));
        TestSupport.assertEquals(AuthorizationDecision.ALLOW, sharedResource.expectedDecision(),
                "shared-scope permission should authorize an explicitly shared resource");
        assertions++;
        return assertions;
    }

    private static EffectiveAuthorizationRequest request(String principal, String subjectTenant, String resourceTenant,
                                                          String action, AuthorizationDecision observed,
                                                          boolean shared) {
        return new EffectiveAuthorizationRequest(principal, subjectTenant, resourceTenant, "doc-1", "document",
                "/documents/1", "", action, observed, shared, NOW);
    }

    private static AuthorizationPolicySnapshot basePolicy() {
        return AuthorizationPolicySnapshot.create("p-base", "1", "lab",
                List.of(new TenantMembership("tm-a", "user-a", "tenant-a", TenantMembershipType.DIRECT, true, List.of("e1"))),
                List.of(new RoleAssignment("ra-manager", "user-a", "manager", "tenant-a",
                        AuthorizationScope.tenant("tenant-a"), true, List.of("e2"))),
                List.of(new RoleInheritance("rh1", "manager", "viewer", "tenant-a", List.of("e3"))),
                List.of(new Permission("perm-read", "READ_DOCUMENT", "document", "", "",
                        AuthorizationScope.tenant("tenant-a"), List.of("e4"))),
                List.of(new RolePermissionAssignment("rpa1", "viewer", "perm-read", "tenant-a", List.of("e5"))),
                List.of(), List.of(), List.of("e-policy"), AuthorizationDecision.DENY, NOW);
    }

    private static AuthorizationPolicySnapshot globalPolicy() {
        return AuthorizationPolicySnapshot.create("p-global", "1", "lab", List.of(),
                List.of(new RoleAssignment("ra-global", "global-admin", "global-admin", "",
                        AuthorizationScope.global(), true, List.of("e1"))),
                List.of(),
                List.of(new Permission("perm-read-global", "READ_DOCUMENT", "document", "", "",
                        AuthorizationScope.global(), List.of("e2"))),
                List.of(new RolePermissionAssignment("rpa-global", "global-admin", "perm-read-global", "",
                        List.of("e3"))),
                List.of(), List.of(), List.of("e-policy"), AuthorizationDecision.DENY, NOW);
    }

    private static AuthorizationPolicySnapshot delegatedPolicy() {
        return AuthorizationPolicySnapshot.create("p-delegated", "1", "lab", List.of(), List.of(), List.of(),
                List.of(new Permission("perm-read-b", "READ_DOCUMENT", "document", "", "",
                        AuthorizationScope.tenant("tenant-b"), List.of("e2"))),
                List.of(new RolePermissionAssignment("rpa-delegated", "delegated-reader", "perm-read-b",
                        "tenant-b", List.of("e3"))),
                List.of(),
                List.of(new Delegation("d1", "admin-b", "delegate-a", "tenant-b", "tenant-b",
                        "delegated-reader", List.of("READ_DOCUMENT"), AuthorizationScope.tenant("tenant-b"),
                        NOW.minusSeconds(60), NOW.plusSeconds(3600), List.of("e4"))),
                List.of("e-policy"), AuthorizationDecision.DENY, NOW);
    }

    private static AuthorizationPolicySnapshot conflictPolicy(boolean precedence) {
        Integer allowP = precedence ? 10 : null;
        Integer denyP = precedence ? 20 : null;
        String source = precedence ? "explicit-policy-order" : "";
        return AuthorizationPolicySnapshot.create("p-conflict-" + precedence, "1", "lab", List.of(),
                List.of(new RoleAssignment("ra", "user-a", "viewer", "tenant-a",
                        AuthorizationScope.tenant("tenant-a"), true, List.of("e1"))),
                List.of(),
                List.of(new Permission("perm-read", "READ_DOCUMENT", "document", "", "",
                        AuthorizationScope.tenant("tenant-a"), List.of("e2"))),
                List.of(new RolePermissionAssignment("rpa", "viewer", "perm-read", "tenant-a", List.of("e3"))),
                List.of(
                        new AuthorizationRule("allow", AuthorizationRuleEffect.ALLOW, "", "viewer", "perm-read",
                                "tenant-a", AuthorizationScope.tenant("tenant-a"), allowP, source, List.of("e4")),
                        new AuthorizationRule("deny", AuthorizationRuleEffect.DENY, "", "viewer", "perm-read",
                                "tenant-a", AuthorizationScope.tenant("tenant-a"), denyP, source, List.of("e5"))),
                List.of(), List.of("e-policy"), AuthorizationDecision.DENY, NOW);
    }

    private static AuthorizationPolicySnapshot sharedScopePolicy() {
        return AuthorizationPolicySnapshot.create("p-shared", "1", "lab", List.of(),
                List.of(new RoleAssignment("ra-shared", "user-shared", "shared-reader", "",
                        AuthorizationScope.shared(), true, List.of("e1"))),
                List.of(),
                List.of(new Permission("shared-read", "READ_REPORT", "report", "", "",
                        AuthorizationScope.shared(), List.of("e2"))),
                List.of(new RolePermissionAssignment("rp-shared", "shared-reader", "shared-read", "",
                        List.of("e3"))),
                List.of(), List.of(), List.of("e-policy"), AuthorizationDecision.DENY, NOW);
    }

    private static AuthorizationPolicySnapshot multiRolePolicy() {
        return AuthorizationPolicySnapshot.create("p-multi", "1", "lab", List.of(),
                List.of(
                        new RoleAssignment("ra1", "user-multi", "viewer", "tenant-a",
                                AuthorizationScope.tenant("tenant-a"), true, List.of("e1")),
                        new RoleAssignment("ra2", "user-multi", "auditor", "tenant-a",
                                AuthorizationScope.tenant("tenant-a"), true, List.of("e2"))),
                List.of(),
                List.of(
                        new Permission("read", "READ_DOCUMENT", "document", "", "",
                                AuthorizationScope.tenant("tenant-a"), List.of("e3")),
                        new Permission("export", "EXPORT_REPORT", "document", "", "",
                                AuthorizationScope.tenant("tenant-a"), List.of("e4"))),
                List.of(
                        new RolePermissionAssignment("rpa-read", "viewer", "read", "tenant-a", List.of("e5")),
                        new RolePermissionAssignment("rpa-export", "auditor", "export", "tenant-a", List.of("e6"))),
                List.of(), List.of(), List.of("e-policy"), AuthorizationDecision.DENY, NOW);
    }
}
