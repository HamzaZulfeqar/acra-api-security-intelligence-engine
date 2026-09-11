package io.acra.core.domain.workflow;

import io.acra.core.domain.common.Confidence;

public record WorkflowState(String name, Confidence confidence) {
    public WorkflowState {
        name = name == null || name.isBlank() ? "UNKNOWN" : name;
        if (confidence == null) confidence = Confidence.unknown();
    }
    public static WorkflowState unknown() { return new WorkflowState("UNKNOWN", Confidence.unknown()); }
}
