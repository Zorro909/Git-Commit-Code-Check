package de.zorro909.codecheck.legacy;

import de.zorro909.codecheck.legacy.actions.FixAction;
import de.zorro909.codecheck.legacy.actions.PostAction;
import de.zorro909.codecheck.changeset.ChangeSet;
import de.zorro909.codecheck.changeset.ChangeSetEntry;
import de.zorro909.codecheck.changeset.GitFileStatus;
import de.zorro909.codecheck.legacy.checks.CodeCheck;
import de.zorro909.codecheck.core.diagnostic.ValidationError;
import de.zorro909.codecheck.legacy.editor.EditorExecutor;
import de.zorro909.codecheck.legacy.selector.FileSelector;
import de.zorro909.codecheck.validation.Diagnostic;
import de.zorro909.codecheck.validation.DefaultValidationEngine;
import de.zorro909.codecheck.validation.RuleRegistry;
import de.zorro909.codecheck.validation.ValidationEngine;
import de.zorro909.codecheck.validation.ValidationMode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Pipeline for executing ValidationChecks and Actions
 */
@Singleton
public class ValidationCheckPipeline {

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

    @Inject
    ValidationEngine validationEngine;

    /**
     * Execute all PostActions on a Set of Paths
     * @param paths set of paths
     */
    public void executePostActions(Set<Path> paths) {
        if (postActions == null) {
            return;
        }

        postActions.forEach(action -> action.perform(paths));
    }

    /**
     * Execute all PostActions on a single Path
     * @param path Path to execute on
     */
    public void executePostActions(Path path) {
        if (postActions == null) {
            return;
        }

        postActions.forEach(action -> action.perform(Set.of(path)));
    }

    public boolean executeFixActions(Map<Path, List<ValidationError>> errorsMap) {
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

    public Map<Path, List<ValidationError>> checkForErrors(Stream<Path> changedFiles) {
        ChangeSet changeSet = new ChangeSet(changedFiles
            .map(path -> new ChangeSetEntry(path, GitFileStatus.UNKNOWN, false, false, false, false,
                    "validation pipeline"))
            .toList());
        return validationEngine().validate(changeSet, ValidationMode.INTERACTIVE)
            .diagnostics()
            .stream()
            .map(Diagnostic::toValidationError)
            .collect(Collectors.groupingBy(ValidationError::filePath));
    }

    public Stream<ValidationError> checkFile(Path file) {
        return validationEngine().validateFile(file, ValidationMode.INTERACTIVE)
            .diagnostics()
            .stream()
            .map(Diagnostic::toValidationError);
    }

    private ValidationEngine validationEngine() {
        if (validationEngine != null) {
            return validationEngine;
        }
        RuleRegistry registry = new de.zorro909.codecheck.validation.DefaultRuleRegistry(
                codeChecker == null ? List.of() : codeChecker, fixActions == null ? List.of() : fixActions);
        return new DefaultValidationEngine(registry);
    }

}
