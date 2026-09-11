package io.acra.core.plugin;

import io.acra.core.domain.workflow.WorkflowState;

public interface WorkflowDetector { String id(); WorkflowState infer(AnalysisContext context); }
