package io.acra.core.active.analysis;

import io.acra.core.analysis.semantic.ResponseSemanticFingerprint;
import io.acra.core.analysis.semantic.SemanticResponseClass;
import io.acra.core.domain.http.HttpResponse;

public final class AuthorizationOutcomeNormalizer {
    public AuthorizationOutcome normalize(HttpResponse response, ResponseSemanticFingerprint fingerprint) {
        if (response == null || fingerprint == null) return AuthorizationOutcome.UNKNOWN;
        int status = response.status();
        if (status == 401) return AuthorizationOutcome.AUTHENTICATION_REQUIRED;
        if (status == 403) return AuthorizationOutcome.DENY;
        if (status == 404) return AuthorizationOutcome.NOT_FOUND;
        if (status == 206) return AuthorizationOutcome.PARTIAL;
        if (status >= 500) return AuthorizationOutcome.ERROR;
        if (status >= 200 && status < 300) {
            if (fingerprint.responseClass() == SemanticResponseClass.ERROR_LIKE) return AuthorizationOutcome.DENY;
            if (fingerprint.responseClass() == SemanticResponseClass.UNKNOWN) return AuthorizationOutcome.UNKNOWN;
            return AuthorizationOutcome.ALLOW;
        }
        if (status >= 400) return AuthorizationOutcome.ERROR;
        return AuthorizationOutcome.UNKNOWN;
    }
}
