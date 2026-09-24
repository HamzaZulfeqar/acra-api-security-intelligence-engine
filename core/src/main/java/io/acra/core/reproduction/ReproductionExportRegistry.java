package io.acra.core.reproduction;

import java.util.List;

public final class ReproductionExportRegistry {

    public List<ReproductionExportCapability> capabilities() {
        return List.of(
                new ReproductionExportCapability(
                        ReproductionExportTarget.JSON,
                        ReproductionExportCapabilityState.CONTRACT_DEFINED,
                        "acra-reproduction-json-v1"),
                new ReproductionExportCapability(
                        ReproductionExportTarget.SARIF,
                        ReproductionExportCapabilityState.CONTRACT_DEFINED,
                        "sarif-2.1.0"),
                new ReproductionExportCapability(
                        ReproductionExportTarget.BURP_ISSUE,
                        ReproductionExportCapabilityState.CONTRACT_DEFINED,
                        "acra-burp-issue-v1"));
    }

    public ReproductionExportCapability capability(ReproductionExportTarget target) {
        if (target == null) throw new IllegalArgumentException("target required");
        return capabilities().stream()
                .filter(value -> value.target() == target)
                .findFirst()
                .orElseThrow();
    }
}
