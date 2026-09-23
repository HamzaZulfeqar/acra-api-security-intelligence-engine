package io.acra.core.plugin;

import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import java.time.Instant;

public interface AuthorizationPolicyImporter {
    String id();
    AuthorizationPolicySnapshot importPolicy(String content, Instant capturedAt);
}
