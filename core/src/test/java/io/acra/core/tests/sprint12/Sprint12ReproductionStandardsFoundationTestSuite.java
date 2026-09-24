package io.acra.core.tests.sprint12;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.reporting.s12.S12BurpIssueProjector;
import io.acra.core.reporting.s12.S12ReproductionJsonExporter;
import io.acra.core.reporting.s12.S12ReproductionPackage;
import io.acra.core.reporting.s12.S12ReproductionPackageFactory;
import io.acra.core.reporting.s12.S12SarifExporter;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.tests.TestSupport;
import java.util.List;

public final class Sprint12ReproductionStandardsFoundationTestSuite {
    private Sprint12ReproductionStandardsFoundationTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT12_REPRODUCTION_STANDARDS_FOUNDATION PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        FindingCandidate candidate = fixture(FindingCandidateState.CANDIDATE);
        S12ReproductionPackageFactory factory = new S12ReproductionPackageFactory();
        S12ReproductionPackage reproduction = factory.from(candidate);

        TestSupport.assertEquals(S12ReproductionPackage.VERSION, reproduction.packageVersion(),
                "reproduction package version is explicit");
        assertions++;
        TestSupport.assertEquals(candidate.candidateId(), reproduction.sourceCandidateId(),
                "reproduction package preserves source candidate ID");
        assertions++;
        TestSupport.assertEquals(FindingCandidateState.CANDIDATE, reproduction.candidateState(),
                "candidate state remains review-only");
        assertions++;
        TestSupport.assertEquals(candidate.expectedDecision(), reproduction.expectedDecision(),
                "expected authorization is preserved");
        assertions++;
        TestSupport.assertEquals(candidate.observedDecision(), reproduction.observedDecision(),
                "observed authorization is preserved");
        assertions++;
        TestSupport.assertEquals(candidate.supportingEvidenceIds(), reproduction.evidenceIds(),
                "supporting evidence IDs are preserved");
        assertions++;
        TestSupport.assertTrue(reproduction.limitations().stream()
                        .anyMatch(value -> value.contains("not a confirmed vulnerability")),
                "package states candidate is not confirmed vulnerability");
        assertions++;

        S12ReproductionPackage repeated = factory.from(candidate);
        TestSupport.assertEquals(reproduction.packageId(), repeated.packageId(),
                "reproduction package ID is deterministic");
        assertions++;
        TestSupport.assertEquals(reproduction.fingerprint(), repeated.fingerprint(),
                "reproduction package fingerprint is deterministic");
        assertions++;

