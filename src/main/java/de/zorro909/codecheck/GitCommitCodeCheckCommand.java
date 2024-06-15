package de.zorro909.codecheck;

import de.zorro909.codecheck.checks.ValidationError;
import de.zorro909.codecheck.selector.FileSelector;
import io.micronaut.configuration.picocli.MicronautFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;


/**
 * The GitCommitCodeCheckCommand class represents a command-line command that performs code checks on files
 * in a Git repository.
 */
@Singleton
@Command(name = "git-commit-code-check", description = "...", mixinStandardHelpOptions = true)
public class GitCommitCodeCheckCommand implements Runnable {

    @Inject
    RepositoryPathProvider repositoryPathProvider;

    @Inject
    Optional<DaemonServer> daemonServer;

    @Inject
    ValidationCheckPipeline validationCheckPipeline;

    @Inject
    FileSelector fileSelector;

    @Inject
    Optional<FileWatcher> fileWatcher;

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
    @Option(names = "--batch", defaultValue = "false")
    boolean batch;

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
                GitCommitCodeCheckCommand.class, Environment.CLI).start()) {
            CommandLine commandLine = new CommandLine(GitCommitCodeCheckCommand.class,
                                                      new MicronautFactory(
                                                              context)).setCaseInsensitiveEnumValuesAllowed(
                    true).setUsageHelpAutoWidth(true);
            context.registerSingleton(args);
            int exitCode = commandLine.execute(args);
            System.exit(exitCode);
        }
    }

    @SneakyThrows
    public void run() {
        // business logic here
        if (verbose) {
            System.out.println("Hi!");
        }
        if (daemon) {
            if(watch){
                fileWatcher.get().watch();
            }
            daemonServer.get().run();
            return;
        }

        Stream<Path> changedFiles = fileSelector.selectFiles();

        Map<Path, List<ValidationError>> errorsMap = validationCheckPipeline.checkForErrors(
                changedFiles);

        // Can be dependent on IO Resources
        changedFiles.close();

        System.out.println("Overview of code checks:");
        System.out.println("------------------------");
        if (errorsMap.isEmpty()) {
            System.out.println("No validation errors found. Great job!");
        } else {
            System.out.println("Validation errors found in " + errorsMap.size() + " files");
            System.out.println("Here are the details:");

            boolean success = validationCheckPipeline.executeFixActions(errorsMap);

            if (!success && !noExitCode) {
                System.exit(1);
            }

            validationCheckPipeline.executePostActions(errorsMap.keySet());

        }
    }


}
