package de.zorro909.codecheck.cli;

public record CommandOutcome(int exitCode) {

    public static CommandOutcome success() {
        return new CommandOutcome(0);
    }

    public static CommandOutcome failure() {
        return new CommandOutcome(1);
    }
}
