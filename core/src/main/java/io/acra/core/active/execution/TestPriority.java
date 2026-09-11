package io.acra.core.active.execution;

public record TestPriority(
        int risk,
        int contextCompleteness,
        int resourceSensitivity,
        int endpointImportance,
        int testClass,
        int dependencyStatus,
        int userPriority) {
    public TestPriority {
        check(risk);
        check(contextCompleteness);
        check(resourceSensitivity);
        check(endpointImportance);
        check(testClass);
        check(dependencyStatus);
        check(userPriority);
    }

    public long score() {
        return 5L * risk + 4L * contextCompleteness + 4L * resourceSensitivity
                + 3L * endpointImportance + 3L * testClass + 5L * dependencyStatus + 6L * userPriority;
    }

    private static void check(int value) {
        if (value < 0 || value > 100) throw new IllegalArgumentException("priority input must be 0-100");
    }
}
