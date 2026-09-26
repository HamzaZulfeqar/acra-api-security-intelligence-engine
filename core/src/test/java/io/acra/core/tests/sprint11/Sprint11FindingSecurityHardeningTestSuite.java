package io.acra.core.tests.sprint11;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingLifecycleService;
import io.acra.core.domain.finding.FindingLifecycleState;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.product.finding.FindingReviewWorkspace;
import io.acra.core.reporting.finding.FindingBurpIssueDraftGenerator;
import io.acra.core.reporting.finding.FindingReproductionJsonExporter;
import io.acra.core.reporting.finding.FindingReproductionPackageGenerator;
import io.acra.core.reporting.finding.FindingReproductionSarifExporter;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;

public final class Sprint11FindingSecurityHardeningTestSuite {
    private static final Instant AT = Instant.parse("2026-09-25T04:30:00Z");

    private Sprint11FindingSecurityHardeningTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT11_FINDING_SECURITY_HARDENING PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        FindingLifecycleService lifecycle = new FindingLifecycleService();

        TestSupport.assertThrows(
                IllegalArgumentException.class,
                () -> lifecycle.open(
                        candidate("", "project-a", "resource-a", "clean"),
                        risk("", FindingSeverity.HIGH, FindingConfidence.HIGH, "clean"),
                        AT),
                "blank candidate identity fails closed");
        assertions++;

        TestSupport.assertThrows(
                IllegalArgumentException.class,
                () -> lifecycle.open(
                        candidate("candidate-a", "project-a", "resource-a", "clean"),
                        risk("", FindingSeverity.HIGH, FindingConfidence.HIGH, "clean"),
                        AT),
                "blank risk candidate identity fails closed");
        assertions++;

        FindingReviewWorkspace projectA = new FindingReviewWorkspace("project-a");
        FindingReviewWorkspace projectB = new FindingReviewWorkspace("project-b");
        var sourceA = candidate("same-candidate", "project-a", "resource-a", "clean");
        var sourceB = candidate("same-candidate", "project-b", "resource-a", "clean");
        var findingA = projectA.open(
                sourceA, risk(sourceA.candidateId(), FindingSeverity.HIGH, FindingConfidence.HIGH, "clean"), AT);
        var findingB = projectB.open(
                sourceB, risk(sourceB.candidateId(), FindingSeverity.HIGH, FindingConfidence.HIGH, "clean"), AT);

        TestSupport.assertFalse(
                findingA.findingId().equals(findingB.findingId()),
                "finding identity includes project boundary");
        assertions++;
        TestSupport.assertEquals("project-a", findingA.projectId(),
                "project A finding remains project isolated");
        assertions++;
        TestSupport.assertEquals("project-b", findingB.projectId(),
                "project B finding remains project isolated");
        assertions++;

        TestSupport.assertThrows(
                IllegalArgumentException.class,
                () -> projectA.reviewCase(findingB.findingId()),
                "workspace cannot read a foreign-project finding identity");
        assertions++;
        TestSupport.assertThrows(
                IllegalArgumentException.class,
                () -> projectA.transition(
                        findingB.findingId(),
                        FindingLifecycleState.VALIDATED,
                        AT.plusSeconds(1),
                        "reviewer-a",
                        "foreign transition",
                        List.of("e-foreign")),
                "workspace cannot transition a foreign-project finding");
        assertions++;

        String[] secrets = {
            "Authorization: Bearer s11-hardening-bearer",
            "password=SuperSecretS11!",
            "Cookie: session=s11-hardening-cookie",
            "X-API-Key: s11-hardening-api-key"
        };

        for (int index = 0; index < secrets.length; index++) {
            String secret = secrets[index];
            String candidateId = "candidate-secret-" + index;
            FindingReviewWorkspace workspace = new FindingReviewWorkspace("project-secret");
            FindingCandidate candidate = candidate(
                    candidateId,
                    "project-secret",
                    "resource-secret-" + index,
                    secret);
            AuthorizationRiskAssessment risk = risk(
                    candidateId,
                    FindingSeverity.HIGH,
                    FindingConfidence.HIGH,
                    secret);

            var opened = workspace.open(candidate, risk, AT.plusSeconds(index));
            workspace.transition(
                    opened.findingId(),
                    FindingLifecycleState.VALIDATED,
                    AT.plusSeconds(10 + index),
                    secret,
                    secret,
                    List.of(secret, "review-evidence-" + index));
            workspace.transition(
                    opened.findingId(),
                    FindingLifecycleState.CONFIRMED,
                    AT.plusSeconds(20 + index),
                    "reviewer-confirm-" + index,
                    "independent confirmation",
                    List.of("confirm-evidence-" + index));

            var reproduction = new FindingReproductionPackageGenerator().generate(
                    workspace.reviewCase(opened.findingId()), AT.plusSeconds(30 + index));
            String json = new FindingReproductionJsonExporter().json(reproduction).content();
            String sarif = new FindingReproductionSarifExporter().sarif(reproduction).content();
            String burp = new io.acra.core.serialization.DomainSerializer().serialize(
                    new FindingBurpIssueDraftGenerator().generate(reproduction));

            assertNoSecret(json, secret, "JSON", index);
            assertions++;
            assertNoSecret(sarif, secret, "SARIF", index);
            assertions++;
            assertNoSecret(burp, secret, "Burp draft", index);
            assertions++;
            TestSupport.assertNotContains(json, "\"reviewerReference\"",
                    "JSON structurally excludes reviewer identity");
            assertions++;
            TestSupport.assertNotContains(sarif, "\"reviewerReference\"",
                    "SARIF structurally excludes reviewer identity");
            assertions++;
            TestSupport.assertNotContains(burp, "\"reviewerReference\"",
                    "Burp draft structurally excludes reviewer identity");
            assertions++;
        }