        var jsonA = new S12ReproductionJsonExporter().export(reproduction);
        var jsonB = new S12ReproductionJsonExporter().export(reproduction);
        TestSupport.assertEquals(jsonA.content(), jsonB.content(),
                "JSON reproduction export is deterministic");
        assertions++;
        TestSupport.assertEquals(TokenFingerprint.sha256(jsonA.content()), jsonA.sha256(),
                "JSON reproduction digest matches content");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"packageVersion\":\"s12-reproduction-package-v1\"",
                "JSON declares stable reproduction package version");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "DummyPassword",
                "JSON excludes candidate rationale secret material");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "\"principalId\"",
                "JSON structurally excludes raw principal identifier");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "\"rationale\"",
                "JSON structurally excludes candidate rationale");
        assertions++;

        var sarifA = new S12SarifExporter().export(reproduction);
        var sarifB = new S12SarifExporter().export(reproduction);
        TestSupport.assertEquals(sarifA.content(), sarifB.content(),
                "SARIF export is deterministic");
        assertions++;
        TestSupport.assertEquals(TokenFingerprint.sha256(sarifA.content()), sarifA.sha256(),
                "SARIF digest matches content");
        assertions++;
        TestSupport.assertContains(sarifA.content(), "\"version\":\"2.1.0\"",
                "SARIF declares version 2.1.0");
        assertions++;
        TestSupport.assertContains(sarifA.content(), "\"name\":\"ACRA\"",
                "SARIF declares ACRA tool driver");
        assertions++;
        TestSupport.assertContains(sarifA.content(), "\"kind\":\"review\"",
                "candidate maps to SARIF review result");
        assertions++;
        TestSupport.assertContains(sarifA.content(), "\"level\":\"none\"",
                "review SARIF result carries level none");
        assertions++;
        TestSupport.assertContains(sarifA.content(), "\"reviewOnly\":true",
                "SARIF preserves review-only boundary");
        assertions++;
        TestSupport.assertNotContains(sarifA.content(), "DummyPassword",
                "SARIF excludes candidate rationale secret material");
        assertions++;
        TestSupport.assertNotContains(sarifA.content(), "user-a",
                "SARIF excludes raw principal identifier");
        assertions++;

        var burp = new S12BurpIssueProjector().project(reproduction);
        TestSupport.assertEquals("s12-burp-issue-projection-v1", burp.projectionVersion(),
                "Burp issue projection version is explicit");
        assertions++;
        TestSupport.assertEquals("INFORMATION", burp.severity(),
                "review-only Burp projection uses informational severity");
        assertions++;
        TestSupport.assertEquals("TENTATIVE", burp.confidence(),
                "review-only Burp projection uses tentative confidence");
        assertions++;
        TestSupport.assertTrue(!burp.publishable(),
                "Phase 1 Burp projection cannot publish a Scanner issue");
        assertions++;
        TestSupport.assertContains(burp.detail(), "Human verification is required",
                "Burp projection preserves human-review requirement");
        assertions++;
        TestSupport.assertNotContains(burp.detail(), "DummyPassword",
                "Burp projection excludes candidate rationale secret material");
        assertions++;

        S12ReproductionPackage rejected = factory.from(fixture(FindingCandidateState.REJECTED));
        var rejectedSarif = new S12SarifExporter().export(rejected);
        TestSupport.assertContains(rejectedSarif.content(), "\"kind\":\"informational\"",
                "rejected control maps to SARIF informational result");
        assertions++;
        TestSupport.assertContains(rejectedSarif.content(), "\"level\":\"none\"",
                "informational SARIF result carries level none");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new S12ReproductionPackage(
                        "tampered-id",
                        reproduction.packageVersion(),
                        reproduction.sourceCandidateId(),
                        reproduction.candidateState(),
                        reproduction.projectId(),
                        reproduction.endpoint(),
                        reproduction.resourceId(),
                        reproduction.expectedDecision(),
                        reproduction.observedDecision(),
                        reproduction.dimensions(),
                        reproduction.evidenceIds(),
                        reproduction.policyReferences(),
                        reproduction.confidence(),
                        reproduction.limitations(),
                        ""),
                "tampered package ID fails closed");
        assertions++;

        FindingCandidate noEvidence = new FindingCandidate(
                "s12-no-evidence",
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
                List.of(),
                List.of(),
                List.of("policy-s12"),
                "HIGH",
                "candidate without evidence",
                FindingFingerprint.of(
                        "/api/v1/documents/1002",
                        "document:1002",
                        "user-a",
                        "FOREIGN_TENANT",
                        "DENY_TO_ALLOW",
                        "OBJECT_AUTHORIZATION"));
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> factory.from(noEvidence),
                "review candidate without supporting evidence fails closed");
        assertions++;

        return assertions;
    }

    private static FindingCandidate fixture(FindingCandidateState state) {
        return new FindingCandidate(
                "s12-candidate-001-" + state.name().toLowerCase(),
                state,
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
                "Review fixture; password=DummyPassword; candidate remains review-only",
                FindingFingerprint.of(
                        "/api/v1/documents/1002",
                        "document:1002",
                        "user-a",
                        "FOREIGN_TENANT",
                        "DENY_TO_ALLOW",
                        "OBJECT_AUTHORIZATION"));
    }
}
