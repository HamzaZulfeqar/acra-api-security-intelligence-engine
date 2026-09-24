package io.acra.core.active.research;

import io.acra.core.domain.common.Validation;
import java.util.List;

public record AblationVariant(
        String variantId,
        int order,
        List<AblationDimension> enabledDimensions,
        String description) {

    public AblationVariant {
        variantId = Validation.requireNonBlank(variantId, "variantId");
        if (order < 0) throw new IllegalArgumentException("order cannot be negative");
        enabledDimensions = List.copyOf(enabledDimensions == null ? List.of() : enabledDimensions);
        if (enabledDimensions.stream().distinct().count() != enabledDimensions.size()) {
            throw new IllegalArgumentException("duplicate ablation dimension");
        }
        description = Validation.requireNonBlank(description, "description");
    }
}
