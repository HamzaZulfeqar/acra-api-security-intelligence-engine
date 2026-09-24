package io.acra.core.security;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class UniversalRedactor {

    public static final String REDACTED = "<redacted>";

    private static final Set<String> SENSITIVE_HEADERS = Set.of(
        "authorization",
        "proxy-authorization",
        "cookie",
        "set-cookie",
        "x-api-key",
        "api-key",
        "x-auth-token",
        "x-api-token",
        "api-token"
    );

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "password",
        "passwd",
        "secret",
        "token",
        "apikey",
        "accesskey",
        "accesstoken",
        "refreshtoken",
        "privatekey",
        "csrftoken",
        "session",
        "sessionid",
        "sessionsecret",
        "sessiontoken",
        "authorization",
        "proxyauthorization",
        "cookie",
        "setcookie",
        "authenticationcookie",
        "xapikey",
        "xauthtoken",
        "xapitoken"
    );

    private static final Pattern HEADER_TEXT = Pattern.compile(
        "(?im)((?:^|\\r?\\n)\\s*(?:authorization|proxy[-_]authorization|cookie|set[-_]cookie|x[-_]api[-_]key|api[-_]key|x[-_]auth[-_]token|x[-_]api[-_]token)\\s*[:=]\\s*)([^\\r\\n]+)"
    );

    private static final Pattern BEARER = Pattern.compile(
        "(?i)\\bBearer\\s+[A-Za-z0-9._~+\\-/]+=*\\b"
    );

    private static final Pattern BASIC = Pattern.compile(
        "(?i)\\bBasic\\s+[A-Za-z0-9+/]+=*"
    );

    private static final Pattern JWT = Pattern.compile(
        "\\beyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}(?:\\.[A-Za-z0-9_-]{8,})?\\b"
    );

    private static final Pattern KEY_VALUE = Pattern.compile(
        "(?i)(\\\"?(?:password|passwd|secret|token|api[_-]?key|access[_-]?token|refresh[_-]?token|private[_-]?key|csrf[_-]?token|session(?:[_-]?(?:id|secret|token))?)\\\"?\\s*[:=]\\s*\\\"?)([^\\\"&,;\\s}]+)"
    );

    public String redactHeader(String name, String value) {
        if (name != null && SENSITIVE_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
            return REDACTED;
        }
        return redactText(value);
    }

    public String redactText(String value) {
        if (value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }

        String out = HEADER_TEXT.matcher(value)
            .replaceAll("$1" + REDACTED);

        out = BEARER.matcher(out)
            .replaceAll("Bearer " + REDACTED);

        out = BASIC.matcher(out)
            .replaceAll("Basic " + REDACTED);

        out = JWT.matcher(out)
            .replaceAll(REDACTED);

        out = KEY_VALUE.matcher(out)
            .replaceAll("$1" + REDACTED);

        return out;
    }

    /**
     * Field-name protection complements pattern redaction
     * for generic maps and records.
     */
    public boolean isSensitiveField(String name) {
        return name != null
            && SENSITIVE_FIELDS.contains(
                name.toLowerCase(Locale.ROOT)
                    .replace("-", "")
                    .replace("_", "")
            );
    }

    public String fingerprint(String rawSecret) {
        return TokenFingerprint.sha256(rawSecret);
    }
}