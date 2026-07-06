package de.zorro909.codecheck.java;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public record ProjectModel(Path repositoryRoot,
                           Path mavenRoot,
                           List<MavenModule> modules,
                           int languageLevel) {

    public ProjectModel {
        repositoryRoot = repositoryRoot.toAbsolutePath().normalize();
        mavenRoot = mavenRoot.toAbsolutePath().normalize();
        modules = List.copyOf(modules);
    }

    public Optional<MavenModule> moduleFor(Path file) {
        Path absolute = file.toAbsolutePath().normalize();
        return modules.stream()
                      .filter(module -> module.owns(absolute))
                      .max(Comparator.comparingInt(module -> module.moduleRoot().getNameCount()));
    }
}
