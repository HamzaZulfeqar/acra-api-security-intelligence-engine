package io.acra.core.tests.sprint12;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.reproduction.ReproductionExportCapabilityState;
import io.acra.core.reproduction.ReproductionExportRegistry;
import io.acra.core.reproduction.ReproductionExportTarget;
import io.acra.core.reproduction.ReproductionInteroperabilityBundle;
import io.acra.core.reproduction.ReproductionInteroperabilityService;
import io.acra.core.tests.TestSupport;
import java.util.List;

public final class Sprint12InteroperabilityHardeningTestSuite {
    private Sprint12InteroperabilityHardeningTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT12_INTEROPERABILITY_HARDENING PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        ReproductionInteroperabilityService service = new ReproductionInteroperabilityService();
        ReproductionInteroperabilityBundle bundle = service.project(
                candidate(FindingCandidateState.CANDIDATE),
                FindingSeverity.HIGH,
                "Authorization mismatch under review",
                "https://api.example.test");

        var pkg = bundle.reproductionPackage();

        TestSupport.assertTrue(bundle.bundleId().startsWith("interop-"),
                "interoperability bundle has deterministic identifier");
        assertions++;
        TestSupport.assertEquals(64, bundle.fingerprint().length(),
                "interoperability bundle fingerprint is SHA-256");
        assertions++;
        TestSupport.assertTrue(pkg.reviewOnly(),
                "shared reproduction package is review-only");
        assertions++;
        TestSupport.assertTrue(bundle.burpIssue().reviewOnly(),
                "Burp projection is review-only");
        assertions++;
        TestSupport.assertEquals(pkg.packageId(), bundle.burpIssue().packageId(),
                "Burp projection retains package lineage");
        assertions++;
        TestSupport.assertContains(bundle.jsonArtifact().content(), pkg.packageId(),
                "JSON retains package lineage");
        assertions++;
        TestSupport.assertContains(bundle.sarifArtifact().content(), pkg.packageId(),
                "SARIF retains package lineage");
        assertions++;
        TestSupport.assertContains(bundle.jsonArtifact().content(), pkg.candidateId(),
                "JSON retains candidate lineage");
        assertions++;
        TestSupport.assertContains(bundle.sarifArtifact().content(), pkg.candidateId(),
                "SARIF retains candidate lineage");
        assertions++;

        for (String evidenceId : pkg.evidenceIds()) {
            TestSupport.assertContains(bundle.jsonArtifact().content(), evidenceId,
                    "JSON retains each evidence ID");
            assertions++;
            TestSupport.assertContains(bundle.sarifArtifact().content(), evidenceId,
                    "SARIF retains each evidence ID");
            assertions++;
            TestSupport.assertTrue(bundle.burpIssue().evidenceIds().contains(evidenceId),
                    "Burp projection retains each evidence ID");
            assertions++;
        }

        TestSupport.assertContains(bundle.jsonArtifact().content(), "\"reviewOnly\":true",
                "JSON explicitly carries review-only state");
        assertions++;
        TestSupport.assertContains(bundle.sarifArtifact().content(), "\"acraReviewOnly\":true",
                "SARIF explicitly carries review-only state");
        assertions++;
        TestSupport.assertContains(bundle.sarifArtifact().content(), "\"kind\":\"review\"",
                "SARIF result is a review result");
        assertions++;
        TestSupport.assertContains(bundle.burpIssue().detail(), "not automatically confirmed",
                "Burp detail explicitly prevents automatic confirmation");
        assertions++;

