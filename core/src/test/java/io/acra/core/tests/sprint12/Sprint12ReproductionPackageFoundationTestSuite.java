package io.acra.core.tests.sprint12;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationReport;
import io.acra.core.domain.authorization.ContextStatus;
import io.acra.core.domain.authorization.CorrelationState;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.reproduction.ReproductionJsonReporter;
import io.acra.core.reproduction.ReproductionPackageBuilder;
import io.acra.core.reproduction.ReproductionPackageExporter;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.tests.TestSupport;
import java.util.List;
import java.util.Map;

public final class Sprint12ReproductionPackageFoundationTestSuite {
    private Sprint12ReproductionPackageFoundationTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT12_REPRODUCTION_PACKAGE_FOUNDATION PASS assertions=" + assertions);
    }

    public static int run() {
        FindingCandidate candidate = candidate(
                FindingCandidateState.CANDIDATE,
                "Authorization: Bearer super-secret-token",
                "password=DoNotExport");
        AuthorizationReport report = report(candidate, FindingSeverity.HIGH, FindingConfidence.HIGH);
        var builder = new ReproductionPackageBuilder();
        var value = builder.build(candidate, report);
        int assertions = 0;

        TestSupport.assertEquals("acra-reproduction-package-v1", value.packageVersion(),
                "reproduction package version is explicit");
        assertions++;
        TestSupport.assertEquals(FindingCandidateState.CANDIDATE, value.candidateState(),
                "candidate state is preserved");
        assertions++;
        TestSupport.assertTrue(value.reviewOnly(),
                "reproduction package remains review-only");
        assertions++;
        TestSupport.assertTrue(value.issueEligible(),
                "candidate package is eligible for a review issue adapter");
        assertions++;
        TestSupport.assertEquals(FindingSeverity.HIGH, value.severity(),
                "severity remains independent");
        assertions++;
        TestSupport.assertEquals(FindingConfidence.HIGH, value.confidence(),
                "confidence remains independent");
        assertions++;
        TestSupport.assertEquals(TokenFingerprint.sha256(candidate.principalId()),
                value.principalFingerprint(),
                "raw principal is replaced by deterministic fingerprint");
        assertions++;
        TestSupport.assertEquals(candidate.fingerprint().fingerprint(),
                value.findingFingerprint(),
                "finding fingerprint identity is retained");
        assertions++;
        TestSupport.assertEquals(candidate.supportingEvidenceIds(), value.evidenceIds(),
                "evidence lineage is retained");
        assertions++;
        TestSupport.assertEquals(candidate.policyReferences(), value.policyReferences(),
                "policy lineage is retained");
        assertions++;

        var repeated = builder.build(candidate, report);
        TestSupport.assertEquals(value.packageId(), repeated.packageId(),
                "package identity is deterministic");
        assertions++;
        TestSupport.assertEquals(value.fingerprint(), repeated.fingerprint(),
                "package fingerprint is deterministic");
        assertions++;

        var exporter = new ReproductionPackageExporter();
        var jsonA = exporter.json(value);
        var jsonB = exporter.json(repeated);
        TestSupport.assertEquals(jsonA.content(), jsonB.content(),
                "canonical reproduction JSON is deterministic");
        assertions++;
        TestSupport.assertEquals(jsonA.sha256(), jsonB.sha256(),
                "canonical reproduction JSON digest is deterministic");
        assertions++;
        TestSupport.assertEquals(TokenFingerprint.sha256(jsonA.content()), jsonA.sha256(),
                "JSON digest matches exported content");
        assertions++;
        TestSupport.assertContains(jsonA.content(),
                "\"packageVersion\":\"acra-reproduction-package-v1\"",
                "JSON declares reproduction schema version");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"reviewOnly\":true",
                "JSON preserves review-only boundary");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"issueEligible\":true",
                "JSON preserves issue-adapter eligibility");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "super-secret-token",
                "JSON excludes bearer secret");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "DoNotExport",
                "JSON excludes secret material embedded in candidate rationale");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), candidate.principalId(),
                "JSON excludes raw principal");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "\"rationale\"",
                "reproduction package structurally excludes candidate rationale");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "\"contradictoryEvidence\"",
                "reproduction package structurally excludes contradictory raw narrative");
        assertions++;

        ReproductionJsonReporter reporter = new ReproductionJsonReporter();
        TestSupport.assertEquals("reproduction-json-v1", reporter.id(),
                "Reporter plugin exposes stable reproduction JSON identifier");
        assertions++;
        TestSupport.assertEquals(jsonA.content(),
                reporter.render(Map.of("reproductionPackage", value)),
                "Reporter plugin uses canonical reproduction exporter");
        assertions++;

        FindingCandidate inconclusive = candidate(
                FindingCandidateState.INCONCLUSIVE,
                "user-a",
                "insufficient verified evidence");
        var inconclusivePackage = builder.build(
                inconclusive,
                report(inconclusive, FindingSeverity.INFO, FindingConfidence.INSUFFICIENT));
        TestSupport.assertTrue(inconclusivePackage.reviewOnly(),
                "inconclusive reproduction remains review-only");
        assertions++;
        TestSupport.assertTrue(!inconclusivePackage.issueEligible(),
                "inconclusive package is not eligible for issue creation");
        assertions++;

        FindingCandidate rejected = candidate(
                FindingCandidateState.REJECTED,
                "user-a",
                "secure control");
        var rejectedPackage = builder.build(
                rejected,
                report(rejected, FindingSeverity.INFO, FindingConfidence.MEDIUM));
        TestSupport.assertTrue(!rejectedPackage.issueEligible(),
                "rejected control is not eligible for issue creation");
        assertions++;

        AuthorizationReport wrongProject = new AuthorizationReport(
                "report-wrong",
                "other-project",
                "exec-1",
                "test-1",
                ContextStatus.RESOLVED,
                CorrelationState.CONSISTENT,
                candidate.state(),
                FindingSeverity.HIGH,
                FindingConfidence.HIGH,
                candidate.dimensions(),
                candidate.assessmentIds(),
                candidate.supportingEvidenceIds(),
                List.of());
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> builder.build(candidate, wrongProject),
                "cross-project report/candidate mismatch fails closed");
        assertions++;

        AuthorizationReport wrongState = new AuthorizationReport(
                "report-wrong-state",
                candidate.projectId(),
                "exec-1",
                "test-1",
                ContextStatus.RESOLVED,
                CorrelationState.CONSISTENT,
                FindingCandidateState.REJECTED,
                FindingSeverity.HIGH,
                FindingConfidence.HIGH,
                candidate.dimensions(),
                candidate.assessmentIds(),
                candidate.supportingEvidenceIds(),
                List.of());
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> builder.build(candidate, wrongState),
                "candidate/report state mismatch fails closed");
        assertions++;

        return assertions;
    }

    private static FindingCandidate candidate(
            FindingCandidateState state,
            String principal,
            String rationale) {
        return new FindingCandidate(
                "s12-candidate-" + state.name().toLowerCase(),
                state,
                "acra-s12",
                List.of("test-1"),
                List.of("exec-1"),
                List.of("obs-1"),
                List.of("assessment-1"),
                List.of("BOLA", "TENANT"),
                "/api/v1/documents/{id}",
                "document-42",
                principal,
                "CROSS_TENANT",
                AuthorizationDecision.DENY,
                state == FindingCandidateState.REJECTED
                        ? AuthorizationDecision.DENY
                        : AuthorizationDecision.ALLOW,
                List.of("evidence-1", "evidence-2"),
                List.of("conflict-note"),
                List.of("policy-1"),
                state == FindingCandidateState.CANDIDATE ? "HIGH" : "MEDIUM",
                rationale,
                FindingFingerprint.of(
                        "/api/v1/documents/{id}",
                        "document-42",
                        principal,
                        "CROSS_TENANT",
                        "BOLA+TENANT",
                        state.name()));
    }

    private static AuthorizationReport report(
            FindingCandidate candidate,
            FindingSeverity severity,
            FindingConfidence confidence) {
        return new AuthorizationReport(
                "report-" + candidate.candidateId(),
                candidate.projectId(),
                "exec-1",
                "test-1",
                ContextStatus.RESOLVED,
                CorrelationState.CONSISTENT,
                candidate.state(),
                severity,
                confidence,
                candidate.dimensions(),
                candidate.assessmentIds(),
                candidate.supportingEvidenceIds(),
                candidate.state() == FindingCandidateState.INCONCLUSIVE
                        ? List.of("FINDING_CANDIDATE_INCONCLUSIVE")
                        : List.of());
    }
}
