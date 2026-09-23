package io.acra.core.domain.authorization;

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class EffectiveAuthorizationMatrix {
    private final NavigableMap<String, EffectiveAuthorizationMatrixEntry> entries = new TreeMap<>();

    public synchronized void put(EffectiveAuthorizationMatrixEntry entry) {
        if (entry == null) throw new IllegalArgumentException("entry required");
        entries.put(key(entry), entry);
    }

    public synchronized List<EffectiveAuthorizationMatrixEntry> entries() {
        return List.copyOf(entries.values());
    }

    public synchronized int size() {
        return entries.size();
    }

    private String key(EffectiveAuthorizationMatrixEntry entry) {
        return String.join("|", entry.principalId(), entry.resourceTenantId(), entry.resourceId(),
                entry.action(), entry.endpoint(), entry.policyFingerprint());
    }
}
