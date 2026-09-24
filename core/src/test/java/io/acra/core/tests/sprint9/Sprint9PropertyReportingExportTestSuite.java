package io.acra.core.tests.sprint9;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.PolicyValidationState;
import io.acra.core.domain.authorization.PropertyAuthorizationAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.engine.PolicyValidationEvaluator;
import io.acra.core.product.property.S9PropertyWorkspace;
import io.acra.core.property.PropertyAccessObservation;
import io.acra.core.property.S9PropertyCoverageTracker;
import io.acra.core.reporting.s9.S9PropertyJsonReporter;
import io.acra.core.reporting.s9.S9PropertyReportStatus;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Sprint9PropertyReportingExportTestSuite {
    private static final String ENDPOINT = "/api/v1/s9/users/user-a/profile";
    private static final Instant GENERATED_AT = Instant.parse("2026-09-24T12:55:00Z");

    private Sprint9PropertyReportingExportTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT9_PROPERTY_REPORTING_EXPORT PASS assertions=" + assertions);
    }

    public static int run() {
        S9PropertyWorkspace workspace = fixtureWorkspace();
        var report = workspace.report(GENERATED_AT);
        int assertions = 0;

        TestSupport.assertEquals(S9PropertyReportStatus.READY_FOR_REVIEW, report.status(),
                "non-empty property workspace produces a review-ready report");
        assertions++;
        TestSupport.assertEquals(3, report.summary().policyContextCount(),
                "report preserves explicit property-policy denominator");
        assertions++;
        TestSupport.assertEquals(1, report.summary().readPolicyContextCount(),
                "report preserves READ coverage");
        assertions++;
        TestSupport.assertEquals(2, report.summary().updatePolicyContextCount(),
                "report preserves UPDATE coverage");
        assertions++;
        TestSupport.assertEquals(2, report.summary().observedContextCount(),
                "report preserves observed property contexts");
        assertions++;
        TestSupport.assertEquals(2, report.summary().assessedContextCount(),
                "report preserves assessed property contexts");
        assertions++;
        TestSupport.assertEquals(1, report.summary().unobservedContextCount(),
                "unobserved property policy remains visible");
        assertions++;
        TestSupport.assertEquals(1, report.summary().findingCandidateCount(),
                "one review-only property candidate is counted");
        assertions++;
        TestSupport.assertEquals(1, report.summary().rejectedControlCount(),
                "one secure property control is counted as rejected");
        assertions++;
        TestSupport.assertEquals(0, report.summary().confirmedFindingCount(),
                "Sprint 9 reporting cannot auto-confirm vulnerabilities");
        assertions++;

        var jsonA = workspace.exportJson(GENERATED_AT);
        var jsonB = workspace.exportJson(GENERATED_AT);
        TestSupport.assertEquals(jsonA.content(), jsonB.content(),
                "canonical property JSON export is deterministic");
        assertions++;
        TestSupport.assertEquals(jsonA.sha256(), jsonB.sha256(),
                "deterministic property JSON export produces stable digest");
        assertions++;
        TestSupport.assertEquals(TokenFingerprint.sha256(jsonA.content()), jsonA.sha256(),
                "property JSON SHA-256 matches exported content");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"confirmedFindingCount\":0",
                "canonical JSON preserves zero confirmed findings");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"unobservedContextCount\":1",
                "canonical JSON preserves unobserved coverage");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "L2",
                "property JSON does not expose synthetic property values");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "User A Updated",
                "property JSON does not expose mutable profile values");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "DummyPassword",
                "property JSON excludes raw secret material");
        assertions++;

        var markdown = workspace.exportMarkdown(GENERATED_AT);
        TestSupport.assertContains(markdown.content(), "Confirmed findings: 0",
                "Markdown preserves review-only boundary");
        assertions++;
        TestSupport.assertContains(markdown.content(), "Unobserved contexts: 1",
                "Markdown preserves coverage gaps");
        assertions++;
        TestSupport.assertContains(markdown.content(), "property=is_admin",
                "Markdown identifies the explicit property policy without rendering property values");
        assertions++;
        TestSupport.assertNotContains(markdown.content(), "L2",
                "Markdown does not expose synthetic property values");
        assertions++;
        TestSupport.assertNotContains(markdown.content(), "DummyPassword",
                "Markdown remains secret-safe");
        assertions++;

        S9PropertyJsonReporter reporter = new S9PropertyJsonReporter();
        TestSupport.assertEquals("s9-property-json-v1", reporter.id(),
                "Reporter plugin exposes stable Sprint 9 identifier");
        assertions++;
        TestSupport.assertEquals(jsonA.content(), reporter.render(Map.of("report", report)),
                "Reporter plugin uses the canonical Sprint 9 JSON exporter");
        assertions++;

        var repeated = workspace.report(GENERATED_AT);
        TestSupport.assertEquals(report.reportId(), repeated.reportId(),
                "report identity is deterministic for the same workspace state");
        assertions++;

        return assertions;
    }

    private static S9PropertyWorkspace fixtureWorkspace() {
        S9PropertyWorkspace workspace = new S9PropertyWorkspace();
        S9PropertyCoverageTracker coverage = new S9PropertyCoverageTracker();

        var salaryRead = policy(
                "s9-report-salary-read", "salary_band",
                PolicyValidationEvaluator.PropertyOperation.READ, AuthorizationDecision.DENY);
        var adminUpdate = policy(
                "s9-report-admin-update", "is_admin",
                PolicyValidationEvaluator.PropertyOperation.UPDATE, AuthorizationDecision.DENY);
        var displayUpdate = policy(
                "s9-report-display-update", "display_name",
                PolicyValidationEvaluator.PropertyOperation.UPDATE, AuthorizationDecision.ALLOW);

        for (var policy : List.of(salaryRead, adminUpdate, displayUpdate)) {
            workspace.recordPolicy(policy);
            coverage.recordPolicy(policy);
        }

        PropertyAccessObservation salaryObservation = observation(
                "s9-report-obs-salary", "salary_band",
                PolicyValidationEvaluator.PropertyOperation.READ, AuthorizationDecision.DENY);
        PropertyAuthorizationAssessment salaryAssessment = assessment(
                "s9-report-assessment-salary", salaryRead,
                AuthorizationDecision.DENY, PolicyValidationState.DENIED);
        FindingCandidate salaryFinding = finding(
                "s9-report-finding-salary", salaryAssessment, FindingCandidateState.REJECTED);
        workspace.recordObservation(salaryObservation);
        workspace.recordAssessment(salaryAssessment);
        workspace.recordCandidate(salaryFinding);
        coverage.recordObservation(salaryRead, salaryObservation);
        coverage.recordAssessment(salaryRead, salaryObservation, salaryAssessment, salaryFinding);

        PropertyAccessObservation adminObservation = observation(
                "s9-report-obs-admin", "is_admin",
                PolicyValidationEvaluator.PropertyOperation.UPDATE, AuthorizationDecision.ALLOW);
        PropertyAuthorizationAssessment adminAssessment = assessment(
                "s9-report-assessment-admin", adminUpdate,
                AuthorizationDecision.ALLOW, PolicyValidationState.CONFLICTING);
        FindingCandidate adminFinding = finding(
                "s9-report-finding-admin", adminAssessment, FindingCandidateState.CANDIDATE);
        workspace.recordObservation(adminObservation);
        workspace.recordAssessment(adminAssessment);
        workspace.recordCandidate(adminFinding);
        coverage.recordObservation(adminUpdate, adminObservation);
        coverage.recordAssessment(adminUpdate, adminObservation, adminAssessment, adminFinding);

        workspace.replaceCoverage(coverage);
        return workspace;
    }

    private static PolicyValidationEvaluator.PropertyPolicy policy(
            String reference,
            String property,
            PolicyValidationEvaluator.PropertyOperation operation,
            AuthorizationDecision expected) {
        return new PolicyValidationEvaluator.PropertyPolicy(
                reference,
                "GT-S9-PROPERTY-AUTHORIZATION password=DummyPassword",
                ENDPOINT,
                property,
                operation,
                "viewer",
                "tenant-a",
                expected,
                List.of("evidence-" + reference));
    }

    private static PropertyAccessObservation observation(
            String id,
            String property,
            PolicyValidationEvaluator.PropertyOperation operation,
            AuthorizationDecision observed) {
        return new PropertyAccessObservation(
                id,
                "s9-report-execution",
                "s9-report-test",
                ENDPOINT,
                property,
                operation,
                observed,
                List.of("evidence-" + id));
    }

    private static PropertyAuthorizationAssessment assessment(
            String id,
            PolicyValidationEvaluator.PropertyPolicy policy,
            AuthorizationDecision observed,
            PolicyValidationState state) {
        return new PropertyAuthorizationAssessment(
                id,
                policy.endpoint(),
                policy.property(),
                policy.operation().name(),
                policy.roleId(),
                policy.tenantId(),
                policy.policyReference(),
                policy.expectedDecision(),
                observed,
                state,
                state == PolicyValidationState.INCONCLUSIVE ? "INSUFFICIENT" : "HIGH",
                List.of("evidence-" + id),
                "Sprint 9 reporting fixture",
                List.of());
    }

    private static FindingCandidate finding(
            String id,
            PropertyAuthorizationAssessment assessment,
            FindingCandidateState state) {
        return new FindingCandidate(
                id,
                state,
                "acra-s9-report",
                List.of("s9-report-test"),
                List.of("s9-report-execution"),
                List.of("s9-report-observation"),
                List.of(assessment.assessmentId()),
                List.of("PROPERTY", "PROPERTY_" + assessment.operation()),
                assessment.endpoint(),
                "user-a#" + assessment.property(),
                "user-a",
                "tenant-a",
                assessment.expectedDecision(),
                assessment.observedDecision(),
                assessment.evidenceIds(),
                List.of(),
                List.of(assessment.policyReference()),
                state == FindingCandidateState.CANDIDATE ? "HIGH" : "MEDIUM",
                "Sprint 9 report fixture; password=DummyPassword; candidate is not an automatically confirmed vulnerability",
                FindingFingerprint.of(
                        assessment.endpoint(),
                        "user-a#" + assessment.property(),
                        "user-a",
                        "tenant-a",
                        "PROPERTY_" + assessment.operation(),
                        state.name()));
    }
}
