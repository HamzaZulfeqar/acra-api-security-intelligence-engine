package io.acra.burp.tests.sprint12;

import burp.api.montoya.scanner.audit.issues.AuditIssue;
import io.acra.burp.ACRAExtension;
import io.acra.burp.scanner.S12AuditIssueSink;
import io.acra.burp.scanner.S12BurpIssuePublicationApproval;
import io.acra.burp.scanner.S12BurpIssuePublicationReceipt;
import io.acra.burp.scanner.S12BurpIssuePublisher;
import io.acra.burp.scanner.S12MontoyaAuditIssueAdapter;
import io.acra.burp.scanner.S12MontoyaAuditIssueFactory;
import io.acra.burp.scanner.S12MontoyaAuditIssueSpec;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.reporting.s12.S12BurpIssueProjector;
import io.acra.core.reporting.s12.S12ReproductionPackageFactory;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class Sprint12BurpIssuePublicationBoundaryTestSuite {
    private Sprint12BurpIssuePublicationBoundaryTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT12_BURP_ISSUE_PUBLICATION_BOUNDARY PASS assertions=" + assertions);
    }

    public static int run() {
        var projection = new S12BurpIssueProjector().project(
                new S12ReproductionPackageFactory().from(fixture()));
        var adapter = new S12MontoyaAuditIssueAdapter();
        var factory = new FakeFactory();
        var sink = new FakeSink();
        var publisher = new S12BurpIssuePublisher(adapter, factory, sink);
        int assertions = 0;

        assertTrue(!projection.publishable(),
                "core projection remains non-publishable before explicit approval");
        assertions++;

        var denied = new S12BurpIssuePublicationApproval(
                projection.candidateId(), false, "denied-ref",
                "https://acra-lab.invalid/api/v1/documents/1002");
        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish(projection, denied),
                "publisher rejects denied approval");
        assertions++;
        assertEquals(0, factory.created.get(),
                "denied approval creates no Montoya issue");
        assertions++;
        assertEquals(0, sink.added.get(),
                "denied approval publishes nothing");
        assertions++;

        var mismatch = new S12BurpIssuePublicationApproval(
                "other-candidate", true, "mismatch-ref",
                "https://acra-lab.invalid/api/v1/documents/1002");
        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish(projection, mismatch),
                "publisher rejects candidate-mismatched approval");
        assertions++;
        assertEquals(0, factory.created.get(),
                "mismatched approval creates no issue");
        assertions++;
        assertEquals(0, sink.added.get(),
                "mismatched approval publishes nothing");
        assertions++;

        var approved = new S12BurpIssuePublicationApproval(
                projection.candidateId(), true, "human-approval-002",
                "https://acra-lab.invalid/api/v1/documents/1002");

        S12BurpIssuePublicationReceipt receiptA = publisher.publish(projection, approved);
        assertEquals(1, factory.created.get(),
                "approved publication creates exactly one Montoya issue");
        assertions++;
        assertEquals(1, sink.added.get(),
                "approved publication adds exactly one issue to injected sink");
        assertions++;
        assertEquals(
                S12BurpIssuePublicationReceipt.IMPORTED_REVIEW_CANDIDATE,
                receiptA.state(),
                "receipt remains review-candidate import rather than confirmed vulnerability");
        assertions++;
        assertEquals(projection.candidateId(), receiptA.candidateId(),
                "receipt preserves candidate identity");
        assertions++;
        assertEquals(approved.approvalReference(), receiptA.approvalReference(),
                "receipt preserves explicit human approval reference");
        assertions++;

        FakeFactory repeatFactory = new FakeFactory();
        FakeSink repeatSink = new FakeSink();
        S12BurpIssuePublicationReceipt receiptB =
                new S12BurpIssuePublisher(adapter, repeatFactory, repeatSink)
                        .publish(projection, approved);
        assertEquals(receiptA.receiptId(), receiptB.receiptId(),
                "publication receipt identity is deterministic");
        assertions++;
        assertEquals(receiptA.fingerprint(), receiptB.fingerprint(),
                "publication receipt fingerprint is deterministic");
        assertions++;

        assertEquals("INFORMATION", factory.lastSpec.severity().name(),
                "published spec remains informational");
        assertions++;
        assertEquals("TENTATIVE", factory.lastSpec.confidence().name(),
                "published spec remains tentative");
        assertions++;
        assertTrue(!projection.publishable(),
                "successful publication does not mutate core projection publishable state");
        assertions++;
        assertEquals(FindingCandidateState.CANDIDATE, fixture().state(),
                "successful publication does not mutate FindingCandidate state");
        assertions++;

        boolean extensionReferencesPublisher = java.util.Arrays.stream(ACRAExtension.class.getDeclaredFields())
                .anyMatch(field -> field.getType().equals(S12BurpIssuePublisher.class));
        assertTrue(!extensionReferencesPublisher,
                "ACRAExtension bootstrap has no publisher field");
        assertions++;

        return assertions;
    }

    private static final class FakeFactory implements S12MontoyaAuditIssueFactory {
        private final AtomicInteger created = new AtomicInteger();
        private S12MontoyaAuditIssueSpec lastSpec;

        @Override
        public AuditIssue create(S12MontoyaAuditIssueSpec spec) {
            lastSpec = spec;
            created.incrementAndGet();
            return (AuditIssue) Proxy.newProxyInstance(
                    AuditIssue.class.getClassLoader(),
                    new Class<?>[]{AuditIssue.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("toString")) return "FakeAuditIssue";
                        if (method.getReturnType().equals(boolean.class)) return false;
                        if (method.getReturnType().equals(int.class)) return 0;
                        if (method.getReturnType().equals(long.class)) return 0L;
                        return null;
                    });
        }
    }

    private static final class FakeSink implements S12AuditIssueSink {
        private final AtomicInteger added = new AtomicInteger();

        @Override
        public void add(AuditIssue issue) {
            if (issue == null) throw new AssertionError("sink received null issue");
            added.incrementAndGet();
        }
    }

    private static FindingCandidate fixture() {
        return new FindingCandidate(
                "s12-publish-candidate",
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
                List.of("evidence-s12-a"),
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

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertThrows(
            Class<? extends Throwable> expected,
            Runnable action,
            String message) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError(message + " wrong exception=" + failure, failure);
        }
        throw new AssertionError(message + " expected exception=" + expected.getSimpleName());
    }
}
