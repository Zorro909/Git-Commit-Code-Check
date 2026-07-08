package de.zorro909.codecheck.core.project;

import com.github.javaparser.ast.CompilationUnit;
import de.zorro909.codecheck.core.diagnostic.Diagnostic;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public record ParseOutcome(Path file, Optional<CompilationUnit> compilationUnit, List<Diagnostic> diagnostics) {

    public ParseOutcome {
        file = file.toAbsolutePath().normalize();
        diagnostics = List.copyOf(diagnostics);
    }

    public boolean parsed() {
        return compilationUnit.isPresent() && diagnostics.stream()
            .noneMatch(diagnostic -> diagnostic
                .kind() == de.zorro909.codecheck.core.diagnostic.DiagnosticKind.PARSE_ERROR);
    }
}
