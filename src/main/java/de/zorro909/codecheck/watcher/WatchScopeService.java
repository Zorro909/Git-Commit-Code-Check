package de.zorro909.codecheck.watcher;

import de.zorro909.codecheck.RepositoryPathProvider;
import de.zorro909.codecheck.java.MavenModule;
import de.zorro909.codecheck.java.ProjectModel;
import de.zorro909.codecheck.java.ProjectModelService;
import de.zorro909.codecheck.core.validation.rule.FileInterest;
import de.zorro909.codecheck.core.validation.rule.RuleRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

@Singleton
public class WatchScopeService {

    private final Path repositoryRoot;

    private final RuleRegistry ruleRegistry;

    private final ProjectModelService projectModelService;

    @Inject
    public WatchScopeService(@Named(RepositoryPathProvider.REPOSITORY_DIRECTORY) Path repositoryRoot,
            RuleRegistry ruleRegistry, ProjectModelService projectModelService) {
        this.repositoryRoot = repositoryRoot.toAbsolutePath().normalize();
        this.ruleRegistry = ruleRegistry;
        this.projectModelService = projectModelService;
    }

    public WatchScope watchScope() {
        Set<WatchedPath> watchedPaths = new LinkedHashSet<>();
        ProjectModel model = projectModelService.currentModel();
        for (MavenModule module : model.modules()) {
            addProjectFiles(watchedPaths, module);
            addRuleInterests(watchedPaths, module);
            module.generatedSourceRoots()
                .forEach(root -> watchedPaths
                    .add(new WatchedPath(root, WatchPathKind.CONTEXT, "generated source context")));
            module.generatedTestSourceRoots()
                .forEach(root -> watchedPaths
                    .add(new WatchedPath(root, WatchPathKind.CONTEXT, "generated test context")));
        }
        watchedPaths.add(new WatchedPath(userConfigPath(), WatchPathKind.CONFIG, "user config"));
        watchedPaths
            .add(new WatchedPath(repositoryRoot.resolve(".codecheck.yaml"), WatchPathKind.CONFIG, "repo config"));
        return new WatchScope(watchedPaths);
    }

    private void addProjectFiles(Set<WatchedPath> watchedPaths, MavenModule module) {
        watchedPaths.add(new WatchedPath(module.moduleRoot().resolve("pom.xml"), WatchPathKind.PROJECT_MODEL,
                "Maven project model"));
    }

    private void addRuleInterests(Set<WatchedPath> watchedPaths, MavenModule module) {
        for (FileInterest interest : ruleRegistry.watchPlan().validatedFiles()) {
            if (interest.matches(module.moduleRoot().resolve("src/main/java/Example.java"))) {
                module.sourceRoots()
                    .forEach(root -> watchedPaths
                        .add(new WatchedPath(root, WatchPathKind.VALIDATED, interest.description())));
            }
            if (interest.matches(module.moduleRoot().resolve("src/test/java/ExampleTest.java"))) {
                module.testRoots()
                    .forEach(root -> watchedPaths
                        .add(new WatchedPath(root, WatchPathKind.VALIDATED, interest.description())));
            }
            if (interest.includeGlobs().contains("pom.xml")) {
                watchedPaths.add(new WatchedPath(module.moduleRoot().resolve("pom.xml"), WatchPathKind.PROJECT_MODEL,
                        interest.description()));
            }
        }
        for (FileInterest interest : ruleRegistry.watchPlan().contextFiles()) {
            module.contextRoots()
                .forEach(
                        root -> watchedPaths.add(new WatchedPath(root, WatchPathKind.CONTEXT, interest.description())));
        }
    }

    private Path userConfigPath() {
        return Path.of(System.getProperty("user.home"), ".config", "git-commit-code-check", "config.yaml");
    }

}
