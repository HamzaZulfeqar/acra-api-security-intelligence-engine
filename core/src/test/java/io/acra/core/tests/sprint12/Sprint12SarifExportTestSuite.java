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
import io.acra.core.reproduction.ReproductionPackageBuilder;
import io.acra.core.reproduction.ReproductionSarifExporter;
import io.acra.core.reproduction.ReproductionSarifReporter;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class Sprint12SarifExportTestSuite {
    private Sprint12SarifExportTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT12_SARIF_EXPORT PASS assertions=" + assertions);
    }

    public static int run() {
        var candidate = candidate(FindingCandidateState.CANDIDATE);
        var value = new ReproductionPackageBuilder().build(
                candidate,
                report(candidate, FindingSeverity.CRITICAL, FindingConfidence.HIGH));
        var exporter = new ReproductionSarifExporter();
        var sarifA = exporter.sarif(value);
        var sarifB = exporter.sarif(value);
        int assertions = 0;

        TestSupport.assertEquals("SARIF", sarifA.format(),
                "artifact format is SARIF");
        assertions++;
        TestSupport.assertEquals("application/sarif+json", sarifA.mediaType(),
                "artifact media type is SARIF JSON");
        assertions++;
        TestSupport.assertTrue(sarifA.fileName().endsWith(".sarif"),
                "SARIF file uses .sarif extension");
        assertions++;
        TestSupport.assertEquals(sarifA.content(), sarifB.content(),
                "SARIF export is deterministic");
        assertions++;
        TestSupport.assertEquals(sarifA.sha256(), sarifB.sha256(),
                "SARIF digest is deterministic");
        assertions++;
        TestSupport.assertEquals(TokenFingerprint.sha256(sarifA.content()), sarifA.sha256(),
                "SARIF SHA-256 matches content");
        assertions++;

        TestSupport.assertContains(sarifA.content(), "\"version\":\"2.1.0\"",
                "SARIF version is 2.1.0");
        assertions++;
        TestSupport.assertContains(sarifA.content(),
                "\"$schema\":\"https://docs.oasis-open.org/sarif/sarif/v2.1.0/errata01/os/schemas/sarif-schema-2.1.0.json\"",
                "SARIF uses official 2.1.0 errata schema URI");
        assertions++;
        TestSupport.assertContains(sarifA.content(), "\"name\":\"ACRA\"",
                "SARIF identifies ACRA tool driver");
        assertions++;
        TestSupport.assertContains(sarifA.content(), "\"ruleId\":\"ACRA.AUTHORIZATION.BOLA_TENANT\"",
                "SARIF rule identity is deterministic from dimensions");
        assertions++;
        TestSupport.assertContains(sarifA.content(), "\"kind\":\"review\"",
                "candidate is exported as SARIF review, not fail");
        assertions++;
        TestSupport.assertContains(sarifA.content(), "\"level\":\"none\"",
                "review result uses SARIF level none");
        assertions++;
        TestSupport.assertContains(sarifA.content(), "\"acraSeverity\":\"CRITICAL\"",
                "ACRA severity is preserved independently in properties");
        assertions++;
        TestSupport.assertContains(sarifA.content(), "\"acraConfidence\":\"HIGH\"",
                "ACRA confidence is preserved independently in properties");
        assertions++;
        TestSupport.assertContains(sarifA.content(), "\"acraReviewOnly\":true",
                "SARIF preserves review-only boundary");
        assertions++;
        TestSupport.assertContains(sarifA.content(), "\"acraIssueEligible\":true",
                "SARIF preserves issue eligibility");
        assertions++;
        TestSupport.assertContains(sarifA.content(), "\"acraFinding/v1\"",
                "SARIF result carries stable ACRA finding fingerprint");
        assertions++;
        TestSupport.assertContains(sarifA.content(), "\"acraReproduction/v1\"",
                "SARIF result carries reproduction fingerprint");
        assertions++;
        TestSupport.assertNotContains(sarifA.content(), "\"locations\"",
                "SARIF does not fabricate source-code locations for API endpoints");
        assertions++;
        TestSupport.assertNotContains(sarifA.content(), "super-secret-token",
                "SARIF excludes bearer secret");
        assertions++;
        TestSupport.assertNotContains(sarifA.content(), "DoNotExport",
                "SARIF excludes candidate rationale secret material");
        assertions++;
        TestSupport.assertNotContains(sarifA.content(), candidate.principalId(),
                "SARIF excludes raw principal");
        assertions++;
        TestSupport.assertNotContains(sarifA.content(), "\"rationale\"",
                "SARIF structurally excludes candidate rationale");
        assertions++;

        ReproductionSarifReporter reporter = new ReproductionSarifReporter();
        TestSupport.assertEquals("reproduction-sarif-v1", reporter.id(),
                "Reporter plugin exposes stable SARIF identifier");
        assertions++;
        TestSupport.assertEquals(sarifA.content(),
                reporter.render(Map.of("reproductionPackage", value)),
                "SARIF Reporter uses canonical exporter");
        assertions++;

        var rejected = candidate(FindingCandidateState.REJECTED);
        var rejectedSarif = exporter.sarif(new ReproductionPackageBuilder().build(
                rejected,
                report(rejected, FindingSeverity.INFO, FindingConfidence.MEDIUM)));
        TestSupport.assertContains(rejectedSarif.content(), "\"kind\":\"pass\"",
                "rejected control maps to SARIF pass");
        assertions++;
        TestSupport.assertContains(rejectedSarif.content(), "\"level\":\"none\"",
                "SARIF pass has level none");
        assertions++;

        var inconclusive = candidate(FindingCandidateState.INCONCLUSIVE);
        var inconclusiveSarif = exporter.sarif(new ReproductionPackageBuilder().build(
                inconclusive,
                report(inconclusive, FindingSeverity.INFO, FindingConfidence.INSUFFICIENT)));
        TestSupport.assertContains(inconclusiveSarif.content(), "\"kind\":\"open\"",
                "inconclusive candidate maps to SARIF open");
        assertions++;
        TestSupport.assertContains(inconclusiveSarif.content(), "\"level\":\"none\"",
                "SARIF open has level none");
        assertions++;

        try {
            Path out = Path.of("build", "s12-foundation", "reporting");
            Files.createDirectories(out);
            Files.writeString(out.resolve("ACRA-REPRODUCTION.sarif"),
                    sarifA.content(), StandardCharsets.UTF_8);
            Files.writeString(out.resolve("ACRA-REPRODUCTION.sarif.sha256"),
                    sarifA.sha256() + "  ACRA-REPRODUCTION.sarif\n", StandardCharsets.UTF_8);
            TestSupport.assertTrue(Files.isRegularFile(out.resolve("ACRA-REPRODUCTION.sarif")),
                    "SARIF artifact is written for CI validation");
            assertions++;
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("unable to write Sprint 12 SARIF artifact", failure);
        }

        return assertions;
    }

    private static FindingCandidate candidate(FindingCandidateState state) {
        return new FindingCandidate(
                "s12-sarif-" + state.name().toLowerCase(),
                state,
                "acra-s12",
                List.of("test-1"),
                List.of("exec-1"),
                List.of("obs-1"),
                List.of("assessment-1"),
                List.of("BOLA", "TENANT"),
                "/api/v1/documents/{id}",
                "document-42",
                "Authorization: Bearer super-secret-token",
                "CROSS_TENANT",
                AuthorizationDecision.DENY,
                state == FindingCandidateState.REJECTED
                        ? AuthorizationDecision.DENY
                        : AuthorizationDecision.ALLOW,
                List.of("evidence-1", "evidence-2"),
                List.of("contradiction=password=DoNotExport"),
                List.of("policy-1"),
                state == FindingCandidateState.CANDIDATE ? "HIGH" : "MEDIUM",
                "password=DoNotExport",
                FindingFingerprint.of(
                        "/api/v1/documents/{id}",
                        "document-42",
                        "Authorization: Bearer super-secret-token",
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
