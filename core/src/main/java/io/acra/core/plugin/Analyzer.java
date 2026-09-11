package io.acra.core.plugin;

import java.util.Set;

public interface Analyzer {
    String id();
    String name();
    Set<String> supportedContexts();
    AnalyzerOutput analyze(AnalysisContext context);
    Set<Capability> capabilities();
}
