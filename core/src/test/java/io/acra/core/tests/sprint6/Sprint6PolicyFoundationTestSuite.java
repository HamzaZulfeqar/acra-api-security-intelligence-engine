package io.acra.core.tests.sprint6;

import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.authorization.AuthorizationRule;
import io.acra.core.domain.authorization.AuthorizationRuleEffect;
import io.acra.core.domain.authorization.AuthorizationScope;
import io.acra.core.domain.authorization.Delegation;
import io.acra.core.domain.authorization.EffectiveRoleResolution;
import io.acra.core.domain.authorization.Permission;
import io.acra.core.domain.authorization.RoleAssignment;
import io.acra.core.domain.authorization.RoleInheritance;
import io.acra.core.domain.authorization.RolePermissionAssignment;
import io.acra.core.domain.authorization.RoleResolutionState;
import io.acra.core.domain.authorization.TenantMembership;
import io.acra.core.domain.authorization.TenantMembershipType;
import io.acra.core.engine.RoleHierarchyResolver;
import io.acra.core.serialization.DomainSerializer;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;

public final class Sprint6PolicyFoundationTestSuite {
    private static final Instant NOW = Instant.parse("2026-09-23T14:30:00Z");

    private Sprint6PolicyFoundationTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT6_POLICY_FOUNDATION PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        AuthorizationPolicySnapshot first = snapshot(false);
        AuthorizationPolicySnapshot reordered = snapshot(true);
        TestSupport.assertEquals(first.fingerprint(), reordered.fingerprint(),
                "policy fingerprint must be deterministic independent of caller list order");
        assertions++;

        EffectiveRoleResolution tenantA = new RoleHierarchyResolver().resolve(first, "user-a", "tenant-a");
        TestSupport.assertEquals(RoleResolutionState.RESOLVED, tenantA.state(),
                "tenant-a role resolution should resolve");
        assertions++;
        TestSupport.assertTrue(tenantA.directRoleIds().contains("manager"), "manager direct role retained");
        assertions++;
        TestSupport.assertTrue(tenantA.directRoleIds().contains("auditor"), "multi-role assignment retained");
        assertions++;
        TestSupport.assertTrue(tenantA.inheritedRoleIds().contains("editor"), "manager inherits editor");
        assertions++;
        TestSupport.assertTrue(tenantA.inheritedRoleIds().contains("viewer"), "transitive viewer inheritance resolved");
        assertions++;

        EffectiveRoleResolution tenantB = new RoleHierarchyResolver().resolve(first, "user-a", "tenant-b");
        TestSupport.assertEquals(List.of("global-auditor"), tenantB.directRoleIds(),
                "tenant-bound roles must not bleed into another tenant while global roles remain");
        assertions++;

        AuthorizationPolicySnapshot cycle = cycleSnapshot();
        EffectiveRoleResolution cycleResult = new RoleHierarchyResolver().resolve(cycle, "user-c", "tenant-a");
        TestSupport.assertEquals(RoleResolutionState.CONFLICTING, cycleResult.state(),
                "role hierarchy cycles must fail closed");
        assertions++;
        TestSupport.assertTrue(cycleResult.reasons().stream().anyMatch(r -> r.startsWith("ROLE_HIERARCHY_CYCLE:")),
                "cycle reason should be preserved");
        assertions++;

        String json = new DomainSerializer().serialize(secretSnapshot());
        TestSupport.assertNotContains(json, "DummyPassword", "policy snapshots must remain secret-safe");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new RoleInheritance("bad", "viewer", "viewer", "tenant-a", List.of()),
                "self inheritance must be rejected");
        assertions++;
        return assertions;
    }

    private static AuthorizationPolicySnapshot snapshot(boolean reverse) {
        List<RoleAssignment> roles = List.of(
                new RoleAssignment("ra-manager", "user-a", "manager", "tenant-a",
                        AuthorizationScope.tenant("tenant-a"), true, List.of("e-ra-manager")),
                new RoleAssignment("ra-auditor", "user-a", "auditor", "tenant-a",
                        AuthorizationScope.tenant("tenant-a"), true, List.of("e-ra-auditor")),
                new RoleAssignment("ra-global", "user-a", "global-auditor", "",
                        AuthorizationScope.global(), true, List.of("e-ra-global")));
        List<RoleInheritance> hierarchy = List.of(
                new RoleInheritance("rh-manager-editor", "manager", "editor", "tenant-a", List.of("e-h1")),
                new RoleInheritance("rh-editor-viewer", "editor", "viewer", "tenant-a", List.of("e-h2")));
        if (reverse) {
            roles = List.of(roles.get(2), roles.get(1), roles.get(0));
            hierarchy = List.of(hierarchy.get(1), hierarchy.get(0));
        }
        return AuthorizationPolicySnapshot.create("policy-s6", "1", "lab",
                List.of(new TenantMembership("tm-a", "user-a", "tenant-a", TenantMembershipType.DIRECT,
                        true, List.of("e-tm"))),
                roles, hierarchy,
                List.of(new Permission("p-read", "READ_DOCUMENT", "document", "", "",
                        AuthorizationScope.tenant("tenant-a"), List.of("e-p"))),
                List.of(new RolePermissionAssignment("rpa-viewer-read", "viewer", "p-read", "tenant-a",
                        List.of("e-rpa"))),
                List.of(new AuthorizationRule("rule-read", AuthorizationRuleEffect.ALLOW, "", "viewer",
                        "p-read", "tenant-a", AuthorizationScope.tenant("tenant-a"), 10,
                        "explicit-lab-policy", List.of("e-rule"))),
                List.of(), List.of("e-policy"), NOW);
    }

    private static AuthorizationPolicySnapshot cycleSnapshot() {
        return AuthorizationPolicySnapshot.create("policy-cycle", "1", "lab", List.of(),
                List.of(new RoleAssignment("ra-c", "user-c", "a", "tenant-a",
                        AuthorizationScope.tenant("tenant-a"), true, List.of("e"))),
                List.of(new RoleInheritance("a-b", "a", "b", "tenant-a", List.of("e")),
                        new RoleInheritance("b-a", "b", "a", "tenant-a", List.of("e"))),
                List.of(), List.of(), List.of(), List.of(), List.of("e"), NOW);
    }

    private static AuthorizationPolicySnapshot secretSnapshot() {
        Delegation delegation = new Delegation("d1", "admin-a", "user-a", "tenant-a", "tenant-b",
                "delegated-admin", List.of("READ"), AuthorizationScope.tenant("tenant-b"),
                NOW, NOW.plusSeconds(3600), List.of("password=DummyPassword"));
        return AuthorizationPolicySnapshot.create("policy-secret", "1", "password=DummyPassword",
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(delegation),
                List.of("password=DummyPassword"), NOW);
    }
}
