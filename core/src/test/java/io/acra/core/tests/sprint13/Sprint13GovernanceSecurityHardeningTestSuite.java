package io.acra.core.tests.sprint13;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.AuthorizationImpactProfile;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingLifecycleAction;
import io.acra.core.domain.finding.FindingLifecycleState;
import io.acra.core.domain.finding.FindingLifecycleTransitionRequest;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.engine.AuthorizationSeverityEvaluator;
import io.acra.core.product.finding.FindingGovernanceQueue;
import io.acra.core.product.finding.FindingGovernanceWorkspace;
import io.acra.core.reporting.s13.S13GovernanceJsonReporter;
import io.acra.core.reporting.s13.S13GovernanceReportSummary;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Sprint13GovernanceSecurityHardeningTestSuite {
    private Sprint13GovernanceSecurityHardeningTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT13_GOVERNANCE_SECURITY_HARDENING PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> request(
                        FindingLifecycleAction.CONFIRM,
                        "Authorization: Bearer secret-token",
                        "decision-safe",
                        List.of("evidence-safe")),
                "secret-bearing reviewer reference rejected");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> request(
                        FindingLifecycleAction.CONFIRM,
                        "reviewer-safe",
                        "token=decision-secret",
                        List.of("evidence-safe")),
                "secret-bearing decision reference rejected");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> request(
                        FindingLifecycleAction.CONFIRM,
                        "reviewer-safe",
                        "decision-safe",
                        List.of("Cookie: session=secret-cookie")),
                "secret-bearing lifecycle evidence reference rejected");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> request(
                        FindingLifecycleAction.CONFIRM,
                        "reviewer-safe",
                        "decision-safe",
                        List.of()),
                "lifecycle transition without evidence rejected");
        assertions++;

        FindingGovernanceWorkspace workspace = new FindingGovernanceWorkspace();
        FindingCandidate criticalCandidate = candidate("candidate-critical", "document:critical");
        var criticalRisk = new AuthorizationSeverityEvaluator().evaluate(
                criticalCandidate,
                new AuthorizationImpactProfile(
                        true, true, false, true, true, false, List.of("critical-impact")));
        var critical = workspace.open(criticalCandidate, criticalRisk);

        TestSupport.assertEquals(FindingSeverity.CRITICAL, critical.severity(),
                "critical prioritization context is preserved");
        assertions++;
        TestSupport.assertEquals(FindingLifecycleState.REVIEW_REQUIRED, critical.state(),
                "critical severity cannot auto-confirm lifecycle");
        assertions++;
        TestSupport.assertEquals(0L, workspace.snapshot().queueCount(FindingGovernanceQueue.CONFIRMED),
                "critical review candidate does not enter confirmed queue");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> workspace.transition(
                        critical.findingId(),
                        critical.fingerprint(),
                        request(
                                FindingLifecycleAction.START_REMEDIATION,
                                "reviewer-invalid",
                                "decision-invalid",
                                List.of("evidence-invalid"))),
                "invalid transition skips confirmation and fails closed");
        assertions++;

        var confirmed = workspace.transition(
                critical.findingId(),
                critical.fingerprint(),
                request(
                        FindingLifecycleAction.CONFIRM,
                        "reviewer-confirm",
                        "decision-confirm",
                        List.of("evidence-confirm")));

        TestSupport.assertEquals(FindingLifecycleState.CONFIRMED, confirmed.state(),
                "explicit reviewed confirmation succeeds");
        assertions++;
        TestSupport.assertTrue(confirmed.evidenceIds().contains("evidence-confirm"),
                "transition evidence is retained in governed finding");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> workspace.transition(
                        confirmed.findingId(),
                        critical.fingerprint(),
                        request(
                                FindingLifecycleAction.START_REMEDIATION,
                                "reviewer-stale",
                                "decision-stale",
                                List.of("evidence-stale"))),
                "stale finding fingerprint rejected");
        assertions++;

        FindingCandidate driftCandidate = candidate("candidate-critical", "document:changed");
        var driftRisk = new AuthorizationSeverityEvaluator().evaluate(
                driftCandidate, AuthorizationImpactProfile.none());
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> workspace.open(driftCandidate, driftRisk),
                "same candidate ID with different fingerprint rejected");
        assertions++;

        var riskDrift = new AuthorizationSeverityEvaluator().evaluate(
                criticalCandidate, AuthorizationImpactProfile.none());
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> workspace.open(criticalCandidate, riskDrift),
                "same candidate cannot silently drift risk assessment");
        assertions++;

        FindingCandidate fpCandidate = candidate("candidate-fp", "document:fp");
        var fp = workspace.open(
                fpCandidate,
                new AuthorizationSeverityEvaluator().evaluate(
                        fpCandidate, AuthorizationImpactProfile.none()));
        fp = workspace.transition(
                fp.findingId(),
                fp.fingerprint(),
                request(
                        FindingLifecycleAction.MARK_FALSE_POSITIVE,
                        "reviewer-fp",
                        "decision-fp",
                        List.of("evidence-fp")));
        fp = workspace.transition(
                fp.findingId(),
                fp.fingerprint(),
                request(
                        FindingLifecycleAction.CLOSE,
                        "reviewer-close",
                        "decision-close",
                        List.of("evidence-close")));

        TestSupport.assertEquals(FindingLifecycleState.CLOSED, fp.state(),
                "false-positive path can explicitly close");
        assertions++;
        TestSupport.assertTrue(!fp.confirmed(),
                "false-positive closure remains historically unconfirmed");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> workspace.transition(
                        fp.findingId(),
                        fp.fingerprint(),
                        request(
                                FindingLifecycleAction.CONFIRM,
                                "reviewer-reopen",
                                "decision-reopen",
                                List.of("evidence-reopen"))),
                "closed finding cannot silently reopen");
        assertions++;

        var beforeFingerprints = workspace.snapshot().findings().stream()
                .map(value -> value.fingerprint()).toList();
        var report = workspace.report(Instant.parse("2026-09-25T01:00:00Z"));
        var json = workspace.exportJson(Instant.parse("2026-09-25T01:00:00Z"));
        var markdown = workspace.exportMarkdown(Instant.parse("2026-09-25T01:00:00Z"));
        var afterFingerprints = workspace.snapshot().findings().stream()
                .map(value -> value.fingerprint()).toList();

        TestSupport.assertEquals(beforeFingerprints, afterFingerprints,
                "report generation/export cannot mutate lifecycle state");
        assertions++;
        TestSupport.assertContains(json.content(), "\"confirmedHistoryCount\":1",
                "report preserves one historically confirmed finding");
        assertions++;
        TestSupport.assertContains(json.content(), "\"state\":\"CLOSED\"",
                "report preserves explicit terminal lifecycle state");
        assertions++;
        TestSupport.assertNotContains(json.content(), "\"principalId\"",
                "report excludes raw candidate principal schema");
        assertions++;
        TestSupport.assertNotContains(json.content(), "\"rationale\"",
                "report excludes candidate/risk rationale schema");
        assertions++;
        TestSupport.assertNotContains(json.content(), "secret-token",
                "report contains no rejected secret material");
        assertions++;
        TestSupport.assertContains(markdown.content(),
                "severity/confidence do not confirm findings",
                "Markdown preserves lifecycle governance boundary");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new S13GovernanceReportSummary(
                        2, 1, 0, 0, 0, 0, 0, 0),
                "report summary queue denominator mismatch rejected");
        assertions++;

        S13GovernanceJsonReporter reporter = new S13GovernanceJsonReporter();
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> reporter.render(Map.of("wrong", report)),
                "Reporter rejects missing governance report model");
        assertions++;

        return assertions;
    }

    private static FindingLifecycleTransitionRequest request(
            FindingLifecycleAction action,
            String reviewer,
            String decision,
            List<String> evidence) {
        return new FindingLifecycleTransitionRequest(action, reviewer, decision, evidence);
    }

    private static FindingCandidate candidate(String id, String resource) {
        return new FindingCandidate(
                id,
                FindingCandidateState.CANDIDATE,
                "acra-s13-hardening",
                List.of("test-" + id),
                List.of("execution-" + id),
                List.of("observation-" + id),
                List.of("assessment-" + id),
                List.of("OBJECT_AUTHORIZATION"),
                "/api/v1/documents/{id}",
                resource,
                "user-a",
                "FOREIGN_TENANT",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("evidence-" + id),
                List.of(),
                List.of("policy-" + id),
                "HIGH",
                "Sprint 13 hardening review-only fixture",
                FindingFingerprint.of(
                        "/api/v1/documents/{id}",
                        resource,
                        "user-a",
                        "FOREIGN_TENANT",
                        "DENY_TO_ALLOW",
                        "OBJECT_AUTHORIZATION"));
    }
}
