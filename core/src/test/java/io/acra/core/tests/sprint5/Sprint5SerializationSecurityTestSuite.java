package io.acra.core.tests.sprint5;

import io.acra.core.domain.authorization.Action;
import io.acra.core.domain.authorization.ActionType;
import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.BflaAssessment;
import io.acra.core.domain.authorization.BflaAssessmentStatus;
import io.acra.core.domain.authorization.BflaConfidence;
import io.acra.core.domain.authorization.BolaAssessment;
import io.acra.core.domain.authorization.BolaAssessmentStatus;
import io.acra.core.domain.authorization.BolaConfidence;
import io.acra.core.domain.authorization.ContextStatus;
import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.evidence.EvidenceSource;
import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.domain.identity.Principal;
import io.acra.core.domain.identity.Role;
import io.acra.core.domain.resource.Resource;
import io.acra.core.domain.tenant.Tenant;
import io.acra.core.domain.workflow.WorkflowState;
import io.acra.core.engine.BolaAssessmentEvaluator;
import io.acra.core.engine.BflaAssessmentEvaluator;
import io.acra.core.serialization.DomainSerializer;
import io.acra.core.tests.TestSupport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Regression checks for the existing S5 records and their shared export boundary. */
public final class Sprint5SerializationSecurityTestSuite {
    private static final DomainSerializer SERIALIZER = new DomainSerializer();
    private static int assertions;

    private Sprint5SerializationSecurityTestSuite() { }

    public static void main(String[] args) {
        assertions = 0;
        immutableInputsAndProvenance();
        secretPatterns();
        contextualSecretFields();
        redactedReferencesAreIncomplete();
        deterministicSerialization();
        System.out.println("SPRINT5_SERIALIZATION_SECURITY PASS assertions=" + assertions);
    }

    private static void immutableInputsAndProvenance() {
        ArrayList<String> evidence = new ArrayList<>(List.of("evidence-1", "evidence-2"));
        AuthorizationContext context = context("safe-reference", evidence);
        BolaAssessment bola = bola("safe-reference", evidence);
        BflaAssessment bfla = bfla("safe-reference", evidence);
        evidence.clear();
        equal(List.of("evidence-1", "evidence-2"), context.evidenceIds(), "context copies caller evidence");
        equal(List.of("evidence-1", "evidence-2"), bola.evidenceIds(), "BOLA copies caller evidence");
        equal(List.of("evidence-1", "evidence-2"), bfla.evidenceIds(), "BFLA copies caller evidence");
        unmodifiable(context.evidenceIds(), "context evidence is immutable");
        unmodifiable(bola.evidenceIds(), "BOLA evidence is immutable");
        unmodifiable(bfla.evidenceIds(), "BFLA evidence is immutable");
        for (Object value : List.of(bola, bfla)) {
            String json = SERIALIZER.serialize(value);
            for (String reference : List.of("assessment-safe-reference", "observation-1", "execution-1", "test-1",
                    "evidence-1", "evidence-2")) {
                check(json.contains(reference), "export retains " + reference);
            }
        }
        equal("safe-reference", context.principal().principalId(), "safe principal reference survives");
        equal("safe-reference", context.resource().resourceId(), "safe resource reference survives");
        equal("safe-reference", context.role().roleId(), "safe role reference survives");
        equal("safe-reference", context.tenant().tenantId(), "safe tenant reference survives");
        equal(AuthorizationDecision.DENY, context.expectedDecision(), "expected decision survives sanitization");
        equal(AuthorizationDecision.ALLOW, context.observedDecision(), "observed decision survives sanitization");
        equal(ContextStatus.RESOLVED, context.status(), "context status survives sanitization");
    }

