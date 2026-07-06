package de.zorro909.codecheck.runner;

import java.util.List;

public record TestRunResult(boolean success,
                            int exitCode,
                            String stdout,
                            String stderr,
                            String containerId,
                            List<String> mavenCommand) {

    public TestRunResult {
        mavenCommand = List.copyOf(mavenCommand);
    }
}
