package io.acra.core.plugin;

import io.acra.core.domain.testing.TestCase;
import java.util.List;

public interface MutationProvider { String id(); List<TestCase> propose(AnalysisContext context); }
