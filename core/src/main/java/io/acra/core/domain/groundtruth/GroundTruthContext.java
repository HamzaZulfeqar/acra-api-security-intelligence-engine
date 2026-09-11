package io.acra.core.domain.groundtruth;

import io.acra.core.domain.authorization.*;
import io.acra.core.domain.identity.*;
import io.acra.core.domain.resource.Resource;
import io.acra.core.domain.tenant.Tenant;
import io.acra.core.domain.workflow.WorkflowState;
import io.acra.core.domain.common.Validation;

public record GroundTruthContext(String groundTruthId, Principal principal, Role role, Tenant tenant, Resource resource,
                                 String ownerPrincipalId, Action action, AuthorizationDecision expectedDecision,
                                 WorkflowState workflowState, boolean vulnerableCase) {
    public GroundTruthContext {
        groundTruthId=Validation.requireNonBlank(groundTruthId,"groundTruthId");
        ownerPrincipalId=ownerPrincipalId==null?"":ownerPrincipalId;
        if(action==null) action=new Action(ActionType.UNKNOWN,null,null,"");
        if(expectedDecision==null) expectedDecision=AuthorizationDecision.UNKNOWN;
        if(workflowState==null) workflowState=WorkflowState.unknown();
    }
}
