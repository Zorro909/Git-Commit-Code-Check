package de.zorro909.codecheck;

import de.zorro909.codecheck.actions.FixAction;
import de.zorro909.codecheck.actions.PostAction;
import de.zorro909.codecheck.checks.CodeCheck;
import de.zorro909.codecheck.checks.ValidationError;
import de.zorro909.codecheck.editor.EditorExecutor;
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
import java.util.*;
import java.util.stream.Collectors;
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
    FileSelector fileSelector;

    @Inject
    EditorExecutor editorExecutor;

    @Inject
    List<CodeCheck> codeChecker;

    @Inject
    List<FixAction> fixActions;

    @Inject
    List<PostAction> postActions;

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

        Stream<Path> changedFiles = fileSelector.selectFiles();

        Map<Path, List<ValidationError>> errorsMap = checkForErrors(changedFiles);

        // Can be dependent on IO Resources
        changedFiles.close();

        System.out.println("Overview of code checks:");
        System.out.println("------------------------");
        if (errorsMap.isEmpty()) {
            System.out.println("No validation errors found. Great job!");
        } else {
            System.out.println("Validation errors found in " + errorsMap.size() + " files");
            System.out.println("Here are the details:");

            boolean success = executeFixActions(errorsMap);

            if (!success && !noExitCode) {
                System.exit(1);
            }

            executePostActions(errorsMap.keySet());

        }
    }

    private void executePostActions(Set<Path> paths) {
        if (postActions == null) {
            return;
        }

        postActions.forEach(action -> action.perform(paths));
    }

    private boolean executeFixActions(Map<Path, List<ValidationError>> errorsMap) {
        boolean fixed = true;

        for (Map.Entry<Path, List<ValidationError>> entry : new HashSet<>(errorsMap.entrySet())) {
            Path filePath = entry.getKey();
            Optional<ValidationError> error = Optional.ofNullable(entry.getValue().get(0));

            while (error.isPresent()) {
                if (!executeFixAction(error.get())) {
                    checkFile(filePath).forEach(System.out::println);
                    fixed = false;
                    break;
                }

                error = checkFile(filePath).findFirst();
            }
        }
        return fixed;
    }

    private boolean executeFixAction(ValidationError validationError) {
        if (fixActions == null) {
            return false;
        }

        return fixActions.stream()
                         .filter(action -> action.canFixError(validationError))
                         .anyMatch(action -> action.fixError(validationError));
    }

    private Map<Path, List<ValidationError>> checkForErrors(Stream<Path> changedFiles) {
        return changedFiles.flatMap(this::checkFile)
                           .collect(Collectors.groupingBy(ValidationError::filePath));
    }

    private Stream<ValidationError> checkFile(Path file) {
        codeChecker.forEach(cc -> cc.resetCache(file));
        return codeChecker.stream()
                          .filter(checker -> checker.isResponsible(file))
                          .flatMap(checker -> checker.check(file).stream());
    }

}
