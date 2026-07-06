package de.zorro909.codecheck.runner;

public record CommandResult(int exitCode,
                            String stdout,
                            String stderr) {

    public boolean success() {
        return exitCode == 0;
    }
}
