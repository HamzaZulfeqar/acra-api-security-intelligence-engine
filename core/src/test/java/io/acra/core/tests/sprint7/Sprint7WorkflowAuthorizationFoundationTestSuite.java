package io.acra.core.tests.sprint7;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.authorization.AuthorizationRuleEffect;
import io.acra.core.domain.authorization.AuthorizationScope;
import io.acra.core.domain.authorization.Delegation;
import io.acra.core.domain.authorization.PolicyResolutionState;
import io.acra.core.domain.workflow.WorkflowAuthorizationRequest;
import io.acra.core.domain.workflow.WorkflowPolicySnapshot;
import io.acra.core.domain.workflow.WorkflowTokenBinding;
import io.acra.core.domain.workflow.WorkflowTransitionRule;
import io.acra.core.engine.WorkflowAuthorizationResolver;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.serialization.DomainSerializer;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;

public final class Sprint7WorkflowAuthorizationFoundationTestSuite {
    private static final Instant NOW = Instant.parse("2026-09-23T17:10:00Z");
    private static final String RAW_TOKEN = "synthetic-super-secret-workflow-token";
    private static final String TOKEN_FP = TokenFingerprint.sha256(RAW_TOKEN);
    private static final String OTHER_FP = TokenFingerprint.sha256("different-synthetic-token");

    private Sprint7WorkflowAuthorizationFoundationTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT7_WORKFLOW_FOUNDATION PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        WorkflowAuthorizationResolver resolver = new WorkflowAuthorizationResolver();

        var submit = resolver.resolve(policy(), authorizationPolicy(),
                request("author-a", List.of("author"), "SUBMIT", "DRAFT", "SUBMITTED",
                        true, true, "", "", AuthorizationDecision.ALLOW));
        TestSupport.assertEquals(AuthorizationDecision.ALLOW, submit.expectedDecision(),
                "author may submit draft under explicit workflow policy");
        assertions++;
        TestSupport.assertEquals(PolicyResolutionState.RESOLVED_ALLOW, submit.state(),
                "submit transition resolves allow");
        assertions++;

        var wrongRole = resolver.resolve(policy(), authorizationPolicy(),
                request("author-a", List.of("author"), "APPROVE", "SUBMITTED", "APPROVED",
                        true, true, "", TOKEN_FP, AuthorizationDecision.ALLOW));
        TestSupport.assertEquals(AuthorizationDecision.DENY, wrongRole.expectedDecision(),
                "author must not inherit approver transition");
        assertions++;
        TestSupport.assertTrue(wrongRole.reasons().stream().anyMatch(v -> v.contains("REQUIRED_ROLE_NOT_PRESENT")),
                "wrong role reason retained");
        assertions++;

        var missingApproval = resolver.resolve(policy(), authorizationPolicy(),
                request("approver-a", List.of("approver"), "APPROVE", "SUBMITTED", "APPROVED",
                        false, true, "", TOKEN_FP, AuthorizationDecision.ALLOW));
        TestSupport.assertEquals(AuthorizationDecision.DENY, missingApproval.expectedDecision(),
                "approval-required transition fails closed");
        assertions++;

        var missingSeparation = resolver.resolve(policy(), authorizationPolicy(),
                request("approver-a", List.of("approver"), "APPROVE", "SUBMITTED", "APPROVED",
                        true, false, "", TOKEN_FP, AuthorizationDecision.ALLOW));
        TestSupport.assertEquals(AuthorizationDecision.DENY, missingSeparation.expectedDecision(),
                "role separation requirement fails closed");
        assertions++;

        var boundApproval = resolver.resolve(policy(), authorizationPolicy(),
                request("approver-a", List.of("approver"), "APPROVE", "SUBMITTED", "APPROVED",
                        true, true, "", TOKEN_FP, AuthorizationDecision.ALLOW));
        TestSupport.assertEquals(AuthorizationDecision.ALLOW, boundApproval.expectedDecision(),
                "valid fingerprint-bound approver context may approve");
        assertions++;
        TestSupport.assertEquals(List.of("binding-approve"), boundApproval.matchedBindingIds(),
                "token-binding identity retained as evidence-safe reference");
        assertions++;

