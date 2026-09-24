package io.acra.core.tests.sprint12;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.reproduction.ReproductionExportCapabilityState;
import io.acra.core.reproduction.ReproductionExportRegistry;
import io.acra.core.reproduction.ReproductionExportTarget;
import io.acra.core.reproduction.ReproductionPackageProjector;
import io.acra.core.tests.TestSupport;
import java.util.List;

public final class Sprint12ReproductionPackageFoundationTestSuite {
    private Sprint12ReproductionPackageFoundationTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT12_REPRODUCTION_PACKAGE_FOUNDATION PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        FindingCandidate candidate = candidate();
        ReproductionPackageProjector projector = new ReproductionPackageProjector();
        var pkg = projector.project(candidate, FindingSeverity.HIGH, "Authorization mismatch under review");

        TestSupport.assertEquals("acra-reproduction-package-v1", pkg.schemaVersion(),
                "reproduction schema is versioned");
        assertions++;
        TestSupport.assertEquals(candidate.candidateId(), pkg.candidateId(),
                "candidate identity is retained");
        assertions++;
        TestSupport.assertEquals(FindingCandidateState.CANDIDATE, pkg.candidateState(),
                "candidate lifecycle is retained");
        assertions++;
        TestSupport.assertTrue(pkg.reviewOnly(),
                "reproduction package cannot claim confirmed vulnerability");
        assertions++;
        TestSupport.assertEquals(FindingSeverity.HIGH, pkg.severity(),
                "severity remains independent metadata");
        assertions++;
        TestSupport.assertEquals(AuthorizationDecision.DENY, pkg.expectedDecision(),
                "expected authorization decision is retained");
        assertions++;
        TestSupport.assertEquals(AuthorizationDecision.ALLOW, pkg.observedDecision(),
                "observed authorization decision is retained");
        assertions++;
        TestSupport.assertEquals(
                List.of(
                        ReproductionExportTarget.JSON,
                        ReproductionExportTarget.SARIF,
                        ReproductionExportTarget.BURP_ISSUE),
                pkg.declaredTargets(),
                "FR-013 target contracts are explicit");
        assertions++;
        TestSupport.assertTrue(pkg.packageId().startsWith("rp-"),
                "package ID uses deterministic prefix");
        assertions++;
        TestSupport.assertEquals(67, pkg.fingerprint().length(),
                "package fingerprint is SHA-256");
        assertions++;

        var repeated = projector.project(candidate, FindingSeverity.HIGH, "Authorization mismatch under review");
        TestSupport.assertEquals(pkg.packageId(), repeated.packageId(),
                "package ID is deterministic");
        assertions++;
        TestSupport.assertEquals(pkg.fingerprint(), repeated.fingerprint(),
                "package fingerprint is deterministic");
        assertions++;

        FindingCandidate secretCandidate = secretCandidate();
        var secretPackage = projector.project(
                secretCandidate,
                FindingSeverity.MEDIUM,
                "Authorization: Bearer abc.def.ghi");
        TestSupport.assertNotContains(secretPackage.summary(), "abc.def.ghi",
                "summary redacts bearer-like secret material");
        assertions++;
        TestSupport.assertNotContains(secretPackage.endpoint(), "topsecret",
                "endpoint redacts sensitive key-value material");
        assertions++;
        TestSupport.assertNotContains(String.join(",", secretPackage.evidenceIds()), "topsecret",
                "evidence IDs are redacted defensively");
        assertions++;

        ReproductionExportRegistry registry = new ReproductionExportRegistry();
        TestSupport.assertEquals(3, registry.capabilities().size(),
                "registry exposes exactly three FR-013 target contracts");
        assertions++;
        for (ReproductionExportTarget target : ReproductionExportTarget.values()) {
            var capability = registry.capability(target);
            TestSupport.assertEquals(target, capability.target(),
                    "registry resolves each export target");
            assertions++;
            TestSupport.assertEquals(ReproductionExportCapabilityState.CONTRACT_DEFINED, capability.state(),
                    "Phase 1 does not falsely claim renderer implementation");
            assertions++;
        }

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> projector.project(candidateWithoutEvidence(), FindingSeverity.HIGH, "missing evidence"),
                "reproduction package requires supporting evidence");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new io.acra.core.reproduction.ReproductionPackage(
                        pkg.schemaVersion(), "", pkg.candidateId(), pkg.candidateState(), false,
                        pkg.projectId(), pkg.severity(), pkg.confidence(), pkg.endpoint(), pkg.resourceId(),
                        pkg.expectedDecision(), pkg.observedDecision(), pkg.dimensions(), pkg.policyReferences(),
                        pkg.evidenceIds(), pkg.summary(), pkg.declaredTargets(), ""),
                "reproduction package cannot disable review-only boundary");
        assertions++;

        return assertions;
    }

    private static FindingCandidate candidate() {
        return new FindingCandidate(
                "candidate-s12-001",
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

    private static FindingCandidate candidateWithoutEvidence() {
        FindingCandidate value = candidate();
        return new FindingCandidate(
                value.candidateId(), value.state(), value.projectId(), value.testIds(), value.executionIds(),
                value.observationIds(), value.assessmentIds(), value.dimensions(), value.endpoint(),
                value.resourceId(), value.principalId(), value.tenantRelationship(), value.expectedDecision(),
                value.observedDecision(), List.of(), value.contradictoryEvidence(), value.policyReferences(),
                value.confidence(), value.rationale(), value.fingerprint());
    }

    private static FindingCandidate secretCandidate() {
        return new FindingCandidate(
                "candidate-secret",
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
                "Bearer abc.def.ghi",
                FindingFingerprint.of(
                        "/api/v1/resource", "resource-secret", "user-a", "SAME_TENANT",
                        "DENY_TO_ALLOW", "OBJECT_AUTHORIZATION"));
    }
}
