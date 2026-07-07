package de.zorro909.codecheck.legacy.checks.java;

import de.zorro909.codecheck.legacy.FileLoader;
import de.zorro909.codecheck.legacy.checks.CodeCheck;
import de.zorro909.codecheck.java.JavaParserService;
import de.zorro909.codecheck.java.ParseOutcome;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JavaCheckerSharedParserTest {

    @Test
    void allJavaCheckersShareTheSingletonParserService(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, "class Sample {}");

        try (ApplicationContext context = ApplicationContext.builder()
            .singletons(new String[0], new FileLoader(Path.of(""), Optional.empty()))
            .start()) {
            JavaParserService parserService = context.getBean(JavaParserService.class);
            ParseOutcome expected = parserService.parse(file);

            List<JavaChecker> checkers = context.getBeansOfType(CodeCheck.class)
                .stream()
                .filter(JavaChecker.class::isInstance)
                .map(JavaChecker.class::cast)
                .toList();

            assertThat(checkers).isNotEmpty();
            for (JavaChecker checker : checkers) {
                assertThat(checker.load(file))
                    .as(checker.getClass().getSimpleName() + " should reuse the singleton parser cache")
                    .isSameAs(expected);
            }
        }
    }

}
