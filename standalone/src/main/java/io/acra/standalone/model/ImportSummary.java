package io.acra.standalone.model;

public record ImportSummary(
        String importType,
        int observations,
        int uniqueEndpoints,
        int inventorySize
) {
    public ImportSummary {
        importType = importType == null ? "" : importType;
        if (observations < 0 || uniqueEndpoints < 0 || inventorySize < 0) {
            throw new IllegalArgumentException("import counts must be non-negative");
        }
    }
}
