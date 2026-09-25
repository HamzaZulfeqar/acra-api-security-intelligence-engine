package io.acra.core.active.execution;

import io.acra.core.active.model.RequestDefinition;
import io.acra.core.active.model.SecurityTest;
import java.util.List;

public final class AuthenticatedContextSubstitutionResolver {

    public RequestDefinition resolve(SecurityTest test, String contextRef) {
        if (test == null) throw new IllegalArgumentException("test required");
        if (contextRef == null || contextRef.isBlank()) throw new IllegalArgumentException("contextRef required");

        List<RequestDefinition> matches = List.of(
                test.baselineDefinition(),
                test.positiveControl(),
                test.negativeControl()).stream()
                .filter(definition -> contextRef.equals(definition.contextRef()))
                .toList();

        if (matches.isEmpty()) {
            throw new IllegalArgumentException("authenticated context reference is not present in explicit test controls");
        }
        if (matches.size() > 1) {
            String firstId = matches.getFirst().definitionId();
            boolean sameDefinition = matches.stream().allMatch(value -> value.definitionId().equals(firstId));
            if (!sameDefinition) {
                throw new IllegalArgumentException("authenticated context reference is ambiguous across explicit controls");
            }
        }
        return matches.getFirst();
    }
}
