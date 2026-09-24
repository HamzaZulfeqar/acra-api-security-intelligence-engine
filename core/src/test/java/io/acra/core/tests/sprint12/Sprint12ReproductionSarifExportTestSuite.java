package io.acra.core.tests.sprint12;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.reproduction.ReproductionExportTarget;
import io.acra.core.reproduction.ReproductionPackageProjector;
import io.acra.core.reproduction.ReproductionSarifExporter;
import io.acra.core.reproduction.ReproductionSarifReporter;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class Sprint12ReproductionSarifExportTestSuite {
    private Sprint12ReproductionSarifExportTestSuite() { }

    public static void main(String[] args) throws Exception {
        int assertions = run();
        System.out.println("SPRINT12_REPRODUCTION_SARIF_EXPORT PASS assertions=" + assertions);
    }

    public static int run() throws Exception {
        int assertions = 0;
        var pkg = new ReproductionPackageProjector().project(
                candidate(), FindingSeverity.HIGH, "Authorization mismatch under review");
        ReproductionSarifExporter exporter = new ReproductionSarifExporter();
        var artifact = exporter.export(pkg);

        TestSupport.assertEquals(ReproductionExportTarget.SARIF, artifact.target(),
                "artifact target is SARIF");
        assertions++;
        TestSupport.assertEquals("application/sarif+json", artifact.mediaType(),
                "SARIF media type is explicit");
        assertions++;
        TestSupport.assertEquals(pkg.packageId() + ".sarif", artifact.fileName(),
                "SARIF filename uses .sarif convention");
        assertions++;
        TestSupport.assertEquals(64, artifact.sha256().length(),
                "SARIF artifact uses SHA-256");
        assertions++;
        TestSupport.assertContains(artifact.content(), "\"version\":\"2.1.0\"",
                "SARIF version is 2.1.0");
        assertions++;
        TestSupport.assertContains(artifact.content(), "\"$schema\":\"" + ReproductionSarifExporter.SARIF_SCHEMA + "\"",
                "SARIF schema URI is explicit");
        assertions++;
        TestSupport.assertContains(artifact.content(), "\"runs\"",
                "SARIF has runs array");
        assertions++;
        TestSupport.assertContains(artifact.content(), "\"driver\":{\"name\":\"ACRA\"",
                "SARIF run identifies ACRA tool driver");
        assertions++;
        TestSupport.assertContains(artifact.content(), "\"results\"",
                "SARIF successful run contains results");
        assertions++;
        TestSupport.assertContains(artifact.content(), "\"ruleId\":\"ACRA-AUTHORIZATION-REVIEW\"",
                "SARIF result has stable ACRA rule ID");
        assertions++;
        TestSupport.assertContains(artifact.content(), "\"kind\":\"review\"",
                "SARIF result kind preserves human-review semantics");
        assertions++;
        TestSupport.assertContains(artifact.content(), "\"level\":\"error\"",
                "HIGH severity maps to SARIF error level");
        assertions++;
        TestSupport.assertContains(artifact.content(), "\"acraReviewOnly\":true",
                "SARIF property bag explicitly preserves review-only boundary");
        assertions++;
        TestSupport.assertContains(artifact.content(), "\"acraCandidateState\":\"CANDIDATE\"",
                "SARIF retains candidate lifecycle state");
        assertions++;
        TestSupport.assertContains(artifact.content(), "\"acraExpectedDecision\":\"DENY\"",
                "SARIF retains expected decision");
        assertions++;
        TestSupport.assertContains(artifact.content(), "\"acraObservedDecision\":\"ALLOW\"",
                "SARIF retains observed decision");
        assertions++;
        TestSupport.assertContains(artifact.content(), "evidence-sarif-1",
                "SARIF retains evidence lineage");
        assertions++;

        var repeated = exporter.export(pkg);
        TestSupport.assertEquals(artifact.content(), repeated.content(),
                "SARIF content is deterministic");
        assertions++;
        TestSupport.assertEquals(artifact.sha256(), repeated.sha256(),
                "SARIF digest is deterministic");
        assertions++;

        ReproductionSarifReporter reporter = new ReproductionSarifReporter();
        TestSupport.assertEquals("acra-reproduction-sarif-v1", reporter.id(),
                "SARIF Reporter ID is versioned");
        assertions++;
        TestSupport.assertEquals(
                artifact.content(),
                reporter.render(Map.of("reproductionPackage", pkg)),
                "SARIF Reporter uses canonical exporter");
        assertions++;

        var low = exporter.export(new ReproductionPackageProjector().project(
                candidate(), FindingSeverity.LOW, "low review"));
        TestSupport.assertContains(low.content(), "\"level\":\"note\"",
                "LOW severity maps to SARIF note");
        assertions++;

        var medium = exporter.export(new ReproductionPackageProjector().project(
                candidate(), FindingSeverity.MEDIUM, "medium review"));
        TestSupport.assertContains(medium.content(), "\"level\":\"warning\"",
                "MEDIUM severity maps to SARIF warning");
        assertions++;

        var secret = exporter.export(new ReproductionPackageProjector().project(
                secretCandidate(), FindingSeverity.HIGH, "Bearer aaa.bbb.ccc token=topsecret"));
        TestSupport.assertNotContains(secret.content(), "aaa.bbb.ccc",
                "SARIF redacts bearer-like secret");
        assertions++;
        TestSupport.assertNotContains(secret.content(), "topsecret",
                "SARIF redacts sensitive token value");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> exporter.export(null),
                "SARIF exporter rejects null package");
        assertions++;

        Path output = Path.of("build", "s12-foundation", "reproduction-sample.sarif");
        Files.createDirectories(output.getParent());
        Files.writeString(output, artifact.content(), StandardCharsets.UTF_8);
        TestSupport.assertTrue(Files.exists(output),
                "SARIF verification artifact is written for schema-shape validation");
        assertions++;

        return assertions;
    }

    private static FindingCandidate candidate() {
        return new FindingCandidate(
                "candidate-sarif-001",
                FindingCandidateState.CANDIDATE,
                "project-s12",
                List.of("test-1"),
                List.of("exec-1"),
                List.of("obs-1"),
                List.of("assessment-1"),
                List.of("OBJECT", "TENANT"),
                "/api/v1/documents/42",
                "document:42",
                "user-a",
                "CROSS_TENANT",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("evidence-sarif-1", "evidence-sarif-2"),
                List.of(),
                List.of("policy-sarif-1"),
                "HIGH",
                "Expected DENY but observed ALLOW",
                FindingFingerprint.of(
                        "/api/v1/documents/{id}", "document:42", "user-a", "CROSS_TENANT",
                        "DENY_TO_ALLOW", "OBJECT_AUTHORIZATION"));
    }

    private static FindingCandidate secretCandidate() {
        return new FindingCandidate(
                "candidate-sarif-secret",
                FindingCandidateState.CANDIDATE,
                "project-secret",
                List.of("test-secret"),
                List.of("exec-secret"),
                List.of("obs-secret"),
                List.of("assessment-secret"),
                List.of("OBJECT"),
                "/api/v1/resource?token=topsecret",
                "resource-secret",
                "user-a",
                "SAME_TENANT",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("evidence-token=topsecret"),
                List.of(),
                List.of("policy-token=topsecret"),
                "HIGH",
                "Bearer aaa.bbb.ccc",
                FindingFingerprint.of(
                        "/api/v1/resource", "resource-secret", "user-a", "SAME_TENANT",
                        "DENY_TO_ALLOW", "OBJECT_AUTHORIZATION"));
    }
}
