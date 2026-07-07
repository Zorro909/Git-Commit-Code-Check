package de.zorro909.codecheck.runner;

import java.util.List;

public record TestRunRequest(List<String> modules, List<String> testClasses, boolean retryFailingTests, boolean fullRun,
        boolean generateCoverageReport, List<String> additionalMavenArgs) {

    public TestRunRequest {
        modules = List.copyOf(modules);
        testClasses = List.copyOf(testClasses);
        additionalMavenArgs = List.copyOf(additionalMavenArgs);
    }

    public static TestRunRequest full() {
        return new TestRunRequest(List.of(), List.of(), false, true, true, List.of());
    }

    public static TestRunRequest targeted(List<String> modules, List<String> testClasses) {
        return new TestRunRequest(modules, testClasses, false, false, true, List.of());
    }
}
