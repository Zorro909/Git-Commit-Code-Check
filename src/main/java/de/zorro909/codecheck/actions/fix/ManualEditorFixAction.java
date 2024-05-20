package de.zorro909.codecheck.actions.fix;

import de.zorro909.codecheck.RequiresCliOption;
import de.zorro909.codecheck.actions.FixAction;
import de.zorro909.codecheck.checks.ValidationError;
import de.zorro909.codecheck.editor.EditorExecutor;
import io.micronaut.core.annotation.Order;
import jakarta.inject.Singleton;

@Singleton
@Order(99)
@RequiresCliOption("!--batch")
public class ManualEditorFixAction implements FixAction {

    private final EditorExecutor editorExecutor;

    public ManualEditorFixAction(EditorExecutor editorExecutor) {
        this.editorExecutor = editorExecutor;
    }

    @Override
    public boolean canFixError(ValidationError validationError) {
        return true;
    }

    @Override
    public boolean fixError(ValidationError validationError) {
        return editorExecutor.openAndWait(validationError.filePath(), validationError.lineNumber());
    }
}
