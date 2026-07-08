package de.zorro909.codecheck.command;

import de.zorro909.codecheck.legacy.ValidationCheckPipeline;
import de.zorro909.codecheck.core.changeset.ChangeSet;
import de.zorro909.codecheck.core.changeset.ChangeSetService;
import de.zorro909.codecheck.infra.git.GitCommandException;
import de.zorro909.codecheck.core.diagnostic.ValidationError;
import de.zorro909.codecheck.core.config.CodeCheckConfigLoader;
import de.zorro909.codecheck.core.config.ConfigException;
import de.zorro909.codecheck.core.config.ConfigOverrides;
import de.zorro909.codecheck.core.reporting.ModeSeverityPolicy;
import de.zorro909.codecheck.core.reporting.TerminalDiagnosticRenderer;
import de.zorro909.codecheck.legacy.selector.FileSelector;
import de.zorro909.codecheck.core.diagnostic.Diagnostic;
import de.zorro909.codecheck.core.validation.ValidationEngine;
import de.zorro909.codecheck.core.validation.ValidationMode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Singleton
public class CodeCheckCommandService {

    private final AssistantDaemonController assistantDaemonController;

    private final ValidationCheckPipeline validationCheckPipeline;

    private final ValidationEngine validationEngine;

    private final ChangeSetService changeSetService;

    private final CodeCheckConfigLoader configLoader;

    private final TerminalDiagnosticRenderer diagnosticRenderer;

    private final PrintStream out;

    private final PrintStream err;

    @Inject
    public CodeCheckCommandService(AssistantDaemonController assistantDaemonController,
            ValidationCheckPipeline validationCheckPipeline, ValidationEngine validationEngine,
            ChangeSetService changeSetService, CodeCheckConfigLoader configLoader) {
        this(assistantDaemonController, validationCheckPipeline, validationEngine, changeSetService, configLoader,
                new TerminalDiagnosticRenderer(new ModeSeverityPolicy()), System.out, System.err);
    }

    public CodeCheckCommandService(AssistantDaemonController assistantDaemonController,
            ValidationCheckPipeline validationCheckPipeline, FileSelector fileSelector) {
        this(assistantDaemonController, validationCheckPipeline, null, fromFileSelector(fileSelector),
                CodeCheckConfigLoader.defaultsOnly(), new TerminalDiagnosticRenderer(new ModeSeverityPolicy()),
                System.out, System.err);
    }

    CodeCheckCommandService(AssistantDaemonController assistantDaemonController,
            ValidationCheckPipeline validationCheckPipeline, FileSelector fileSelector, PrintStream out,
            PrintStream err) {
        this(assistantDaemonController, validationCheckPipeline, null, fromFileSelector(fileSelector),
                CodeCheckConfigLoader.defaultsOnly(), new TerminalDiagnosticRenderer(new ModeSeverityPolicy()), out,
                err);
    }

    CodeCheckCommandService(AssistantDaemonController assistantDaemonController,
            ValidationCheckPipeline validationCheckPipeline, FileSelector fileSelector,
            CodeCheckConfigLoader configLoader, PrintStream out, PrintStream err) {
        this(assistantDaemonController, validationCheckPipeline, null, fromFileSelector(fileSelector), configLoader,
                new TerminalDiagnosticRenderer(new ModeSeverityPolicy()), out, err);
    }

    CodeCheckCommandService(AssistantDaemonController assistantDaemonController,
            ValidationCheckPipeline validationCheckPipeline, ChangeSetService changeSetService,
            CodeCheckConfigLoader configLoader, PrintStream out, PrintStream err) {
        this(assistantDaemonController, validationCheckPipeline, null, changeSetService, configLoader,
                new TerminalDiagnosticRenderer(new ModeSeverityPolicy()), out, err);
    }

    CodeCheckCommandService(AssistantDaemonController assistantDaemonController,
            ValidationCheckPipeline validationCheckPipeline, ValidationEngine validationEngine,
            ChangeSetService changeSetService, CodeCheckConfigLoader configLoader, PrintStream out, PrintStream err) {
        this(assistantDaemonController, validationCheckPipeline, validationEngine, changeSetService, configLoader,
                new TerminalDiagnosticRenderer(new ModeSeverityPolicy()), out, err);
    }

