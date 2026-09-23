package io.acra.core.domain.authorization;

import java.util.List;

public record EffectiveRoleResolution(
        String principalId,
        String tenantId,
        List<String> directRoleIds,
        List<String> inheritedRoleIds,
        List<String> effectiveRoleIds,
        RoleResolutionState state,
        List<String> reasons) {

    public EffectiveRoleResolution {
        directRoleIds = sorted(directRoleIds);
        inheritedRoleIds = sorted(inheritedRoleIds);
        effectiveRoleIds = sorted(effectiveRoleIds);
        state = state == null ? RoleResolutionState.UNKNOWN : state;
        reasons = sorted(reasons);
    }

    private static List<String> sorted(List<String> values) {
        return List.copyOf(values == null ? List.<String>of() : values).stream()
                .filter(v -> v != null && !v.isBlank()).distinct().sorted().toList();
    }
}
