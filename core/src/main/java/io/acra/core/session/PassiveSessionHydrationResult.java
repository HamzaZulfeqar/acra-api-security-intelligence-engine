package io.acra.core.session;

import java.util.List;

public record PassiveSessionHydrationResult(
        AuthenticationSessionObservation observation,
        boolean confirmationConflict,
        List<String> reasons) {

    public PassiveSessionHydrationResult {
        if (observation == null) throw new IllegalArgumentException("observation required");
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
    }
}
