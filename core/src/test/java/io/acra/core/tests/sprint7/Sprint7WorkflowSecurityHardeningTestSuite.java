package io.acra.core.tests.sprint7;

import io.acra.core.active.execution.TestExecutionResult;
import io.acra.core.active.model.ExecutionEnvironment;
import io.acra.core.active.model.RequestDefinition;
import io.acra.core.active.model.TestContract;
import io.acra.core.active.model.TargetDescriptor;
import io.acra.core.active.planning.S7WorkflowPlanningCandidate;
import io.acra.core.active.planning.S7WorkflowTestSeedFactory;
import io.acra.core.active.safety.ValidationDecision;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.PolicyResolutionState;
import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.endpoint.Endpoint;
import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpRequest;
import io.acra.core.domain.resource.Resource;
import io.acra.core.domain.testing.TestState;
import io.acra.core.domain.workflow.WorkflowAuthorizationRequest;
import io.acra.core.domain.workflow.WorkflowAuthorizationResolution;
import io.acra.core.domain.workflow.WorkflowTokenBinding;
import io.acra.core.engine.S7WorkflowCoverageTracker;
import io.acra.core.recon.SecurityContextFingerprint;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class Sprint7WorkflowSecurityHardeningTestSuite {
    private static final Instant NOW = Instant.parse("2026-09-24T07:00:00Z");
    private static final String RAW_TOKEN = "s7-security-raw-token";
    private static final String TOKEN_FP = TokenFingerprint.sha256(RAW_TOKEN);

    private Sprint7WorkflowSecurityHardeningTestSuite() { }

    public static void main(String[] args) {
        int assertions = 0;

        expectFailure(() -> new WorkflowTokenBinding(
                "binding-raw", "document-approval", "APPROVE", "approver-a", "approver", "tenant-a",
                RAW_TOKEN, List.of("e-binding")), "raw token binding rejected");
        assertions++;

        expectFailure(() -> new WorkflowAuthorizationRequest(
                "document-approval", "approver-a", List.of("approver"), "tenant-a", "document-1",
                "APPROVE", "SUBMITTED", "APPROVED", true, true, "", RAW_TOKEN,
                AuthorizationDecision.UNKNOWN, NOW), "raw request token context rejected");
        assertions++;

        WorkflowTokenBinding normalized = new WorkflowTokenBinding(
                "binding-sha", "document-approval", "APPROVE", "approver-a", "approver", "tenant-a",
                TOKEN_FP.toUpperCase(java.util.Locale.ROOT), List.of("e-binding"));
        TestSupport.assertEquals(TOKEN_FP, normalized.tokenContextFingerprint(),
                "SHA-256 token context normalized without exposing raw token");
        assertions++;

        S7WorkflowTestSeedFactory factory = new S7WorkflowTestSeedFactory();
        var valid = factory.generate(candidate(endpoint(HttpMethod.POST), request("SUBMITTED", false),
                resolution("author-a", "SUBMITTED", AuthorizationDecision.ALLOW, PolicyResolutionState.RESOLVED_ALLOW),
                resolution("author-a", "APPROVED", AuthorizationDecision.DENY, PolicyResolutionState.RESOLVED_DENY)));
        TestSupport.assertEquals(1, valid.seeds().size(), "valid controlled transition remains generatable");
        assertions++;

        var conflicting = factory.generate(candidate(endpoint(HttpMethod.POST), request("SUBMITTED", false),
                resolution("author-a", "SUBMITTED", AuthorizationDecision.ALLOW, PolicyResolutionState.RESOLVED_ALLOW),
                resolution("author-a", "APPROVED", AuthorizationDecision.UNKNOWN, PolicyResolutionState.CONFLICTING)));
        TestSupport.assertTrue(conflicting.seeds().isEmpty(), "conflicting workflow policy generates no seed");
        assertions++;
        TestSupport.assertTrue(conflicting.skippedReasons().stream()
                        .anyMatch(value -> value.contains("WORKFLOW_POLICY_UNRESOLVED")),
                "conflicting policy skip is explicit");
        assertions++;

        var contextDrift = factory.generate(candidate(endpoint(HttpMethod.POST), request("SUBMITTED", false),
                resolution("author-a", "SUBMITTED", AuthorizationDecision.ALLOW, PolicyResolutionState.RESOLVED_ALLOW),
                resolution("other-author", "APPROVED", AuthorizationDecision.DENY, PolicyResolutionState.RESOLVED_DENY)));
        TestSupport.assertTrue(contextDrift.seeds().isEmpty(), "principal drift blocks target-state generation");
        assertions++;
        TestSupport.assertTrue(contextDrift.skippedReasons().stream()
                        .anyMatch(value -> value.contains("MUTATION_CHANGES_MORE_THAN_TARGET_STATE")),
                "context drift reason is explicit");
        assertions++;

        var ambiguousBody = factory.generate(candidate(endpoint(HttpMethod.POST), request("SUBMITTED", true),
                resolution("author-a", "SUBMITTED", AuthorizationDecision.ALLOW, PolicyResolutionState.RESOLVED_ALLOW),
                resolution("author-a", "APPROVED", AuthorizationDecision.DENY, PolicyResolutionState.RESOLVED_DENY)));
        TestSupport.assertTrue(ambiguousBody.seeds().isEmpty(), "ambiguous source-state body blocks mutation");
        assertions++;
        TestSupport.assertTrue(ambiguousBody.skippedReasons().stream()
                        .anyMatch(value -> value.contains("BASELINE_BODY_DOES_NOT_CONTAIN_ONE_SOURCE_TARGET_STATE")),
                "ambiguous body reason is explicit");
        assertions++;

        var destructive = factory.generate(candidate(endpoint(HttpMethod.DELETE), request("SUBMITTED", false),
                resolution("author-a", "SUBMITTED", AuthorizationDecision.ALLOW, PolicyResolutionState.RESOLVED_ALLOW),
                resolution("author-a", "APPROVED", AuthorizationDecision.DENY, PolicyResolutionState.RESOLVED_DENY)));
        TestSupport.assertTrue(destructive.seeds().isEmpty(), "DELETE workflow transition is not auto-generated");
        assertions++;

        S7WorkflowCoverageTracker tracker = new S7WorkflowCoverageTracker();
        WorkflowAuthorizationResolution deny = resolution(
                "author-a", "APPROVED", AuthorizationDecision.DENY, PolicyResolutionState.RESOLVED_DENY);
        tracker.recordResolution(deny);
        WorkflowAuthorizationResolution drift = resolution(
                "author-a", "APPROVED", AuthorizationDecision.ALLOW, PolicyResolutionState.RESOLVED_ALLOW);
        expectFailure(() -> tracker.recordResolution(drift),
                "same coverage identity cannot silently change policy resolution");
        assertions++;

        TestExecutionResult unplanned = new TestExecutionResult(
                "S7-SEC-EXEC", "S7-UNPLANNED-TEST", TestState.BLOCKED,
                ValidationDecision.allowed(), null, null, List.of());
        expectFailure(() -> tracker.recordExecution(deny, unplanned),
                "execution cannot be credited before matching workflow planning");
        assertions++;

        String serialized = new io.acra.core.serialization.DomainSerializer().serialize(valid.seeds().getFirst());
        TestSupport.assertNotContains(serialized, RAW_TOKEN, "generated workflow seed excludes raw token");
        assertions++;
        TestSupport.assertNotContains(serialized, TOKEN_FP,
                "generated workflow seed does not persist token-context fingerprint");
        assertions++;

        System.out.println("SPRINT7_WORKFLOW_SECURITY_HARDENING PASS assertions=" + assertions);
    }

    private static S7WorkflowPlanningCandidate candidate(
            Endpoint endpoint,
            RequestDefinition baseline,
            WorkflowAuthorizationResolution baselineResolution,
            WorkflowAuthorizationResolution targetResolution) {
        RequestDefinition positive = request("SUBMITTED", false);
        RequestDefinition negative = request("SUBMITTED", false);
        return new S7WorkflowPlanningCandidate(
                "S7-SEC-CANDIDATE-" + endpoint.method() + "-" + baselineResolution.resolutionId()
                        + "-" + targetResolution.resolutionId(),
                endpoint,
                baseline,
                positive,
                negative,
                context(targetResolution.principalId(), targetResolution.toState()),
                context(targetResolution.principalId(), targetResolution.toState()),
                resource("SUBMITTED"),
                resource(targetResolution.toState()),
                baselineResolution,
                targetResolution,
                Set.of(TestContract.TRANSITION),
                List.of(),
                List.of("controlled-security-fixture"),
                false,
                false);
    }

    private static RequestDefinition request(String toState, boolean duplicateState) {
        String body = "{\"action\":\"SUBMIT\",\"from_state\":\"DRAFT\",\"to_state\":\"" + toState
                + "\"" + (duplicateState ? ",\"note\":\"" + toState + "\"" : "") + "}";
        HttpRequest request = HttpRequest.of(
                HttpMethod.POST, "http", "localhost", 18082,
                "/api/v1/s7/workflows/document-approval/resources/document-1/transition",
                List.of(new HttpHeader("Authorization", "Bearer " + RAW_TOKEN),
                        new HttpHeader("Content-Type", "application/json")),
                body.getBytes(StandardCharsets.UTF_8),
                HttpProtocol.HTTP_1_1);
        return new RequestDefinition(
                "S7-SEC-REQ-" + Math.abs(body.hashCode()),
                request,
                "ctx-author",
                "workflow-resource:document-1");
    }

    private static Endpoint endpoint(HttpMethod method) {
        return new Endpoint(
                "S7-SEC-ENDPOINT-" + method,
                method,
                "/api/v1/s7/workflows/document-approval/resources/document-1/transition",
                "/api/v1/s7/workflows/{workflow}/resources/{resource}/transition",
                "/api/v1/s7/workflows/{workflow}/resources/{resource}/transition",
                "localhost", "v1", List.of());
    }

    private static WorkflowAuthorizationResolution resolution(
            String principal,
            String toState,
            AuthorizationDecision expected,
            PolicyResolutionState state) {
        return new WorkflowAuthorizationResolution(
                "s7-sec-" + principal + "-" + toState + "-" + expected,
                "s7-security-policy-fingerprint",
                "document-approval",
                principal,
                "tenant-a",
                "document-1",
                "SUBMIT",
                "DRAFT",
                toState,
                List.of("submit-rule"),
                List.of(),
                List.of(),
                expected,
                AuthorizationDecision.UNKNOWN,
                state,
                List.of("e-policy", "e-rule"),
                state == PolicyResolutionState.CONFLICTING ? List.of("conflict") : List.of());
    }

    private static SecurityContextFingerprint context(String principal, String state) {
        return new SecurityContextFingerprint(
                principal, "author", "tenant-a", "document-1", principal, "SUBMIT", state,
                "POST /api/v1/s7/workflows/{workflow}/resources/{resource}/transition",
                "raw", "ctx-author", AuthorizationDecision.UNKNOWN, AuthorizationDecision.UNKNOWN,
                List.of("e-policy"));
    }

    private static Resource resource(String state) {
        return new Resource("document-1", "workflow-document", null, "author-a", "tenant-a", state,
                Confidence.unknown());
    }

    private static void expectFailure(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
