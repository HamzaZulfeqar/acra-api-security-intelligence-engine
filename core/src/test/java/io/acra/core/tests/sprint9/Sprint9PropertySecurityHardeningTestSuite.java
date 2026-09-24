package io.acra.core.tests.sprint9;

import io.acra.core.active.execution.RequestBuilder;
import io.acra.core.active.model.Mutation;
import io.acra.core.active.model.MutationLocation;
import io.acra.core.active.model.MutationType;
import io.acra.core.active.model.SafetyClass;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpRequest;
import io.acra.core.engine.PolicyValidationEvaluator;
import io.acra.core.product.property.S9PropertyWorkspace;
import io.acra.core.property.PropertyAccessObservation;
import io.acra.core.property.S9PropertyCoverageTracker;
import io.acra.core.security.UniversalRedactor;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

public final class Sprint9PropertySecurityHardeningTestSuite {
    private static final String ENDPOINT = "/api/v1/s9/users/user-a/profile";

    private Sprint9PropertySecurityHardeningTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT9_PROPERTY_SECURITY_HARDENING PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> observation("/api/v1/s9/users/user-a/profile?field=is_admin", "is_admin",
                        PolicyValidationEvaluator.PropertyOperation.READ, List.of("e1")),
                "property observation query material rejected");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> observation("/api/v1/s9/users/user-a/profile#admin", "is_admin",
                        PolicyValidationEvaluator.PropertyOperation.READ, List.of("e1")),
                "property observation fragment material rejected");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> observation(ENDPOINT, "is_admin",
                        PolicyValidationEvaluator.PropertyOperation.UPDATE, List.of()),
                "property observation without evidence rejected");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> observation(ENDPOINT, "is_admin",
                        PolicyValidationEvaluator.PropertyOperation.UPDATE, List.of("e1", "e1")),
                "duplicate property evidence references rejected");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> observation(ENDPOINT, "Authorization: Bearer s9-secret-token",
                        PolicyValidationEvaluator.PropertyOperation.UPDATE, List.of("e1")),
                "secret-bearing property metadata rejected");
        assertions++;

        S9PropertyCoverageTracker coverage = new S9PropertyCoverageTracker();
        var updatePolicy = policy("s9-sec-policy", "is_admin",
                PolicyValidationEvaluator.PropertyOperation.UPDATE, AuthorizationDecision.DENY);
        coverage.recordPolicy(updatePolicy);

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> coverage.recordObservation(updatePolicy,
                        observation(ENDPOINT, "is_admin",
                                PolicyValidationEvaluator.PropertyOperation.READ, List.of("e-read"))),
                "operation-mismatched observation rejected by coverage tracker");
        assertions++;

        var drifted = policy("s9-sec-policy", "is_admin",
                PolicyValidationEvaluator.PropertyOperation.UPDATE, AuthorizationDecision.ALLOW);
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> coverage.recordPolicy(drifted),
                "policy decision drift for existing coverage identity rejected");
        assertions++;

        HttpRequest baseline = HttpRequest.of(
                HttpMethod.PATCH,
                "http",
                "localhost",
                18082,
                ENDPOINT,
                List.of(),
                "{\"is_admin\":false}".getBytes(StandardCharsets.UTF_8),
                HttpProtocol.HTTP_1_1);
        HttpRequest mutated = new RequestBuilder().mutate(baseline, propertyMutation());
        TestSupport.assertContains(new String(mutated.body(), StandardCharsets.UTF_8), "\"is_admin\":true",
                "declared property body mutation changes exactly one explicit property occurrence");
        assertions++;

        HttpRequest duplicate = HttpRequest.of(
                HttpMethod.PATCH,
                "http",
                "localhost",
                18082,
                ENDPOINT,
                List.of(),
                "{\"is_admin\":false,\"nested\":{\"is_admin\":false}}".getBytes(StandardCharsets.UTF_8),
                HttpProtocol.HTTP_1_1);
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new RequestBuilder().mutate(duplicate, propertyMutation()),
                "property mutation with multiple matching body occurrences fails closed");
        assertions++;

        TestSupport.assertEquals(SafetyClass.STATE_CHANGING, propertyMutation().safetyClass(),
                "privileged property mutation remains explicitly state-changing");
        assertions++;

        S9PropertyWorkspace workspace = new S9PropertyWorkspace();
        FindingCandidate nonProperty = new FindingCandidate(
                "fc-non-property",
                FindingCandidateState.CANDIDATE,
                "project",
                List.of("test"),
                List.of("execution"),
                List.of("observation"),
                List.of("assessment"),
                List.of("WORKFLOW"),
                ENDPOINT,
                "user-a",
                "user-a",
                "tenant-a",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("e1"),
                List.of(),
                List.of("policy"),
                "HIGH",
                "not a property projection",
                FindingFingerprint.of(ENDPOINT, "user-a", "user-a", "tenant-a", "WORKFLOW", "CANDIDATE"));
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> workspace.recordCandidate(nonProperty),
                "property workspace rejects non-property finding projections");
        assertions++;

        TestSupport.assertEquals(0,
                workspace.report(Instant.parse("2026-09-24T13:10:00Z")).summary().confirmedFindingCount(),
                "property report cannot auto-confirm findings");
        assertions++;

        String secret = "s9-redactor-secret";
        String embedded = "{\"rationale\":\"Authorization: Bearer " + secret
                + "; review-only\",\"reportVersion\":\"s9-property-report-v1\","
                + "\"summary\":{\"confirmedFindingCount\":0}}";
        String redacted = new UniversalRedactor().redactText(embedded);
        TestSupport.assertNotContains(redacted, secret,
                "embedded bearer secret is redacted");
        assertions++;
        TestSupport.assertContains(redacted, "s9-property-report-v1",
                "redaction preserves subsequent property report fields");
        assertions++;

        return assertions;
    }

    private static PropertyAccessObservation observation(
            String endpoint,
            String property,
            PolicyValidationEvaluator.PropertyOperation operation,
            List<String> evidence) {
        return new PropertyAccessObservation(
                "s9-sec-observation",
                "s9-sec-execution",
                "s9-sec-test",
                endpoint,
                property,
                operation,
                AuthorizationDecision.ALLOW,
                evidence);
    }

    private static PolicyValidationEvaluator.PropertyPolicy policy(
            String reference,
            String property,
            PolicyValidationEvaluator.PropertyOperation operation,
            AuthorizationDecision expected) {
        return new PolicyValidationEvaluator.PropertyPolicy(
                reference,
                "s9-security-fixture",
                ENDPOINT,
                property,
                operation,
                "viewer",
                "tenant-a",
                expected,
                List.of("e-policy"));
    }

    private static Mutation propertyMutation() {
        return new Mutation(
                "S9-SEC-PROPERTY-MUTATION",
                MutationType.PROPERTY,
                MutationLocation.BODY,
                "\"is_admin\":false",
                "\"is_admin\":true",
                "viewer-property-context",
                "viewer-property-context",
                "controlled explicit privileged-property mutation",
                "authorization decision must remain DENY",
                SafetyClass.STATE_CHANGING,
                "s9-sec-property-is-admin");
    }
}
