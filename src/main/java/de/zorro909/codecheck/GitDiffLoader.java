package de.zorro909.codecheck;

import jakarta.inject.Singleton;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is responsible for loading the list of changed files in a Git repository.
 */
@Singleton
public class GitDiffLoader {

    /**
     * This method is responsible for retrieving the list of changed files in a Git repository.
     *
     * @param repositoryDirectory The path to the Git repository directory.
     * @return A set of java.nio.file.Path objects representing the changed files.
     * @throws IOException If an I/O error occurs during the retrieval process.
     */
    public Set<Path> getChangedFiles(Path repositoryDirectory) throws IOException {
        Set<String> result = new HashSet<>();

        try (Git git = Git.open(repositoryDirectory.toFile())) {

            Status status = git.status().call();

            result.addAll(status.getAdded());
            result.addAll(status.getChanged());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result.stream().map(Paths::get).collect(Collectors.toSet());
    }

}
