package de.zorro909.codecheck.editor;

import com.github.javaparser.Position;

import java.nio.file.Path;

public interface EditorExecutor {

    default boolean open(Path path) {
        return open(path, new Position(1, 1));
    }

    boolean open(Path path, Position line);

    default boolean openAndWait(Path path) {
        return openAndWait(path, new Position(1, 1));
    }

    boolean openAndWait(Path path, Position line);

}
