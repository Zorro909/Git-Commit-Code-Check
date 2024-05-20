package de.zorro909.codecheck.editor;

import java.nio.file.Path;

public interface EditorExecutor {

    default boolean open(Path path) {
        return open(path, 1);
    }

    boolean open(Path path, Integer line);

    default boolean openAndWait(Path path) {
        return openAndWait(path, 1);
    }

    boolean openAndWait(Path path, Integer line);

}
