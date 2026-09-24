package io.acra.core.oauth;

import io.acra.core.recon.IdentityConfidenceState;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public record OAuthContextObservation(
        String observationId,
        String requestId,
        OAuthProtocol protocol,
        String issuer,
        String authorizationEndpoint,
        String tokenEndpoint,
        String clientId,
        List<String> resourceIndicators,
        List<String> audiences,
        List<String> scopes,
        List<String> responseTypes,
        String grantType,
        String redirectUriFingerprint,
        PkceMethod pkceMethod,
        boolean statePresent,
        boolean noncePresent,
        IdentityConfidenceState identityState,
        Instant observedAt,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    public OAuthContextObservation {
        observationId = requiredSafe(observationId, "observationId");
        requestId = requiredSafe(requestId, "requestId");
        protocol = protocol == null ? OAuthProtocol.UNKNOWN : protocol;
        issuer = issuer == null || issuer.isBlank() || "UNKNOWN".equalsIgnoreCase(issuer.strip())\n                ? "UNKNOWN"\n                : uriReference(issuer, "issuer");
        authorizationEndpoint = optionalUriReference(authorizationEndpoint, "authorizationEndpoint");
        tokenEndpoint = optionalUriReference(tokenEndpoint, "tokenEndpoint");
        clientId = normalized(clientId);
        resourceIndicators = normalizedList(resourceIndicators);
        audiences = normalizedList(audiences);
        scopes = normalizedList(scopes);
        responseTypes = normalizedList(responseTypes);
        grantType = normalized(grantType);
        redirectUriFingerprint = optionalFingerprint(redirectUriFingerprint);
        pkceMethod = pkceMethod == null ? PkceMethod.UNKNOWN : pkceMethod;
        identityState = identityState == null ? IdentityConfidenceState.UNKNOWN : identityState;
        if (observedAt == null) throw new IllegalArgumentException("observedAt required");
        evidenceIds = List.copyOf(evidenceIds == null ? List.<String>of() : evidenceIds).stream()
                .map(value -> requiredSafe(value, "evidenceId"))
                .distinct()
                .sorted()
                .toList();
        if (evidenceIds.isEmpty()) throw new IllegalArgumentException("evidenceIds required");
    }

    public String contextId() {
        String material = protocol.name() + "|" + issuer + "|" + clientId;
        return "s11-oauth-context-" + TokenFingerprint.sha256(material).substring(0, 24);
    }

    public boolean identityVerified() {
        return identityState == IdentityConfidenceState.USER_CONFIRMED
                || identityState == IdentityConfidenceState.LAB_CONFIRMED;
    }

    public boolean redirectUriObserved() {
        return !redirectUriFingerprint.isEmpty();
    }

    private static List<String> normalizedList(List<String> values) {
        return List.copyOf(values == null ? List.<String>of() : values).stream()
                .map(OAuthContextObservation::normalized)
                .filter(value -> !"UNKNOWN".equals(value))
                .distinct()
                .sorted()
                .toList();
    }

    private static String uriReference(String value, String name) {
        String normalized = requiredSafe(value, name);
        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException(name + " must be a valid URI", error);
        }
        if (!uri.isAbsolute() || uri.getScheme() == null
                || (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme()))
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException(name + " must be an absolute HTTP(S) URI without user-info, query or fragment");
        }
        return normalized;
    }

    private static String optionalUriReference(String value, String name) {
        return value == null || value.isBlank() ? "" : uriReference(value, name);
    }

    private static String optionalFingerprint(String value) {
        if (value == null || value.isBlank()) return "";
        String safe = requiredSafe(value, "redirectUriFingerprint").toLowerCase(Locale.ROOT);
        if (!SHA256.matcher(safe).matches()) {
            throw new IllegalArgumentException("redirectUriFingerprint must be a SHA-256 fingerprint");
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
