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
import io.acra.core.reporting.finding.FindingReproductionJsonExporter;
import io.acra.core.reporting.finding.FindingReproductionJsonReporter;
import io.acra.core.reporting.finding.FindingReproductionPackageGenerator;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Sprint11FindingReproductionJsonExportTestSuite {
    private static final Instant AT = Instant.parse("2026-09-25T02:30:00Z");

    private Sprint11FindingReproductionJsonExportTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT11_FINDING_REPRODUCTION_JSON_EXPORT PASS assertions=" + assertions);
    }

    public static int run() {
        FindingReviewWorkspace workspace = new FindingReviewWorkspace("project-s11");
        FindingCandidate candidate = candidate();
        var finding = workspace.open(candidate, risk(candidate.candidateId()), AT);
        workspace.transition(
                finding.findingId(),
                FindingLifecycleState.VALIDATED,
                AT.plusSeconds(10),
                "reviewer-a",
                "Authorization: Bearer s11-json-secret",
                List.of("review-json-evidence"));

        var reproduction = new FindingReproductionPackageGenerator().generate(
                workspace.reviewCase(finding.findingId()), AT.plusSeconds(20));
        FindingReproductionJsonExporter exporter = new FindingReproductionJsonExporter();

        var jsonA = exporter.json(reproduction);
        var jsonB = exporter.json(reproduction);
        int assertions = 0;

        TestSupport.assertEquals("JSON", jsonA.format(),
                "canonical reproduction export declares JSON format");
        assertions++;
        TestSupport.assertEquals("application/json", jsonA.mediaType(),
                "canonical reproduction export declares JSON media type");
        assertions++;
        TestSupport.assertEquals(reproduction.reproductionId() + ".json", jsonA.fileName(),
                "canonical filename is derived from reproduction identity");
        assertions++;
        TestSupport.assertEquals(jsonA.content(), jsonB.content(),
                "identical reproduction state produces deterministic JSON");
        assertions++;
        TestSupport.assertEquals(jsonA.sha256(), jsonB.sha256(),
                "identical reproduction state produces stable SHA-256");
        assertions++;
        TestSupport.assertEquals(TokenFingerprint.sha256(jsonA.content()), jsonA.sha256(),
                "export digest matches canonical JSON content");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"schemaVersion\":\"1.0\"",
                "domain envelope retains canonical serializer schema version");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"schemaVersion\":\"s11-finding-reproduction-v1\"",
                "reproduction payload declares Sprint 11 schema version");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"state\":\"VALIDATED\"",
                "JSON preserves explicit lifecycle state");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"confirmedFinding\":false",
                "validated reproduction cannot claim confirmed finding");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"severity\":\"HIGH\"",
                "JSON preserves independent severity");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"confidence\":\"MEDIUM\"",
                "JSON preserves independent confidence");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"expectedDecision\":\"DENY\"",
                "JSON preserves expected authorization decision");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"observedDecision\":\"ALLOW\"",
                "JSON preserves observed authorization decision");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"findingFingerprint\":",
                "JSON preserves stable finding fingerprint");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "review-json-evidence",
                "JSON preserves minimized review evidence reference");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"reviewTrail\":",
                "JSON preserves minimized review trail");
        assertions++;

        TestSupport.assertNotContains(jsonA.content(), "DummyPassword",
                "JSON excludes source candidate rationale secret");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "s11-json-secret",
                "JSON excludes review-reason secret");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "reviewer-a",
                "JSON excludes reviewer identity");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "\"rationale\"",
                "JSON reproduction schema structurally excludes rationale");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "\"reviewerReference\"",
                "JSON reproduction schema structurally excludes reviewerReference");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "\"reason\"",
                "JSON reproduction review trail structurally excludes review reason");
        assertions++;

        FindingReproductionJsonReporter reporter = new FindingReproductionJsonReporter();
        TestSupport.assertEquals("s11-finding-reproduction-json-v1", reporter.id(),
                "Reporter plugin exposes stable Sprint 11 identifier");
        assertions++;
        TestSupport.assertEquals(jsonA.content(), reporter.render(Map.of("reproduction", reproduction)),
                "Reporter plugin renders canonical reproduction JSON");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> reporter.render(null),
                "Reporter rejects null report model");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> reporter.render(Map.of()),
                "Reporter rejects missing reproduction model");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> exporter.json(null),
                "Exporter rejects null reproduction package");
        assertions++;

        workspace.transition(
                finding.findingId(),
                FindingLifecycleState.CONFIRMED,
                AT.plusSeconds(30),
                "reviewer-b",
                "Independent confirmation",
                List.of("confirm-json-evidence"));
        var confirmedPackage = new FindingReproductionPackageGenerator().generate(
                workspace.reviewCase(finding.findingId()), AT.plusSeconds(40));
        var confirmedJson = exporter.json(confirmedPackage);
        TestSupport.assertContains(confirmedJson.content(), "\"state\":\"CONFIRMED\"",
                "JSON reflects explicit confirmed lifecycle state");
        assertions++;
        TestSupport.assertContains(confirmedJson.content(), "\"confirmedFinding\":true",
                "JSON claims confirmation only after explicit review confirmation");
        assertions++;
        TestSupport.assertFalse(jsonA.sha256().equals(confirmedJson.sha256()),
                "lifecycle state/evidence change changes canonical export digest");
        assertions++;

        try {
            Path out = Path.of("build", "s11-foundation", "reporting");
            Files.createDirectories(out);
            Path jsonPath = out.resolve("S11-FINDING-REPRODUCTION.json");
            Path shaPath = out.resolve("S11-FINDING-REPRODUCTION.json.sha256");
            Files.writeString(jsonPath, confirmedJson.content(), StandardCharsets.UTF_8);
            Files.writeString(
                    shaPath,
                    confirmedJson.sha256() + "  S11-FINDING-REPRODUCTION.json\n",
                    StandardCharsets.UTF_8);
            TestSupport.assertTrue(Files.isRegularFile(jsonPath),
                    "canonical Sprint 11 reproduction JSON artifact written");
            assertions++;
            TestSupport.assertTrue(Files.isRegularFile(shaPath),
                    "canonical Sprint 11 reproduction SHA-256 sidecar written");
            assertions++;
            TestSupport.assertEquals(
                    confirmedJson.content(),
                    Files.readString(jsonPath, StandardCharsets.UTF_8),
                    "written JSON artifact equals canonical export content");
            assertions++;
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("unable to write Sprint 11 reproduction artifacts", failure);
        }

        return assertions;
    }

    private static FindingCandidate candidate() {
        return new FindingCandidate(
                "candidate-json",
                FindingCandidateState.CANDIDATE,
                "project-s11",
                List.of("test-json"),
                List.of("execution-json"),
                List.of("observation-json"),
                List.of("assessment-json"),
                List.of("BATCH_AUTHORIZATION"),
                "/api/v1/s11/review",
                "resource-b",
                "user-a",
                "CROSS_TENANT",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("candidate-json-evidence"),
                List.of(),
                List.of("policy-json"),
                "MEDIUM",
                "password=DummyPassword; excluded source rationale",
                FindingFingerprint.of(
                        "/api/v1/s11/review",
                        "resource-b",
                        "user-a",
                        "CROSS_TENANT",
                        "BATCH_AUTHORIZATION",
                        "CANDIDATE"));
    }

    private static AuthorizationRiskAssessment risk(String candidateId) {
        return new AuthorizationRiskAssessment(
                "risk-" + candidateId,
                candidateId,
                FindingSeverity.HIGH,
                FindingConfidence.MEDIUM,
                75,
                "risk rationale excluded from reproduction",
                List.of("authorization-impact"));
    }
}
