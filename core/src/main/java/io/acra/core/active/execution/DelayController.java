package io.acra.core.active.execution;

import java.time.Duration;

@FunctionalInterface
public interface DelayController {
    void delay(Duration duration) throws InterruptedException;

    static DelayController system() {
        return duration -> Thread.sleep(duration.toMillis());
    }
}
