package io.acra.core.domain.workflow;

import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public final class WorkflowTransitionCoverageMatrix {
    private final NavigableMap<String, WorkflowTransitionCoverageEntry> entries = new TreeMap<>();

    public synchronized void put(WorkflowTransitionCoverageEntry entry) {
        if (entry == null) throw new IllegalArgumentException("entry required");
        entries.put(entry.coverageId(), entry);
    }

    public synchronized Optional<WorkflowTransitionCoverageEntry> find(String coverageId) {
        if (coverageId == null || coverageId.isBlank()) return Optional.empty();
        return Optional.ofNullable(entries.get(coverageId));
    }

    public synchronized List<WorkflowTransitionCoverageEntry> entries() {
        return List.copyOf(entries.values());
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized WorkflowCoverageSummary summary() {
        int resolved = 0;
        int planned = 0;
        int attempted = 0;
        int observed = 0;
        for (WorkflowTransitionCoverageEntry entry : entries.values()) {
            if (entry.resolved()) resolved++;
            if (entry.stage().ordinal() >= WorkflowCoverageStage.PLANNED.ordinal()) planned++;
            if (entry.stage().ordinal() >= WorkflowCoverageStage.ATTEMPTED.ordinal()) attempted++;
            if (entry.stage() == WorkflowCoverageStage.OBSERVED) observed++;
        }
        int total = entries.size();
        return new WorkflowCoverageSummary(total, resolved, total - resolved, planned, attempted, observed);
    }

    public synchronized List<String> unresolvedCoverageIds() {
        return entries.values().stream()
                .filter(entry -> !entry.resolved())
                .map(WorkflowTransitionCoverageEntry::coverageId)
                .toList();
    }

    public synchronized List<String> missingPlanningCoverageIds() {
        return entries.values().stream()
                .filter(WorkflowTransitionCoverageEntry::resolved)
                .filter(entry -> entry.stage() == WorkflowCoverageStage.POLICY_ONLY)
                .map(WorkflowTransitionCoverageEntry::coverageId)
                .toList();
    }

    public synchronized List<String> missingObservationCoverageIds() {
        return entries.values().stream()
                .filter(WorkflowTransitionCoverageEntry::resolved)
                .filter(entry -> entry.stage() != WorkflowCoverageStage.OBSERVED)
                .map(WorkflowTransitionCoverageEntry::coverageId)
                .toList();
    }
}
