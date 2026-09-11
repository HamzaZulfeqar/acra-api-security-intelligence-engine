package io.acra.core.engine;

import io.acra.core.domain.authorization.AuthorizationAssessmentAggregate;
import io.acra.core.active.evidence.EvidenceReferenceValidator;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.BflaAssessment;
import io.acra.core.domain.authorization.BolaAssessment;
import io.acra.core.domain.authorization.CorrelationState;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Deterministic, conservative correlation of existing assessment references.
 * This API has no authenticated independence or policy-version evidence, so it
 * cannot establish corroboration or raise confidence above MEDIUM.
 */
public final class AuthorizationAssessmentCorrelator {
    private static final UniversalRedactor REDACTOR = new UniversalRedactor();
    private AuthorizationAssessmentCorrelator() {}

    public static AuthorizationAssessmentAggregate correlate(List<BolaAssessment> bolas, List<BflaAssessment> bflas) {
        return correlate(bolas, bflas, null, null);
    }

    public static AuthorizationAssessmentAggregate correlate(List<BolaAssessment> bolas, List<BflaAssessment> bflas,
                                                             EvidenceReferenceValidator validator, String projectId) {
        List<Assessment> supplied = new ArrayList<>();
        boolean absent = false;
        if (bolas != null) for (BolaAssessment assessment : bolas) {
            if (assessment == null) absent = true;
            else supplied.add(from(assessment));
        }
        if (bflas != null) for (BflaAssessment assessment : bflas) {
            if (assessment == null) absent = true;
            else supplied.add(from(assessment));
        }

        Set<String> ids = new TreeSet<>();
        Set<String> observations = new TreeSet<>();
        Set<String> executions = new TreeSet<>();
        Set<String> evidence = new TreeSet<>();
        Set<String> conflicts = new TreeSet<>();
        Map<String, Set<String>> payloadsById = new TreeMap<>();
        Map<String, Set<String>> comparableFacts = new TreeMap<>();
        Set<String> payloads = new TreeSet<>();
        Set<String> executionFacts = new TreeSet<>();
        boolean incomplete = absent || supplied.isEmpty();
        boolean inconclusive = false;
        boolean lowConfidence = false;

        for (Assessment assessment : supplied) {
            addReference(ids, assessment.id());
            addReference(observations, assessment.observation());
            addReference(executions, assessment.execution());
            assessment.evidence().forEach(value -> addReference(evidence, value));
            String payload = assessment.payload();
            payloads.add(payload);
            executionFacts.add(assessment.executionFact());
            if (!missing(assessment.id())) {
                payloadsById.computeIfAbsent(assessment.id(), ignored -> new TreeSet<>()).add(payload);
            }
            boolean complete = assessment.complete();
            if (validator != null && !validator.validate(assessment.evidence(), assessment.observation(),
                    assessment.execution(), assessment.test(), projectId).valid()) complete = false;
            incomplete |= !complete || assessment.confidence().equals("INSUFFICIENT");
            lowConfidence |= assessment.confidence().equals("LOW");
            boolean definite = definitive(assessment.expected()) && definitive(assessment.observed());
            inconclusive |= !definite || assessment.status().equals("INCONCLUSIVE")
                    || assessment.status().equals("BLOCKED");

            if (complete && definite && !assessment.status().equals("INCONCLUSIVE")
                    && !assessment.status().equals("BLOCKED")) {
                boolean violation = assessment.expected() == AuthorizationDecision.DENY
                        && assessment.observed() == AuthorizationDecision.ALLOW;
                boolean claimsCandidate = assessment.status().endsWith("_CANDIDATE");
                if (claimsCandidate != violation) {
                    conflicts.add("Assessment status contradicts decisions: " + assessment.id());
                }
                comparableFacts.computeIfAbsent(assessment.comparisonKey(), ignored -> new TreeSet<>())
                        .add(canonical(assessment.ownerOrRole(), assessment.expected().name(),
                                assessment.observed().name(), assessment.status()));
            }
        }
        payloadsById.forEach((id, variants) -> {
            if (variants.size() > 1) conflicts.add("Assessment ID has conflicting payloads: " + id);
        });
        comparableFacts.forEach((key, variants) -> {
            if (variants.size() > 1) conflicts.add("Comparable authorization facts conflict: "
                    + TokenFingerprint.sha256(key));
        });

        CorrelationState state;
        String confidence;
        String rationale;
        if (!conflicts.isEmpty()) {
            state = CorrelationState.CONFLICTING;
            confidence = "INSUFFICIENT";
            rationale = "Conflicting evidence retained; no record overrides another";
        } else if (incomplete) {
            state = CorrelationState.INSUFFICIENT;
            confidence = "INSUFFICIENT";
            rationale = "Assessment context, evidence or provenance is incomplete";
        } else if (inconclusive) {
            state = CorrelationState.INCONCLUSIVE;
            confidence = "INSUFFICIENT";
            rationale = "An assessment is blocked, inconclusive or lacks definite decisions";
        } else if (supplied.size() > 1 && executionFacts.size() == 1) {
            state = CorrelationState.DUPLICATE;
            confidence = "LOW";
            rationale = "Repeated same-execution assessment; no independent support established";
        } else {
            state = CorrelationState.CONSISTENT;
            confidence = lowConfidence ? "LOW" : "MEDIUM";
            rationale = "No comparable conflict found; independent corroboration is unverified";
        }
        String material = canonical(state.name(), confidence, canonical(payloads.toArray(String[]::new)),
                Boolean.toString(absent));
        return new AuthorizationAssessmentAggregate("corr-" + TokenFingerprint.sha256(material),
                List.copyOf(ids), List.copyOf(observations), List.copyOf(executions), List.copyOf(evidence),
                state, confidence, rationale, List.copyOf(conflicts));
    }

