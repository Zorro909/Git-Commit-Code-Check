package de.zorro909.codecheck;


import jakarta.inject.Singleton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

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
        Set<Path> changedFiles = new HashSet<>();

        try {
            ProcessBuilder builder = new ProcessBuilder("git", "diff", "--cached", "--name-status");
            builder.directory(repositoryDirectory.toAbsolutePath().toFile());
            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Assuming the output format is "A\tfile1\nM\tfile2\n..."
                    String[] splitLine = line.split("\t");
                    if (splitLine.length > 1 && (splitLine[0].equals("A") || splitLine[0].equals(
                            "M"))) {
                        changedFiles.add(Paths.get(splitLine[1]));
                    }
                }
            }
            process.waitFor();
        } catch (InterruptedException | IOException e) {
            throw new IOException("Failed to execute git command", e);
        }

        return changedFiles;
    }

}
