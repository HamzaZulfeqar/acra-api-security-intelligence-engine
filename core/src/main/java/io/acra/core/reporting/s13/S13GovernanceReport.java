package io.acra.core.reporting.s13;

import io.acra.core.domain.common.Validation;
import java.time.Instant;
import java.util.List;

public record S13GovernanceReport(
        String reportId,
        String reportVersion,
        S13GovernanceReportStatus status,
        Instant generatedAt,
        S13GovernanceReportSummary summary,
        List<S13GovernanceFindingProjection> findings,
        List<S13GovernanceEventProjection> events,
        List<String> limitations) {

    public S13GovernanceReport {
        reportId = Validation.requireNonBlank(reportId, "reportId");
        reportVersion = Validation.requireNonBlank(reportVersion, "reportVersion");
        if (status == null || generatedAt == null || summary == null) {
            throw new IllegalArgumentException("status/generatedAt/summary required");
        }
        findings = List.copyOf(findings == null ? List.of() : findings);
        events = List.copyOf(events == null ? List.of() : events);
        limitations = List.copyOf(limitations == null ? List.of() : limitations);
        if (findings.size() != summary.findingCount()) {
            throw new IllegalArgumentException("report finding count mismatch");
        }
        if (events.size() != summary.lifecycleEventCount()) {
            throw new IllegalArgumentException("report event count mismatch");
        }
    }
}
