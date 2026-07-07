package de.zorro909.codecheck.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSystemCodeCheckConfigLoaderTest {

    @Test
    void missingConfigFilesReturnDefaults(@TempDir Path tempDir) {
        CodeCheckConfig config = loader(tempDir).load();

        assertThat(config.git().mainBranches()).containsExactly("develop", "main", "master");
        assertThat(config.git().releaseBranchPattern()).isEqualTo("release/.*");
        assertThat(config.daemon().inactivityTimeout()).isEqualTo(Duration.ofMinutes(30));
        assertThat(config.maven().docker().image()).isEqualTo("team/mvnd-jdk25:latest");
    }

    @Test
    void userConfigOverridesDefaults(@TempDir Path tempDir) throws Exception {
        Path userConfig = tempDir.resolve("user.yaml");
        Files.writeString(userConfig, """
                daemon:
                  inactivityTimeout: "45m"
                  saveDebounce: "2s"
                """);

        CodeCheckConfig config = loader(tempDir.resolve("repo"), userConfig).load();

        assertThat(config.daemon().inactivityTimeout()).isEqualTo(Duration.ofMinutes(45));
        assertThat(config.daemon().saveDebounce()).isEqualTo(Duration.ofSeconds(2));
    }

    @Test
    void repoConfigOverridesUserConfig(@TempDir Path tempDir) throws Exception {
        Path repo = tempDir.resolve("repo");
        Files.createDirectories(repo);
        Path userConfig = tempDir.resolve("user.yaml");
        Files.writeString(userConfig, """
                git:
                  mainBranches: [main]
                """);
        Files.writeString(repo.resolve(".codecheck.yaml"), """
                git:
                  mainBranches: [trunk, main]
                  releaseBranchPattern: "release/.+"
                """);

        CodeCheckConfig config = loader(repo, userConfig).load();

        assertThat(config.git().mainBranches()).containsExactly("trunk", "main");
        assertThat(config.git().releaseBranchPattern()).isEqualTo("release/.+");
    }

    @Test
    void repoConfigSetsMavenGoalsAndDockerImage(@TempDir Path tempDir) throws Exception {
        Path repo = tempDir.resolve("repo");
        Files.createDirectories(repo);
        Files.writeString(repo.resolve(".codecheck.yaml"), """
                maven:
                  goals: ["verify", "jacoco:report"]
                  args: ["-DskipITs"]
                  targetedTestProperty: "-Dit.test"
                  docker:
                    image: "example/mvnd:jdk25"
                    containerIdleTimeout: "90s"
                    mountM2: false
                """);

        CodeCheckConfig config = loader(repo).load();

        assertThat(config.maven().goals()).containsExactly("verify", "jacoco:report");
        assertThat(config.maven().args()).containsExactly("-DskipITs");
        assertThat(config.maven().targetedTestProperty()).isEqualTo("-Dit.test");
        assertThat(config.maven().docker().image()).isEqualTo("example/mvnd:jdk25");
        assertThat(config.maven().docker().containerIdleTimeout()).isEqualTo(Duration.ofSeconds(90));
        assertThat(config.maven().docker().mountM2()).isFalse();
    }

    @Test
    void cliOverridesWinOverFileConfig(@TempDir Path tempDir) throws Exception {
        Path repo = tempDir.resolve("repo");
        Files.createDirectories(repo);
        Files.writeString(repo.resolve(".codecheck.yaml"), """
                git:
                  mainBranches: [develop]
                daemon:
                  saveDebounce: "10s"
                """);

        CodeCheckConfig config = loader(repo).load(new ConfigOverrides(
                List.of("release-main"), null, Duration.ofMillis(250)));

        assertThat(config.git().mainBranches()).containsExactly("release-main");
        assertThat(config.daemon().saveDebounce()).isEqualTo(Duration.ofMillis(250));
    }

    @Test
    void invalidConfigFailsWithActionableFieldPath(@TempDir Path tempDir) throws Exception {
        Path repo = tempDir.resolve("repo");
        Files.createDirectories(repo);
        Files.writeString(repo.resolve(".codecheck.yaml"), """
                daemon:
                  saveDebounce: "soon"
                """);

        assertThatThrownBy(() -> loader(repo).load())
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining(".codecheck.yaml")
                .hasMessageContaining("daemon.saveDebounce")
                .hasMessageContaining("invalid duration");
    }

    @Test
    void invalidReleaseBranchPatternFailsWithFieldPath(@TempDir Path tempDir) throws Exception {
        Path repo = tempDir.resolve("repo");
        Files.createDirectories(repo);
        Files.writeString(repo.resolve(".codecheck.yaml"), """
                git:
                  releaseBranchPattern: "release/["
                """);

        assertThatThrownBy(() -> loader(repo).load())
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("git.releaseBranchPattern")
                .hasMessageContaining("invalid regular expression");
    }

    @Test
    void unknownEnumValueFailsWithFieldPath(@TempDir Path tempDir) throws Exception {
        Path repo = tempDir.resolve("repo");
        Files.createDirectories(repo);
        Files.writeString(repo.resolve(".codecheck.yaml"), """
                maven:
                  runner: host-maven
                """);

        assertThatThrownBy(() -> loader(repo).load())
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("maven.runner")
                .hasMessageContaining("unknown value");
    }

    @Test
    void unknownConfigKeysProduceWarnings(@TempDir Path tempDir) throws Exception {
        Path repo = tempDir.resolve("repo");
        Files.createDirectories(repo);
        Files.writeString(repo.resolve(".codecheck.yaml"), """
                git:
                  mainbranches: [trunk]
                unknownSection:
                  foo: bar
                """);
        List<String> warnings = new ArrayList<>();
        FileSystemCodeCheckConfigLoader loader = new FileSystemCodeCheckConfigLoader(
                repo, repo.resolve("missing-user.yaml"), warnings::add);

        CodeCheckConfig config = loader.load();

        assertThat(config.git().mainBranches()).containsExactly("develop", "main", "master");
        assertThat(warnings).anySatisfy(warning -> assertThat(warning)
                .contains(".codecheck.yaml")
                .contains("git")
                .contains("unknown key 'mainbranches'"));
        assertThat(warnings).anySatisfy(warning -> assertThat(warning)
                .contains("unknown key 'unknownSection'"));
    }

    @Test
    void knownConfigKeysProduceNoWarnings(@TempDir Path tempDir) throws Exception {
        Path repo = tempDir.resolve("repo");
        Files.createDirectories(repo);
        Files.writeString(repo.resolve(".codecheck.yaml"), """
                git:
                  mainBranches: [trunk]
                maven:
                  docker:
                    image: "example/mvnd:jdk25"
                """);
        List<String> warnings = new ArrayList<>();
        FileSystemCodeCheckConfigLoader loader = new FileSystemCodeCheckConfigLoader(
                repo, repo.resolve("missing-user.yaml"), warnings::add);

        loader.load();

        assertThat(warnings).isEmpty();
    }

    private FileSystemCodeCheckConfigLoader loader(Path repo) {
        return loader(repo, repo.resolve("missing-user.yaml"));
    }

    private FileSystemCodeCheckConfigLoader loader(Path repo, Path userConfig) {
        return new FileSystemCodeCheckConfigLoader(repo, userConfig);
    }
}