        var wrongBinding = resolver.resolve(policy(), authorizationPolicy(),
                request("approver-a", List.of("approver"), "APPROVE", "SUBMITTED", "APPROVED",
                        true, true, "", OTHER_FP, AuthorizationDecision.ALLOW));
        TestSupport.assertEquals(AuthorizationDecision.DENY, wrongBinding.expectedDecision(),
                "wrong token-context fingerprint fails closed");
        assertions++;
        TestSupport.assertTrue(wrongBinding.reasons().stream().anyMatch(v -> v.contains("TOKEN_BINDING_MISMATCH")),
                "token binding mismatch reason retained");
        assertions++;

        var delegated = resolver.resolve(policy(), authorizationPolicy(),
                request("delegate-a", List.of("approver"), "APPROVE", "SUBMITTED", "APPROVED",
                        true, true, "delegation-approve", TOKEN_FP, AuthorizationDecision.ALLOW));
        TestSupport.assertEquals(AuthorizationDecision.ALLOW, delegated.expectedDecision(),
                "valid explicit delegation may satisfy delegated approval");
        assertions++;
        TestSupport.assertEquals(List.of("delegation-approve"), delegated.delegationIds(),
                "validated delegation identity retained");
        assertions++;

        var expiredDelegation = resolver.resolve(policy(), expiredAuthorizationPolicy(),
                request("delegate-a", List.of("approver"), "APPROVE", "SUBMITTED", "APPROVED",
                        true, true, "delegation-approve", TOKEN_FP, AuthorizationDecision.ALLOW));
        TestSupport.assertEquals(AuthorizationDecision.DENY, expiredDelegation.expectedDecision(),
                "expired delegation fails closed");
        assertions++;

        var terminal = resolver.resolve(policy(), authorizationPolicy(),
                request("author-a", List.of("author"), "CANCEL", "SHIPPED", "CANCELLED",
                        true, true, "", "", AuthorizationDecision.ALLOW));
        TestSupport.assertEquals(AuthorizationDecision.DENY, terminal.expectedDecision(),
                "terminal source transition denied");
        assertions++;

        var conflict = resolver.resolve(conflictPolicy(false), authorizationPolicy(),
                request("approver-a", List.of("approver"), "APPROVE", "SUBMITTED", "APPROVED",
                        true, true, "", "", AuthorizationDecision.ALLOW));
        TestSupport.assertEquals(PolicyResolutionState.CONFLICTING, conflict.state(),
                "allow/deny conflict without precedence remains conflicting");
        assertions++;

