package io.acra.core.session;

import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.recon.IdentityConfidenceState;
import io.acra.core.security.UniversalRedactor;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public record AuthenticationSessionObservation(
        String observationId,
        String sessionId,
        String tokenFingerprint,
        String principalId,
        String roleId,
        String tenantId,
        List<String> scopes,
        AuthenticationType authenticationType,
        IdentityConfidenceState identityState,
        Instant observedAt,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    public AuthenticationSessionObservation {
        observationId = requiredSafe(observationId, "observationId");
        sessionId = requiredSafe(sessionId, "sessionId");
        tokenFingerprint = fingerprint(tokenFingerprint);
        principalId = normalized(principalId);
        roleId = normalized(roleId);
        tenantId = normalized(tenantId);
        scopes = List.copyOf(scopes == null ? List.<String>of() : scopes).stream()
                .map(AuthenticationSessionObservation::normalized)
                .distinct()
                .sorted()
                .toList();
        authenticationType = authenticationType == null ? AuthenticationType.UNKNOWN : authenticationType;
        identityState = identityState == null ? IdentityConfidenceState.UNKNOWN : identityState;
        if (observedAt == null) throw new IllegalArgumentException("observedAt required");
        evidenceIds = List.copyOf(evidenceIds == null ? List.<String>of() : evidenceIds).stream()
                .map(value -> requiredSafe(value, "evidenceId"))
                .distinct()
                .sorted()
                .toList();
        if (evidenceIds.isEmpty()) throw new IllegalArgumentException("evidenceIds required");
    }

    public boolean identityVerified() {
        return identityState == IdentityConfidenceState.USER_CONFIRMED
                || identityState == IdentityConfidenceState.LAB_CONFIRMED;
    }

    private static String fingerprint(String value) {
        String safe = requiredSafe(value, "tokenFingerprint").toLowerCase(Locale.ROOT);
        if (!SHA256.matcher(safe).matches()) {
            throw new IllegalArgumentException("tokenFingerprint must be a SHA-256 fingerprint");
        }
        return safe;
    }

    private static String normalized(String value) {
        if (value == null || value.isBlank()) return "UNKNOWN";
        return requiredSafe(value, "context value");
    }

    private static String requiredSafe(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        String stripped = value.strip();
        if (!stripped.equals(REDACTOR.redactText(stripped))) {
            throw new IllegalArgumentException(name + " contains sensitive material");
        }
        return stripped;
    }
}
