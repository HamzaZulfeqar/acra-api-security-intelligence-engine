package io.acra.core.domain.authorization;

import java.util.ArrayList;
import java.util.List;

public record AuthorizationPolicyCoverage(
        boolean tenantContext,
        boolean roleContext,
        boolean hierarchyContext,
        boolean permissionContext,
        boolean policyRuleContext,
        boolean decisionResolved) {

    public int knownCount() {
        int count = 0;
        if (tenantContext) count++;
        if (roleContext) count++;
        if (hierarchyContext) count++;
        if (permissionContext) count++;
        if (policyRuleContext) count++;
        if (decisionResolved) count++;
        return count;
    }

    public int total() {
        return 6;
    }

    public double ratio() {
        return knownCount() / (double) total();
    }

    public List<String> missingDimensions() {
        List<String> missing = new ArrayList<>();
        if (!tenantContext) missing.add("tenant");
        if (!roleContext) missing.add("role");
        if (!hierarchyContext) missing.add("hierarchy");
        if (!permissionContext) missing.add("permission");
        if (!policyRuleContext) missing.add("policyRule");
        if (!decisionResolved) missing.add("decision");
        return List.copyOf(missing);
    }
}