        FindingReviewWorkspace deterministic = new FindingReviewWorkspace("project-deterministic");
        FindingCandidate deterministicCandidate = candidate(
                "candidate-deterministic", "project-deterministic", "resource-d", "clean");
        var deterministicFinding = deterministic.open(
                deterministicCandidate,
                risk(
                        deterministicCandidate.candidateId(),
                        FindingSeverity.MEDIUM,
                        FindingConfidence.MEDIUM,
                        "clean"),
                AT);
        deterministic.transition(
                deterministicFinding.findingId(),
                FindingLifecycleState.VALIDATED,
                AT.plusSeconds(1),
                "reviewer-a",
                "validated",
                List.of("evidence-v"));
        deterministic.transition(
                deterministicFinding.findingId(),
                FindingLifecycleState.CONFIRMED,
                AT.plusSeconds(2),
                "reviewer-b",
                "confirmed",
                List.of("evidence-c"));

        var caseValue = deterministic.reviewCase(deterministicFinding.findingId());
        FindingReproductionPackageGenerator packageGenerator =
                new FindingReproductionPackageGenerator();
        var packageA = packageGenerator.generate(caseValue, AT.plusSeconds(100));
        var packageB = packageGenerator.generate(caseValue, AT.plusSeconds(200));

        TestSupport.assertEquals(packageA.reproductionId(), packageB.reproductionId(),
                "reproduction identity ignores render timestamp");
        assertions++;
        TestSupport.assertEquals(packageA.findingFingerprint(), packageB.findingFingerprint(),
                "finding fingerprint remains stable");
        assertions++;

        FindingReproductionJsonExporter jsonExporter = new FindingReproductionJsonExporter();
        FindingReproductionSarifExporter sarifExporter = new FindingReproductionSarifExporter();
        TestSupport.assertEquals(
                jsonExporter.json(packageA).content(),
                jsonExporter.json(packageA).content(),
                "canonical JSON is byte deterministic for same package");
        assertions++;
        TestSupport.assertEquals(
                sarifExporter.sarif(packageA).content(),
                sarifExporter.sarif(packageA).content(),
                "canonical SARIF is byte deterministic for same package");
        assertions++;

        TestSupport.assertThrows(
                UnsupportedOperationException.class,
                () -> deterministic.snapshot().cases().clear(),
                "workspace snapshot cannot be mutated by caller");
        assertions++;
        TestSupport.assertThrows(
                UnsupportedOperationException.class,
                () -> packageA.reviewTrail().clear(),
                "reproduction review trail cannot be mutated by caller");
        assertions++;

        return assertions;
    }

    private static void assertNoSecret(
            String rendered,
            String originalSecret,
            String surface,
            int index) {
        String[] markers = {
            "s11-hardening-bearer",
            "SuperSecretS11",
            "s11-hardening-cookie",
            "s11-hardening-api-key"
        };
        TestSupport.assertNotContains(
                rendered,
                originalSecret,
                surface + " excludes original secret fixture " + index);
        for (String marker : markers) {
            if (originalSecret.contains(marker)) {
                TestSupport.assertNotContains(
                        rendered,
                        marker,
                        surface + " excludes secret marker " + index);
            }
        }
    }

    private static FindingCandidate candidate(
            String candidateId,
            String projectId,
            String resourceId,
            String rationale) {
        return new FindingCandidate(
                candidateId,
                FindingCandidateState.CANDIDATE,
                projectId,
                List.of("test-" + resourceId),
                List.of("execution-" + resourceId),
                List.of("observation-" + resourceId),
                List.of("assessment-" + resourceId),
                List.of("BATCH_AUTHORIZATION"),
                "/api/v1/s11/hardening",
                resourceId,
                "user-a",
                "CROSS_TENANT",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("candidate-evidence-" + resourceId),
                List.of(),
                List.of("policy-" + resourceId),
                "HIGH",
                rationale,
                FindingFingerprint.of(
                        "/api/v1/s11/hardening",
                        resourceId,
                        "user-a",
                        "CROSS_TENANT",
                        "BATCH_AUTHORIZATION",
                        "CANDIDATE"));
    }

    private static AuthorizationRiskAssessment risk(
            String candidateId,
            FindingSeverity severity,
            FindingConfidence confidence,
            String rationale) {
        return new AuthorizationRiskAssessment(
                "risk-" + (candidateId.isBlank() ? "blank" : candidateId),
                candidateId,
                severity,
                confidence,
                80,
                rationale,
                List.of("authorization-impact"));
    }
}
