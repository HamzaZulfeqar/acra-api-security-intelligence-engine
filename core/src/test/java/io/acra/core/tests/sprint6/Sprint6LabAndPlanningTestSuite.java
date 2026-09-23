package io.acra.core.tests.sprint6;

import io.acra.core.active.model.TestContract;
import io.acra.core.active.planning.S6PolicyPlanningAdvisor;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.authorization.AuthorizationScope;
import io.acra.core.domain.authorization.Delegation;
import io.acra.core.domain.authorization.EffectiveAuthorizationRequest;
import io.acra.core.domain.authorization.EffectiveAuthorizationResolution;
import io.acra.core.domain.authorization.Permission;
import io.acra.core.domain.authorization.RoleAssignment;
import io.acra.core.domain.authorization.RolePermissionAssignment;
import io.acra.core.engine.EffectiveAuthorizationResolver;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;

public final class Sprint6LabAndPlanningTestSuite {
    private static final Instant NOW = Instant.parse("2026-09-23T14:55:00Z");

    private Sprint6LabAndPlanningTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT6_LAB_PLANNING PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        EffectiveAuthorizationResolver resolver = new EffectiveAuthorizationResolver();
        S6PolicyPlanningAdvisor advisor = new S6PolicyPlanningAdvisor();

        EffectiveAuthorizationResolution cross = resolver.resolve(defaultDenyPolicy(),
                request("user-a", "tenant-a", "tenant-b", AuthorizationDecision.ALLOW));
        var crossRec = advisor.recommend(cross);
        TestSupport.assertTrue(crossRec.stream().anyMatch(r -> r.contract() == TestContract.CROSS_TENANT),
                "cross-tenant policy context should recommend CROSS_TENANT coverage");
        assertions++;
        TestSupport.assertTrue(crossRec.stream().anyMatch(r -> r.contract() == TestContract.ROLE_COMPARISON),
                "effective role context should recommend ROLE_COMPARISON coverage");
        assertions++;
        TestSupport.assertTrue(crossRec.get(0).priorityBoost() >= 85,
                "policy mismatch candidate should receive high planning priority");
        assertions++;

        EffectiveAuthorizationResolution global = resolver.resolve(globalPolicy(),
                request("global-admin", "tenant-a", "tenant-b", AuthorizationDecision.ALLOW));
        var globalRec = advisor.recommend(global);
        TestSupport.assertTrue(globalRec.stream().anyMatch(r -> r.contract() == TestContract.CROSS_TENANT
                        && r.reason().contains("false-positive control")),
                "global access should be kept as a false-positive control, not assumed vulnerable");
        assertions++;

        EffectiveAuthorizationResolution delegated = resolver.resolve(delegatedPolicy(),
                request("delegate-a", "tenant-a", "tenant-b", AuthorizationDecision.ALLOW));
        TestSupport.assertTrue(advisor.recommend(delegated).stream().anyMatch(
                        r -> r.contract() == TestContract.CROSS_TENANT && r.priorityBoost() == 55),
                "delegated access should remain reviewable at lower audit priority");
        assertions++;
        return assertions;
    }

    private static EffectiveAuthorizationRequest request(String principal, String subject, String resource,
                                                         AuthorizationDecision observed) {
        return new EffectiveAuthorizationRequest(principal, subject, resource, "report-b", "report",
                "/api/v1/s6/tenants/" + resource + "/reports/report-b", "", "READ_REPORT",
                observed, false, NOW);
    }

    private static AuthorizationPolicySnapshot defaultDenyPolicy() {
        return AuthorizationPolicySnapshot.create("s6-lab-deny", "1", "lab", List.of(),
                List.of(new RoleAssignment("ra1", "user-a", "viewer", "tenant-a",
                        AuthorizationScope.tenant("tenant-a"), true, List.of("e1"))),
                List.of(),
                List.of(new Permission("p1", "READ_REPORT", "report", "", "",
                        AuthorizationScope.tenant("tenant-a"), List.of("e2"))),
                List.of(new RolePermissionAssignment("rp1", "viewer", "p1", "tenant-a", List.of("e3"))),
                List.of(), List.of(), List.of("e1","e2","e3"), AuthorizationDecision.DENY, NOW);
    }

    private static AuthorizationPolicySnapshot globalPolicy() {
        return AuthorizationPolicySnapshot.create("s6-lab-global", "1", "lab", List.of(),
                List.of(new RoleAssignment("ra1", "global-admin", "global-admin", "",
                        AuthorizationScope.global(), true, List.of("e1"))),
                List.of(),
                List.of(new Permission("p1", "READ_REPORT", "report", "", "",
                        AuthorizationScope.global(), List.of("e2"))),
                List.of(new RolePermissionAssignment("rp1", "global-admin", "p1", "", List.of("e3"))),
                List.of(), List.of(), List.of("e1","e2","e3"), AuthorizationDecision.DENY, NOW);
    }

    private static AuthorizationPolicySnapshot delegatedPolicy() {
        return AuthorizationPolicySnapshot.create("s6-lab-delegated", "1", "lab", List.of(), List.of(), List.of(),
                List.of(new Permission("p1", "READ_REPORT", "report", "", "",
                        AuthorizationScope.tenant("tenant-b"), List.of("e2"))),
                List.of(new RolePermissionAssignment("rp1", "delegated-reader", "p1", "tenant-b", List.of("e3"))),
                List.of(),
                List.of(new Delegation("d1", "admin-b", "delegate-a", "tenant-b", "tenant-b",
                        "delegated-reader", List.of("READ_REPORT"), AuthorizationScope.tenant("tenant-b"),
                        NOW.minusSeconds(60), NOW.plusSeconds(3600), List.of("e4"))),
                List.of("e2","e3","e4"), AuthorizationDecision.DENY, NOW);
    }
}
