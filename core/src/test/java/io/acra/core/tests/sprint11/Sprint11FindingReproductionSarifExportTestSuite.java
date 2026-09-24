package io.acra.core.tests.sprint11;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingLifecycleState;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.product.finding.FindingReviewWorkspace;
import io.acra.core.reporting.finding.FindingReproductionPackage;
import io.acra.core.reporting.finding.FindingReproductionPackageGenerator;
import io.acra.core.reporting.finding.FindingReproductionSarifExporter;
import io.acra.core.reporting.finding.FindingReproductionSarifReporter;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Sprint11FindingReproductionSarifExportTestSuite {
    private static final Instant AT = Instant.parse("2026-09-25T03:00:00Z");

    private Sprint11FindingReproductionSarifExportTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT11_FINDING_REPRODUCTION_SARIF_EXPORT PASS assertions=" + assertions);
    }

    public static int run() {
        FindingReproductionSarifExporter exporter = new FindingReproductionSarifExporter();
        int assertions = 0;

        FindingReviewWorkspace reviewWorkspace = workspace("candidate-review", FindingSeverity.HIGH);
        var reviewFinding = reviewWorkspace.snapshot().cases().get(0).finding();
        FindingReproductionPackage needsReview = packageFor(reviewWorkspace, reviewFinding.findingId(), AT.plusSeconds(1));
        var reviewSarif = exporter.sarif(needsReview);

        TestSupport.assertEquals("SARIF", reviewSarif.format(),
                "SARIF export declares SARIF format");
        assertions++;
        TestSupport.assertEquals("application/sarif+json", reviewSarif.mediaType(),
                "SARIF export declares SARIF media type");
        assertions++;
        TestSupport.assertEquals(needsReview.reproductionId() + ".sarif", reviewSarif.fileName(),
                "SARIF filename derives from reproduction identity");
        assertions++;
        TestSupport.assertEquals(TokenFingerprint.sha256(reviewSarif.content()), reviewSarif.sha256(),
                "SARIF digest matches canonical content");
        assertions++;
        TestSupport.assertContains(reviewSarif.content(), "\"version\":\"2.1.0\"",
                "SARIF log declares version 2.1.0");
        assertions++;
        TestSupport.assertContains(
                reviewSarif.content(),
                "sarif-schema-2.1.0.json",
                "SARIF log references the OASIS 2.1.0 schema");
        assertions++;
        TestSupport.assertContains(reviewSarif.content(), "\"name\":\"ACRA\"",
                "SARIF tool driver identifies ACRA");
        assertions++;
        TestSupport.assertContains(reviewSarif.content(), "\"kind\":\"review\"",
                "needs-review maps to SARIF review kind");
        assertions++;
        TestSupport.assertContains(reviewSarif.content(), "\"level\":\"none\"",
                "SARIF review kind uses level none");
        assertions++;
        TestSupport.assertContains(
                reviewSarif.content(),
                "\"ruleId\":\"ACRA.AUTHORIZATION.BATCH_AUTHORIZATION\"",
                "SARIF rule identity derives from authorization dimension");
        assertions++;
        TestSupport.assertContains(
                reviewSarif.content(),
                "\"partialFingerprints\":",
                "SARIF contains stable partial fingerprint");
        assertions++;
        TestSupport.assertContains(
                reviewSarif.content(),
                needsReview.findingFingerprint(),
                "SARIF partial fingerprint retains ACRA finding fingerprint");
        assertions++;
        TestSupport.assertContains(
                reviewSarif.content(),
                "\"confirmedFinding\":false",
                "review SARIF does not claim confirmation");
        assertions++;

        reviewWorkspace.transition(
                reviewFinding.findingId(),
                FindingLifecycleState.VALIDATED,
                AT.plusSeconds(10),
                "reviewer-a",
                "Authorization: Bearer sarif-review-secret",
                List.of("sarif-review-evidence"));
        FindingReproductionPackage validated = packageFor(
                reviewWorkspace, reviewFinding.findingId(), AT.plusSeconds(11));
        var validatedSarif = exporter.sarif(validated);
        TestSupport.assertContains(validatedSarif.content(), "\"kind\":\"review\"",
                "validated finding remains SARIF review kind");
        assertions++;
        TestSupport.assertContains(validatedSarif.content(), "\"level\":\"none\"",
                "validated review remains level none");
        assertions++;
        TestSupport.assertNotContains(validatedSarif.content(), "sarif-review-secret",
                "SARIF excludes review-reason secret");
        assertions++;
        TestSupport.assertNotContains(validatedSarif.content(), "reviewer-a",
                "SARIF excludes reviewer identity");
        assertions++;
        TestSupport.assertNotContains(validatedSarif.content(), "\"rationale\"",
                "SARIF structurally excludes candidate/risk rationale");
        assertions++;
        TestSupport.assertNotContains(validatedSarif.content(), "\"reviewerReference\"",
                "SARIF structurally excludes reviewer reference");
        assertions++;
        TestSupport.assertNotContains(validatedSarif.content(), "\"reason\"",
                "SARIF structurally excludes transition reason");
        assertions++;

        reviewWorkspace.transition(
                reviewFinding.findingId(),
                FindingLifecycleState.CONFIRMED,
                AT.plusSeconds(20),
                "reviewer-b",
                "Independent confirmation",
                List.of("sarif-confirm-evidence"));
        FindingReproductionPackage confirmed = packageFor(
                reviewWorkspace, reviewFinding.findingId(), AT.plusSeconds(21));
        var confirmedSarif = exporter.sarif(confirmed);
        TestSupport.assertContains(confirmedSarif.content(), "\"kind\":\"fail\"",
                "confirmed finding maps to SARIF fail kind");
        assertions++;
        TestSupport.assertContains(confirmedSarif.content(), "\"level\":\"error\"",
                "high-severity confirmed finding maps to SARIF error level");
        assertions++;
        TestSupport.assertContains(confirmedSarif.content(), "\"confirmedFinding\":true",
                "confirmed SARIF records explicit confirmation");
        assertions++;
        TestSupport.assertNotContains(confirmedSarif.content(), "\"suppressions\":",
                "confirmed finding is not automatically suppressed");
        assertions++;

        reviewWorkspace.transition(
                reviewFinding.findingId(),
                FindingLifecycleState.ACCEPTED_RISK,
                AT.plusSeconds(30),
                "risk-owner",
                "Explicit risk acceptance",
                List.of("sarif-risk-evidence"));
        FindingReproductionPackage accepted = packageFor(
                reviewWorkspace, reviewFinding.findingId(), AT.plusSeconds(31));
        var acceptedSarif = exporter.sarif(accepted);
        TestSupport.assertContains(acceptedSarif.content(), "\"kind\":\"fail\"",
                "accepted-risk result remains a SARIF problem");
        assertions++;
        TestSupport.assertContains(acceptedSarif.content(), "\"status\":\"accepted\"",
                "accepted risk maps to accepted SARIF suppression");
        assertions++;
        TestSupport.assertContains(acceptedSarif.content(), "\"kind\":\"external\"",
                "accepted risk uses external suppression kind");
        assertions++;
        TestSupport.assertContains(
                acceptedSarif.content(),
                "Risk acceptance recorded in the ACRA finding lifecycle.",
                "accepted suppression uses generic non-sensitive justification");
        assertions++;

        FindingReviewWorkspace fpWorkspace = workspace("candidate-fp", FindingSeverity.MEDIUM);
        var fpFinding = fpWorkspace.snapshot().cases().get(0).finding();
        fpWorkspace.transition(
                fpFinding.findingId(),
                FindingLifecycleState.FALSE_POSITIVE,
                AT.plusSeconds(40),
                "reviewer-fp",
                "Reviewed control disproves candidate",
                List.of("sarif-fp-evidence"));
        var falsePositiveSarif = exporter.sarif(
                packageFor(fpWorkspace, fpFinding.findingId(), AT.plusSeconds(41)));
        TestSupport.assertContains(falsePositiveSarif.content(), "\"kind\":\"pass\"",
                "false positive maps to SARIF pass kind");
        assertions++;
        TestSupport.assertContains(falsePositiveSarif.content(), "\"level\":\"none\"",
                "false-positive SARIF pass uses level none");
        assertions++;
        TestSupport.assertContains(falsePositiveSarif.content(), "\"confirmedFinding\":false",
                "false positive never claims confirmation");
        assertions++;

        FindingReviewWorkspace mediumWorkspace = workspace("candidate-medium", FindingSeverity.MEDIUM);
        var mediumFinding = mediumWorkspace.snapshot().cases().get(0).finding();
        mediumWorkspace.transition(
                mediumFinding.findingId(),
                FindingLifecycleState.VALIDATED,
                AT.plusSeconds(50),
                "r1",
                "validated",
                List.of("e-medium-1"));
        mediumWorkspace.transition(
                mediumFinding.findingId(),
                FindingLifecycleState.CONFIRMED,
                AT.plusSeconds(51),
                "r2",
                "confirmed",
                List.of("e-medium-2"));
        var mediumSarif = exporter.sarif(
                packageFor(mediumWorkspace, mediumFinding.findingId(), AT.plusSeconds(52)));
        TestSupport.assertContains(mediumSarif.content(), "\"level\":\"warning\"",
                "medium confirmed severity maps to SARIF warning");
        assertions++;

        FindingReviewWorkspace lowWorkspace = workspace("candidate-low", FindingSeverity.LOW);
        var lowFinding = lowWorkspace.snapshot().cases().get(0).finding();
        lowWorkspace.transition(
                lowFinding.findingId(),
                FindingLifecycleState.VALIDATED,
                AT.plusSeconds(60),
                "r1",
                "validated",
                List.of("e-low-1"));
        lowWorkspace.transition(
                lowFinding.findingId(),
                FindingLifecycleState.CONFIRMED,
                AT.plusSeconds(61),
                "r2",
                "confirmed",
                List.of("e-low-2"));
        var lowSarif = exporter.sarif(
                packageFor(lowWorkspace, lowFinding.findingId(), AT.plusSeconds(62)));
        TestSupport.assertContains(lowSarif.content(), "\"level\":\"note\"",
                "low confirmed severity maps to SARIF note");
        assertions++;

        var repeated = exporter.sarif(confirmed);
        TestSupport.assertEquals(confirmedSarif.content(), repeated.content(),
                "identical confirmed state produces deterministic SARIF");
        assertions++;
        TestSupport.assertEquals(confirmedSarif.sha256(), repeated.sha256(),
                "identical confirmed state produces stable SARIF SHA-256");
        assertions++;

        FindingReproductionSarifReporter reporter = new FindingReproductionSarifReporter();
        TestSupport.assertEquals("s11-finding-reproduction-sarif-v1", reporter.id(),
                "SARIF Reporter exposes stable plugin identifier");
        assertions++;
        TestSupport.assertEquals(
                confirmedSarif.content(),
                reporter.render(Map.of("reproduction", confirmed)),
                "SARIF Reporter renders canonical export content");
        assertions++;
        TestSupport.assertThrows(
                IllegalArgumentException.class,
                () -> reporter.render(Map.of()),
                "SARIF Reporter rejects missing reproduction model");
        assertions++;
        TestSupport.assertThrows(
                IllegalArgumentException.class,
                () -> exporter.sarif(null),
                "SARIF exporter rejects null reproduction package");
        assertions++;

        try {
            Path out = Path.of("build", "s11-foundation", "reporting");
            Files.createDirectories(out);
            Path sarifPath = out.resolve("S11-FINDING-REPRODUCTION.sarif");
            Path shaPath = out.resolve("S11-FINDING-REPRODUCTION.sarif.sha256");
            Files.writeString(sarifPath, confirmedSarif.content(), StandardCharsets.UTF_8);
            Files.writeString(
                    shaPath,
                    confirmedSarif.sha256() + "  S11-FINDING-REPRODUCTION.sarif\n",
                    StandardCharsets.UTF_8);
            TestSupport.assertTrue(Files.isRegularFile(sarifPath),
                    "canonical SARIF artifact written");
            assertions++;
            TestSupport.assertTrue(Files.isRegularFile(shaPath),
                    "SARIF SHA-256 sidecar written");
            assertions++;
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("unable to write Sprint 11 SARIF artifacts", failure);
        }

        return assertions;
    }

    private static FindingReviewWorkspace workspace(
            String candidateId,
            FindingSeverity severity) {
        FindingReviewWorkspace workspace = new FindingReviewWorkspace("project-s11");
        FindingCandidate candidate = candidate(candidateId);
        workspace.open(candidate, risk(candidateId, severity), AT);
        return workspace;
    }

    private static FindingReproductionPackage packageFor(
            FindingReviewWorkspace workspace,
            String findingId,
            Instant generatedAt) {
        return new FindingReproductionPackageGenerator().generate(
                workspace.reviewCase(findingId), generatedAt);
    }

    private static FindingCandidate candidate(String id) {
        return new FindingCandidate(
                id,
                FindingCandidateState.CANDIDATE,
                "project-s11",
                List.of("test-" + id),
                List.of("execution-" + id),
                List.of("observation-" + id),
                List.of("assessment-" + id),
                List.of("BATCH_AUTHORIZATION"),
                "/api/v1/s11/review",
                "resource-b",
                "user-a",
                "CROSS_TENANT",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("candidate-" + id + "-evidence"),
                List.of(),
                List.of("policy-" + id),
                "MEDIUM",
                "password=DummyPassword; excluded rationale",
                FindingFingerprint.of(
                        "/api/v1/s11/review",
                        "resource-b",
                        "user-a",
                        "CROSS_TENANT",
                        "BATCH_AUTHORIZATION",
                        "CANDIDATE"));
    }

    private static AuthorizationRiskAssessment risk(
            String candidateId,
            FindingSeverity severity) {
        return new AuthorizationRiskAssessment(
                "risk-" + candidateId,
                candidateId,
                severity,
                FindingConfidence.MEDIUM,
                severity == FindingSeverity.HIGH ? 80 : 60,
                "risk rationale excluded from reproduction",
                List.of("authorization-impact"));
    }
}
