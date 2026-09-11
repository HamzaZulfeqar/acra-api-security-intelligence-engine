package io.acra.core.active.model;

import io.acra.core.security.TokenFingerprint;

public final class TestSignature {
    private TestSignature() {}

    public static String from(SecurityTest test) {
        if (test == null) throw new IllegalArgumentException("test required");
        String resource = test.targetResource() == null ? "UNKNOWN" :
                test.targetResource().resourceType() + ':' + test.targetResource().resourceId();
        String canonical = String.join("\n",
                test.target().targetId(),
                test.endpoint().routeTemplate(),
                test.method().name(),
                test.sourceContext().principal(),
                test.targetContext().principal(),
                resource,
                test.mutation().deduplicationKey(),
                test.category().name());
        return TokenFingerprint.sha256(canonical);
    }
}
