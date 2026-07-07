package de.zorro909.codecheck.reporting;

import de.zorro909.codecheck.core.diagnostic.ValidationError;
import de.zorro909.codecheck.core.validation.ValidationMode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Singleton
public class TerminalDiagnosticRenderer {

    private final ModeSeverityPolicy severityPolicy;

    @Inject
    public TerminalDiagnosticRenderer(ModeSeverityPolicy severityPolicy) {
        this.severityPolicy = severityPolicy;
    }

    public void render(ValidationMode mode, Map<Path, List<ValidationError>> errorsMap, PrintStream out) {
        out.println("Overview of code checks:");
        out.println("------------------------");
        if (errorsMap.isEmpty()) {
            out.println("No validation errors found.");
            return;
        }

        List<ValidationError> visibleErrors = errorsMap.values()
            .stream()
            .flatMap(List::stream)
            .filter(error -> severityPolicy.visible(mode, error.severity()))
            .toList();

        if (visibleErrors.isEmpty()) {
            out.println("No blocking validation errors found.");
            return;
        }

        out.println("Validation diagnostics:");
        visibleErrors.stream().map(ValidationError::toString).forEach(out::println);

        if ((mode == ValidationMode.BATCH || mode == ValidationMode.PRE_COMMIT) && blocks(mode, errorsMap)) {
            out.println("Blocking HIGH diagnostics found; exiting with failure.");
        }
    }

    public boolean blocks(ValidationMode mode, Map<Path, List<ValidationError>> errorsMap) {
        return errorsMap.values()
            .stream()
            .flatMap(List::stream)
            .anyMatch(error -> severityPolicy.blocks(mode, error.severity()));
    }

}
