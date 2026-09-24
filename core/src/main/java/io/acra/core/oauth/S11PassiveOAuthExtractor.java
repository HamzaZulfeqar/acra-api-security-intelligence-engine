package io.acra.core.oauth;

import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.common.ConfidenceBasis;
import io.acra.core.domain.evidence.Evidence;
import io.acra.core.domain.evidence.EvidenceSource;
import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.recon.IdentityConfidenceState;
import io.acra.core.security.TokenFingerprint;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class S11PassiveOAuthExtractor {

    public Optional<OAuthExtractionResult> extract(HttpTransaction transaction) {
        if (transaction == null) throw new IllegalArgumentException("transaction required");

        Map<String, List<String>> query = parse(transaction.request().query());
        Map<String, List<String>> form = formParameters(transaction);

        boolean authorizationRequest = has(query, "client_id") && has(query, "response_type");
        boolean tokenRequest = has(form, "grant_type");
        if (!authorizationRequest && !tokenRequest) return Optional.empty();

        Map<String, List<String>> primary = authorizationRequest ? query : form;
        EvidenceSource source = authorizationRequest ? EvidenceSource.QUERY : EvidenceSource.BODY;
        List<Evidence> evidence = new ArrayList<>();

        String issuer = metadata(transaction, "oauth.issuer");
        if (issuer == null || issuer.isBlank()) issuer = "UNKNOWN";
        else evidence.add(evidence(transaction, EvidenceSource.TRANSACTION, "metadata:oauth.issuer",
                issuer, "oauth-metadata"));

        String clientId = first(primary, "client_id");
        if (clientId == null || clientId.isBlank()) clientId = "UNKNOWN";
        else evidence.add(evidence(transaction, source, fieldLocation(source, "client_id"),
                clientId, "oauth-parameter"));

        List<String> scopes = splitSpaceValues(primary.get("scope"));
        if (!scopes.isEmpty()) {
            evidence.add(evidence(transaction, source, fieldLocation(source, "scope"),
                    String.join(" ", scopes), "oauth-parameter"));
        }

        List<String> responseTypes = splitSpaceValues(primary.get("response_type"));
        if (!responseTypes.isEmpty()) {
            evidence.add(evidence(transaction, source, fieldLocation(source, "response_type"),
                    String.join(" ", responseTypes), "oauth-parameter"));
        }

        String grantType = first(primary, "grant_type");
        if (grantType == null || grantType.isBlank()) grantType = "UNKNOWN";
        else evidence.add(evidence(transaction, source, fieldLocation(source, "grant_type"),
                grantType, "oauth-parameter"));

        List<String> resources = normalizedValues(primary.get("resource"));
        if (!resources.isEmpty()) {
            evidence.add(evidence(transaction, source, fieldLocation(source, "resource"),
                    String.join(" ", resources), "oauth-parameter"));
        }

        List<String> audiences = normalizedValues(primary.get("audience"));
        if (!audiences.isEmpty()) {
            evidence.add(evidence(transaction, source, fieldLocation(source, "audience"),
                    String.join(" ", audiences), "oauth-parameter"));
        }

        String redirect = first(primary, "redirect_uri");
        String redirectFingerprint = "";
        if (redirect != null && !redirect.isBlank()) {
            redirectFingerprint = TokenFingerprint.sha256(redirect);
            evidence.add(evidence(transaction, source, fieldLocation(source, "redirect_uri"),
                    redirectFingerprint, "sha256-redirect-uri-fingerprint"));
        }

        boolean statePresent = hasNonBlank(primary, "state");
        if (statePresent) {
            evidence.add(evidence(transaction, source, fieldLocation(source, "state"),
                    "present", "presence-only"));
        }

        boolean noncePresent = hasNonBlank(primary, "nonce");
        if (noncePresent) {
            evidence.add(evidence(transaction, source, fieldLocation(source, "nonce"),
                    "present", "presence-only"));
        }

        PkceMethod pkceMethod = pkce(primary);
        if (hasNonBlank(primary, "code_challenge")) {
            evidence.add(evidence(transaction, source, fieldLocation(source, "code_challenge"),
                    "present", "presence-only"));
            evidence.add(evidence(transaction, source, fieldLocation(source, "code_challenge_method"),
                    pkceMethod.name(), "oauth-pkce-method"));
        } else if (hasNonBlank(primary, "code_verifier")) {
            evidence.add(evidence(transaction, source, fieldLocation(source, "code_verifier"),
                    "present", "presence-only"));
        }

        String authorizationEndpoint = authorizationRequest
                ? endpoint(transaction)
                : optionalMetadata(transaction, "oauth.authorization_endpoint", evidence);
        String tokenEndpoint = tokenRequest
                ? endpoint(transaction)
                : optionalMetadata(transaction, "oauth.token_endpoint", evidence);

        OAuthProtocol protocol = protocol(transaction, scopes);
        evidence.sort(Comparator.comparing(Evidence::evidenceId));
        List<String> evidenceIds = evidence.stream().map(Evidence::evidenceId).toList();

        OAuthContextObservation observation = new OAuthContextObservation(
                "s11-oauth-observation-" + TokenFingerprint.sha256(
                        transaction.requestId() + "|" + protocol + "|" + issuer + "|" + clientId + "|"
                                + authorizationEndpoint + "|" + tokenEndpoint + "|" + redirectFingerprint)
                        .substring(0, 24),
                transaction.requestId(),
                protocol,
                issuer,
                authorizationEndpoint,
                tokenEndpoint,
                clientId,
                resources,
                audiences,
                scopes,
                responseTypes,
                grantType,
                redirectFingerprint,
                pkceMethod,
                statePresent,
                noncePresent,
                IdentityConfidenceState.UNKNOWN,
                transaction.timestamp(),
                evidenceIds);

        return Optional.of(new OAuthExtractionResult(observation, evidence));
    }

    private static OAuthProtocol protocol(HttpTransaction transaction, List<String> scopes) {
        String explicit = metadata(transaction, "oauth.protocol");
        if (explicit != null && explicit.equalsIgnoreCase("oidc")) return OAuthProtocol.OIDC;
        if (scopes.stream().anyMatch("openid"::equals)) return OAuthProtocol.OIDC;
        return OAuthProtocol.OAUTH2;
    }

    private static PkceMethod pkce(Map<String, List<String>> params) {
        if (!hasNonBlank(params, "code_challenge")) {
            return hasNonBlank(params, "code_verifier") ? PkceMethod.UNKNOWN : PkceMethod.NONE;
        }
        String method = first(params, "code_challenge_method");
        if (method == null || method.isBlank()) return PkceMethod.UNKNOWN;
        if ("S256".equalsIgnoreCase(method)) return PkceMethod.S256;
        if ("plain".equalsIgnoreCase(method)) return PkceMethod.PLAIN;
        return PkceMethod.UNKNOWN;
    }

    private static String optionalMetadata(
            HttpTransaction transaction,
            String key,
            List<Evidence> evidence) {
        String value = metadata(transaction, key);
        if (value == null || value.isBlank()) return "";
        evidence.add(evidence(transaction, EvidenceSource.TRANSACTION, "metadata:" + key,
                value, "oauth-metadata"));
        return value;
    }

    private static String metadata(HttpTransaction transaction, String key) {
        return transaction.metadata().get(key);
    }

    private static Evidence evidence(
            HttpTransaction transaction,
            EvidenceSource source,
            String location,
            String value,
            String method) {
        Confidence confidence = Confidence.of(
                source == EvidenceSource.TRANSACTION
                        ? ConfidenceBasis.EXPLICIT_METADATA
                        : ConfidenceBasis.EXACT_OBSERVED);
        return Evidence.create(source, transaction.requestId(), location, value, method, confidence,
                transaction.timestamp());
    }

    private static String fieldLocation(EvidenceSource source, String field) {
        return (source == EvidenceSource.QUERY ? "query:" : "body:") + field;
    }

    private static String endpoint(HttpTransaction transaction) {
        int port = transaction.request().port();
        boolean defaultPort = ("https".equalsIgnoreCase(transaction.request().scheme()) && port == 443)
                || ("http".equalsIgnoreCase(transaction.request().scheme()) && port == 80);
        return transaction.request().scheme() + "://" + transaction.request().host()
                + (defaultPort ? "" : ":" + port)
                + transaction.request().path();
    }

    private static Map<String, List<String>> formParameters(HttpTransaction transaction) {
        boolean form = transaction.request().firstHeader("Content-Type")
                .map(value -> value.toLowerCase(Locale.ROOT)
                        .contains("application/x-www-form-urlencoded"))
                .orElse(false);
        return form ? parse(transaction.request().bodyUtf8()) : Map.of();
    }

    private static Map<String, List<String>> parse(String encoded) {
        if (encoded == null || encoded.isBlank()) return Map.of();
        Map<String, List<String>> result = new LinkedHashMap<>();
        try {
            for (String pair : encoded.split("&")) {
                if (pair.isBlank()) continue;
                int eq = pair.indexOf('=');
                String rawName = eq < 0 ? pair : pair.substring(0, eq);
                String rawValue = eq < 0 ? "" : pair.substring(eq + 1);
                String name = URLDecoder.decode(rawName, StandardCharsets.UTF_8);
                String value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
                result.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
            }
        } catch (IllegalArgumentException error) {
            return Map.of();
        }
        Map<String, List<String>> immutable = new LinkedHashMap<>();
        result.forEach((key, value) -> immutable.put(key, List.copyOf(value)));
        return Map.copyOf(immutable);
    }

    private static boolean has(Map<String, List<String>> params, String key) {
        return params.containsKey(key);
    }

    private static boolean hasNonBlank(Map<String, List<String>> params, String key) {
        List<String> values = params.get(key);
        return values != null && values.stream().anyMatch(value -> value != null && !value.isBlank());
    }

    private static String first(Map<String, List<String>> params, String key) {
        List<String> values = params.get(key);
        return values == null || values.isEmpty() ? null : values.getFirst();
    }

    private static List<String> splitSpaceValues(List<String> values) {
        if (values == null) return List.of();
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value == null) continue;
            for (String part : value.strip().split("\\s+")) {
                if (!part.isBlank()) result.add(part);
            }
        }
        return result.stream().distinct().sorted().toList();
    }

    private static List<String> normalizedValues(List<String> values) {
        if (values == null) return List.of();
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::strip)
                .distinct()
                .sorted()
                .toList();
    }
}
