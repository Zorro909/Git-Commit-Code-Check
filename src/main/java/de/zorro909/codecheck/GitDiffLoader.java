package de.zorro909.codecheck;

import jakarta.inject.Singleton;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class GitDiffLoader {

    public Set<Path> getChangedFiles(Path repositoryDirectory) throws IOException {
        Set<String> result = new HashSet<>();

        try (Repository repository = openRepository(repositoryDirectory)) {
            try (Git git = new Git(repository)) {

                Status status = git.status().call();

                result.addAll(status.getAdded());
                result.addAll(status.getChanged());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result.stream().map(Paths::get).collect(Collectors.toSet());
    }

    private Repository openRepository(Path repositoryDirectory) throws IOException {
        return new FileRepositoryBuilder().setGitDir(repositoryDirectory.toFile()).readEnvironment().findGitDir().build();
    }

}
