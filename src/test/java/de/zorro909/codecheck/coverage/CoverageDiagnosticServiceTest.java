package de.zorro909.codecheck.coverage;

import de.zorro909.codecheck.core.diagnostic.ValidationError;
import de.zorro909.codecheck.core.diagnostic.DiagnosticKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CoverageDiagnosticServiceTest {

    @Test
    void mapStructMapperDiagnosticsUseGeneratedImplementationCoverage(@TempDir Path repo) throws Exception {
        Path mapper = write(repo.resolve("src/main/java/com/example/UserMapper.java"), """
                package com.example;

                import org.mapstruct.Mapper;

                @Mapper
                public interface UserMapper {}
                """);
        CoverageSnapshot snapshot = new CoverageSnapshot(Map.of("com/example/UserMapperImpl",
                new ClassCoverage("com/example/UserMapperImpl", new CoverageMetric(3, 7), new CoverageMetric(2, 2))));
        CoverageThreshold fallback = new CoverageThreshold(new CoverageThresholdMatch(null, null), 0.80, 0.70);
        CoverageDiagnosticService service = new CoverageDiagnosticService(
                new CoverageThresholdPolicy(List.of(), fallback), new MapStructCoverageAttributor());

        var diagnostics = service.diagnostics(mapper, "com/example/UserMapper", snapshot);

        assertThat(diagnostics).hasSize(2);
        assertThat(diagnostics).allSatisfy(diagnostic -> {
            assertThat(diagnostic.file()).isEqualTo(mapper);
            assertThat(diagnostic.kind()).isEqualTo(DiagnosticKind.COVERAGE_FAILURE);
            assertThat(diagnostic.severity()).isEqualTo(ValidationError.Severity.HIGH);
        });
        assertThat(diagnostics).extracting(diagnostic -> diagnostic.ruleId().value()).containsOnly("coverage.jacoco");
    }

    private static Path write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, content);
    }

}
