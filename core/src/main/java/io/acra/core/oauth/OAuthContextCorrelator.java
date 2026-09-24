package io.acra.core.oauth;

import io.acra.core.security.TokenFingerprint;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class OAuthContextCorrelator {

    public OAuthContextCorrelation correlate(
            OAuthContextObservation previous,
            OAuthContextObservation current) {

        if (previous == null || current == null) {
            return result(previous, current, OAuthContextCorrelationState.INCONCLUSIVE,
                    List.of(), List.of("OBSERVATION_MISSING"));
        }
        if (previous.protocol() == OAuthProtocol.UNKNOWN || current.protocol() == OAuthProtocol.UNKNOWN) {
            return result(previous, current, OAuthContextCorrelationState.INCONCLUSIVE,
                    List.of(), List.of("PROTOCOL_UNKNOWN"));
        }
        if ("UNKNOWN".equals(previous.clientId()) || "UNKNOWN".equals(current.clientId())) {
            return result(previous, current, OAuthContextCorrelationState.INCONCLUSIVE,
                    List.of(), List.of("CLIENT_ID_UNKNOWN"));
        }
        if (!previous.clientId().equals(current.clientId())) {
            return result(previous, current, OAuthContextCorrelationState.INCONCLUSIVE,
                    List.of(), List.of("DIFFERENT_CLIENT_CONTEXT"));
        }

        List<OAuthContextDimension> drift = new ArrayList<>();
        if (!previous.issuer().equals(current.issuer())) drift.add(OAuthContextDimension.ISSUER);
        if (!Objects.equals(previous.redirectUriFingerprint(), current.redirectUriFingerprint())) {
            drift.add(OAuthContextDimension.REDIRECT_URI);
        }
        if (!previous.resourceIndicators().equals(current.resourceIndicators())) {
            drift.add(OAuthContextDimension.RESOURCE);
        }
        if (!previous.audiences().equals(current.audiences())) drift.add(OAuthContextDimension.AUDIENCE);
        if (!previous.scopes().equals(current.scopes())) drift.add(OAuthContextDimension.SCOPE);
        if (!previous.responseTypes().equals(current.responseTypes())) {
            drift.add(OAuthContextDimension.RESPONSE_TYPE);
        }
        if (!previous.grantType().equals(current.grantType())) drift.add(OAuthContextDimension.GRANT_TYPE);
        if (previous.pkceMethod() != current.pkceMethod()) drift.add(OAuthContextDimension.PKCE);
        if (previous.statePresent() != current.statePresent()) drift.add(OAuthContextDimension.STATE_SIGNAL);
        if (previous.noncePresent() != current.noncePresent()) drift.add(OAuthContextDimension.NONCE_SIGNAL);

        OAuthContextCorrelationState state = drift.isEmpty()
                ? OAuthContextCorrelationState.STABLE
                : OAuthContextCorrelationState.CONTEXT_DRIFT;
        return result(previous, current, state, drift, List.of());
    }

    private OAuthContextCorrelation result(
            OAuthContextObservation previous,
            OAuthContextObservation current,
            OAuthContextCorrelationState state,
            List<OAuthContextDimension> drift,
            List<String> reasons) {

        String previousId = previous == null ? "" : previous.observationId();
        String currentId = current == null ? "" : current.observationId();
        String material = previousId + "|" + currentId + "|" + state.name() + "|" + drift;
        Set<String> evidence = new LinkedHashSet<>();
        if (previous != null) evidence.addAll(previous.evidenceIds());
        if (current != null) evidence.addAll(current.evidenceIds());
        return new OAuthContextCorrelation(
                "s11-oauth-correlation-" + TokenFingerprint.sha256(material).substring(0, 24),
                previousId,
                currentId,
                state,
                drift,
                List.copyOf(evidence),
                reasons);
    }
}
