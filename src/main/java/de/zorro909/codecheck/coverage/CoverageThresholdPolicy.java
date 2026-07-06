package de.zorro909.codecheck.coverage;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class CoverageThresholdPolicy {

    private final List<CoverageThreshold> thresholds;
    private final CoverageThreshold fallback;

    public CoverageThresholdPolicy(List<CoverageThreshold> thresholds,
                                   CoverageThreshold fallback) {
        this.thresholds = List.copyOf(thresholds);
        this.fallback = fallback;
    }

    public CoverageThreshold thresholdFor(Path sourceFile) {
        return thresholds.stream()
                         .filter(threshold -> matches(threshold.match(), sourceFile))
                         .max(Comparator.comparingInt(threshold -> specificity(threshold.match())))
                         .orElse(fallback);
    }

    private boolean matches(CoverageThresholdMatch match, Path sourceFile) {
        return annotationMatches(match.annotation(), sourceFile)
               || globMatches(match.glob(), sourceFile)
               || classMatches(match.className(), sourceFile)
               || packageMatches(match.packageName(), sourceFile);
    }

    private boolean annotationMatches(String annotation, Path sourceFile) {
        if (annotation == null || annotation.isBlank()) {
            return false;
        }
        try {
            String source = Files.readString(sourceFile);
            String simpleName = annotation.substring(annotation.lastIndexOf('.') + 1);
            return source.contains("@" + annotation) || source.contains("@" + simpleName);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + sourceFile, e);
        }
    }

    private boolean globMatches(String glob, Path sourceFile) {
        return glob != null && !glob.isBlank()
               && FileSystems.getDefault().getPathMatcher("glob:" + glob).matches(sourceFile);
    }

    private boolean classMatches(String className, Path sourceFile) {
        if (className == null || className.isBlank()) {
            return false;
        }
        String sourceClass = className(sourceFile);
        return className.equals(sourceClass) || className.equals(sourceClass.replace('/', '.'));
    }

    private boolean packageMatches(String packageName, Path sourceFile) {
        if (packageName == null || packageName.isBlank()) {
            return false;
        }
        String sourceClass = className(sourceFile).replace('/', '.');
        if (packageName.endsWith("..*")) {
            String prefix = packageName.substring(0, packageName.length() - 3);
            return sourceClass.startsWith(prefix + ".");
        }
        if (packageName.endsWith(".*")) {
            String prefix = packageName.substring(0, packageName.length() - 2);
            String remainder = sourceClass.substring(Math.min(sourceClass.length(), prefix.length()));
            return sourceClass.startsWith(prefix + ".") && remainder.indexOf('.', 1) == -1;
        }
        return sourceClass.startsWith(packageName + ".");
    }

    private String className(Path sourceFile) {
        try {
            String source = Files.readString(sourceFile);
            String packageName = java.util.regex.Pattern.compile("package\\s+([\\w.]+)\\s*;")
                                                        .matcher(source)
                                                        .results()
                                                        .map(match -> match.group(1))
                                                        .findFirst()
                                                        .orElse("");
            String simpleName = sourceFile.getFileName().toString().replaceFirst("\\.java$", "");
            return packageName.isBlank() ? simpleName : packageName + "." + simpleName;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + sourceFile, e);
        }
    }

    private int specificity(CoverageThresholdMatch match) {
        int score = 0;
        if (match.annotation() != null && !match.annotation().isBlank()) {
            score += 2;
        }
        if (match.glob() != null && !match.glob().isBlank()) {
            score += 1;
        }
        if (match.packageName() != null && !match.packageName().isBlank()) {
            score += 2;
        }
        if (match.className() != null && !match.className().isBlank()) {
            score += 3;
        }
        return score;
    }
}
