package io.acra.core.plugin;

import io.acra.core.domain.evidence.Evidence;
import java.util.List;

public interface EvidenceProvider { String id(); List<Evidence> collect(AnalysisContext context); }
