package de.zorro909.codecheck.checks;

import java.nio.file.Path;

public record ValidationError(Path filePath,
                              String errorMessage,
                              int lineNumber,
                              Severity severity) {
    public enum Severity {
        LOW, MEDIUM, HIGH
    }


    @Override
    public String toString() {
        String fileName = filePath.getFileName().toString();
        return "[" + severity + "] " + fileName + ":" + lineNumber + " : " + errorMessage;
    }
}
