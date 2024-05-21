package de.zorro909.codecheck.checks;

import com.github.javaparser.Position;

import java.nio.file.Path;
import java.util.Optional;

public record ValidationError(Path filePath,
                              String errorMessage,
                              Position position,
                              Severity severity) {
    public enum Severity {
        LOW, MEDIUM, HIGH
    }

    public ValidationError(Path filePath, String errorMessage, Optional<Position> position,
                           Severity severity) {
        this(filePath, errorMessage, position.orElse(new Position(1, 1)), severity);
    }

    @Override
    public String toString() {
        String fileName = filePath.getFileName().toString();
        return "[" + severity + "] " + fileName + ":" + position + " : " + errorMessage;
    }
}
