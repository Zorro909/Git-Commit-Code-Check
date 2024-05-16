package de.zorro909.codecheck;

public record ValidationError(String filePath, String errorMessage, int lineNumber, Severity severity) {
    public enum Severity {
        LOW,
        MEDIUM,
        HIGH
    }
}
