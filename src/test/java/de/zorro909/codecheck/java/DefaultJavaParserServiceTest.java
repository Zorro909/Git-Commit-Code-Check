package de.zorro909.codecheck.java;

import de.zorro909.codecheck.config.CodeCheckConfigLoader;
import de.zorro909.codecheck.validation.DiagnosticKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultJavaParserServiceTest {

    @Test
    void parsesModuleSourceFileWithModuleSourceRoots(@TempDir Path repo) throws Exception {
        writeRootPom(repo, "service");
        Path source = write(repo, "service/src/main/java/com/example/User.java", """
                package com.example;
                public class User {}
                """);
        JavaParserService parserService = parserService(repo);

        ParseOutcome outcome = parserService.parse(source);

        assertThat(outcome.compilationUnit()).isPresent();
        assertThat(outcome.diagnostics()).isEmpty();
    }

    @Test
    void moduleTestFileResolvesMainSourceTypes(@TempDir Path repo) throws Exception {
        writeRootPom(repo, "service");
        write(repo, "service/src/main/java/com/example/User.java", """
                package com.example;
                public class User {}
                """);
        Path test = write(repo, "service/src/test/java/com/example/UserTest.java", """
                package com.example;
                class UserTest {
                    User user;
                }
                """);
        JavaParserService parserService = parserService(repo);

        ParseOutcome outcome = parserService.parse(test);

        assertThat(outcome.compilationUnit()).isPresent();
        assertThat(outcome.diagnostics()).noneMatch(diagnostic -> diagnostic.kind()
                                                                 == DiagnosticKind.SYMBOL_WARNING);
    }

    @Test
    void generatedImplementationCanBeParsedAsContext(@TempDir Path repo) throws Exception {
        writeRootPom(repo, "service");
        Path generated = write(repo,
                               "service/target/generated-sources/annotations/com/example/UserMapperImpl.java",
                               """
                                       package com.example;
                                       public class UserMapperImpl {}
                                       """);
        JavaParserService parserService = parserService(repo);

        ParseOutcome outcome = parserService.parse(generated);

        assertThat(outcome.compilationUnit()).isPresent();
        assertThat(outcome.diagnostics()).isEmpty();
    }

    @Test
    void parseFailureBecomesHighParseDiagnostic(@TempDir Path repo) throws Exception {
        writeRootPom(repo, "service");
        Path broken = write(repo, "service/src/main/java/com/example/Broken.java", """
                package com.example;
                public class Broken {
                """);
        JavaParserService parserService = parserService(repo);

        ParseOutcome outcome = parserService.parse(broken);

        assertThat(outcome.compilationUnit()).isEmpty();
        assertThat(outcome.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.kind()).isEqualTo(DiagnosticKind.PARSE_ERROR);
            assertThat(diagnostic.severity())
                    .isEqualTo(de.zorro909.codecheck.checks.ValidationError.Severity.HIGH);
        });
    }

    @Test
    void unresolvedSymbolBecomesMediumSymbolWarning(@TempDir Path repo) throws Exception {
        writeRootPom(repo, "service");
        Path source = write(repo, "service/src/main/java/com/example/NeedsMissingType.java", """
                package com.example;
                public class NeedsMissingType {
                    MissingType missing;
                }
                """);
        JavaParserService parserService = parserService(repo);

        ParseOutcome outcome = parserService.parse(source);

        assertThat(outcome.compilationUnit()).isPresent();
        assertThat(outcome.diagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.kind()).isEqualTo(DiagnosticKind.SYMBOL_WARNING);
            assertThat(diagnostic.severity())
                    .isEqualTo(de.zorro909.codecheck.checks.ValidationError.Severity.MEDIUM);
            assertThat(diagnostic.message()).contains("MissingType");
        });
    }

    @Test
    void invalidateRemovesCachedParseOutcome(@TempDir Path repo) throws Exception {
        writeRootPom(repo, "service");
        Path source = write(repo, "service/src/main/java/com/example/Reparsed.java", """
                package com.example;
                public class Reparsed {}
                """);
        JavaParserService parserService = parserService(repo);
        ParseOutcome first = parserService.parse(source);

        parserService.invalidate(source);
        ParseOutcome second = parserService.parse(source);

        assertThat(second).isNotSameAs(first);
    }

    private JavaParserService parserService(Path repo) {
        return new DefaultJavaParserService(new MavenProjectModelService(
                repo, CodeCheckConfigLoader.defaultsOnly()));
    }

    private void writeRootPom(Path repo, String module) throws Exception {
        Files.writeString(repo.resolve("pom.xml"), """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>example</groupId>
                  <artifactId>root</artifactId>
                  <version>1</version>
                  <modules>
                    <module>%s</module>
                  </modules>
                </project>
                """.formatted(module));
        Files.createDirectories(repo.resolve(module));
        Files.writeString(repo.resolve(module).resolve("pom.xml"),
                          "<project><modelVersion>4.0.0</modelVersion></project>");
    }

    private Path write(Path repo, String relativePath, String content) throws Exception {
        Path file = repo.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }
}
