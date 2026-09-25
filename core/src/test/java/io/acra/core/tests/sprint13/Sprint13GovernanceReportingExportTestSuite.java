package io.acra.core.tests.sprint13;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.AuthorizationImpactProfile;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingLifecycleAction;
import io.acra.core.domain.finding.FindingLifecycleTransitionRequest;
import io.acra.core.engine.AuthorizationSeverityEvaluator;
import io.acra.core.product.finding.FindingGovernanceWorkspace;
import io.acra.core.reporting.s13.S13GovernanceJsonReporter;
import io.acra.core.reporting.s13.S13GovernanceReportStatus;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Sprint13GovernanceReportingExportTestSuite {
    private static final Instant GENERATED_AT = Instant.parse("2026-09-25T00:55:00Z");

    private Sprint13GovernanceReportingExportTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT13_GOVERNANCE_REPORTING_EXPORT PASS assertions=" + assertions);
    }

    public static int run() {
        FindingGovernanceWorkspace workspace = fixtureWorkspace();
        var before = workspace.snapshot();
        var report = workspace.report(GENERATED_AT);
        int assertions = 0;

        TestSupport.assertEquals(S13GovernanceReportStatus.READY_FOR_REVIEW, report.status(),
                "non-empty governance workspace produces review-ready report");
        assertions++;
        TestSupport.assertEquals("s13-governance-report-v1", report.reportVersion(),
                "governance report version is explicit");
        assertions++;
        TestSupport.assertEquals(5, report.summary().findingCount(),
                "report preserves five governed findings");
        assertions++;
        TestSupport.assertEquals(1L, report.summary().reviewRequiredCount(),
                "report preserves review-required queue");
        assertions++;
        TestSupport.assertEquals(1L, report.summary().confirmedCount(),
                "report preserves confirmed queue");
        assertions++;
        TestSupport.assertEquals(1L, report.summary().remediationCount(),
                "report preserves remediation queue");
        assertions++;
        TestSupport.assertEquals(1L, report.summary().retestCount(),
                "report preserves retest queue");
        assertions++;
        TestSupport.assertEquals(1L, report.summary().terminalCount(),
                "report preserves terminal queue");
        assertions++;
        TestSupport.assertEquals(3L, report.summary().confirmedHistoryCount(),
                "report preserves historical confirmation independently of terminal state");
        assertions++;
        TestSupport.assertEquals(7, report.summary().lifecycleEventCount(),
                "report preserves append-only lifecycle event denominator");
        assertions++;
        TestSupport.assertEquals(5, report.findings().size(),
                "report emits five minimized finding projections");
        assertions++;
        TestSupport.assertEquals(7, report.events().size(),
                "report emits seven lifecycle event projections");
        assertions++;

        var sameStateDifferentTime = workspace.report(GENERATED_AT.plusSeconds(120));
        TestSupport.assertEquals(report.reportId(), sameStateDifferentTime.reportId(),
                "report identity is independent of render timestamp");
        assertions++;
        TestSupport.assertEquals(
                report.findings().stream().map(value -> value.findingId()).toList(),
                sameStateDifferentTime.findings().stream().map(value -> value.findingId()).toList(),
                "report finding ordering is deterministic");
        assertions++;
        TestSupport.assertEquals(
                report.events().stream().map(value -> value.eventId()).toList(),
                sameStateDifferentTime.events().stream().map(value -> value.eventId()).toList(),
                "report event ordering is deterministic");
        assertions++;

        var jsonA = workspace.exportJson(GENERATED_AT);
        var jsonB = workspace.exportJson(GENERATED_AT);
        TestSupport.assertEquals(jsonA.content(), jsonB.content(),
                "canonical governance JSON export is deterministic");
        assertions++;
        TestSupport.assertEquals(jsonA.sha256(), jsonB.sha256(),
                "canonical governance JSON digest is deterministic");
        assertions++;
        TestSupport.assertEquals(TokenFingerprint.sha256(jsonA.content()), jsonA.sha256(),
                "JSON SHA-256 matches exported content");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"reportVersion\":\"s13-governance-report-v1\"",
                "JSON contains stable governance report version");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"confirmedHistoryCount\":3",
                "JSON preserves confirmed-history count");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"lifecycleEventCount\":7",
                "JSON preserves lifecycle event count");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"action\":\"CONFIRM\"",
                "JSON preserves explicit confirmation history");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "DummyPassword",
                "JSON excludes source-candidate rationale secret material");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "\"principalId\"",
                "JSON contains no raw candidate principal field");
        assertions++;

        var markdownA = workspace.exportMarkdown(GENERATED_AT);
        var markdownB = workspace.exportMarkdown(GENERATED_AT);
        TestSupport.assertEquals(markdownA.content(), markdownB.content(),
                "canonical governance Markdown export is deterministic");
        assertions++;
        TestSupport.assertEquals(markdownA.sha256(), markdownB.sha256(),
                "canonical governance Markdown digest is deterministic");
        assertions++;
        TestSupport.assertContains(markdownA.content(), "Historically confirmed: 3",
                "Markdown preserves confirmed-history count");
        assertions++;
        TestSupport.assertContains(markdownA.content(), "Lifecycle events: 7",
                "Markdown preserves event count");
        assertions++;
        TestSupport.assertContains(markdownA.content(), "CONFIRM",
                "Markdown renders lifecycle action history");
        assertions++;
        TestSupport.assertContains(markdownA.content(),
                "severity/confidence do not confirm findings",
                "Markdown preserves lifecycle trust boundary");
        assertions++;
        TestSupport.assertNotContains(markdownA.content(), "DummyPassword",
                "Markdown excludes source-candidate rationale secret material");
        assertions++;

        S13GovernanceJsonReporter reporter = new S13GovernanceJsonReporter();
        TestSupport.assertEquals("s13-governance-json-v1", reporter.id(),
                "Reporter plugin exposes stable Sprint 13 identifier");
        assertions++;
        TestSupport.assertEquals(jsonA.content(), reporter.render(Map.of("report", report)),
                "Reporter plugin uses canonical governance JSON exporter");
        assertions++;

        var after = workspace.snapshot();
        TestSupport.assertEquals(
                before.findings().stream().map(value -> value.fingerprint()).toList(),
                after.findings().stream().map(value -> value.fingerprint()).toList(),
                "report/export does not mutate governed finding state");
        assertions++;

        FindingGovernanceWorkspace empty = new FindingGovernanceWorkspace();
        TestSupport.assertEquals(
                S13GovernanceReportStatus.NO_GOVERNED_FINDINGS,
                empty.report(GENERATED_AT).status(),
                "empty governance workspace reports explicit no-findings state");
        assertions++;

        try {
            Path out = Path.of("build", "s13-foundation", "reporting");
            Files.createDirectories(out);
            Files.writeString(
                    out.resolve("S13-GOVERNANCE-REPORT.json"),
                    jsonA.content(), StandardCharsets.UTF_8);
            Files.writeString(
                    out.resolve("S13-GOVERNANCE-REPORT.json.sha256"),
                    jsonA.sha256() + "  S13-GOVERNANCE-REPORT.json\n",
                    StandardCharsets.UTF_8);
            Files.writeString(
                    out.resolve("S13-GOVERNANCE-REPORT.md"),
                    markdownA.content(), StandardCharsets.UTF_8);
            TestSupport.assertTrue(
                    Files.isRegularFile(out.resolve("S13-GOVERNANCE-REPORT.json")),
                    "canonical governance JSON artifact written");
            assertions++;
            TestSupport.assertTrue(
                    Files.isRegularFile(out.resolve("S13-GOVERNANCE-REPORT.md")),
                    "canonical governance Markdown artifact written");
            assertions++;
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("unable to write Sprint 13 governance report artifacts", failure);
        }

        return assertions;
    }

    private static FindingGovernanceWorkspace fixtureWorkspace() {
        FindingGovernanceWorkspace workspace = new FindingGovernanceWorkspace();

        open(workspace, "review", "document:1301", true);

        var confirmed = open(workspace, "confirmed", "document:1302", false);
        transition(workspace, confirmed, FindingLifecycleAction.CONFIRM, "confirmed");

        var remediation = open(workspace, "remediation", "document:1303", false);
        remediation = transition(workspace, remediation, FindingLifecycleAction.CONFIRM, "remediation-confirm");
        transition(workspace, remediation, FindingLifecycleAction.START_REMEDIATION, "remediation-start");

        var retest = open(workspace, "retest", "document:1304", false);
        retest = transition(workspace, retest, FindingLifecycleAction.CONFIRM, "retest-confirm");
        retest = transition(workspace, retest, FindingLifecycleAction.START_REMEDIATION, "retest-start");
        transition(workspace, retest, FindingLifecycleAction.REQUEST_RETEST, "retest-request");

        var terminal = open(workspace, "terminal", "document:1305", false);
        transition(workspace, terminal, FindingLifecycleAction.MARK_FALSE_POSITIVE, "terminal-fp");

        return workspace;
    }

    private static io.acra.core.domain.finding.GovernedFinding open(
            FindingGovernanceWorkspace workspace,
            String suffix,
            String resource,
            boolean critical) {
        FindingCandidate candidate = candidate("candidate-" + suffix, resource);
        return workspace.open(
                candidate,
                new AuthorizationSeverityEvaluator().evaluate(
                        candidate,
                        critical
                                ? new AuthorizationImpactProfile(
                                        true, true, false, true, true, false,
                                        List.of("critical-review-context"))
                                : AuthorizationImpactProfile.none()));
    }

    private static io.acra.core.domain.finding.GovernedFinding transition(
            FindingGovernanceWorkspace workspace,
            io.acra.core.domain.finding.GovernedFinding finding,
            FindingLifecycleAction action,
            String suffix) {
        return workspace.transition(
                finding.findingId(),
                finding.fingerprint(),
                new FindingLifecycleTransitionRequest(
                        action,
                        "reviewer-" + suffix,
                        "decision-" + suffix,
                        List.of("evidence-" + suffix)));
    }

    private static FindingCandidate candidate(String id, String resource) {
        return new FindingCandidate(
                id,
                FindingCandidateState.CANDIDATE,
                "acra-s13-report",
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
                "Sprint 13 report fixture; password=DummyPassword; review-only",
                FindingFingerprint.of(
                        "/api/v1/documents/{id}",
                        resource,
                        "user-a",
                        "FOREIGN_TENANT",
                        "DENY_TO_ALLOW",
                        "OBJECT_AUTHORIZATION"));
    }
}