    private static Assessment from(BolaAssessment assessment) {
        return new Assessment("BOLA", assessment.assessmentId(), assessment.observationId(),
                assessment.executionId(), assessment.testId(), assessment.principalId(), assessment.resourceId(),
                assessment.ownerPrincipalId(), assessment.action(), assessment.expectedDecision(),
                assessment.observedDecision(), assessment.status().name(), assessment.confidence().name(),
                assessment.evidenceIds(), assessment.rationale());
    }

    private static Assessment from(BflaAssessment assessment) {
        return new Assessment("BFLA", assessment.assessmentId(), assessment.observationId(),
                assessment.executionId(), assessment.testId(), assessment.principalId(), assessment.endpoint(),
                assessment.roleId(), assessment.action(), assessment.expectedDecision(),
                assessment.observedDecision(), assessment.status().name(), assessment.confidence().name(),
                assessment.evidenceIds(), assessment.rationale());
    }

    private record Assessment(String property, String id, String observation, String execution, String test,
            String principal, String target, String ownerOrRole, String action, AuthorizationDecision expected,
            AuthorizationDecision observed, String status, String confidence, List<String> evidence, String rationale) {
        private String evidenceKey() {
            return canonical(new TreeSet<>(evidence).toArray(String[]::new));
        }

        private boolean complete() {
            return !missing(id) && !missing(observation) && !missing(execution) && !missing(test)
                    && !missing(principal) && !missing(target) && !missing(ownerOrRole) && !missing(action)
                    && !evidence.isEmpty() && evidence.stream().noneMatch(AuthorizationAssessmentCorrelator::missing);
        }

        private String comparisonKey() {
            // testId is the only available experiment/policy boundary in the existing records.
            return canonical(property, test, principal, target, action);
        }

        private String executionFact() {
            return canonical(comparisonKey(), observation, execution, ownerOrRole, decision(expected),
                    decision(observed), status, evidenceKey());
        }

        private String payload() {
            return canonical(id, executionFact(), confidence, rationale);
        }
    }

    private static String decision(AuthorizationDecision value) { return value == null ? null : value.name(); }

    private static boolean definitive(AuthorizationDecision decision) {
        return decision == AuthorizationDecision.ALLOW || decision == AuthorizationDecision.DENY;
    }

    private static boolean missing(String value) {
        return value == null || value.isBlank() || value.strip().equalsIgnoreCase("UNKNOWN")
                || value.toLowerCase(Locale.ROOT).contains(UniversalRedactor.REDACTED)
                || !value.equals(REDACTOR.redactText(value));
    }

    private static void addReference(Set<String> values, String value) {
        if (!missing(value)) values.add(value);
    }

    private static String canonical(String... values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (value == null) result.append("-1:");
            else result.append(value.length()).append(':').append(value);
        }
        return result.toString();
    }
}
