package io.acra.core.domain.authorization;

import java.util.ArrayList;
import java.util.List;

public record AuthorizationContextCompleteness(
        boolean principal,
        boolean role,
        boolean tenant,
        boolean resource,
        boolean owner,
        boolean action,
        boolean workflow,
        boolean endpoint,
        boolean policy,
        boolean evidence,
        boolean expectedDecision,
        boolean observedDecision) {

    public int knownCount() {
        int count = 0;
        if (principal) count++;
        if (role) count++;
        if (tenant) count++;
        if (resource) count++;
        if (owner) count++;
        if (action) count++;
        if (workflow) count++;
        if (endpoint) count++;
        if (policy) count++;
        if (evidence) count++;
        if (expectedDecision) count++;
        if (observedDecision) count++;
        return count;
    }

    public int total() {
        return 12;
    }

    public boolean objectLevelComplete() {
        return principal && resource && owner && action && evidence && expectedDecision && observedDecision;
    }

    public boolean functionLevelComplete() {
        return principal && role && action && endpoint && policy && evidence && expectedDecision && observedDecision;
    }

    public boolean tenantLevelComplete() {
        return principal && tenant && resource && action && policy && evidence && expectedDecision && observedDecision;
    }

    public boolean workflowLevelComplete() {
        return principal && role && workflow && action && policy && evidence && expectedDecision && observedDecision;
    }

    public boolean propertyLevelComplete() {
        return principal && role && tenant && resource && endpoint && policy && evidence
                && expectedDecision && observedDecision;
    }

    public boolean allDimensionsComplete() {
        return knownCount() == total();
    }

    public List<String> missingDimensions() {
        List<String> missing = new ArrayList<>();
        if (!principal) missing.add("principal");
        if (!role) missing.add("role");
        if (!tenant) missing.add("tenant");
        if (!resource) missing.add("resource");
        if (!owner) missing.add("owner");
        if (!action) missing.add("action");
        if (!workflow) missing.add("workflow");
        if (!endpoint) missing.add("endpoint");
        if (!policy) missing.add("policy");
        if (!evidence) missing.add("evidence");
        if (!expectedDecision) missing.add("expectedDecision");
        if (!observedDecision) missing.add("observedDecision");
        return List.copyOf(missing);
    }
}
