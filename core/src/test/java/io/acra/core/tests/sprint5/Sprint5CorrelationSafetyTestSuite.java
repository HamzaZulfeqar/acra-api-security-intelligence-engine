package io.acra.core.tests.sprint5;

import io.acra.core.domain.authorization.*;
import io.acra.core.engine.AuthorizationAssessmentCorrelator;
import io.acra.core.serialization.DomainSerializer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Offline regression of conservative handling of supplied records; no transport. */
public final class Sprint5CorrelationSafetyTestSuite {
    private static int assertions;
    private Sprint5CorrelationSafetyTestSuite() { }

    public static void main(String[] args) {
        assertions = 0;
        emptyAndIncomplete();
        distinctPropertiesAndDuplicates();
        conflicts();
        determinismAndReplay();
        immutableSanitizedAggregate();
        System.out.println("SPRINT5_CORRELATION_SAFETY PASS assertions=" + assertions);
    }

    private static void emptyAndIncomplete() {
        equal(CorrelationState.INSUFFICIENT, correlate(List.of()).state(), "empty input");
        equal(CorrelationState.INSUFFICIENT, AuthorizationAssessmentCorrelator.correlate(null, null).state(), "null lists");
        equal(CorrelationState.INSUFFICIENT, correlate(Arrays.asList((BolaAssessment)null)).state(), "null record");
        for (String reference : List.of("", " ", "UNKNOWN", " unknown ", "<redacted>", "prefix-<REDACTED>",
                "Bearer SyntheticCredential", "password=SyntheticCredential", "Cookie: sid=SyntheticCredential",
                "Basic ZHVtbXk6ZHVtbXk=")) {
            equal(CorrelationState.INSUFFICIENT, correlate(List.of(bola(reference, "o", "x", "t", "r",
                    AuthorizationDecision.DENY, AuthorizationDecision.DENY, BolaAssessmentStatus.NO_VIOLATION, List.of("e")))).state(), "invalid assessment reference");
            equal(CorrelationState.INSUFFICIENT, correlate(List.of(bola("a", "o", "x", "t", "r",
                    AuthorizationDecision.DENY, AuthorizationDecision.DENY, BolaAssessmentStatus.NO_VIOLATION, List.of(reference)))).state(), "invalid evidence reference");
        }
        equal(CorrelationState.INSUFFICIENT, correlate(List.of(bola("a", "o", "x", "t", "r",
                AuthorizationDecision.DENY, AuthorizationDecision.DENY, BolaAssessmentStatus.NO_VIOLATION, List.of()))).state(), "empty evidence");
        for (AuthorizationDecision decision : List.of(AuthorizationDecision.UNKNOWN, AuthorizationDecision.ERROR,
                AuthorizationDecision.AMBIGUOUS, AuthorizationDecision.CONDITIONAL)) {
            equal(CorrelationState.INCONCLUSIVE, correlate(List.of(bola("a", "o", "x", "t", "r", decision,
                    AuthorizationDecision.DENY, BolaAssessmentStatus.NO_VIOLATION, List.of("e")))).state(), "nonbinary decision");
        }
        var endpointUnknown = new BflaAssessment("f", "o", "x", "t", "p", "role", "READ", "",
                AuthorizationDecision.DENY, AuthorizationDecision.DENY, BflaAssessmentStatus.NO_VIOLATION,
                BflaConfidence.MEDIUM, List.of("e"), "fixture");
        equal(CorrelationState.INSUFFICIENT, AuthorizationAssessmentCorrelator.correlate(List.of(), List.of(endpointUnknown)).state(), "missing endpoint cannot support aggregate");
    }

    private static void distinctPropertiesAndDuplicates() {
        var object = ordinary("a", "o", "x", "r");
        var function = new BflaAssessment("f", "o", "x", "t", "p", "role", "READ", "endpoint-ref",
                AuthorizationDecision.DENY, AuthorizationDecision.DENY, BflaAssessmentStatus.NO_VIOLATION,
                BflaConfidence.MEDIUM, List.of("e"), "fixture");
        var both = AuthorizationAssessmentCorrelator.correlate(List.of(object), List.of(function));
        equal(CorrelationState.CONSISTENT, both.state(), "different properties in same execution are not duplicates");
        equal(List.of("a", "f"), both.assessmentIds(), "both property references retained");
        var duplicate = correlate(List.of(object, object));
        equal(CorrelationState.DUPLICATE, duplicate.state(), "exact repeat");
        equal(List.of("a"), duplicate.assessmentIds(), "duplicate references deduplicated");
        equal("LOW", duplicate.confidence(), "duplicates do not gain confidence");
        var differentResource = correlate(List.of(object, ordinary("b", "o", "x", "r2")));
        equal(CorrelationState.CONSISTENT, differentResource.state(), "different subjects not silently deduplicated");
    }

