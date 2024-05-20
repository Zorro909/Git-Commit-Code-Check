package de.zorro909.codecheck.selector.impl;


import de.zorro909.codecheck.selector.FileSelector;
import io.micronaut.context.annotation.Secondary;
import jakarta.inject.Singleton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Singleton
@Secondary
public class GitDiffSelector implements FileSelector {

    private static final String GIT_COMMAND = "git";
    private static final String[] GIT_PARAMS = {GIT_COMMAND, "diff", "--cached", "--name-status"};
    private static final String FILE_ADDED = "A";
    private static final String FILE_MODIFIED = "M";

    private final Path repositoryDirectory;

    public GitDiffSelector(Path repositoryDirectory) {
        this.repositoryDirectory = repositoryDirectory;
    }

    @Override
    public Stream<Path> selectFiles() throws IOException {
        try {
            Stream<String> lines = executeGitDiffCommand();
            return processGitDiffOutput(lines);
        } catch (InterruptedException | IOException e) {
            throw new IOException("Failed to execute git command", e);
        }
    }

    private Stream<String> executeGitDiffCommand() throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(GIT_PARAMS);
        builder.directory(repositoryDirectory.toAbsolutePath().toFile());
        Process process = builder.start();
        return new BufferedReader(new InputStreamReader(process.getInputStream())).lines();
    }

    private Stream<Path> processGitDiffOutput(Stream<String> output) {
        return output.map(line -> line.split("\t"))
                     .filter(this::isFileAddedOrModified)
                     .map(splitLine -> Paths.get(splitLine[1]));
    }

    private boolean isFileAddedOrModified(String[] splitLine) {
        return splitLine.length > 1 && (splitLine[0].equals(FILE_ADDED) || splitLine[0].equals(
                FILE_MODIFIED));
    }
}