        var precedence = resolver.resolve(conflictPolicy(true), authorizationPolicy(),
                request("approver-a", List.of("approver"), "APPROVE", "SUBMITTED", "APPROVED",
                        true, true, "", "", AuthorizationDecision.ALLOW));
        TestSupport.assertEquals(AuthorizationDecision.DENY, precedence.expectedDecision(),
                "higher explicit deny precedence wins deterministically");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new WorkflowTokenBinding("unsafe", "document-approval", "APPROVE",
                        "approver-a", "approver", "tenant-a", RAW_TOKEN, List.of()),
                "raw token must not be accepted where a fingerprint is required");
        assertions++;

        String serialized = new DomainSerializer().serialize(boundApproval);
        TestSupport.assertNotContains(serialized, RAW_TOKEN, "raw token absent from resolution serialization");
        assertions++;
        TestSupport.assertContains(serialized, TOKEN_FP, "fingerprint remains available for deterministic correlation only through policy/request state");
        assertions++;

        return assertions;
    }

    private static WorkflowPolicySnapshot policy() {
        return WorkflowPolicySnapshot.create(
                "workflow-policy-main",
                "1",
                "controlled-s7-fixture",
                List.of(
                        rule("submit", "DRAFT", "SUBMITTED", "SUBMIT", List.of("author"),
                                false, false, false, false, "", AuthorizationRuleEffect.ALLOW, null, ""),
                        rule("approve", "SUBMITTED", "APPROVED", "APPROVE", List.of("approver"),
                                true, true, false, true, "binding-approve", AuthorizationRuleEffect.ALLOW, null, ""),
                        rule("cancel-shipped", "SHIPPED", "CANCELLED", "CANCEL", List.of("author"),
                                false, false, true, false, "", AuthorizationRuleEffect.DENY, null, "")),
                List.of(new WorkflowTokenBinding(
                        "binding-approve", "document-approval", "APPROVE", "",
                        "approver", "tenant-a", TOKEN_FP, List.of("e-binding"))),
                List.of("e-workflow-policy"),
                AuthorizationDecision.DENY,
                NOW);
    }

    private static WorkflowPolicySnapshot conflictPolicy(boolean precedence) {
        Integer allowP = precedence ? 10 : null;
        Integer denyP = precedence ? 20 : null;
        String source = precedence ? "explicit-policy-order" : "";
        return WorkflowPolicySnapshot.create(
                "workflow-policy-conflict-" + precedence,
                "1",
                "controlled-s7-fixture",
                List.of(
                        rule("allow-approve", "SUBMITTED", "APPROVED", "APPROVE", List.of("approver"),
                                false, false, false, false, "", AuthorizationRuleEffect.ALLOW, allowP, source),
                        rule("deny-approve", "SUBMITTED", "APPROVED", "APPROVE", List.of("approver"),
                                false, false, false, false, "", AuthorizationRuleEffect.DENY, denyP, source)),
                List.of(),
                List.of("e-conflict-policy"),
                AuthorizationDecision.DENY,
                NOW);
    }

    private static WorkflowTransitionRule rule(
            String id,
            String from,
            String to,
            String action,
            List<String> roles,
            boolean approval,
            boolean separation,
            boolean terminal,
            boolean delegation,
            String tokenBinding,
            AuthorizationRuleEffect effect,
            Integer precedence,
            String precedenceSource) {
        return new WorkflowTransitionRule(
                id,
                "document-approval",
                from,
                to,
                action,
                "tenant-a",
                roles,
                approval,
                separation,
                terminal,
                delegation,
                tokenBinding,
                effect,
                precedence,
                precedenceSource,
                List.of("e-" + id));
    }

    private static WorkflowAuthorizationRequest request(
            String principal,
            List<String> roles,
            String action,
            String from,
            String to,
            boolean approval,
            boolean separation,
            String delegationId,
            String tokenFingerprint,
            AuthorizationDecision observed) {
        return new WorkflowAuthorizationRequest(
                "document-approval",
                principal,
                roles,
                "tenant-a",
                "document-1",
                action,
                from,
                to,
                approval,
                separation,
                delegationId,
                tokenFingerprint,
                observed,
                NOW);
    }

    private static AuthorizationPolicySnapshot authorizationPolicy() {
        return AuthorizationPolicySnapshot.create(
                "s6-authz-for-s7",
                "1",
                "controlled-s7-fixture",
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(new Delegation(
                        "delegation-approve",
                        "admin-a",
                        "delegate-a",
                        "tenant-a",
                        "tenant-a",
                        "approver",
                        List.of("APPROVE"),
                        AuthorizationScope.tenant("tenant-a"),
                        NOW.minusSeconds(60),
                        NOW.plusSeconds(3600),
                        List.of("e-delegation"))),
                List.of("e-authz"),
                AuthorizationDecision.DENY,
                NOW);
    }

    private static AuthorizationPolicySnapshot expiredAuthorizationPolicy() {
        return AuthorizationPolicySnapshot.create(
                "s6-authz-expired-for-s7",
                "1",
                "controlled-s7-fixture",
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(new Delegation(
                        "delegation-approve",
                        "admin-a",
                        "delegate-a",
                        "tenant-a",
                        "tenant-a",
                        "approver",
                        List.of("APPROVE"),
                        AuthorizationScope.tenant("tenant-a"),
                        NOW.minusSeconds(3600),
                        NOW.minusSeconds(60),
                        List.of("e-delegation-expired"))),
                List.of("e-authz"),
                AuthorizationDecision.DENY,
                NOW);
    }
}