    private static void secretPatterns() {
        // These are visibly synthetic credentials, never credentials from a target or user.
        List<SecretPattern> patterns = List.of(
                new SecretPattern("Bearer DummyBearerSecret", "DummyBearerSecret"),
                new SecretPattern("password=DummyPasswordSecret", "DummyPasswordSecret"),
                new SecretPattern("api_key=DummyApiKeySecret", "DummyApiKeySecret"),
                new SecretPattern("session_secret=DummySessionSecret", "DummySessionSecret"),
                new SecretPattern("sessionId=DummySessionIdSecret", "DummySessionIdSecret"),
                new SecretPattern("refresh_token=DummyRefreshSecret", "DummyRefreshSecret"),
                new SecretPattern("Authorization: Basic ZHVtbXk6bm90LWEtdG9rZW4=", "ZHVtbXk6bm90LWEtdG9rZW4="),
                new SecretPattern("Basic ZHVtbXk6bm90LWEtdG9rZW4=", "ZHVtbXk6bm90LWEtdG9rZW4="),
                new SecretPattern("Cookie: sid=DummyCookieSecret", "DummyCookieSecret"),
                new SecretPattern("Set-Cookie: sid=DummySetCookieSecret; Secure", "DummySetCookieSecret"),
                new SecretPattern("X-Api-Key: DummyHeaderApiSecret", "DummyHeaderApiSecret"));
        for (SecretPattern pattern : patterns) {
            for (Object model : List.of(context(pattern.text(), List.of(pattern.text())),
                    bola(pattern.text(), List.of(pattern.text())), bfla(pattern.text(), List.of(pattern.text())))) {
                check(!model.toString().contains(pattern.secret()), "record excludes recognized secret " + pattern.secret());
                check(!SERIALIZER.serialize(model).contains(pattern.secret()), "export excludes recognized secret " + pattern.secret());
            }
        }
        for (String header : List.of("Authorization", "Proxy-Authorization", "Cookie", "Set-Cookie", "X-Api-Key")) {
            check(!SERIALIZER.serialize(new HttpHeader(header, "DummyHeaderSecret")).contains("DummyHeaderSecret"),
                    "typed header value excluded: " + header);
        }
        Principal original = new Principal("user-a", "password=DummyOriginalSecret", AuthenticationType.UNKNOWN, Confidence.unknown());
        AuthorizationContext context = new AuthorizationContext(original, null, null, null, "", null, null,
                null, null, List.of(), null);
        check(original.displayName().contains("DummyOriginalSecret"), "sanitization does not mutate source object");
        check(!context.principal().displayName().contains("DummyOriginalSecret"), "context snapshot stores sanitized principal");
    }

    private static void contextualSecretFields() {
        for (String key : List.of("password", "apiKey", "sessionSecret", "sessionId", "refreshToken",
                "Authorization", "Cookie", "Set-Cookie")) {
            check(!SERIALIZER.serialize(Map.of(key, "DummyOpaqueCredential")).contains("DummyOpaqueCredential"),
                    "sensitive map field excludes opaque value: " + key);
        }
        check(!SERIALIZER.serialize(new CredentialFields("DummyPasswordValue", "DummyApiKeyValue", "DummySessionValue"))
                .contains("Dummy"), "record field names apply contextual redaction");
        check(!SERIALIZER.serialize(Map.of("password=DummyKeySecret", "safe-value")).contains("DummyKeySecret"),
                "recognized secret in a map key is excluded");
        check(SERIALIZER.serialize(Map.of("principalId", "user-a", "evidenceId", "evidence-1")).contains("user-a"),
                "ordinary reference fields are not removed");
    }

    private static void deterministicSerialization() {
        for (Object value : List.of(context("reference", List.of("e1", "e2")), bola("reference", List.of("e1", "e2")),
                bfla("reference", List.of("e1", "e2")))) {
            equal(SERIALIZER.serialize(value), SERIALIZER.serialize(value), "repeated serialization is stable");
        }
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("z", "last"); first.put("a", "first");
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("a", "first"); second.put("z", "last");
        equal(SERIALIZER.serialize(first), SERIALIZER.serialize(second), "map insertion order does not alter export");
        equal(SERIALIZER.serialize(context("reference", List.of("e1"))),
                SERIALIZER.serialize(context("reference", List.of("e1"))), "equivalent context snapshots serialize identically");
    }

