package de.zorro909.codecheck.selector.impl;

import de.zorro909.codecheck.RequiresCliOption;
import de.zorro909.codecheck.selector.FileSelector;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Singleton
@RequiresCliOption("--check-all")
public class AllFileSelector implements FileSelector {

    private final Path repositoryDirectory;

    public AllFileSelector(Path repositoryDirectory) {
        this.repositoryDirectory = repositoryDirectory;
    }

    @SuppressWarnings("resource")
    @Override
    public Stream<Path> selectFiles() throws IOException {
        try {
            return Files.walk(this.repositoryDirectory).filter(Files::isRegularFile);
        } catch (IOException e) {
            throw new IOException("Error walking files in repository directory ", e);
        }
    }
}
