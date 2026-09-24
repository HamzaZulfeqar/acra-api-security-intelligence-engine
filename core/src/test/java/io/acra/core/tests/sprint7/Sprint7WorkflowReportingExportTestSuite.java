package io.acra.core.tests.sprint7;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationRuleEffect;
import io.acra.core.domain.authorization.PolicyResolutionState;
import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.domain.workflow.S7WorkflowAnalysisResult;
import io.acra.core.domain.workflow.WorkflowAuthorizationResolution;
import io.acra.core.domain.workflow.WorkflowPolicySnapshot;
import io.acra.core.domain.workflow.WorkflowTransitionAssessment;
import io.acra.core.domain.workflow.WorkflowTransitionAssessmentState;
import io.acra.core.domain.workflow.WorkflowTransitionCoverageEntry;
import io.acra.core.domain.workflow.WorkflowTransitionRule;
import io.acra.core.product.workflow.S7WorkflowWorkspace;
import io.acra.core.reporting.s7.S7WorkflowJsonReporter;
import io.acra.core.reporting.s7.S7WorkflowReportStatus;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Sprint7WorkflowReportingExportTestSuite {
    private static final Instant NOW = Instant.parse("2026-09-24T06:30:00Z");
    private static final String SECRET = "report-secret-12345";

    private Sprint7WorkflowReportingExportTestSuite() { }

    public static void main(String[] args) throws Exception {
        int assertions = run();
        System.out.println("SPRINT7_WORKFLOW_REPORTING_EXPORT PASS assertions=" + assertions);
    }

    public static int run() throws Exception {
        int assertions = 0;
        S7WorkflowWorkspace workspace = fixture();
        var report = workspace.report(NOW);

        TestSupport.assertEquals(S7WorkflowReportStatus.READY_FOR_REVIEW, report.status(),
                "workflow report should be review-ready when policy is loaded");
        assertions++;
        TestSupport.assertEquals(2, report.summary().resolutionCount(), "resolution count");
        assertions++;
        TestSupport.assertEquals(1, report.summary().expectedDenyCount(), "expected deny count");
        assertions++;
        TestSupport.assertEquals(1, report.summary().conflictCount(), "conflict count");
        assertions++;
        TestSupport.assertEquals(1, report.summary().assessmentCandidateCount(), "assessment candidate count");
        assertions++;
        TestSupport.assertEquals(1, report.summary().findingCandidateCount(), "finding candidate count");
        assertions++;
        TestSupport.assertEquals(0, report.summary().confirmedFindingCount(),
                "workflow report must never auto-promote a confirmed finding");
        assertions++;
        TestSupport.assertEquals(2, report.summary().coverageContextCount(), "coverage context count");
        assertions++;
        TestSupport.assertEquals(1, report.summary().observedCoverageCount(), "observed coverage count");
        assertions++;
        TestSupport.assertEquals(0.5, report.summary().observationCoverageRatio(),
                "observation ratio is based on resolved coverage contexts only");
        assertions++;
        TestSupport.assertTrue(report.evidenceIds().containsAll(List.of(
                "e-policy", "e-rule", "e-resolution", "e-assessment", "e-candidate", "e-observation")),
                "workflow report aggregates policy, resolution, assessment, candidate and observation evidence");
        assertions++;

        var json1 = workspace.exportJson(NOW);
        var json2 = workspace.exportJson(NOW);
        TestSupport.assertEquals(json1.content(), json2.content(), "workflow JSON export deterministic");
        assertions++;
        TestSupport.assertEquals(json1.sha256(), json2.sha256(), "workflow JSON digest deterministic");
        assertions++;
        TestSupport.assertTrue(json1.content().contains("s7-workflow-report-v1"), "versioned workflow JSON");
        assertions++;
        TestSupport.assertTrue(json1.content().contains("\"confirmedFindingCount\":0"),
                "workflow JSON preserves review-only finding boundary");
        assertions++;
        TestSupport.assertTrue(json1.content().contains("s7-coverage-"),
                "workflow JSON exports coverage lifecycle records");
        assertions++;
        TestSupport.assertNotContains(json1.content(), SECRET, "workflow JSON excludes raw secret");
        assertions++;
        TestSupport.assertTrue(json1.content().contains("<redacted>"), "workflow JSON contains redaction marker");
        assertions++;

        var markdown = workspace.exportMarkdown(NOW);
        TestSupport.assertTrue(markdown.content().contains("ACRA Sprint 7 Workflow Authorization Report"),
                "workflow Markdown report heading");
        assertions++;
        TestSupport.assertTrue(markdown.content().contains("Finding Candidates - Review Only"),
                "workflow Markdown preserves candidate review boundary");
        assertions++;
        TestSupport.assertTrue(markdown.content().contains("Confirmed findings: 0"),
                "workflow Markdown reports zero confirmed findings");
        assertions++;
        TestSupport.assertTrue(markdown.content().contains("Workflow Coverage"),
                "workflow Markdown includes coverage lifecycle section");
        assertions++;
        TestSupport.assertNotContains(markdown.content(), SECRET, "workflow Markdown excludes raw secret");
        assertions++;

        TestSupport.assertEquals(json1.content(),
                new S7WorkflowJsonReporter().render(Map.of("report", report)),
                "Reporter plugin adapter uses canonical workflow JSON export");
        assertions++;

        S7WorkflowWorkspace empty = new S7WorkflowWorkspace();
        var emptyReport = empty.report(NOW);
        TestSupport.assertEquals(S7WorkflowReportStatus.POLICY_NOT_LOADED, emptyReport.status(),
                "empty workflow workspace reports policy-not-loaded");
        assertions++;
        TestSupport.assertTrue(emptyReport.limitations().stream()
                        .anyMatch(value -> value.contains("Workflow policy is not loaded")),
                "empty workflow report states policy limitation");
        assertions++;

        String output = System.getenv("ACRA_S7_REPORT_OUTPUT_DIR");
        if (output != null && !output.isBlank()) {
            Path directory = Path.of(output);
            Files.createDirectories(directory);
            Files.writeString(directory.resolve("S7-WORKFLOW-REPORT.json"), json1.content(), StandardCharsets.UTF_8);
            Files.writeString(directory.resolve("S7-WORKFLOW-REPORT.json.sha256"),
                    json1.sha256() + "  S7-WORKFLOW-REPORT.json\n", StandardCharsets.UTF_8);
            Files.writeString(directory.resolve("S7-WORKFLOW-REPORT.md"),
                    markdown.content(), StandardCharsets.UTF_8);
            System.out.println("SPRINT7_WORKFLOW_REPORT_ARTIFACT " + directory);
        }
        return assertions;
    }

    private static S7WorkflowWorkspace fixture() {
        S7WorkflowWorkspace workspace = new S7WorkflowWorkspace();
        WorkflowPolicySnapshot policy = WorkflowPolicySnapshot.create(
                "s7-report-policy",
                "1",
                "controlled-report-fixture",
                List.of(new WorkflowTransitionRule(
                        "deny-approve",
                        "document-approval",
                        "SUBMITTED",
                        "APPROVED",
                        "APPROVE",
                        "tenant-a",
                        List.of("approver"),
                        true,
                        true,
                        false,
                        false,
                        "",
                        AuthorizationRuleEffect.DENY,
                        null,
                        "",
                        List.of("e-rule"))),
                List.of(),
                List.of("e-policy"),
                AuthorizationDecision.DENY,
                NOW);
        workspace.loadPolicy(policy);

        WorkflowAuthorizationResolution candidateResolution = new WorkflowAuthorizationResolution(
                "resolution-report-candidate",
                policy.fingerprint(),
                "document-approval",
                "author-a",
                "tenant-a",
                "document-1",
                "APPROVE",
                "SUBMITTED",
                "APPROVED",
                List.of("deny-approve"),
                List.of(),
                List.of(),
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                PolicyResolutionState.RESOLVED_DENY,
                List.of("e-policy", "e-rule", "e-resolution"),
                List.of("expected deny observed allow"));

        WorkflowAuthorizationResolution conflictResolution = new WorkflowAuthorizationResolution(
                "resolution-report-conflict",
                policy.fingerprint(),
                "document-approval",
                "approver-a",
                "tenant-a",
                "document-2",
                "APPROVE",
                "SUBMITTED",
                "APPROVED",
                List.of("deny-approve"),
                List.of(),
                List.of(),
                AuthorizationDecision.UNKNOWN,
                AuthorizationDecision.UNKNOWN,
                PolicyResolutionState.CONFLICTING,
                List.of("e-policy", "e-rule", "e-conflict"),
                List.of("unproven precedence"));

        WorkflowTransitionAssessment assessment = new WorkflowTransitionAssessment(
                "assessment-report-1",
                "document-approval",
                "APPROVE",
                "SUBMITTED",
                "APPROVED",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                WorkflowTransitionAssessmentState.CANDIDATE,
                List.of("e-assessment", "e-resolution"),
                List.of("expected deny observed allow"),
                "controlled workflow transition mismatch");

        FindingCandidate candidate = new FindingCandidate(
                "candidate-report-s7",
                FindingCandidateState.CANDIDATE,
                "acra-s7",
                List.of("S7-REPORT-TEST"),
                List.of("S7-REPORT-EXEC"),
                List.of("S7-REPORT-OBS"),
                List.of(assessment.assessmentId()),
                List.of("WORKFLOW", "APPROVAL"),
                "/api/v1/s7/workflows/document-approval/resources/document-1/transition",
                "document-1",
                "author-a",
                "SAME_TENANT",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("e-candidate", "e-observation"),
                List.of(),
                List.of(policy.fingerprint()),
                "HIGH",
                "token=" + SECRET,
                FindingFingerprint.of(
                        "/api/v1/s7/workflows/document-approval/resources/document-1/transition",
                        "document-1",
                        "author-a",
                        "SAME_TENANT",
                        "WORKFLOW",
                        "POLICY_MISMATCH"));

        AuthorizationRiskAssessment risk = new AuthorizationRiskAssessment(
                "risk-report-s7",
                candidate.candidateId(),
                FindingSeverity.MEDIUM,
                FindingConfidence.HIGH,
                55,
                "workflow integrity review",
                List.of("expected deny observed allow"));

        workspace.recordAnalysis(new S7WorkflowAnalysisResult(candidateResolution, assessment, candidate, risk));
        workspace.recordResolution(conflictResolution);
        workspace.recordCoverage(WorkflowTransitionCoverageEntry.from(candidateResolution)
                .withPlannedTest("S7-REPORT-TEST")
                .withExecution("S7-REPORT-EXEC", "S7-REPORT-OBS", List.of("e-observation")));
        workspace.recordCoverage(WorkflowTransitionCoverageEntry.from(conflictResolution));
        return workspace;
    }
}