    private static void redactedReferencesAreIncomplete() {
        BolaAssessmentEvaluator bolaEvaluator = new BolaAssessmentEvaluator();
        BflaAssessmentEvaluator bflaEvaluator = new BflaAssessmentEvaluator();
        AuthorizationContext valid = context("safe-reference", List.of("evidence-1"));
        for (String reference : List.of("<redacted>", "prefix-<REDACTED>", "Bearer DummyGuardSecret",
                "Authorization: Basic ZHVtbXk6bm90LWEtdG9rZW4=", "Cookie: sid=DummyGuardSecret",
                "Basic ZHVtbXk6bm90LWEtdG9rZW4=",
                "sessionId=DummyGuardSecret")) {
            AuthorizationContext evidence = context("safe-reference", List.of(reference));
            equal(BolaAssessmentStatus.INCONCLUSIVE,
                    bolaEvaluator.evaluate(evidence, "observation-1", "execution-1", "test-1").status(),
                    "redacted evidence cannot support BOLA");
            equal(BflaAssessmentStatus.INCONCLUSIVE,
                    bflaEvaluator.evaluate(evidence, "observation-1", "execution-1", "test-1").status(),
                    "redacted evidence cannot support BFLA");
            equal(BolaAssessmentStatus.INCONCLUSIVE,
                    bolaEvaluator.evaluate(valid, reference, "execution-1", "test-1").status(),
                    "redacted or secret-bearing provenance cannot support BOLA");
            equal(BflaAssessmentStatus.INCONCLUSIVE,
                    bflaEvaluator.evaluate(valid, "observation-1", reference, "test-1").status(),
                    "redacted or secret-bearing provenance cannot support BFLA");
            AuthorizationContext fields = context(reference, List.of("evidence-1"));
            equal(BolaAssessmentStatus.INCONCLUSIVE,
                    bolaEvaluator.evaluate(fields, "observation-1", "execution-1", "test-1").status(),
                    "redacted required object context cannot support BOLA");
            equal(BflaAssessmentStatus.INCONCLUSIVE,
                    bflaEvaluator.evaluate(fields, "observation-1", "execution-1", "test-1").status(),
                    "redacted required function context cannot support BFLA");
        }
    }

    private static AuthorizationContext context(String text, List<String> evidence) {
        Confidence confidence = Confidence.unknown();
        return new AuthorizationContext(new Principal(text, text, AuthenticationType.UNKNOWN, confidence),
                new Role(text, text, EvidenceSource.UNKNOWN, confidence), new Tenant(text, text, EvidenceSource.UNKNOWN, confidence),
                new Resource(text, text, text, text, text, text, confidence), text,
                new Action(ActionType.READ, EvidenceSource.UNKNOWN, confidence, text), new WorkflowState(text, confidence),
                AuthorizationDecision.DENY, AuthorizationDecision.ALLOW, evidence, ContextStatus.RESOLVED);
    }

    private static BolaAssessment bola(String text, List<String> evidence) {
        return new BolaAssessment("assessment-" + text, "observation-1", "execution-1", "test-1", text, text, text, text,
                AuthorizationDecision.DENY, AuthorizationDecision.ALLOW, BolaAssessmentStatus.BOLA_CANDIDATE,
                BolaConfidence.HIGH, evidence, text);
    }

    private static BflaAssessment bfla(String text, List<String> evidence) {
        return new BflaAssessment("assessment-" + text, "observation-1", "execution-1", "test-1", text, text, text, text,
                AuthorizationDecision.DENY, AuthorizationDecision.ALLOW, BflaAssessmentStatus.BFLA_CANDIDATE,
                BflaConfidence.HIGH, evidence, text);
    }

    private static void unmodifiable(List<String> list, String message) {
        boolean rejected = false;
        try { list.add("must-not-be-added"); } catch (UnsupportedOperationException expected) { rejected = true; }
        check(rejected, message);
    }

    private static void check(boolean condition, String message) {
        TestSupport.assertTrue(condition, message);
        assertions++;
    }

    private static void equal(Object expected, Object actual, String message) {
        TestSupport.assertEquals(expected, actual, message);
        assertions++;
    }

    private record SecretPattern(String text, String secret) { }
    public record CredentialFields(String password, String apiKey, String sessionSecret) { }
}
