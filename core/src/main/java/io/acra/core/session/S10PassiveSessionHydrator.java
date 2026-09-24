package io.acra.core.session;

import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.engine.SecurityContextSnapshot;
import io.acra.core.recon.ConfirmedIdentityContext;
import io.acra.core.recon.IdentityConfidenceState;
import io.acra.core.recon.IdentityConfirmationRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class S10PassiveSessionHydrator {
    private final IdentityConfirmationRegistry confirmations;

    public S10PassiveSessionHydrator(IdentityConfirmationRegistry confirmations) {
        if (confirmations == null) throw new IllegalArgumentException("identity confirmation registry required");
        this.confirmations = confirmations;
    }

    public Optional<PassiveSessionHydrationResult> hydrate(
            HttpTransaction transaction,
            SecurityContextSnapshot snapshot) {

        if (transaction == null || snapshot == null) {
            throw new IllegalArgumentException("transaction and security context snapshot required");
        }

        String fingerprint = snapshot.identity().tokenFingerprint();
        if (fingerprint == null || fingerprint.isBlank()) {
            return Optional.empty();
        }

        List<String> reasons = new ArrayList<>();
        String sessionId = sessionId(transaction, reasons);
        List<String> scopes = scopes(transaction);

        String extractedPrincipal = snapshot.identity().principal().resolved()
                .map(value -> value.principalId())
                .filter(value -> !value.isBlank())
                .orElse("UNKNOWN");
        String extractedRole = snapshot.identity().role().resolved()
                .map(value -> value.roleId())
                .filter(value -> !value.isBlank())
                .orElse("UNKNOWN");
        String extractedTenant = snapshot.tenant().resolved()
                .map(value -> value.tenantId())
                .filter(value -> !value.isBlank())
                .orElse("UNKNOWN");

        String principal = extractedPrincipal;
        String role = extractedRole;
        String tenant = extractedTenant;
        IdentityConfidenceState identityState = inferredState(extractedPrincipal, extractedRole, extractedTenant);
        boolean conflict = false;

        Optional<ConfirmedIdentityContext> confirmed = confirmations.find(fingerprint);
        if (confirmed.isPresent() && isVerified(confirmed.get().state())) {
            ConfirmedIdentityContext value = confirmed.get();
            conflict = conflicts(extractedPrincipal, value.principalId())
                    || conflicts(extractedRole, value.role())
                    || conflicts(extractedTenant, value.tenantId());
            if (conflict) {
                identityState = IdentityConfidenceState.SUSPECTED;
                reasons.add("CONFIRMED_IDENTITY_CONTEXT_CONFLICT");
            } else {
                principal = normalized(value.principalId());
                role = normalized(value.role());
                tenant = normalized(value.tenantId());
                identityState = value.state();
                reasons.add("IDENTITY_CONTEXT_CONFIRMED");
            }
        } else {
            reasons.add("IDENTITY_CONTEXT_NOT_CONFIRMED");
        }

        List<String> evidenceIds = snapshot.evidence().stream()
                .map(value -> value.evidenceId())
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .toList();
        if (evidenceIds.isEmpty()) {
            throw new IllegalArgumentException("passive session hydration requires evidence provenance");
        }

        AuthenticationSessionObservation observation = new AuthenticationSessionObservation(
                "s10-passive-" + transaction.requestId(),
                sessionId,
                fingerprint,
                principal,
                role,
                tenant,
                scopes,
                snapshot.identity().authenticationType(),
                identityState,
                transaction.timestamp(),
                evidenceIds);

        return Optional.of(new PassiveSessionHydrationResult(
                observation,
                conflict,
                List.copyOf(new LinkedHashSet<>(reasons))));
    }

    private static String sessionId(HttpTransaction transaction, List<String> reasons) {
        String value = firstNonBlank(
                transaction.metadata().get("session_id"),
                transaction.metadata().get("sessionId"));
        if (value != null) return value;
        reasons.add("SESSION_ID_UNAVAILABLE");
        return "unresolved:" + transaction.requestId();
    }

    private static List<String> scopes(HttpTransaction transaction) {
        String raw = firstNonBlank(
                transaction.metadata().get("auth.scopes"),
                transaction.metadata().get("auth_scopes"));
        if (raw == null) return List.of();
        return Arrays.stream(raw.split("[,\\s]+"))
                .map(String::strip)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    private static IdentityConfidenceState inferredState(String principal, String role, String tenant) {
        return unknown(principal) && unknown(role) && unknown(tenant)
                ? IdentityConfidenceState.UNKNOWN
                : IdentityConfidenceState.INFERRED;
    }

    private static boolean isVerified(IdentityConfidenceState state) {
        return state == IdentityConfidenceState.USER_CONFIRMED
                || state == IdentityConfidenceState.LAB_CONFIRMED;
    }

    private static boolean conflicts(String extracted, String confirmed) {
        return !unknown(extracted)
                && !unknown(confirmed)
                && !extracted.equals(confirmed);
    }

    private static boolean unknown(String value) {
        return value == null || value.isBlank() || "UNKNOWN".equals(value.toUpperCase(Locale.ROOT));
    }

    private static String normalized(String value) {
        return unknown(value) ? "UNKNOWN" : value;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first.strip();
        if (second != null && !second.isBlank()) return second.strip();
        return null;
    }
}
