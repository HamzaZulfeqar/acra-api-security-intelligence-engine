package io.acra.core.product.authorization;

import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.authorization.S6AuthorizationAnalysisResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

public final class S6AuthorizationWorkspace {
    private AuthorizationPolicySnapshot policy;
    private final TreeMap<String, S6AuthorizationAnalysisResult> analyses = new TreeMap<>();

    public synchronized void loadPolicy(AuthorizationPolicySnapshot snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("policy snapshot required");
        policy = snapshot;
    }

    public synchronized void record(S6AuthorizationAnalysisResult result) {
        if (result == null || result.effectiveResolution() == null) {
            throw new IllegalArgumentException("S6 analysis with effective resolution required");
        }
        analyses.put(result.effectiveResolution().resolutionId(), result);
    }

    public synchronized void clearAnalyses() {
        analyses.clear();
    }

    public synchronized S6AuthorizationProductSnapshot snapshot() {
        List<S6AuthorizationAnalysisResult> ordered = new ArrayList<>(analyses.values());
        ordered.sort(Comparator.comparing(value -> value.effectiveResolution().resolutionId()));
        return new S6AuthorizationProductSnapshot(policy, ordered);
    }
}
