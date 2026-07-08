package de.zorro909.codecheck.core.project;

import de.zorro909.codecheck.core.config.CodeCheckConfig;
import de.zorro909.codecheck.core.config.CodeCheckConfigLoader;
import de.zorro909.codecheck.core.config.ConfigOverrides;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MavenProjectModelServiceTest {

    @Test
    void discoversRootAndMultiModuleMavenSourceRoots(@TempDir Path repo) throws Exception {
        writePom(repo, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>example</groupId>
                  <artifactId>root</artifactId>
                  <version>1</version>
                  <modules>
                    <module>service-a</module>
                    <module>service-b</module>
                  </modules>
                </project>
                """);
        writePom(repo.resolve("service-a"), "<project><modelVersion>4.0.0</modelVersion></project>");
        writePom(repo.resolve("service-b"), "<project><modelVersion>4.0.0</modelVersion></project>");

        ProjectModel model = new MavenProjectModelService(repo, loader(21)).currentModel();

        assertThat(model.languageLevel()).isEqualTo(21);
        assertThat(model.modules()).extracting(module -> module.id().value())
            .containsExactly(".", "service-a", "service-b");
        MavenModule serviceA = model.modules()
            .stream()
            .filter(module -> module.id().equals(new ModuleId("service-a")))
            .findFirst()
            .orElseThrow();
        assertThat(serviceA.sourceRoots())
            .contains(repo.resolve("service-a/src/main/java").toAbsolutePath().normalize());
        assertThat(serviceA.testRoots()).contains(repo.resolve("service-a/src/test/java").toAbsolutePath().normalize());
    }

    @Test
    void generatedSourceRootsAreContextOnly(@TempDir Path repo) throws Exception {
        writePom(repo, "<project><modelVersion>4.0.0</modelVersion></project>");

        MavenModule module = new MavenProjectModelService(repo, loader(25)).currentModel().modules().getFirst();

        Path generated = repo.resolve("target/generated-sources/annotations").toAbsolutePath().normalize();
        assertThat(module.generatedSourceRoots()).contains(generated);
        assertThat(module.contextRoots()).contains(generated);
        assertThat(module.validationSourceRoots()).doesNotContain(generated);
    }

    private void writePom(Path directory, String xml) throws Exception {
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("pom.xml"), xml);
    }

    private CodeCheckConfigLoader loader(int languageLevel) {
        CodeCheckConfig config = CodeCheckConfig.defaults()
            .withJavaProject(new CodeCheckConfig.JavaProject(languageLevel,
                    CodeCheckConfig.GeneratedSourceDetection.MAVEN_DEFAULTS));
        return new CodeCheckConfigLoader() {
            @Override
            public CodeCheckConfig load() {
                return config;
            }

            @Override
            public CodeCheckConfig load(ConfigOverrides overrides) {
                return overrides.apply(config);
            }
        };
    }

}
