package io.acra.core.domain.workflow;

import io.acra.core.domain.common.Validation;
import java.util.List;

public record Workflow(String workflowId, List<WorkflowState> states) {
    public Workflow {
        workflowId = Validation.requireNonBlank(workflowId, "workflowId");
        states = List.copyOf(states == null ? List.of() : states);
    }
}
