package de.zorro909.codecheck.command;

import de.zorro909.codecheck.ValidationCheckPipeline;
import de.zorro909.codecheck.checks.ValidationError;
import de.zorro909.codecheck.config.CodeCheckConfigLoader;
import de.zorro909.codecheck.config.ConfigException;
import de.zorro909.codecheck.config.ConfigOverrides;
import de.zorro909.codecheck.selector.FileSelector;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Singleton
public class CodeCheckCommandService {

    private final AssistantDaemonController assistantDaemonController;
    private final ValidationCheckPipeline validationCheckPipeline;
    private final FileSelector fileSelector;
    private final CodeCheckConfigLoader configLoader;
    private final PrintStream out;
    private final PrintStream err;

    @Inject
    public CodeCheckCommandService(AssistantDaemonController assistantDaemonController,
                                   ValidationCheckPipeline validationCheckPipeline,
                                   FileSelector fileSelector,
                                   CodeCheckConfigLoader configLoader) {
        this(assistantDaemonController, validationCheckPipeline, fileSelector, configLoader,
             System.out, System.err);
    }

    public CodeCheckCommandService(AssistantDaemonController assistantDaemonController,
                                   ValidationCheckPipeline validationCheckPipeline,
                                   FileSelector fileSelector) {
        this(assistantDaemonController, validationCheckPipeline, fileSelector,
             CodeCheckConfigLoader.defaultsOnly(), System.out, System.err);
    }

    CodeCheckCommandService(AssistantDaemonController assistantDaemonController,
                            ValidationCheckPipeline validationCheckPipeline,
                            FileSelector fileSelector,
                            PrintStream out,
                            PrintStream err) {
        this(assistantDaemonController, validationCheckPipeline, fileSelector,
             CodeCheckConfigLoader.defaultsOnly(), out, err);
    }

    CodeCheckCommandService(AssistantDaemonController assistantDaemonController,
                            ValidationCheckPipeline validationCheckPipeline,
                            FileSelector fileSelector,
                            CodeCheckConfigLoader configLoader,
                            PrintStream out,
                            PrintStream err) {
        this.assistantDaemonController = assistantDaemonController;
        this.validationCheckPipeline = validationCheckPipeline;
        this.fileSelector = fileSelector;
        this.configLoader = configLoader;
        this.out = out;
        this.err = err;
    }

    public CommandOutcome startAssistantDaemon() {
        if (!loadConfig()) {
            return CommandOutcome.failure();
        }
        try {
            assistantDaemonController.startOrAttach();
            return CommandOutcome.success();
        } catch (Exception e) {
            err.println("Failed to start or attach to assistant daemon: " + e.getMessage());
            return CommandOutcome.failure();
        }
    }

    public CommandOutcome runInteractiveCheck(boolean noExitCode) {
        if (!loadConfig()) {
            return CommandOutcome.failure();
        }
        try {
            Map<Path, List<ValidationError>> errorsMap = collectErrors();
            printOverview(errorsMap, _ -> true);

            if (errorsMap.isEmpty()) {
                return CommandOutcome.success();
            }

            boolean fixed = validationCheckPipeline.executeFixActions(errorsMap);
            if (fixed) {
                validationCheckPipeline.executePostActions(errorsMap.keySet());
                return CommandOutcome.success();
            }

            return noExitCode ? CommandOutcome.success() : CommandOutcome.failure();
        } catch (IOException e) {
            err.println("Failed to run interactive check: " + e.getMessage());
            return CommandOutcome.failure();
        }
    }

    public CommandOutcome runBatchCheck() {
        return runNonInteractive("batch check", _ -> true);
    }

    public CommandOutcome runPreCommit() {
        return runNonInteractive("pre-commit check",
                                 error -> error.severity() != ValidationError.Severity.LOW);
    }

    public CommandOutcome printStatus() {
        if (!loadConfig()) {
            return CommandOutcome.failure();
        }
        assistantDaemonController.printStatus(out);
        return CommandOutcome.success();
    }

    public CommandOutcome applyFix(String diagnosticId) {
        if (!loadConfig()) {
            return CommandOutcome.failure();
        }
        try {
            assistantDaemonController.applyFix(diagnosticId);
            return CommandOutcome.success();
        } catch (Exception e) {
            err.println("Failed to apply fix: " + e.getMessage());
            return CommandOutcome.failure();
        }
    }

    private CommandOutcome runNonInteractive(String label,
                                             Predicate<ValidationError> diagnosticFilter) {
        if (!loadConfig()) {
            return CommandOutcome.failure();
        }
        try {
            Map<Path, List<ValidationError>> errorsMap = collectErrors();
            printOverview(errorsMap, diagnosticFilter);
            return hasHighSeverity(errorsMap) ? CommandOutcome.failure() : CommandOutcome.success();
        } catch (IOException e) {
            err.println("Failed to run " + label + ": " + e.getMessage());
            return CommandOutcome.failure();
        }
    }

    private Map<Path, List<ValidationError>> collectErrors() throws IOException {
        try (Stream<Path> changedFiles = fileSelector.selectFiles()) {
            return validationCheckPipeline.checkForErrors(changedFiles);
        }
    }

    private void printOverview(Map<Path, List<ValidationError>> errorsMap,
                               Predicate<ValidationError> diagnosticFilter) {
        out.println("Overview of code checks:");
        out.println("------------------------");
        if (errorsMap.isEmpty()) {
            out.println("No validation errors found.");
            return;
        }

        long visibleErrors = errorsMap.values()
                                      .stream()
                                      .flatMap(List::stream)
                                      .filter(diagnosticFilter)
                                      .count();

        if (visibleErrors == 0) {
            out.println("No blocking validation errors found.");
            return;
        }

        out.println("Validation diagnostics:");
        errorsMap.values()
                 .stream()
                 .flatMap(List::stream)
                 .filter(diagnosticFilter)
                 .map(ValidationError::toString)
                 .forEach(out::println);
    }

    private boolean hasHighSeverity(Map<Path, List<ValidationError>> errorsMap) {
        return errorsMap.values()
                        .stream()
                        .flatMap(List::stream)
                        .anyMatch(error -> error.severity() == ValidationError.Severity.HIGH);
    }

    private boolean loadConfig() {
        try {
            configLoader.load(ConfigOverrides.none());
            return true;
        } catch (ConfigException e) {
            err.println(e.getMessage());
            return false;
        }
    }
}
