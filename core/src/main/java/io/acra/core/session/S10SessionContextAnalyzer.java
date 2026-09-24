package io.acra.core.session;

import io.acra.core.security.TokenFingerprint;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class S10SessionContextAnalyzer {

    public SessionCorrelationResult analyze(
            AuthenticationSessionObservation previous,
            AuthenticationSessionObservation current) {

        if (current == null) throw new IllegalArgumentException("current observation required");

        if (previous == null) {
            return result(
                    current,
                    null,
                    SessionCorrelationState.BASELINE,
                    false,
                    current.identityVerified(),
                    false,
                    List.of(),
                    current.evidenceIds(),
                    current.identityVerified()
                            ? List.of("FIRST_VERIFIED_SESSION_OBSERVATION")
                            : List.of("FIRST_SESSION_OBSERVATION_IDENTITY_UNVERIFIED"));
        }

        List<String> evidence = union(previous.evidenceIds(), current.evidenceIds());
        if (!previous.sessionId().equals(current.sessionId())) {
            return result(
                    current,
                    previous,
                    SessionCorrelationState.INCONCLUSIVE,
                    previous.identityVerified(),
                    current.identityVerified(),
                    !previous.tokenFingerprint().equals(current.tokenFingerprint()),
                    List.of(),
                    evidence,
                    List.of("SESSION_ID_CHANGED"));
        }

        boolean rotated = !previous.tokenFingerprint().equals(current.tokenFingerprint());
        List<SessionContextDimension> drift = drift(previous, current);

        if (!previous.identityVerified() || !current.identityVerified()) {
            List<String> reasons = new ArrayList<>();
            reasons.add("TOKEN_FINGERPRINT_DOES_NOT_VERIFY_PRINCIPAL");
            if (!previous.identityVerified()) reasons.add("PREVIOUS_IDENTITY_UNVERIFIED");
            if (!current.identityVerified()) reasons.add("CURRENT_IDENTITY_UNVERIFIED");
            if (!drift.isEmpty()) reasons.add("UNVERIFIED_CONTEXT_DIFFERENCE");
            return result(
                    current,
                    previous,
                    SessionCorrelationState.UNVERIFIED_IDENTITY,
                    previous.identityVerified(),
                    current.identityVerified(),
                    rotated,
                    drift,
                    evidence,
                    reasons);
        }

        if (!drift.isEmpty()) {
            List<String> reasons = new ArrayList<>();
            reasons.add(rotated ? "TOKEN_ROTATION_CONTEXT_DRIFT" : "SESSION_CONTEXT_DRIFT");
            for (SessionContextDimension dimension : drift) {
                reasons.add(dimension.name() + "_CHANGED");
            }
            return result(
                    current,
                    previous,
                    SessionCorrelationState.CONTEXT_DRIFT,
                    true,
                    true,
                    rotated,
                    drift,
                    evidence,
                    reasons);
        }

        if (rotated) {
            return result(
                    current,
                    previous,
                    SessionCorrelationState.TOKEN_ROTATED,
                    true,
                    true,
                    true,
                    List.of(),
                    evidence,
                    List.of("TOKEN_ROTATED_CONTEXT_STABLE"));
        }

        return result(
                current,
                previous,
                SessionCorrelationState.STABLE,
                true,
                true,
                false,
                List.of(),
                evidence,
                List.of("SESSION_CONTEXT_STABLE"));
    }

    private static List<SessionContextDimension> drift(
            AuthenticationSessionObservation previous,
            AuthenticationSessionObservation current) {

        List<SessionContextDimension> dimensions = new ArrayList<>();
        if (!previous.principalId().equals(current.principalId())) {
            dimensions.add(SessionContextDimension.PRINCIPAL);
        }
        if (!previous.roleId().equals(current.roleId())) {
            dimensions.add(SessionContextDimension.ROLE);
        }
        if (!previous.tenantId().equals(current.tenantId())) {
            dimensions.add(SessionContextDimension.TENANT);
        }
        if (!previous.scopes().equals(current.scopes())) {
            dimensions.add(SessionContextDimension.SCOPE);
        }
        return List.copyOf(dimensions);
    }

    private static SessionCorrelationResult result(
            AuthenticationSessionObservation current,
            AuthenticationSessionObservation previous,
            SessionCorrelationState state,
            boolean previousVerified,
            boolean currentVerified,
            boolean rotated,
            List<SessionContextDimension> drift,
            List<String> evidence,
            List<String> reasons) {

        String previousId = previous == null ? "" : previous.observationId();
        String material = current.sessionId() + "|" + previousId + "|" + current.observationId()
                + "|" + state.name() + "|" + rotated + "|" + drift;
        String id = "s10-session-" + TokenFingerprint.sha256(material).substring(0, 24);

        return new SessionCorrelationResult(
                id,
                current.sessionId(),
                previousId,
                current.observationId(),
                state,
                previousVerified,
                currentVerified,
                rotated,
                drift,
                evidence,
                List.copyOf(new LinkedHashSet<>(reasons)));
    }

    private static List<String> union(List<String> left, List<String> right) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.addAll(left == null ? List.of() : left);
        values.addAll(right == null ? List.of() : right);
        return List.copyOf(values);
    }
}
