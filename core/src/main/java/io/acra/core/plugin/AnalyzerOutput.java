package io.acra.core.plugin;

import java.util.List;

public record AnalyzerOutput(String analyzerId, List<String> observations, List<String> evidenceIds) {
    public AnalyzerOutput { observations=List.copyOf(observations==null?List.of():observations); evidenceIds=List.copyOf(evidenceIds==null?List.of():evidenceIds); }
}
