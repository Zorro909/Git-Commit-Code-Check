package de.zorro909.codecheck.legacy.actions.fix;

import com.github.javaparser.Position;
import de.zorro909.codecheck.RequiresCliOption;
import de.zorro909.codecheck.legacy.actions.FixAction;
import de.zorro909.codecheck.core.diagnostic.ValidationError;
import de.zorro909.codecheck.legacy.editor.EditorExecutor;
import de.zorro909.codecheck.validation.FixInteraction;
import de.zorro909.codecheck.validation.FixerMetadata;
import io.micronaut.core.annotation.Order;
import jakarta.inject.Singleton;

import java.nio.file.Path;

@Singleton
@Order(99)
@RequiresCliOption("!--batch")
public class ManualEditorFixAction implements FixAction {

    private final EditorExecutor editorExecutor;

    public ManualEditorFixAction(EditorExecutor editorExecutor) {
        this.editorExecutor = editorExecutor;
    }

    @Override
    public FixerMetadata fixerMetadata() {
        return new FixerMetadata("Manual IDE editor", FixInteraction.INTERACTIVE);
    }

    @Override
    public boolean canFixError(ValidationError validationError) {
        return true;
    }

    @Override
    public boolean fixError(ValidationError validationError) {
        System.out.println(validationError);

        Path filePath = validationError.filePath();
        Position position = validationError.position();

        return editorExecutor.openAndWait(filePath, position);
    }

}
