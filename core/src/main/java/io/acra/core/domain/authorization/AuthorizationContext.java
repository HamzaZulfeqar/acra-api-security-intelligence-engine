package io.acra.core.domain.authorization;

import io.acra.core.domain.identity.*;
import io.acra.core.domain.tenant.Tenant;
import io.acra.core.domain.resource.Resource;
import io.acra.core.domain.workflow.WorkflowState;
import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record AuthorizationContext(Principal principal, Role role, Tenant tenant, Resource resource,
                                   String ownerPrincipalId, Action action, WorkflowState workflowState,
                                   AuthorizationDecision expectedDecision, AuthorizationDecision observedDecision,
                                   List<String> evidenceIds, ContextStatus status) {
    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public AuthorizationContext {
        if (principal != null) principal = new Principal(safe(principal.principalId()), safe(principal.displayName()),
                principal.authenticationType(), principal.confidence());
        if (role != null) role = new Role(safe(role.roleId()), safe(role.name()), role.source(), role.confidence());
        if (tenant != null) tenant = new Tenant(safe(tenant.tenantId()), safe(tenant.name()), tenant.source(), tenant.confidence());
        if (resource != null) resource = new Resource(safe(resource.resourceId()), safe(resource.resourceType()),
                safe(resource.parentResourceId()), safe(resource.ownerPrincipalId()), safe(resource.tenantId()),
                safe(resource.state()), resource.confidence());
        ownerPrincipalId = safe(ownerPrincipalId);
        if (action == null) action = new Action(ActionType.UNKNOWN, null, null, "");
        else action = new Action(action.actionType(), action.source(), action.confidence(), safe(action.applicationAction()));
        if (workflowState == null) workflowState = WorkflowState.unknown();
        else workflowState = new WorkflowState(safe(workflowState.name()), workflowState.confidence());
        if (expectedDecision == null) expectedDecision = AuthorizationDecision.UNKNOWN;
        if (observedDecision == null) observedDecision = AuthorizationDecision.UNKNOWN;
        evidenceIds = List.copyOf(evidenceIds == null ? List.<String>of() : evidenceIds).stream()
                .map(AuthorizationContext::safe).toList();
        if (status == null) status = ContextStatus.UNKNOWN;
    }

    private static String safe(String value) { return REDACTOR.redactText(value); }
}
