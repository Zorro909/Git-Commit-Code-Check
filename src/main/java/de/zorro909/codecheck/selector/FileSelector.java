package de.zorro909.codecheck.selector;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface FileSelector {
    /**
     * Selects and returns a list of files.
     *
     * @return A stream of java.nio.file.Path objects representing the selected files.
     * @throws IOException If an I/O error occurs during the file selection process.
     */
    Stream<Path> selectFiles() throws IOException;
}