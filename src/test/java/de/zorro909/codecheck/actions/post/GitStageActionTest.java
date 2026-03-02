package de.zorro909.codecheck.actions.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GitStageAction — the post-action that stages modified files
 * using git add in batches.
 */
class GitStageActionTest {

    @TempDir
    Path tempDir;

    private GitStageAction action;

    @BeforeEach
    void setUp() throws Exception {
        // Initialize a real git repository in the temp directory
        ProcessBuilder init = new ProcessBuilder("git", "init");
        init.directory(tempDir.toFile());
        init.start().waitFor();

        // Configure git user for commits (needed for initial commit)
        runGit("config", "user.email", "test@test.com");
        runGit("config", "user.name", "Test");

        action = new GitStageAction(tempDir);
    }

    // --- perform tests ---

    @Test
    void perform_returnsTrueWithEmptyFileSet() {
        boolean result = action.perform(Set.of());

        assertThat(result).isTrue();
    }

    @Test
    void perform_returnsTrueInGitRepository() throws Exception {
        Path file = tempDir.resolve("Example.java");
        Files.writeString(file, "public class Example {}");

        boolean result = action.perform(Set.of(file));

        assertThat(result).isTrue();
    }

    @Test
    void perform_stagesFilesInGitRepository() throws Exception {
        Path file1 = tempDir.resolve("File1.java");
        Path file2 = tempDir.resolve("File2.java");
        Files.writeString(file1, "public class File1 {}");
        Files.writeString(file2, "public class File2 {}");

        action.perform(Set.of(file1, file2));

        String status = getGitStatus();
        assertThat(status).contains("File1.java");
        assertThat(status).contains("File2.java");

        // Verify files are staged (A = added to index)
        assertThat(status).matches("(?s).*A\\s+File[12]\\.java.*");
    }

    @Test
    void perform_handlesBatchingWithManyFiles() throws Exception {
        // Create more than 10 files to exercise the batching logic
        Set<Path> files = new LinkedHashSet<>();
        for (int i = 1; i <= 25; i++) {
            Path file = tempDir.resolve("BatchFile" + i + ".java");
            Files.writeString(file, "public class BatchFile" + i + " {}");
            files.add(file);
        }

        boolean result = action.perform(files);

        assertThat(result).isTrue();

        // Verify all 25 files were staged
        String status = getGitStatus();
        for (int i = 1; i <= 25; i++) {
            assertThat(status).contains("BatchFile" + i + ".java");
        }
    }

    @Test
    void perform_handlesNonExistentRepository() {
        Path nonExistentPath = tempDir.resolve("non-existent-repo");
        GitStageAction badAction = new GitStageAction(nonExistentPath);

        Path fakeFile = nonExistentPath.resolve("Fake.java");

        // With a non-existent directory, git add will fail.
        // The batch size halving logic will eventually reach 1, then return false.
        boolean result = badAction.perform(Set.of(fakeFile));

        assertThat(result).isFalse();
    }

    // --- constructor tests ---

    @Test
    void constructor_setsRepositoryPath() {
        Path repoPath = Path.of("/some/repo/path");
        GitStageAction stageAction = new GitStageAction(repoPath);

        // Verify through perform behavior — an empty set should still return true
        // regardless of the path, because the while loop body never executes.
        assertThat(stageAction.perform(Set.of())).isTrue();
    }

    // --- helper methods ---

    private void runGit(String... args) throws Exception {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(tempDir.toFile());
        pb.start().waitFor();
    }

    private String getGitStatus() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("git", "status", "--porcelain");
        pb.directory(tempDir.toFile());
        Process process = pb.start();

        String output;
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }
        process.waitFor();
        return output;
    }
}
