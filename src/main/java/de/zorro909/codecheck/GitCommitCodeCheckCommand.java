package de.zorro909.codecheck;

import de.zorro909.codecheck.command.CodeCheckCommandService;
import io.micronaut.configuration.picocli.MicronautFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

/**
 * Command-line entry point for Git Commit Code Check.
 */
@Singleton
@Command(name = "git-commit-code-check",
        description = "...",
        mixinStandardHelpOptions = true,
        subcommands = {
                GitCommitCodeCheckCommand.CheckCommand.class,
                GitCommitCodeCheckCommand.PreCommitCommand.class,
                GitCommitCodeCheckCommand.StatusCommand.class,
                GitCommitCodeCheckCommand.FixCommand.class
        })
public class GitCommitCodeCheckCommand implements Callable<Integer> {

    @Inject
    CodeCheckCommandService commandService;

    @Getter
    @Option(names = {"-v", "--verbose"}, description = "...", defaultValue = "false")
    boolean verbose;

    @Getter
    @Option(names = "--no-exit-code", defaultValue = "false")
    boolean noExitCode;

    @Getter
    @Option(names = "--check-all", defaultValue = "false")
    boolean checkAll;

    @Getter
    @Option(names = "--check-branch", defaultValue = "false")
    boolean checkBranch;

    @Getter
    @Option(names = "--git", defaultValue = "true")
    boolean git;

    @Getter
    @Option(names = "--experimental", defaultValue = "false")
    boolean experimental;

    @Getter
    @Option(names = "--daemon", defaultValue = "false")
    boolean daemon;

    @Getter
    @Option(names = "--watch", defaultValue = "false")
    boolean watch;

    public static void main(String[] args) {
        try (ApplicationContext context = ApplicationContext.builder(
                        GitCommitCodeCheckCommand.class, Environment.CLI)
                .singletons((Object) args)
                .start()) {
            CommandLine commandLine = new CommandLine(GitCommitCodeCheckCommand.class,
                                                      new MicronautFactory(
                                                              context)).setCaseInsensitiveEnumValuesAllowed(
                    true).setUsageHelpAutoWidth(true);
            int exitCode = commandLine.execute(args);
            System.exit(exitCode);
        }
    }

    @Override
    public Integer call() {
        return commandService.startAssistantDaemon().exitCode();
    }

    @Command(name = "check", description = "Run a one-shot code check.")
    static class CheckCommand implements Callable<Integer> {

        @ParentCommand
        GitCommitCodeCheckCommand parent;

        @Option(names = "--batch", defaultValue = "false",
                description = "Run without interactive fix actions.")
        boolean batch;

        @Override
        public Integer call() {
            if (batch) {
                return parent.commandService.runBatchCheck().exitCode();
            }
            return parent.commandService.runInteractiveCheck(parent.noExitCode).exitCode();
        }
    }

    @Command(name = "pre-commit", description = "Run deterministic pre-commit validation.")
    static class PreCommitCommand implements Callable<Integer> {

        @ParentCommand
        GitCommitCodeCheckCommand parent;

        @Override
        public Integer call() {
            return parent.commandService.runPreCommit().exitCode();
        }
    }

    @Command(name = "status", description = "Print assistant daemon status.")
    static class StatusCommand implements Callable<Integer> {

        @ParentCommand
        GitCommitCodeCheckCommand parent;

        @Override
        public Integer call() {
            return parent.commandService.printStatus().exitCode();
        }
    }

    @Command(name = "fix", description = "Apply an explicitly selected daemon fix.")
    static class FixCommand implements Callable<Integer> {

        @ParentCommand
        GitCommitCodeCheckCommand parent;

        @Parameters(index = "0", description = "Diagnostic identifier.")
        String diagnosticId;

        @Override
        public Integer call() {
            return parent.commandService.applyFix(diagnosticId).exitCode();
        }
    }
}
