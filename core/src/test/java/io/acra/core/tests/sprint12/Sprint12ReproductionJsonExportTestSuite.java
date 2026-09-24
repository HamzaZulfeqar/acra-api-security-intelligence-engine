package io.acra.core.tests.sprint12;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.reproduction.ReproductionExportTarget;
import io.acra.core.reproduction.ReproductionExportCapabilityState;
import io.acra.core.reproduction.ReproductionJsonExporter;
import io.acra.core.reproduction.ReproductionJsonReporter;
import io.acra.core.reproduction.ReproductionPackageProjector;
import io.acra.core.tests.TestSupport;
import java.util.List;
import java.util.Map;

public final class Sprint12ReproductionJsonExportTestSuite {
    private Sprint12ReproductionJsonExportTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT12_REPRODUCTION_JSON_EXPORT PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        var pkg = new ReproductionPackageProjector().project(
                candidate(), FindingSeverity.HIGH, "Authorization mismatch under review");

        ReproductionJsonExporter exporter = new ReproductionJsonExporter();
        var artifact = exporter.export(pkg);

        TestSupport.assertEquals(ReproductionExportTarget.JSON, artifact.target(),
                "artifact target is JSON");
        assertions++;
        TestSupport.assertEquals("application/json", artifact.mediaType(),
                "JSON media type is explicit");
        assertions++;
        TestSupport.assertEquals(pkg.packageId() + ".json", artifact.fileName(),
                "JSON filename derives from deterministic package ID");
        assertions++;
        TestSupport.assertEquals(64, artifact.sha256().length(),
                "JSON artifact uses SHA-256 digest");
        assertions++;
        TestSupport.assertContains(artifact.content(), "\"schemaVersion\":\"acra-reproduction-package-v1\"",
                "JSON retains reproduction schema version");
        assertions++;
        TestSupport.assertContains(artifact.content(), "\"candidateState\":\"CANDIDATE\"",
                "JSON retains candidate lifecycle state");
        assertions++;
        TestSupport.assertContains(artifact.content(), "\"reviewOnly\":true",
                "JSON explicitly retains review-only boundary");
        assertions++;
        TestSupport.assertContains(artifact.content(), "\"expectedDecision\":\"DENY\"",
                "JSON retains expected authorization decision");
        assertions++;
        TestSupport.assertContains(artifact.content(), "\"observedDecision\":\"ALLOW\"",
                "JSON retains observed authorization decision");
        assertions++;
        TestSupport.assertContains(artifact.content(), "evidence-1",
                "JSON retains supporting evidence lineage");
        assertions++;

        var repeated = exporter.export(pkg);
        TestSupport.assertEquals(artifact.content(), repeated.content(),
                "JSON content is deterministic");
        assertions++;
        TestSupport.assertEquals(artifact.sha256(), repeated.sha256(),
                "JSON digest is deterministic");
        assertions++;

        var secretArtifact = exporter.export(new ReproductionPackageProjector().project(
                secretCandidate(), FindingSeverity.MEDIUM, "Authorization: Bearer aaa.bbb.ccc"));
        TestSupport.assertNotContains(secretArtifact.content(), "aaa.bbb.ccc",
                "JSON does not leak bearer-like material");
        assertions++;
        TestSupport.assertNotContains(secretArtifact.content(), "topsecret",
                "JSON does not leak sensitive token values");
        assertions++;
        TestSupport.assertContains(secretArtifact.content(), "<redacted>",
                "JSON preserves explicit redaction marker");
        assertions++;

        TestSupport.assertEquals(
                ReproductionExportCapabilityState.IMPLEMENTED,
                new io.acra.core.reproduction.ReproductionExportRegistry()
                        .capability(ReproductionExportTarget.JSON).state(),
                "verified JSON renderer is promoted to IMPLEMENTED");
        assertions++;

        ReproductionJsonReporter reporter = new ReproductionJsonReporter();
        TestSupport.assertEquals("acra-reproduction-json-v1", reporter.id(),
                "JSON Reporter ID is versioned");
        assertions++;
        TestSupport.assertEquals(
                artifact.content(),
                reporter.render(Map.of("reproductionPackage", pkg)),
                "Reporter projection uses the canonical JSON exporter");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> reporter.render(Map.of()),
                "Reporter rejects missing reproduction package");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> exporter.export(null),
                "JSON exporter rejects null package");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new io.acra.core.reproduction.ReproductionExportArtifact(
                        ReproductionExportTarget.JSON,
                        "application/json",
                        "bad.json",
                        "not-the-digest",
                        artifact.content()),
                "artifact rejects digest mismatch");
        assertions++;

        return assertions;
    }

    private static FindingCandidate candidate() {
        return new FindingCandidate(
                "candidate-json-001",
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
                List.of("evidence-1", "evidence-2"),
                List.of(),
                List.of("policy-1"),
                "HIGH",
                "Expected DENY but observed ALLOW",
                FindingFingerprint.of(
                        "/api/v1/documents/{id}", "document:42", "user-a", "CROSS_TENANT",
                        "DENY_TO_ALLOW", "OBJECT_AUTHORIZATION"));
    }

    private static FindingCandidate secretCandidate() {
        return new FindingCandidate(
                "candidate-json-secret",
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
                "MEDIUM",
                "Bearer aaa.bbb.ccc",
                FindingFingerprint.of(
                        "/api/v1/resource", "resource-secret", "user-a", "SAME_TENANT",
                        "DENY_TO_ALLOW", "OBJECT_AUTHORIZATION"));
    }
}