    CodeCheckCommandService(AssistantDaemonController assistantDaemonController,
            ValidationCheckPipeline validationCheckPipeline, ValidationEngine validationEngine,
            ChangeSetService changeSetService, CodeCheckConfigLoader configLoader,
            TerminalDiagnosticRenderer diagnosticRenderer, PrintStream out, PrintStream err) {
        this.assistantDaemonController = assistantDaemonController;
        this.validationCheckPipeline = validationCheckPipeline;
        this.validationEngine = validationEngine;
        this.changeSetService = changeSetService;
        this.configLoader = configLoader;
        this.diagnosticRenderer = diagnosticRenderer;
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
        }
        catch (Exception e) {
            err.println("Failed to start or attach to assistant daemon: " + e.getMessage());
            return CommandOutcome.failure();
        }
    }

    public CommandOutcome runInteractiveCheck(boolean noExitCode) {
        if (!loadConfig()) {
            return CommandOutcome.failure();
        }
        try {
            Map<Path, List<ValidationError>> errorsMap = collectErrors(
                    changeSetService.currentInteractiveCheckChangeSet(), ValidationMode.INTERACTIVE);
            diagnosticRenderer.render(ValidationMode.INTERACTIVE, errorsMap, out);

            if (errorsMap.isEmpty()) {
                return CommandOutcome.success();
            }

            boolean fixed = validationCheckPipeline.executeFixActions(errorsMap);
            if (fixed) {
                validationCheckPipeline.executePostActions(errorsMap.keySet());
                return CommandOutcome.success();
            }

            return noExitCode ? CommandOutcome.success() : CommandOutcome.failure();
        }
        catch (IOException e) {
            err.println("Failed to run interactive check: " + e.getMessage());
            return CommandOutcome.failure();
        }
        catch (GitCommandException e) {
            err.println("Failed to select files for interactive check: " + e.getMessage());
            return CommandOutcome.failure();
        }
    }

    public CommandOutcome runBatchCheck() {
        return runNonInteractive("batch check", changeSetService::currentInteractiveCheckChangeSet,
                ValidationMode.BATCH);
    }

    public CommandOutcome runPreCommit() {
        return runNonInteractive("pre-commit check", changeSetService::preCommitChangeSet, ValidationMode.PRE_COMMIT);
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
        }
        catch (Exception e) {
            err.println("Failed to apply fix: " + e.getMessage());
            return CommandOutcome.failure();
        }
    }

    private CommandOutcome runNonInteractive(String label, Supplier<ChangeSet> changeSetSupplier, ValidationMode mode) {
        if (!loadConfig()) {
            return CommandOutcome.failure();
        }
        try {
            Map<Path, List<ValidationError>> errorsMap = collectErrors(changeSetSupplier.get(), mode);
            diagnosticRenderer.render(mode, errorsMap, out);
            return diagnosticRenderer.blocks(mode, errorsMap) ? CommandOutcome.failure() : CommandOutcome.success();
        }
        catch (IOException e) {
            err.println("Failed to run " + label + ": " + e.getMessage());
            return CommandOutcome.failure();
        }
        catch (GitCommandException e) {
            err.println("Failed to select files for " + label + ": " + e.getMessage());
            return CommandOutcome.failure();
        }
    }

    private Map<Path, List<ValidationError>> collectErrors(ChangeSet changeSet, ValidationMode mode)
            throws IOException {
        if (validationEngine != null) {
            return validationEngine.validate(changeSet, mode)
                .diagnostics()
                .stream()
                .map(Diagnostic::toValidationError)
                .collect(java.util.stream.Collectors.groupingBy(ValidationError::filePath));
        }
        try (Stream<Path> changedFiles = changeSet.paths()) {
            return validationCheckPipeline.checkForErrors(changedFiles);
        }
    }

    private boolean loadConfig() {
        try {
            configLoader.load(ConfigOverrides.none());
            return true;
        }
        catch (ConfigException e) {
            err.println(e.getMessage());
            return false;
        }
    }

    private static ChangeSetService fromFileSelector(FileSelector fileSelector) {
        return new ChangeSetService() {
            @Override
            public ChangeSet currentAssistantChangeSet() {
                return fromSelector();
            }

            @Override
            public ChangeSet currentInteractiveCheckChangeSet() {
                return fromSelector();
            }

            @Override
            public ChangeSet preCommitChangeSet() {
                return fromSelector();
            }

            @Override
            public ChangeSet explicitFiles(java.util.Collection<Path> files) {
                return new ChangeSet(files.stream()
                    .map(path -> new de.zorro909.codecheck.core.changeset.ChangeSetEntry(path,
                            de.zorro909.codecheck.core.changeset.GitFileStatus.UNKNOWN, false, false, false, false,
                            "explicit file"))
                    .toList());
            }

            private ChangeSet fromSelector() {
                try (Stream<Path> selected = fileSelector.selectFiles()) {
                    return explicitFiles(selected.toList());
                }
                catch (IOException e) {
                    throw new IllegalStateException("Failed to select files", e);
                }
            }
        };
    }

}
