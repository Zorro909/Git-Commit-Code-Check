package de.zorro909.codecheck.watcher;

import de.zorro909.codecheck.java.MavenModule;
import de.zorro909.codecheck.java.ModuleId;
import de.zorro909.codecheck.java.ProjectModel;
import de.zorro909.codecheck.java.ProjectModelService;
import de.zorro909.codecheck.validation.FileInterest;
import de.zorro909.codecheck.validation.Fixer;
import de.zorro909.codecheck.validation.Rule;
import de.zorro909.codecheck.validation.RuleRegistry;
import de.zorro909.codecheck.validation.WatchPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WatchScopeServiceTest {

    @Test
    void javaRuleInterestAddsModuleJavaRoots(@TempDir Path repo) {
        WatchScopeService service = new WatchScopeService(repo, registry(
                List.of(FileInterest.javaMainSources()), List.of()), projectModel(repo));

        WatchScope scope = service.watchScope();

        assertThat(scope.contains(repo.resolve("module-a/src/main/java"),
                                  WatchPathKind.VALIDATED)).isTrue();
        assertThat(scope.contains(repo.resolve("module-a/target/generated-sources/annotations"),
                                  WatchPathKind.VALIDATED)).isFalse();
    }

    @Test
    void pomInterestAddsPomFilesToProjectModelWatchScope(@TempDir Path repo) {
        FileInterest pomInterest = new FileInterest("POM rule", List.of("pom.xml"),
                                                    path -> path.getFileName()
                                                                .toString()
                                                                .equals("pom.xml"));
        WatchScopeService service = new WatchScopeService(repo, registry(List.of(pomInterest),
                                                                         List.of()),
                                                          projectModel(repo));

        WatchScope scope = service.watchScope();

        assertThat(scope.contains(repo.resolve("module-a/pom.xml"), WatchPathKind.PROJECT_MODEL))
                .isTrue();
    }

    @Test
    void generatedRootsAreContextOnly(@TempDir Path repo) {
        WatchScopeService service = new WatchScopeService(repo, registry(List.of(), List.of()),
                                                          projectModel(repo));

        WatchScope scope = service.watchScope();

        Path generated = repo.resolve("module-a/target/generated-sources/annotations");
        assertThat(scope.contains(generated, WatchPathKind.CONTEXT)).isTrue();
        assertThat(scope.contains(generated, WatchPathKind.VALIDATED)).isFalse();
    }

    @Test
    void configFilesAreWatched(@TempDir Path repo) {
        WatchScopeService service = new WatchScopeService(repo, registry(List.of(), List.of()),
                                                          projectModel(repo));

        WatchScope scope = service.watchScope();

        assertThat(scope.contains(repo.resolve(".codecheck.yaml"), WatchPathKind.CONFIG)).isTrue();
    }

    private RuleRegistry registry(List<FileInterest> validated, List<FileInterest> context) {
        return new RuleRegistry() {
            @Override
            public List<Rule> activeRules() {
                return List.of();
            }

            @Override
            public List<Fixer> activeFixers() {
                return List.of();
            }

            @Override
            public WatchPlan watchPlan() {
                return new WatchPlan(validated, context);
            }
        };
    }

    private ProjectModelService projectModel(Path repo) {
        MavenModule module = new MavenModule(new ModuleId("module-a"), repo.resolve("module-a"),
                                             List.of(repo.resolve("module-a/src/main/java")),
                                             List.of(repo.resolve("module-a/src/test/java")),
                                             List.of(repo.resolve(
                                                     "module-a/target/generated-sources/annotations")),
                                             List.of(repo.resolve(
                                                     "module-a/target/generated-test-sources/test-annotations")));
        ProjectModel model = new ProjectModel(repo, repo, List.of(module), 25);
        return new ProjectModelService() {
            @Override
            public ProjectModel currentModel() {
                return model;
            }

            @Override
            public ProjectModel refresh() {
                return model;
            }
        };
    }
}
