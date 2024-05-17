package de.zorro909.codecheck;

import jakarta.inject.Singleton;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is responsible for loading the list of changed files in a Git repository.
 */
@Singleton
public class GitDiffLoader {

    /**
     * This method is responsible for retrieving the list of changed files in a Git repository.
     * @param repositoryDirectory The path to the Git repository directory.
     * @return A set of java.nio.file.Path objects representing the changed files.
     * @throws IOException If an I/O error occurs during the retrieval process.
     */
    public Set<Path> getChangedFiles(Path repositoryDirectory) throws IOException {
        try (Git git = Git.open(repositoryDirectory.toFile())) {

            List<DiffEntry> diffs = git.diff().setCached(true).setShowNameAndStatusOnly(true).call();

            return diffs.stream()
                .map(DiffEntry::getNewPath)
                .distinct()
                .map(Paths::get)
                .filter(Files::exists)
                .collect(Collectors.toSet());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new HashSet<>();
    }

}
