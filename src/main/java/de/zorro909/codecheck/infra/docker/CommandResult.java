package de.zorro909.codecheck.infra.docker;

public record CommandResult(int exitCode, String stdout, String stderr) {

    public boolean success() {
        return exitCode == 0;
    }
}
