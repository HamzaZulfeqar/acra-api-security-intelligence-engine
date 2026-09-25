package io.acra.core.tests.sprint12;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.reporting.s12.S12ReproductionJsonExporter;
import io.acra.core.reporting.s12.S12ReproductionPackageFactory;
import io.acra.core.reporting.s12.S12SarifExporter;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Sprint12ReproductionEvidenceArtifactTestSuite {
    private Sprint12ReproductionEvidenceArtifactTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT12_REPRODUCTION_EVIDENCE_ARTIFACT PASS assertions=" + assertions);
    }

    public static int run() {
        var reproduction = new S12ReproductionPackageFactory().from(fixture());
        var json = new S12ReproductionJsonExporter().export(reproduction);
        var sarif = new S12SarifExporter().export(reproduction);
        int assertions = 0;

        TestSupport.assertEquals("JSON", json.format(),
                "canonical evidence artifact includes JSON export");
        assertions++;
        TestSupport.assertEquals("SARIF", sarif.format(),
                "canonical evidence artifact includes SARIF export");
        assertions++;
        TestSupport.assertTrue(!json.sha256().equals(sarif.sha256()),
                "JSON and SARIF content identities remain format-specific");
        assertions++;
        TestSupport.assertContains(sarif.content(), "\"version\":\"2.1.0\"",
                "archived SARIF remains standards-version explicit");
        assertions++;
        TestSupport.assertContains(sarif.content(), "\"reviewOnly\":true",
                "archived SARIF remains review-only");
        assertions++;
        TestSupport.assertNotContains(json.content(), "DummyPassword",
                "archived JSON excludes secret-bearing rationale");
        assertions++;
        TestSupport.assertNotContains(sarif.content(), "DummyPassword",
                "archived SARIF excludes secret-bearing rationale");
        assertions++;

        try {
            Path out = Path.of("build", "s12-foundation", "reproduction");
            Files.createDirectories(out);
            Files.writeString(out.resolve("s12-reproduction.json"), json.content(), StandardCharsets.UTF_8);
            Files.writeString(out.resolve("s12-reproduction.json.sha256"),
                    json.sha256() + "  s12-reproduction.json\n", StandardCharsets.UTF_8);
            Files.writeString(out.resolve("s12-reproduction.sarif"), sarif.content(), StandardCharsets.UTF_8);
            Files.writeString(out.resolve("s12-reproduction.sarif.sha256"),
                    sarif.sha256() + "  s12-reproduction.sarif\n", StandardCharsets.UTF_8);

            TestSupport.assertTrue(Files.isRegularFile(out.resolve("s12-reproduction.json")),
                    "canonical JSON evidence artifact written");
            assertions++;
            TestSupport.assertTrue(Files.isRegularFile(out.resolve("s12-reproduction.sarif")),
                    "canonical SARIF evidence artifact written");
            assertions++;
            TestSupport.assertEquals(
                    json.content(),
                    Files.readString(out.resolve("s12-reproduction.json"), StandardCharsets.UTF_8),
                    "written JSON evidence equals canonical export");
            assertions++;
            TestSupport.assertEquals(
                    sarif.content(),
                    Files.readString(out.resolve("s12-reproduction.sarif"), StandardCharsets.UTF_8),
                    "written SARIF evidence equals canonical export");
            assertions++;
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("unable to write Sprint 12 reproduction evidence artifacts", failure);
        }

        return assertions;
    }

    private static FindingCandidate fixture() {
        return new FindingCandidate(
                "s12-evidence-artifact-candidate",
                FindingCandidateState.CANDIDATE,
                "acra-s12",
                List.of("test-s12"),
                List.of("execution-s12"),
                List.of("observation-s12"),
                List.of("assessment-s12"),
                List.of("OBJECT_AUTHORIZATION"),
                "/api/v1/documents/1002",
                "document:1002",
                "user-a",
                "FOREIGN_TENANT",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("evidence-s12-a", "evidence-s12-b"),
                List.of(),
                List.of("policy-s12"),
                "HIGH",
                "Review fixture; password=DummyPassword",
                FindingFingerprint.of(
                        "/api/v1/documents/1002",
                        "document:1002",
                        "user-a",
                        "FOREIGN_TENANT",
                        "DENY_TO_ALLOW",
                        "OBJECT_AUTHORIZATION"));
    }
}
