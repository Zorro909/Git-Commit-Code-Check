package de.zorro909.codecheck.watcher;

import de.zorro909.codecheck.core.diagnostic.ValidationError;
import de.zorro909.codecheck.core.diagnostic.Diagnostic;
import de.zorro909.codecheck.core.diagnostic.DiagnosticKind;
import de.zorro909.codecheck.core.validation.rule.RuleId;
import de.zorro909.codecheck.core.diagnostic.SourcePosition;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IncrementalStateAndInvalidationTest {

    @Test
    void storesLatestStateOnlyByFile() {
        IncrementalValidationState state = new IncrementalValidationState();
        Path file = Path.of("src/main/java/Example.java");
        Diagnostic first = diagnostic(file, "first");
        Diagnostic second = diagnostic(file, "second");

        state.updateCurrent(file, List.of(first));
        state.updateCurrent(file, List.of(second));

        assertThat(state.status(file)).isEqualTo(FileValidationStatus.CURRENT);
        assertThat(state.diagnostics(file)).containsExactly(second);
    }

    @Test
    void contextChangeInvalidatesDependentValidatedFiles() {
        IncrementalValidationState state = new IncrementalValidationState();
        DependencyInvalidationGraph graph = new DependencyInvalidationGraph();
        Path generated = Path.of("target/generated-sources/annotations/MapperImpl.java");
        Path mapper = Path.of("src/main/java/Mapper.java");
        state.updateCurrent(mapper, List.of());
        graph.recordDependency(generated, mapper);

        assertThat(graph.invalidate(generated, state)).containsExactly(mapper.toAbsolutePath().normalize());
        assertThat(state.status(mapper)).isEqualTo(FileValidationStatus.STALE);
    }

    private Diagnostic diagnostic(Path file, String message) {
        return new Diagnostic(file, message, new SourcePosition(1, 1), ValidationError.Severity.LOW,
                DiagnosticKind.RULE_VIOLATION, new RuleId("test"));
    }

}
