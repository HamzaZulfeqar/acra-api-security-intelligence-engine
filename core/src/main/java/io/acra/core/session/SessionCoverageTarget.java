package io.acra.core.session;

import io.acra.core.security.TokenFingerprint;

public record SessionCoverageTarget(
        String coverageId,
        String targetId,
        String sessionId,
        SessionCoverageObjective objective,
        String ruleReference) {

    public SessionCoverageTarget {
        targetId = required(targetId, "targetId");
        sessionId = required(sessionId, "sessionId");
        if (objective == null) throw new IllegalArgumentException("objective required");
        ruleReference = required(ruleReference, "ruleReference");
        String expected = "s10-session-coverage-" + TokenFingerprint.sha256(
                targetId + "|" + sessionId + "|" + objective.name() + "|" + ruleReference).substring(0, 24);
        if (coverageId == null || coverageId.isBlank()) coverageId = expected;
        else if (!coverageId.equals(expected)) throw new IllegalArgumentException("coverageId does not match target identity");
    }

    public static SessionCoverageTarget of(
            String targetId,
            String sessionId,
            SessionCoverageObjective objective,
            String ruleReference) {
        return new SessionCoverageTarget("", targetId, sessionId, objective, ruleReference);
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        return value.strip();
    }
}