        ReproductionInteroperabilityBundle repeated = service.project(
                candidate(FindingCandidateState.CANDIDATE),
                FindingSeverity.HIGH,
                "Authorization mismatch under review",
                "https://api.example.test");
        TestSupport.assertEquals(bundle.bundleId(), repeated.bundleId(),
                "interoperability bundle ID is deterministic");
        assertions++;
        TestSupport.assertEquals(bundle.fingerprint(), repeated.fingerprint(),
                "interoperability bundle fingerprint is deterministic");
        assertions++;
        TestSupport.assertEquals(bundle.jsonArtifact().sha256(), repeated.jsonArtifact().sha256(),
                "JSON digest is deterministic");
        assertions++;
        TestSupport.assertEquals(bundle.sarifArtifact().sha256(), repeated.sarifArtifact().sha256(),
                "SARIF digest is deterministic");
        assertions++;
        TestSupport.assertEquals(bundle.burpIssue().fingerprint(), repeated.burpIssue().fingerprint(),
                "Burp projection fingerprint is deterministic");
        assertions++;

        ReproductionExportRegistry registry = new ReproductionExportRegistry();
        TestSupport.assertEquals(
                ReproductionExportCapabilityState.IMPLEMENTED,
                registry.capability(ReproductionExportTarget.JSON).state(),
                "JSON capability remains IMPLEMENTED");
        assertions++;
        TestSupport.assertEquals(
                ReproductionExportCapabilityState.IMPLEMENTED,
                registry.capability(ReproductionExportTarget.SARIF).state(),
                "SARIF capability remains IMPLEMENTED");
        assertions++;
        TestSupport.assertEquals(
                ReproductionExportCapabilityState.IMPLEMENTED_RUNTIME_UNVERIFIED,
                registry.capability(ReproductionExportTarget.BURP_ISSUE).state(),
                "Burp capability preserves runtime-unverified distinction");
        assertions++;

        var secretBundle = service.project(
                secretCandidate(),
                FindingSeverity.HIGH,
                "Authorization: Bearer aaa.bbb.ccc token=topsecret",
                "https://api.example.test");
        for (String secret : List.of("aaa.bbb.ccc", "topsecret")) {
            TestSupport.assertNotContains(secretBundle.jsonArtifact().content(), secret,
                    "JSON remains secret-safe");
            assertions++;
            TestSupport.assertNotContains(secretBundle.sarifArtifact().content(), secret,
                    "SARIF remains secret-safe");
            assertions++;
            TestSupport.assertNotContains(secretBundle.burpIssue().detail(), secret,
                    "Burp detail remains secret-safe");
            assertions++;
            TestSupport.assertNotContains(secretBundle.burpIssue().baseUrl(), secret,
                    "Burp URL remains secret-safe");
            assertions++;
        }

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> service.project(
                        candidate(FindingCandidateState.REJECTED),
                        FindingSeverity.LOW,
                        "rejected",
                        "https://api.example.test"),
                "REJECTED candidate fails unified Burp interoperability projection");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> service.project(
                        candidate(FindingCandidateState.INCONCLUSIVE),
                        FindingSeverity.LOW,
                        "inconclusive",
                        "https://api.example.test"),
                "INCONCLUSIVE candidate fails unified Burp interoperability projection");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> service.project(
                        candidate(FindingCandidateState.CANDIDATE),
                        FindingSeverity.HIGH,
                        "review",
                        "https://user:pass@api.example.test"),
                "userinfo-bearing origin fails closed");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new ReproductionInteroperabilityBundle(
                        "",
                        pkg,
                        bundle.sarifArtifact(),
                        bundle.jsonArtifact(),
                        bundle.burpIssue(),
                        ""),
                "swapped artifact targets fail closed");
        assertions++;

        return assertions;
    }

    private static FindingCandidate candidate(FindingCandidateState state) {
        return new FindingCandidate(
                "candidate-interop-" + state.name().toLowerCase(),
                state,
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
                List.of("evidence-interop-1", "evidence-interop-2"),
                List.of(),
                List.of("policy-interop-1"),
                "HIGH",
                "Expected DENY but observed ALLOW",
                FindingFingerprint.of(
                        "/api/v1/documents/{id}", "document:42", "user-a", "CROSS_TENANT",
                        "DENY_TO_ALLOW", "OBJECT_AUTHORIZATION"));
    }

    private static FindingCandidate secretCandidate() {
        return new FindingCandidate(
                "candidate-interop-secret",
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
