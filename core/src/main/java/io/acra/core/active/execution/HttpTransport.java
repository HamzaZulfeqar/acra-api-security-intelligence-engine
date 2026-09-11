package io.acra.core.active.execution;

import io.acra.core.active.evidence.RequestSnapshot;
import java.time.Duration;

@FunctionalInterface
public interface HttpTransport {
    TransportResult send(RequestSnapshot request, Duration timeout, CancellationToken cancellation) throws Exception;
}
