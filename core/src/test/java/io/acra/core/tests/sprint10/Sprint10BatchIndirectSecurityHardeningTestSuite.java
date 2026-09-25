package io.acra.core.tests.sprint10;

import io.acra.core.batch.BatchItemObservation;
import io.acra.core.batch.BatchItemPolicy;
import io.acra.core.coverage.S10AuthorizationCoverageTracker;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.product.batchindirect.S10BatchIndirectWorkspace;
import io.acra.core.reference.IndirectReferencePolicy;
import io.acra.core.reference.IndirectReferenceResolution;
import io.acra.core.reference.IndirectReferenceSource;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;

public final class Sprint10BatchIndirectSecurityHardeningTestSuite {
    private static final String BATCH_ENDPOINT = "/api/v1/s10/documents/batch-read";
    private static final String INDIRECT_ENDPOINT = "/api/v1/s10/share/{alias}";

    private Sprint10BatchIndirectSecurityHardeningTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT10_BATCH_INDIRECT_SECURITY_HARDENING PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> batchObservation("/api/v1/s10/documents/batch-read?resource=resource-b",
                        "resource-b", List.of("e1")),
                "batch observation query material rejected");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> batchObservation("/api/v1/s10/documents/batch-read#resource-b",
                        "resource-b", List.of("e1")),
                "batch observation fragment material rejected");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> batchObservation(BATCH_ENDPOINT, "resource-b", List.of()),
                "batch observation without evidence rejected");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> batchObservation(BATCH_ENDPOINT, "resource-b", List.of("e1", "e1")),
                "duplicate batch evidence rejected");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> batchObservation(BATCH_ENDPOINT, "Authorization: Bearer s10-secret-token", List.of("e1")),
                "secret-bearing batch metadata rejected");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> indirectResolution("/api/v1/s10/share/{alias}?alias=share-b",
                        TokenFingerprint.sha256("share-b"), "resource-b", List.of("e1")),
                "indirect resolution query material rejected");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> indirectResolution("/api/v1/s10/share/{alias}#share-b",
                        TokenFingerprint.sha256("share-b"), "resource-b", List.of("e1")),
                "indirect resolution fragment material rejected");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> indirectResolution(INDIRECT_ENDPOINT, "share-b", "resource-b", List.of("e1")),
                "raw indirect alias rejected where SHA-256 fingerprint is required");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> indirectResolution(INDIRECT_ENDPOINT, TokenFingerprint.sha256("share-b"),
                        "resource-b", List.of("e1", "e1")),
                "duplicate indirect evidence rejected");
        assertions++;

        S10AuthorizationCoverageTracker coverage = new S10AuthorizationCoverageTracker();
        BatchItemPolicy batchPolicy = batchPolicy("s10-sec-batch", "resource-b", AuthorizationDecision.DENY);
        coverage.recordPolicy(batchPolicy);

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> coverage.recordObservation(batchPolicy,
                        batchObservation(BATCH_ENDPOINT, "resource-a", List.of("e-mismatch"))),
                "batch resource-mismatched observation rejected by coverage tracker");
        assertions++;

        BatchItemPolicy driftedBatch = batchPolicy("s10-sec-batch", "resource-b", AuthorizationDecision.ALLOW);
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> coverage.recordPolicy(driftedBatch),
                "batch policy decision drift for existing coverage identity rejected");
        assertions++;

        IndirectReferencePolicy indirectPolicy = indirectPolicy(
                "s10-sec-indirect", "resource-b", AuthorizationDecision.DENY);
        coverage.recordPolicy(indirectPolicy);
        IndirectReferencePolicy driftedIndirect = indirectPolicy(
                "s10-sec-indirect", "resource-b", AuthorizationDecision.ALLOW);
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> coverage.recordPolicy(driftedIndirect),
                "indirect policy decision drift for existing coverage identity rejected");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> coverage.recordObservation(indirectPolicy,
                        indirectResolution(INDIRECT_ENDPOINT, TokenFingerprint.sha256("share-a"),
                                "resource-a", List.of("e-indirect-mismatch"))),
                "indirect resolved-target mismatch rejected by coverage tracker");
        assertions++;

        S10BatchIndirectWorkspace workspace = new S10BatchIndirectWorkspace();
        FindingCandidate nonS10 = candidate(
                "fc-non-s10", List.of("WORKFLOW"), "resource-b");
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> workspace.recordCandidate(nonS10),
                "Sprint 10 workspace rejects non-batch/non-indirect candidate projection");
        assertions++;

        FindingCandidate ambiguousS10 = candidate(
                "fc-dual-s10",
                List.of("BATCH_AUTHORIZATION", "INDIRECT_REFERENCE_AUTHORIZATION"),
                "resource-b");
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> workspace.recordCandidate(ambiguousS10),
                "Sprint 10 workspace rejects candidate carrying both authorization families");
        assertions++;

        TestSupport.assertEquals(0,
                workspace.report(Instant.parse("2026-09-25T00:15:00Z")).summary().confirmedFindingCount(),
                "Sprint 10 report cannot auto-confirm findings");
        assertions++;

        String reportJson = workspace.exportJson(Instant.parse("2026-09-25T00:15:00Z")).content();
        TestSupport.assertNotContains(reportJson, "\"policySource\"",
                "canonical report structurally excludes policySource");
        assertions++;
        TestSupport.assertNotContains(reportJson, "\"rationale\"",
                "canonical report structurally excludes candidate rationale");
        assertions++;

        String secret = "s10-redactor-secret";
        String embedded = "{\"authorization\":\"Bearer " + secret
                + "\",\"reportVersion\":\"s10-batch-indirect-report-v1\","
                + "\"summary\":{\"confirmedFindingCount\":0}}";
        String redacted = new UniversalRedactor().redactText(embedded);
        TestSupport.assertNotContains(redacted, secret,
                "embedded bearer secret is redacted");
        assertions++;
        TestSupport.assertContains(redacted, "s10-batch-indirect-report-v1",
                "redaction preserves subsequent Sprint 10 report fields");
        assertions++;

        return assertions;
    }

    private static BatchItemPolicy batchPolicy(
            String reference, String resourceId, AuthorizationDecision expected) {
        return new BatchItemPolicy(
                reference, "s10-security-fixture", BATCH_ENDPOINT,
                resourceId, "READ", "viewer", "tenant-a", expected, List.of("e-policy-" + reference));
    }

    private static IndirectReferencePolicy indirectPolicy(
            String reference, String resourceId, AuthorizationDecision expected) {
        return new IndirectReferencePolicy(
                reference, "s10-security-fixture", INDIRECT_ENDPOINT,
                resourceId, "READ", "viewer", "tenant-a", expected, List.of("e-policy-" + reference));
    }

    private static BatchItemObservation batchObservation(
            String endpoint, String resourceId, List<String> evidence) {
        return new BatchItemObservation(
                "s10-sec-batch-observation",
                "s10-sec-source",
                "s10-sec-execution",
                "s10-sec-test",
                "s10-sec-batch",
                "item-b",
                endpoint,
                resourceId,
                "READ",
                AuthorizationDecision.ALLOW,
                evidence);
    }

    private static IndirectReferenceResolution indirectResolution(
            String endpoint,
            String fingerprint,
            String resourceId,
            List<String> evidence) {
        return new IndirectReferenceResolution(
                "s10-sec-indirect-resolution",
                "s10-sec-source",
                "s10-sec-execution",
                "s10-sec-test",
                endpoint,
                fingerprint,
                "ALIAS",
                resourceId,
                "READ",
                AuthorizationDecision.ALLOW,
                IndirectReferenceSource.OBSERVED,
                evidence);
    }

    private static FindingCandidate candidate(
            String id, List<String> dimensions, String resourceId) {
        return new FindingCandidate(
                id,
                FindingCandidateState.CANDIDATE,
                "project",
                List.of("test"),
                List.of("execution"),
                List.of("observation"),
                List.of("assessment"),
                dimensions,
                BATCH_ENDPOINT,
                resourceId,
                "user-a",
                "tenant-a",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("e1"),
                List.of(),
                List.of("policy"),
                "HIGH",
                "review-only",
                FindingFingerprint.of(
                        BATCH_ENDPOINT, resourceId, "user-a", "tenant-a",
                        String.join("+", dimensions), "CANDIDATE"));
    }
}
