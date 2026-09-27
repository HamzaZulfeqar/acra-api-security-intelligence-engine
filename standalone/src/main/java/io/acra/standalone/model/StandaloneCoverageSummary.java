package io.acra.standalone.model;

import java.util.List;

public record StandaloneCoverageSummary(
        int total,
        int tested,
        int untested,
        int partial,
        int inconclusive,
        int notApplicable
) {
    public StandaloneCoverageSummary {
        if (total < 0 || tested < 0 || untested < 0 || partial < 0 || inconclusive < 0 || notApplicable < 0) {
            throw new IllegalArgumentException("coverage counts must be non-negative");
        }
    }

    public static StandaloneCoverageSummary from(List<StandaloneCoverageRecord> records) {
        List<StandaloneCoverageRecord> safe = records == null ? List.of() : records;
        return new StandaloneCoverageSummary(
                safe.size(),
                (int) safe.stream().filter(r -> r.disposition() == StandaloneCoverageDisposition.TESTED).count(),
                (int) safe.stream().filter(r -> r.disposition() == StandaloneCoverageDisposition.UNTESTED).count(),
                (int) safe.stream().filter(r -> r.disposition() == StandaloneCoverageDisposition.PARTIAL).count(),
                (int) safe.stream().filter(r -> r.disposition() == StandaloneCoverageDisposition.INCONCLUSIVE).count(),
                (int) safe.stream().filter(r -> r.disposition() == StandaloneCoverageDisposition.NOT_APPLICABLE).count()
        );
    }
}
