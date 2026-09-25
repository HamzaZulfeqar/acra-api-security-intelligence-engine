package io.acra.core.tests.sprint12;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.product.reproduction.S12ReproductionWorkspace;
import io.acra.core.reporting.s12.S12ReproductionPackage;
import io.acra.core.reporting.s12.S12ReproductionPackageFactory;
import io.acra.core.tests.TestSupport;
import java.util.List;

public final class Sprint12ReproductionSecurityHardeningTestSuite {
    private Sprint12ReproductionSecurityHardeningTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT12_REPRODUCTION_SECURITY_HARDENING PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        S12ReproductionPackageFactory factory = new S12ReproductionPackageFactory();
        S12ReproductionPackage base = factory.from(candidate(
                "s12-hardening-candidate",
                "/api/v1/documents/1002",
                "document:1002"));

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> copy(base, "/api/v1/documents/1002?token=abc", base.resourceId(),
                        base.projectId(), base.evidenceIds()),
                "reproduction endpoint query material fails closed");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> copy(base, "/api/v1/documents/1002#fragment", base.resourceId(),
                        base.projectId(), base.evidenceIds()),
                "reproduction endpoint fragment material fails closed");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> copy(base, base.endpoint(), base.resourceId(),
                        "Authorization: Bearer abcdefghijklmnopqrstuvwxyz", base.evidenceIds()),
                "secret-bearing project material fails closed");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> copy(base, base.endpoint(), base.resourceId(),
                        base.projectId(), List.of("evidence-safe", "token=supersecret")),
                "secret-bearing evidence reference fails closed");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new S12ReproductionPackage(
                        "",
                        base.packageVersion(),
                        "authorization: Bearer abcdefghijklmnopqrstuvwxyz",
                        base.candidateState(),
                        base.projectId(),
                        base.endpoint(),
                        base.resourceId(),
                        base.expectedDecision(),
                        base.observedDecision(),
                        base.dimensions(),
                        base.evidenceIds(),
                        base.policyReferences(),
                        base.confidence(),
                        base.limitations(),
                        ""),
                "secret-bearing candidate identifier fails closed");
        assertions++;

        S12ReproductionWorkspace workspace = new S12ReproductionWorkspace();
        workspace.recordPackage(base);
        workspace.recordPackage(base);
        TestSupport.assertEquals(1, workspace.snapshot().packageCount(),
                "identical reproduction package is idempotent");
        assertions++;

        S12ReproductionPackage drifted = copy(
                base,
                base.endpoint(),
                "document:1003",
                base.projectId(),
                base.evidenceIds());
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> workspace.recordPackage(drifted),
                "same candidate cannot drift to a different reproduction package");
        assertions++;

        TestSupport.assertEquals(1, workspace.snapshot().packageCount(),
                "rejected candidate drift cannot mutate workspace");
        assertions++;

        workspace.clear();
        TestSupport.assertEquals(0, workspace.snapshot().packageCount(),
                "workspace clear removes package and candidate identity indexes");
        assertions++;

        workspace.recordPackage(drifted);
        TestSupport.assertEquals(1, workspace.snapshot().packageCount(),
                "cleared workspace can accept a new deterministic package");
        assertions++;

        var snapshot = workspace.snapshot();
        TestSupport.assertEquals(0L, snapshot.publishableProjectionCount(),
                "workspace hardening cannot make a Burp projection publishable");
        assertions++;

        var sarif = snapshot.entries().getFirst().sarifExport();
        TestSupport.assertNotContains(sarif.content(), "Authorization:",
                "SARIF export excludes authorization material");
        assertions++;
        TestSupport.assertNotContains(sarif.content(), "token=",
                "SARIF export excludes token-bearing material");
        assertions++;
        TestSupport.assertContains(sarif.content(), "\"reviewOnly\":true",
                "SARIF export retains review-only boundary after hardening");
        assertions++;

        return assertions;
    }

    private static S12ReproductionPackage copy(
            S12ReproductionPackage base,
            String endpoint,
            String resourceId,
            String projectId,
            List<String> evidenceIds) {
        return new S12ReproductionPackage(
                "",
                base.packageVersion(),
                base.sourceCandidateId(),
                base.candidateState(),
                projectId,
                endpoint,
                resourceId,
                base.expectedDecision(),
                base.observedDecision(),
                base.dimensions(),
                evidenceIds,
                base.policyReferences(),
                base.confidence(),
                base.limitations(),
                "");
    }

    private static FindingCandidate candidate(
            String candidateId,
            String endpoint,
            String resourceId) {
        return new FindingCandidate(
                candidateId,
                FindingCandidateState.CANDIDATE,
                "acra-s12",
                List.of("test-s12"),
                List.of("execution-s12"),
                List.of("observation-s12"),
                List.of("assessment-s12"),
                List.of("OBJECT_AUTHORIZATION"),
                endpoint,
                resourceId,
                "user-a",
                "FOREIGN_TENANT",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("evidence-s12-a", "evidence-s12-b"),
                List.of(),
                List.of("policy-s12"),
                "HIGH",
                "review-only hardening fixture",
                FindingFingerprint.of(
                        endpoint,
                        resourceId,
                        "user-a",
                        "FOREIGN_TENANT",
                        "DENY_TO_ALLOW",
                        "OBJECT_AUTHORIZATION"));
    }
}
