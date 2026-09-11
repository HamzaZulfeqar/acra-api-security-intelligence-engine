package io.acra.core.tests.sprint5;

import io.acra.core.domain.authorization.Action;
import io.acra.core.domain.authorization.ActionType;
import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.BflaAssessmentStatus;
import io.acra.core.domain.authorization.BolaAssessmentStatus;
import io.acra.core.domain.authorization.ContextStatus;
import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.evidence.EvidenceSource;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.domain.identity.Principal;
import io.acra.core.domain.identity.Role;
import io.acra.core.domain.resource.Resource;
import io.acra.core.domain.tenant.Tenant;
import io.acra.core.engine.BflaAssessmentEvaluator;
import io.acra.core.engine.BolaAssessmentEvaluator;
import io.acra.core.tests.TestSupport;

import java.util.List;

/** Focused fail-closed guards for the existing S5 BOLA and BFLA foundations. */
public final class Sprint5AssessmentGuardTestSuite {
    private Sprint5AssessmentGuardTestSuite() {
    }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT5_ASSESSMENT_GUARDS PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        assertions += bolaGuards();
        assertions += bflaGuards();
        return assertions;
    }

    private static int bolaGuards() {
        int assertions = 0;
        BolaAssessmentEvaluator evaluator = new BolaAssessmentEvaluator();
        AuthorizationContext candidate = context(principal("user-a"), role("user"),
                resource("document-2001", "user-b"), "user-b", action(ActionType.READ, "READ"),
                AuthorizationDecision.DENY, AuthorizationDecision.ALLOW, List.of("evidence-1"), ContextStatus.RESOLVED);

        TestSupport.assertEquals(BolaAssessmentStatus.BOLA_CANDIDATE,
                evaluator.evaluate(candidate, "observation-1", "execution-1", "test-1").status(),
                "complete BOLA candidate remains eligible");
        assertions++;
        TestSupport.assertEquals(BolaAssessmentStatus.INCONCLUSIVE,
                evaluator.evaluate(withStatus(candidate, ContextStatus.PARTIAL), "observation-1", "execution-1", "test-1").status(),
                "partial BOLA context is fail closed");
        assertions++;
        TestSupport.assertEquals(BolaAssessmentStatus.INCONCLUSIVE,
                evaluator.evaluate(withStatus(candidate, ContextStatus.CONFLICTING_EVIDENCE), "observation-1", "execution-1", "test-1").status(),
                "conflicting BOLA context is fail closed");
        assertions++;
        TestSupport.assertEquals(BolaAssessmentStatus.INCONCLUSIVE,
                evaluator.evaluate(withEvidence(candidate, List.of()), "observation-1", "execution-1", "test-1").status(),
                "BOLA candidate requires evidence");
        assertions++;
        assertions += assertBolaInconclusive(evaluator, candidate, "", "execution-1", "test-1", "BOLA requires observation provenance");
        assertions += assertBolaInconclusive(evaluator, candidate, "observation-1", "", "test-1", "BOLA requires execution provenance");
        assertions += assertBolaInconclusive(evaluator, candidate, "observation-1", "execution-1", "UNKNOWN", "BOLA requires test provenance");
        assertions += assertBolaInconclusive(evaluator, candidate, " unknown ", "execution-1", "test-1",
                "BOLA rejects whitespace-padded unknown provenance");
        assertions += assertBolaInconclusive(evaluator, null, "observation-1", "execution-1", "test-1",
                "BOLA rejects absent context");
        assertions += assertBolaInconclusive(evaluator, withStatus(candidate, ContextStatus.UNKNOWN),
                "observation-1", "execution-1", "test-1", "BOLA rejects unknown context state");
        for (List<String> evidence : List.of(List.of(""), List.of("  "), List.of("UNKNOWN"),
                List.of(" unknown "), List.of("evidence-1", "UNKNOWN"))) {
            assertions += assertBolaInconclusive(evaluator, withEvidence(candidate, evidence),
                    "observation-1", "execution-1", "test-1", "BOLA rejects incomplete evidence references");
        }

        AuthorizationContext unknownPrincipal = context(principal("UNKNOWN"), role("user"),
                candidate.resource(), "user-b", candidate.action(), AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW, List.of("evidence-1"), ContextStatus.RESOLVED);
        assertions += assertBolaInconclusive(evaluator, unknownPrincipal, "observation-1", "execution-1", "test-1",
                "BOLA rejects unknown principal");
        AuthorizationContext unknownAction = context(candidate.principal(), candidate.role(), candidate.resource(), "user-b",
                action(ActionType.UNKNOWN, ""), AuthorizationDecision.DENY, AuthorizationDecision.ALLOW,
                List.of("evidence-1"), ContextStatus.RESOLVED);
        assertions += assertBolaInconclusive(evaluator, unknownAction, "observation-1", "execution-1", "test-1",
                "BOLA rejects unknown action");
        AuthorizationContext missingOwner = context(candidate.principal(), candidate.role(),
                resource("document-2001", null), "", candidate.action(), AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW, List.of("evidence-1"), ContextStatus.RESOLVED);
        assertions += assertBolaInconclusive(evaluator, missingOwner, "observation-1", "execution-1", "test-1",
                "BOLA requires owner evidence");
        AuthorizationContext ownerConflict = context(candidate.principal(), candidate.role(), candidate.resource(), "user-c",
                candidate.action(), AuthorizationDecision.DENY, AuthorizationDecision.ALLOW,
                List.of("evidence-1"), ContextStatus.RESOLVED);
        assertions += assertBolaInconclusive(evaluator, ownerConflict, "observation-1", "execution-1", "test-1",
                "BOLA preserves owner conflict as inconclusive");

        for (AuthorizationDecision decision : List.of(AuthorizationDecision.CONDITIONAL,
                AuthorizationDecision.AMBIGUOUS, AuthorizationDecision.ERROR, AuthorizationDecision.UNKNOWN)) {
            AuthorizationContext nonBinary = context(candidate.principal(), candidate.role(), candidate.resource(), "user-b",
                    candidate.action(), decision, AuthorizationDecision.ALLOW, List.of("evidence-1"), ContextStatus.RESOLVED);
            assertions += assertBolaInconclusive(evaluator, nonBinary, "observation-1", "execution-1", "test-1",
                    "BOLA rejects nonbinary expected decision " + decision);
            AuthorizationContext nonBinaryObservedDecision = context(candidate.principal(), candidate.role(),
                    candidate.resource(), "user-b", candidate.action(), AuthorizationDecision.DENY, decision,
                    List.of("evidence-1"), ContextStatus.RESOLVED);
            assertions += assertBolaInconclusive(evaluator, nonBinaryObservedDecision,
                    "observation-1", "execution-1", "test-1", "BOLA rejects nonbinary observed decision " + decision);
        }
        AuthorizationContext nonBinaryObserved = context(candidate.principal(), candidate.role(), candidate.resource(), "user-b",
                candidate.action(), AuthorizationDecision.DENY, AuthorizationDecision.CONDITIONAL,
                List.of("evidence-1"), ContextStatus.RESOLVED);
        assertions += assertBolaInconclusive(evaluator, nonBinaryObserved, "observation-1", "execution-1", "test-1",
                "BOLA rejects nonbinary observed decision");

        TestSupport.assertEquals("user-b",
                evaluator.evaluate(candidate, "observation-1", "execution-1", "test-1").ownerPrincipalId(),
                "BOLA output retains verified context owner");
        assertions++;
        return assertions;
    }

    private static int bflaGuards() {
        int assertions = 0;
        BflaAssessmentEvaluator evaluator = new BflaAssessmentEvaluator();
        AuthorizationContext candidate = context(principal("user-a"), role("viewer"),
                resource("report-1", "user-a"), "user-a", action(ActionType.EXPORT, "ADMIN_EXPORT"),
                AuthorizationDecision.DENY, AuthorizationDecision.ALLOW, List.of("evidence-2"), ContextStatus.RESOLVED);

        TestSupport.assertEquals(BflaAssessmentStatus.BFLA_CANDIDATE,
                evaluator.evaluate(candidate, "observation-2", "execution-2", "test-2").status(),
                "complete BFLA candidate remains eligible");
        assertions++;
        TestSupport.assertEquals("", evaluator.evaluate(candidate, "observation-2", "execution-2", "test-2").endpoint(),
                "BFLA endpoint is not fabricated from the action");
        assertions++;
        assertions += assertBflaInconclusive(evaluator, withStatus(candidate, ContextStatus.PARTIAL),
                "observation-2", "execution-2", "test-2", "partial BFLA context is fail closed");
        assertions += assertBflaInconclusive(evaluator, withStatus(candidate, ContextStatus.CONFLICTING_EVIDENCE),
                "observation-2", "execution-2", "test-2", "conflicting BFLA context is fail closed");
        assertions += assertBflaInconclusive(evaluator, withEvidence(candidate, List.of()),
                "observation-2", "execution-2", "test-2", "BFLA candidate requires evidence");
        assertions += assertBflaInconclusive(evaluator, candidate, "", "execution-2", "test-2",
                "BFLA requires observation provenance");
        assertions += assertBflaInconclusive(evaluator, candidate, "observation-2", "", "test-2",
                "BFLA requires execution provenance");
        assertions += assertBflaInconclusive(evaluator, candidate, "observation-2", "execution-2", "UNKNOWN",
                "BFLA requires test provenance");
        assertions += assertBflaInconclusive(evaluator, candidate, "observation-2", " unknown ", "test-2",
                "BFLA rejects whitespace-padded unknown provenance");
        assertions += assertBflaInconclusive(evaluator, null, "observation-2", "execution-2", "test-2",
                "BFLA rejects absent context");
        assertions += assertBflaInconclusive(evaluator, withStatus(candidate, ContextStatus.UNKNOWN),
                "observation-2", "execution-2", "test-2", "BFLA rejects unknown context state");
        for (List<String> evidence : List.of(List.of(""), List.of("  "), List.of("UNKNOWN"),
                List.of(" unknown "), List.of("evidence-2", ""))) {
            assertions += assertBflaInconclusive(evaluator, withEvidence(candidate, evidence),
                    "observation-2", "execution-2", "test-2", "BFLA rejects incomplete evidence references");
        }

        AuthorizationContext unknownPrincipal = context(principal("UNKNOWN"), candidate.role(), candidate.resource(), "user-a",
                candidate.action(), AuthorizationDecision.DENY, AuthorizationDecision.ALLOW,
                List.of("evidence-2"), ContextStatus.RESOLVED);
        assertions += assertBflaInconclusive(evaluator, unknownPrincipal, "observation-2", "execution-2", "test-2",
                "BFLA rejects unknown principal");
        AuthorizationContext unknownRole = context(candidate.principal(), Role.unknown(), candidate.resource(), "user-a",
                candidate.action(), AuthorizationDecision.DENY, AuthorizationDecision.ALLOW,
                List.of("evidence-2"), ContextStatus.RESOLVED);
        assertions += assertBflaInconclusive(evaluator, unknownRole, "observation-2", "execution-2", "test-2",
                "BFLA rejects unknown role");
        AuthorizationContext unknownAction = context(candidate.principal(), candidate.role(), candidate.resource(), "user-a",
                action(ActionType.UNKNOWN, "ADMIN_EXPORT"), AuthorizationDecision.DENY, AuthorizationDecision.ALLOW,
                List.of("evidence-2"), ContextStatus.RESOLVED);
        assertions += assertBflaInconclusive(evaluator, unknownAction, "observation-2", "execution-2", "test-2",
                "BFLA rejects unknown action type");
        AuthorizationContext missingApplicationAction = context(candidate.principal(), candidate.role(), candidate.resource(), "user-a",
                action(ActionType.EXPORT, ""), AuthorizationDecision.DENY, AuthorizationDecision.ALLOW,
                List.of("evidence-2"), ContextStatus.RESOLVED);
        assertions += assertBflaInconclusive(evaluator, missingApplicationAction, "observation-2", "execution-2", "test-2",
                "BFLA requires application action");

        for (AuthorizationDecision decision : List.of(AuthorizationDecision.CONDITIONAL,
                AuthorizationDecision.AMBIGUOUS, AuthorizationDecision.ERROR, AuthorizationDecision.UNKNOWN)) {
            AuthorizationContext nonBinary = context(candidate.principal(), candidate.role(), candidate.resource(), "user-a",
                    candidate.action(), decision, AuthorizationDecision.ALLOW, List.of("evidence-2"), ContextStatus.RESOLVED);
            assertions += assertBflaInconclusive(evaluator, nonBinary, "observation-2", "execution-2", "test-2",
                    "BFLA rejects nonbinary expected decision " + decision);
            AuthorizationContext nonBinaryObservedDecision = context(candidate.principal(), candidate.role(),
                    candidate.resource(), "user-a", candidate.action(), AuthorizationDecision.DENY, decision,
                    List.of("evidence-2"), ContextStatus.RESOLVED);
            assertions += assertBflaInconclusive(evaluator, nonBinaryObservedDecision,
                    "observation-2", "execution-2", "test-2", "BFLA rejects nonbinary observed decision " + decision);
        }
        AuthorizationContext nonBinaryObserved = context(candidate.principal(), candidate.role(), candidate.resource(), "user-a",
                candidate.action(), AuthorizationDecision.DENY, AuthorizationDecision.ERROR,
                List.of("evidence-2"), ContextStatus.RESOLVED);
        assertions += assertBflaInconclusive(evaluator, nonBinaryObserved, "observation-2", "execution-2", "test-2",
                "BFLA rejects nonbinary observed decision");
        return assertions;
    }

    private static int assertBolaInconclusive(BolaAssessmentEvaluator evaluator, AuthorizationContext context,
                                               String observationId, String executionId, String testId, String message) {
        TestSupport.assertEquals(BolaAssessmentStatus.INCONCLUSIVE,
                evaluator.evaluate(context, observationId, executionId, testId).status(), message);
        return 1;
    }

    private static int assertBflaInconclusive(BflaAssessmentEvaluator evaluator, AuthorizationContext context,
                                               String observationId, String executionId, String testId, String message) {
        TestSupport.assertEquals(BflaAssessmentStatus.INCONCLUSIVE,
                evaluator.evaluate(context, observationId, executionId, testId).status(), message);
        return 1;
    }

    private static AuthorizationContext context(Principal principal, Role role, Resource resource,
                                                String owner, Action action, AuthorizationDecision expected,
                                                AuthorizationDecision observed, List<String> evidence,
                                                ContextStatus status) {
        return new AuthorizationContext(principal, role,
                new Tenant("tenant-a", "Tenant A", EvidenceSource.UNKNOWN, Confidence.unknown()),
                resource, owner, action, null, expected, observed, evidence, status);
    }

    private static AuthorizationContext withStatus(AuthorizationContext context, ContextStatus status) {
        return context(context.principal(), context.role(), context.resource(), context.ownerPrincipalId(),
                context.action(), context.expectedDecision(), context.observedDecision(), context.evidenceIds(), status);
    }

    private static AuthorizationContext withEvidence(AuthorizationContext context, List<String> evidence) {
        return context(context.principal(), context.role(), context.resource(), context.ownerPrincipalId(),
                context.action(), context.expectedDecision(), context.observedDecision(), evidence, context.status());
    }

    private static Principal principal(String id) {
        return new Principal(id, id, AuthenticationType.UNKNOWN, Confidence.unknown());
    }

    private static Role role(String id) {
        return new Role(id, id.toUpperCase(), EvidenceSource.UNKNOWN, Confidence.unknown());
    }

    private static Resource resource(String id, String owner) {
        return new Resource(id, "document", null, owner, "tenant-a", null, Confidence.unknown());
    }

    private static Action action(ActionType type, String applicationAction) {
        return new Action(type, EvidenceSource.UNKNOWN, Confidence.unknown(), applicationAction);
    }
}