    private static void conflicts() {
        var denies = ordinary("a", "o1", "x1", "r");
        // Already-supplied fixture record; no request or scenario execution is performed.
        var mismatch = bola("b", "o2", "x2", "t", "r", AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW, BolaAssessmentStatus.BOLA_CANDIDATE, List.of("e2"));
        var conflict = correlate(List.of(denies, mismatch));
        equal(CorrelationState.CONFLICTING, conflict.state(), "contradictory comparable records");
        check(!conflict.conflicts().isEmpty(), "conflict details retained");
        equal("INSUFFICIENT", conflict.confidence(), "conflict disables support");
        equal(List.of("x1", "x2"), conflict.executionIds(), "both executions retained");
        equal(List.of("o1", "o2"), conflict.observationIds(), "both observations retained");
        equal(CorrelationState.CONFLICTING, correlate(List.of(denies, denies, mismatch)).state(), "majority repeat cannot override conflict");
        var sameIdOtherTarget = ordinary("a", "o1", "x1", "different-r");
        equal(CorrelationState.CONFLICTING, correlate(List.of(denies, sameIdOtherTarget)).state(), "same ID different payload fails closed");
        var invalidClaim = bola("bad", "o", "x", "t", "r", AuthorizationDecision.DENY,
                AuthorizationDecision.DENY, BolaAssessmentStatus.BOLA_CANDIDATE, List.of("e"));
        equal(CorrelationState.CONFLICTING, correlate(List.of(invalidClaim)).state(), "claim contradicting decisions");
        var missingClaim = bola("bad", "o", "x", "t", "r", AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW, BolaAssessmentStatus.NO_VIOLATION, List.of("e"));
        equal(CorrelationState.CONFLICTING, correlate(List.of(missingClaim)).state(), "opposite claim contradiction visible");
    }

    private static void determinismAndReplay() {
        var first = ordinary("a", "o1", "x1", "r");
        var second = ordinary("b", "o2", "x2", "r");
        var forward = correlate(List.of(first, second));
        var reversed = correlate(List.of(second, first));
        equal(forward, reversed, "set ordering does not affect aggregate");
        equal(new DomainSerializer().serialize(forward), new DomainSerializer().serialize(reversed), "serialized ordering stable");
        check(forward.correlationId().matches("corr-[a-f0-9]{64}"), "full deterministic SHA-256 aggregate ID");
        equal(CorrelationState.CONSISTENT, forward.state(), "replay does not establish corroboration");
        equal(correlate(List.of(first)).confidence(), forward.confidence(), "replay does not increase confidence");
        equal(List.of("x1", "x2"), forward.executionIds(), "replay keeps both execution references");
        equal("x1", first.executionId(), "historical execution unchanged");
        var repeated = correlate(List.of(first, second, first));
        equal(forward, repeated, "duplicate in multiple observations adds no strength");
        var originalEvidence = bola("a", "o", "x", "t", "r", AuthorizationDecision.DENY,
                AuthorizationDecision.DENY, BolaAssessmentStatus.NO_VIOLATION, List.of("e2", "e1"));
        var reorderedEvidence = bola("a", "o", "x", "t", "r", AuthorizationDecision.DENY,
                AuthorizationDecision.DENY, BolaAssessmentStatus.NO_VIOLATION, List.of("e1", "e2", "e1"));
        equal(correlate(List.of(originalEvidence)), correlate(List.of(reorderedEvidence)), "evidence reference sets canonicalized");
        check(!forward.correlationId().equals(correlate(List.of(first)).correlationId()), "distinct evidence set has distinct ID");
    }

    private static void immutableSanitizedAggregate() {
        var source = new ArrayList<>(List.of(ordinary("a", "o", "x", "r")));
        var aggregate = correlate(source);
        source.clear();
        equal(List.of("a"), aggregate.assessmentIds(), "aggregate snapshot independent of caller list");
        boolean rejected = false;
        try { aggregate.evidenceIds().add("new"); } catch (UnsupportedOperationException expected) { rejected = true; }
        check(rejected, "aggregate evidence immutable");
        var secret = new AuthorizationAssessmentAggregate("password=DummyAggregateSecret", List.of("Bearer DummyAggregateSecret"),
                List.of("Cookie: sid=DummyAggregateSecret"), List.of("sessionSecret=DummyAggregateSecret"),
                List.of("apiKey=DummyAggregateSecret"), null, "password=DummyAggregateSecret",
                "Authorization: Basic DummyAggregateSecret", List.of("refreshToken=DummyAggregateSecret"));
        check(!secret.toString().contains("DummyAggregateSecret"), "aggregate excludes recognized secrets in memory");
        check(!new DomainSerializer().serialize(secret).contains("DummyAggregateSecret"), "aggregate excludes secrets from export");
    }

    private static BolaAssessment ordinary(String id, String observation, String execution, String resource) {
        return bola(id, observation, execution, "t", resource, AuthorizationDecision.DENY, AuthorizationDecision.DENY,
                BolaAssessmentStatus.NO_VIOLATION, List.of("e"));
    }
    private static BolaAssessment bola(String id, String observation, String execution, String test, String resource,
            AuthorizationDecision expected, AuthorizationDecision observed, BolaAssessmentStatus status, List<String> evidence) {
        return new BolaAssessment(id, observation, execution, test, "p", resource, "owner", "READ", expected, observed,
                status, BolaConfidence.MEDIUM, evidence, "fixture");
    }
    private static AuthorizationAssessmentAggregate correlate(List<BolaAssessment> records) {
        return AuthorizationAssessmentCorrelator.correlate(records, List.of());
    }
    private static void equal(Object expected, Object actual, String message) {
        check(java.util.Objects.equals(expected, actual), message + ": expected=" + expected + ", actual=" + actual);
    }
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
