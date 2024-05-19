package de.zorro909.codecheck;

import io.micronaut.configuration.picocli.PicocliRunner;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


//TODO: Check Klassen mit *Test Namen, dass sie von einer Klasse erben und eine @Tests Annotation besitzen.
@Command(name = "git-commit-code-check", description = "...", mixinStandardHelpOptions = true)
public class GitCommitCodeCheckCommand implements Runnable {

    @Inject
    public GitDiffLoader gitDiffLoader;

    @Inject
    public List<CodeCheck> codeChecker;

    @Option(names = { "-v", "--verbose" }, description = "...")
    boolean verbose;

    @Option(names = { "--fix" }, description = "Prompt to fix problems")
    boolean fix;

    @Option(names = "--no-exit-code")
    boolean noExitCode;

    /**
     * Starts the main Program
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        PicocliRunner.run(GitCommitCodeCheckCommand.class, args);
    }

    /**
     * Executes the business logic for the GitCommitCodeCheckCommand.
     */
    @SneakyThrows
    public void run() {
        // business logic here
        if (verbose) {
            System.out.println("Hi!");
        }

        Path repoPath = Paths.get("");  // define your repository path
        System.out.println(repoPath.toAbsolutePath());
        Set<Path> changedFiles = gitDiffLoader.getChangedFiles(repoPath);

        Map<Path, List<ValidationError>> errorsMap = checkForErrors(changedFiles);

        System.out.println("Overview of code checks:");
        System.out.println("------------------------");
        if (errorsMap.isEmpty()) {
            System.out.println("No validation errors found. Great job!");
        } else {
            System.out.println("Validation errors found in " + errorsMap.size() + " files");
            System.out.println("Here are the details:");
            if (!fix) {
                errorsMap.values().forEach(errorList -> errorList.forEach(System.out::println));
                if(!noExitCode) System.exit(1);
            } else {
                for (Map.Entry<Path, List<ValidationError>> entry : new HashSet<>(
                    errorsMap.entrySet())) {
                    Path filePath = entry.getKey();
                    Optional<ValidationError> errorOptional = Optional.ofNullable(
                        entry.getValue().get(0));
                    while (errorOptional.isPresent()) {
                        ValidationError error = errorOptional.get();
                        System.out.println(error);
                        int lineNumber = error.lineNumber();

                        // Construct command for IDEA
                        String[] ideaCommand = { "idea64.exe", "--line", String.valueOf(
                            lineNumber), "--wait", filePath.toString() };

                        //  Execute IDEA with file path
                        try {
                            new ProcessBuilder(ideaCommand).start().waitFor();
                        } catch (Exception e) {
                            System.out.println(
                                "Error while trying to start IntelliJ IDEA: " + e.getMessage());
                            throw new RuntimeException(e);
                        }

                        errorOptional = checkFile(filePath).findFirst();
                    }
                }

//                try (Git git = Git.open(new File(""))) {
//                    AddCommand add = git.add();
//                    for (Path changedFile : changedFiles) {
//                        add.addFilepattern(changedFile.toString());
//                    }
//                    add.call();
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
            }
        }

    }

    private Map<Path, List<ValidationError>> checkForErrors(Set<Path> changedFiles) {
        return changedFiles.stream()
            .flatMap(this::checkFile)
            .collect(Collectors.groupingBy(ValidationError::filePath));
    }

    private Stream<ValidationError> checkFile(Path file) {
        codeChecker.forEach(cc -> cc.resetCache(file));
        //System.out.println("Checking file: " + file);
        return codeChecker.stream()
            .filter(checker -> checker.isResponsible(file))
            //.peek(checker -> System.out.println("Using checker '" + checker.getClass().getName() + "'"))
            .flatMap(checker -> checker.check(file).stream());
    }
}
