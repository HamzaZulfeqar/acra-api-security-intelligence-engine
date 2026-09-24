package io.acra.core.reproduction;

public record ReproductionExportCapability(
        ReproductionExportTarget target,
        ReproductionExportCapabilityState state,
        String schemaId) {

    public ReproductionExportCapability {
        if (target == null || state == null) throw new IllegalArgumentException("target/state required");
        schemaId = schemaId == null ? "" : schemaId.strip();
        if (schemaId.isBlank()) throw new IllegalArgumentException("schemaId required");
    }
}
