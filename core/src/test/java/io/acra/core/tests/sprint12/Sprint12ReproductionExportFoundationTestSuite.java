package io.acra.core.tests.sprint12;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.reporting.reproduction.ReproductionPackageExporter;
import io.acra.core.reporting.reproduction.ReproductionPackageFactory;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.tests.TestSupport;
import java.util.List;

public final class Sprint12ReproductionExportFoundationTestSuite {
    private Sprint12ReproductionExportFoundationTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT12_REPRODUCTION_EXPORT_FOUNDATION PASS assertions=" + assertions);
    }

    public static int run() {
        FindingCandidate candidate = candidate(FindingCandidateState.CANDIDATE);
        ReproductionPackageFactory factory = new ReproductionPackageFactory();
        var reproduction = factory.from(candidate);
        var exporter = new ReproductionPackageExporter();
        int assertions = 0;

        TestSupport.assertEquals("acra-reproduction-v1", reproduction.packageVersion(),
                "reproduction package version is explicit");
        assertions++;
        TestSupport.assertEquals(candidate.candidateId(), reproduction.candidateId(),
                "reproduction package preserves candidate identity");
        assertions++;
        TestSupport.assertEquals(FindingCandidateState.CANDIDATE, reproduction.state(),
                "review candidate state is preserved without promotion");
        assertions++;
        TestSupport.assertEquals(AuthorizationDecision.DENY, reproduction.expectedDecision(),
                "expected authorization decision is preserved");
        assertions++;
        TestSupport.assertEquals(AuthorizationDecision.ALLOW, reproduction.observedDecision(),
                "observed authorization decision is preserved");
        assertions++;
        TestSupport.assertEquals(candidate.supportingEvidenceIds(), reproduction.evidenceIds(),
                "evidence references are preserved");
        assertions++;
        TestSupport.assertEquals(candidate.policyReferences(), reproduction.policyReferences(),
                "policy references are preserved");
        assertions++;

        var repeated = factory.from(candidate);
        TestSupport.assertEquals(reproduction.packageId(), repeated.packageId(),
                "reproduction package identity is deterministic");
        assertions++;

        var jsonA = exporter.json(reproduction);
        var jsonB = exporter.json(reproduction);
        TestSupport.assertEquals(jsonA.content(), jsonB.content(),
                "canonical JSON reproduction export is deterministic");
        assertions++;
        TestSupport.assertEquals(jsonA.sha256(), jsonB.sha256(),
                "deterministic JSON export produces stable SHA-256");
        assertions++;
        TestSupport.assertEquals(TokenFingerprint.sha256(jsonA.content()), jsonA.sha256(),
                "JSON artifact digest matches content");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "acra-reproduction-v1",
                "JSON declares reproduction package version");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "CANDIDATE",
                "JSON preserves review-only candidate state");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "DummyPassword",
                "JSON excludes/redacts candidate rationale secret");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "user-a",
                "JSON structurally excludes raw principal identifier");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "tenant-a",
                "JSON structurally excludes raw tenant identifier");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "rationale",
                "JSON reproduction package contains no rationale field");
        assertions++;

        var sarifA = exporter.sarif(reproduction);
        var sarifB = exporter.sarif(reproduction);
        TestSupport.assertEquals(sarifA.content(), sarifB.content(),
                "SARIF export is deterministic");
        assertions++;
        TestSupport.assertEquals(sarifA.sha256(), sarifB.sha256(),
                "SARIF export SHA-256 is deterministic");
        assertions++;
        TestSupport.assertEquals(TokenFingerprint.sha256(sarifA.content()), sarifA.sha256(),
                "SARIF artifact digest matches content");
        assertions++;
        TestSupport.assertContains(sarifA.content(), "\"version\":\"2.1.0\"",
                "SARIF declares version 2.1.0");
        assertions++;
        TestSupport.assertContains(sarifA.content(), "\"ruleId\":\"ACRA-AUTHORIZATION-REVIEW\"",
                "SARIF uses stable ACRA review rule");
        assertions++;
        TestSupport.assertContains(sarifA.content(), "\"level\":\"warning\"",
                "candidate is exported as review warning, not confirmed error");
        assertions++;
        TestSupport.assertContains(sarifA.content(), "\"confirmed\":false",
                "SARIF explicitly preserves non-confirmation boundary");
        assertions++;
        TestSupport.assertNotContains(sarifA.content(), "\"level\":\"error\"",
                "SARIF review artifact cannot claim confirmed error severity");
        assertions++;
        TestSupport.assertNotContains(sarifA.content(), "DummyPassword",
                "SARIF excludes candidate rationale secret");
        assertions++;
        TestSupport.assertNotContains(sarifA.content(), "user-a",
                "SARIF excludes raw principal identifier");
        assertions++;
        TestSupport.assertNotContains(sarifA.content(), "tenant-a",
                "SARIF excludes raw tenant identifier");
        assertions++;

        var rejected = exporter.sarif(factory.from(candidate(FindingCandidateState.REJECTED)));
        TestSupport.assertContains(rejected.content(), "\"level\":\"note\"",
                "rejected control exports as informational SARIF note");
        assertions++;

        var inconclusive = exporter.sarif(factory.from(candidate(FindingCandidateState.INCONCLUSIVE)));
        TestSupport.assertContains(inconclusive.content(), "\"level\":\"note\"",
                "inconclusive projection exports as informational SARIF note");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> factory.from(null),
                "null finding candidate is rejected");
        assertions++;

        return assertions;
    }

    private static FindingCandidate candidate(FindingCandidateState state) {
        return new FindingCandidate(
                "s12-candidate-" + state.name().toLowerCase(),
                state,
                "s12-project",
                List.of("s12-test"),
                List.of("s12-execution"),
                List.of("s12-observation"),
                List.of("s12-assessment"),
                List.of("BATCH_AUTHORIZATION"),
                "/api/v1/documents/resource-b",
                "resource-b",
                "user-a",
                "tenant-a",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("evidence-s12-1", "evidence-s12-2"),
                List.of(),
                List.of("policy-s12"),
                "HIGH",
                "Review candidate password=DummyPassword; do not auto-confirm.",
                FindingFingerprint.of(
                        "/api/v1/documents/resource-b",
                        "resource-b",
                        "user-a",
                        "tenant-a",
                        "BATCH_AUTHORIZATION",
                        state.name()));
    }
}
