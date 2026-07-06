package de.zorro909.codecheck.coverage;

import java.nio.file.Path;
import java.util.List;

public record CoverageRequest(List<Path> sourceFiles,
                              List<Path> testFiles,
                              List<Path> contextFiles,
                              List<Path> buildFiles,
                              List<Path> reportPaths) {

    public CoverageRequest {
        sourceFiles = normalize(sourceFiles);
        testFiles = normalize(testFiles);
        contextFiles = normalize(contextFiles);
        buildFiles = normalize(buildFiles);
        reportPaths = normalize(reportPaths);
    }

    private static List<Path> normalize(List<Path> paths) {
        return paths.stream().map(path -> path.toAbsolutePath().normalize()).toList();
    }
}
