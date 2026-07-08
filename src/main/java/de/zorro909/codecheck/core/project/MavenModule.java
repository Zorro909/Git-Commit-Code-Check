package de.zorro909.codecheck.core.project;

import java.nio.file.Path;
import java.util.List;

public record MavenModule(ModuleId id, Path moduleRoot, List<Path> sourceRoots, List<Path> testRoots,
        List<Path> generatedSourceRoots, List<Path> generatedTestSourceRoots) {

    public MavenModule {
        moduleRoot = moduleRoot.toAbsolutePath().normalize();
        sourceRoots = normalize(sourceRoots);
        testRoots = normalize(testRoots);
        generatedSourceRoots = normalize(generatedSourceRoots);
        generatedTestSourceRoots = normalize(generatedTestSourceRoots);
    }

    public List<Path> validationSourceRoots() {
        return java.util.stream.Stream.concat(sourceRoots.stream(), testRoots.stream()).toList();
    }

    public List<Path> contextRoots() {
        return java.util.stream.Stream.concat(generatedSourceRoots.stream(), generatedTestSourceRoots.stream())
            .toList();
    }

    public boolean owns(Path file) {
        Path absolute = file.toAbsolutePath().normalize();
        return java.util.stream.Stream.of(sourceRoots, testRoots, generatedSourceRoots, generatedTestSourceRoots)
            .flatMap(List::stream)
            .anyMatch(root -> absolute.startsWith(root));
    }

    private static List<Path> normalize(List<Path> paths) {
        return paths.stream().map(path -> path.toAbsolutePath().normalize()).toList();
    }
}
