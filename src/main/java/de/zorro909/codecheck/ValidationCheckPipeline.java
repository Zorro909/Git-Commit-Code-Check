package de.zorro909.codecheck;

import de.zorro909.codecheck.actions.FixAction;
import de.zorro909.codecheck.actions.PostAction;
import de.zorro909.codecheck.checks.CodeCheck;
import de.zorro909.codecheck.checks.ValidationError;
import de.zorro909.codecheck.editor.EditorExecutor;
import de.zorro909.codecheck.selector.FileSelector;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    /**
     * Executes post actions for a given set of validation errors.
     *
     * @param validationErrors A map containing the validation errors, where the key is a file path and the value is the corresponding error.
     */
    public void executePostActions(Map<Path, List<ValidationError>> validationErrors) {
        if (postActions == null) {
            return;
        }

        postActions.forEach(action -> action.perform(validationErrors));
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
        return changedFiles.flatMap(this::checkFile)
                           .collect(Collectors.groupingBy(ValidationError::filePath));
    }

    public Stream<ValidationError> checkFile(Path file) {
        codeChecker.forEach(cc -> cc.resetCache(file));
        return codeChecker.stream()
                          .filter(checker -> checker.isResponsible(file))
                          .flatMap(checker -> checker.check(file).stream()); //Test23
    }

}
