package de.zorro909.codecheck.changeset;

import de.zorro909.codecheck.config.CodeCheckConfig;
import de.zorro909.codecheck.config.CodeCheckConfigLoader;
import de.zorro909.codecheck.config.ConfigOverrides;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GitChangeSetServiceTest {

    @Test
    void mainBranchSelectionIncludesStagedAndUnstagedChangesAndIgnoresDeleted(
            @TempDir Path repo) throws Exception {
        initRepo(repo, "develop");
        write(repo, "Modified.java", "class Modified {}\n");
        write(repo, "Staged.java", "class Staged {}\n");
        write(repo, "Deleted.java", "class Deleted {}\n");
        git(repo, "add", ".");
        git(repo, "commit", "-m", "base");
        write(repo, "Modified.java", "class Modified { int value; }\n");
        write(repo, "Staged.java", "class Staged { int value; }\n");
        git(repo, "add", "Staged.java");
        Files.delete(repo.resolve("Deleted.java"));

        ChangeSet changeSet = service(repo, List.of("develop", "main")).currentInteractiveCheckChangeSet();

        assertThat(paths(changeSet)).containsExactlyInAnyOrder("Modified.java", "Staged.java");
        assertThat(changeSet.entries()).anySatisfy(entry -> {
            assertThat(entry.path()).isEqualTo(Path.of("Modified.java"));
            assertThat(entry.unstaged()).isTrue();
        });
        assertThat(changeSet.entries()).anySatisfy(entry -> {
            assertThat(entry.path()).isEqualTo(Path.of("Staged.java"));
            assertThat(entry.staged()).isTrue();
        });
    }

    @Test
    void featureBranchUsesDirectDiffAgainstFirstExistingMainBranch(@TempDir Path repo)
            throws Exception {
        initRepo(repo, "develop");
        write(repo, "Feature.java", "class Feature {}\n");
        git(repo, "add", ".");
        git(repo, "commit", "-m", "base");
        git(repo, "checkout", "-b", "feature/demo");
        write(repo, "Feature.java", "class Feature { int value; }\n");

        ChangeSet changeSet = service(repo, List.of("develop", "main")).currentInteractiveCheckChangeSet();

        assertThat(paths(changeSet)).containsExactly("Feature.java");
        assertThat(changeSet.entries().get(0).originReason()).contains("direct diff against develop");
    }

    @Test
    void featureBranchFallsBackToNextConfiguredMainBranch(@TempDir Path repo) throws Exception {
        initRepo(repo, "main");
        write(repo, "Fallback.java", "class Fallback {}\n");
        git(repo, "add", ".");
        git(repo, "commit", "-m", "base");
        git(repo, "checkout", "-b", "feature/fallback");
        write(repo, "Fallback.java", "class Fallback { int value; }\n");

        ChangeSet changeSet = service(repo, List.of("develop", "main")).currentInteractiveCheckChangeSet();

        assertThat(paths(changeSet)).containsExactly("Fallback.java");
        assertThat(changeSet.entries().get(0).originReason()).contains("direct diff against main");
    }

    @Test
    void preCommitUsesStagedPaths(@TempDir Path repo) throws Exception {
        initRepo(repo, "develop");
        write(repo, "PreCommit.java", "class PreCommit {}\n");
        git(repo, "add", ".");
        git(repo, "commit", "-m", "base");
        write(repo, "PreCommit.java", "class PreCommit { int staged; }\n");
        git(repo, "add", "PreCommit.java");
        write(repo, "PreCommit.java", "class PreCommit { int staged; int workingTree; }\n");

        ChangeSet changeSet = service(repo, List.of("develop")).preCommitChangeSet();

        assertThat(paths(changeSet)).containsExactly("PreCommit.java");
        assertThat(changeSet.entries().get(0).staged()).isTrue();
    }

    @Test
    void assistantIncludesUntrackedJavaFiles(@TempDir Path repo) throws Exception {
        initRepo(repo, "develop");
        write(repo, "Tracked.java", "class Tracked {}\n");
        git(repo, "add", ".");
        git(repo, "commit", "-m", "base");
        write(repo, "NewAssistantFile.java", "class NewAssistantFile {}\n");
        write(repo, "notes.txt", "notes\n");

        ChangeSet changeSet = service(repo, List.of("develop")).currentAssistantChangeSet();

        assertThat(paths(changeSet)).contains("NewAssistantFile.java");
        assertThat(paths(changeSet)).doesNotContain("notes.txt");
        assertThat(changeSet.entries()).anySatisfy(entry -> {
            assertThat(entry.path()).isEqualTo(Path.of("NewAssistantFile.java"));
            assertThat(entry.untracked()).isTrue();
        });
    }

    private GitChangeSetService service(Path repo, List<String> mainBranches) {
        return new GitChangeSetService(repo, loader(mainBranches));
    }

    private CodeCheckConfigLoader loader(List<String> mainBranches) {
        CodeCheckConfig config = CodeCheckConfig.defaults()
                                               .withGit(new CodeCheckConfig.Git(
                                                       mainBranches,
                                                       "release/.*",
                                                       true));
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

    private List<String> paths(ChangeSet changeSet) {
        return changeSet.paths().map(Path::toString).toList();
    }

    private void initRepo(Path repo, String branch) throws Exception {
        git(repo, "init");
        git(repo, "config", "user.email", "test@example.invalid");
        git(repo, "config", "user.name", "Test User");
        git(repo, "checkout", "-b", branch);
    }

    private void write(Path repo, String relativePath, String content) throws Exception {
        Path file = repo.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private void git(Path repo, String... args) throws Exception {
        List<String> command = new java.util.ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).directory(repo.toFile()).start();
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new AssertionError("git " + String.join(" ", args) + " failed: "
                                     + stdout + stderr);
        }
    }
}
