package de.zorro909.codecheck.validation;

import de.zorro909.codecheck.legacy.FileLoader;
import de.zorro909.codecheck.legacy.checks.java.code.NoMagicValuesCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class NoMagicValuesRuleMetadataTest {

    @Test
    void noMagicValuesCheckIsRepresentedAsJavaMainSourceRule(@TempDir Path tempDir) {
        NoMagicValuesCheck check = new NoMagicValuesCheck(new FileLoader(tempDir, Optional.empty()));
        RuleRegistry registry = new DefaultRuleRegistry(List.of(check), List.of());

        Rule rule = registry.activeRules().get(0);

        assertThat(rule.id()).isEqualTo(new RuleId("java.no-magic-values"));
        assertThat(rule.metadata().name()).isEqualTo("No magic values");
        assertThat(rule.validatedFiles().includeGlobs()).containsExactly("src/main/java/**/*.java");
        assertThat(rule.validatedFiles().matches(Path.of("src/main/java/demo/Example.java"))).isTrue();
        assertThat(rule.validatedFiles().matches(Path.of("src/test/java/demo/ExampleTest.java"))).isFalse();
    }

}
