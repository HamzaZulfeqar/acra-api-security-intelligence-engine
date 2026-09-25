package io.acra.burp.tests.sprint12;

import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;
import io.acra.burp.scanner.BurpIssueDraftAdapter;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.reporting.reproduction.BurpIssueDraftFactory;
import io.acra.core.reporting.reproduction.ReproductionPackageFactory;
import java.util.List;

public final class Sprint12BurpIssueAdapterRuntimeTestSuite {
    private Sprint12BurpIssueAdapterRuntimeTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT12_BURP_ISSUE_ADAPTER_RUNTIME PASS assertions=" + assertions);
    }

    public static int run() {
        var reproduction = new ReproductionPackageFactory().from(candidate());
        var draft = new BurpIssueDraftFactory().from(reproduction);
        BurpIssueDraftAdapter adapter = new BurpIssueDraftAdapter();
        var projection = adapter.project(draft, "https://example.test");
        int assertions = 0;

        check(projection.name().equals(draft.name()), "projection name matches draft");
        assertions++;
        check(projection.detail().equals(draft.detail()), "projection detail matches draft");
        assertions++;
        check(projection.remediation().equals(draft.remediation()), "projection remediation matches draft");
        assertions++;
        check(projection.baseUrl().equals("https://example.test/api/v1/documents/resource-b"),
                "relative draft path resolves against absolute base URL");
        assertions++;
        check(projection.severity() == AuditIssueSeverity.INFORMATION,
                "projection remains informational");
        assertions++;
        check(projection.typicalSeverity() == AuditIssueSeverity.INFORMATION,
                "typical severity remains informational");
        assertions++;
        check(projection.confidence() == AuditIssueConfidence.TENTATIVE,
                "projection remains tentative");
        assertions++;
        check(projection.requestResponses().isEmpty(),
                "projection without evidence messages attaches no request/response objects");
        assertions++;
        check(adapter.status().equals("PROJECTION_ONLY_NOT_SUBMITTED"),
                "adapter status explicitly records non-submission");
        assertions++;
        check(!projection.detail().contains("DummyPassword"),
                "projection detail excludes rationale secret");
        assertions++;
        check(!projection.detail().contains("user-a"),
                "projection detail excludes raw principal");
        assertions++;
        check(!projection.detail().contains("tenant-a"),
                "projection detail excludes raw tenant");
        assertions++;
        check(projection.detail().contains("Expected decision: DENY"),
                "projection preserves expected decision");
        assertions++;
        check(projection.detail().contains("Observed decision: ALLOW"),
                "projection preserves observed decision");
        assertions++;

        expectIllegalArgument(
                () -> adapter.project(draft, "/relative-base"),
                "relative base URL fails closed");
        assertions++;
        expectIllegalArgument(
                () -> adapter.project(draft, "file:///tmp/example"),
                "non-HTTP base URL fails closed");
        assertions++;

        expectBurpRuntimeRequired(
                () -> adapter.toAuditIssue(draft, "https://example.test"),
                "standalone Montoya API must not be mistaken for real Burp runtime");
        assertions++;

        return assertions;
    }

    private static FindingCandidate candidate() {
        return new FindingCandidate(
                "s12-runtime-candidate",
                FindingCandidateState.CANDIDATE,
                "s12-project",
                List.of("s12-test"),
                List.of("s12-execution"),
                List.of("s12-observation"),
                List.of("s12-assessment"),
                List.of("BATCH_AUTHORIZATION"),
                "/api/v1/documents/resource-b",
                "resource-b",
                "user-a",
                "tenant-a",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("evidence-runtime"),
                List.of(),
                List.of("policy-runtime"),
                "HIGH",
                "password=DummyPassword",
                FindingFingerprint.of(
                        "/api/v1/documents/resource-b",
                        "resource-b",
                        "user-a",
                        "tenant-a",
                        "BATCH_AUTHORIZATION",
                        "CANDIDATE"));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectIllegalArgument(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private static void expectBurpRuntimeRequired(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalStateException expected) {
            check(expected.getMessage().contains("real Burp runtime required"),
                    "runtime guard explains the missing Burp object factory");
        }
    }
}
