package de.zorro909.codecheck.runner;

import de.zorro909.codecheck.RepositoryPathProvider;
import de.zorro909.codecheck.config.CodeCheckConfig;
import de.zorro909.codecheck.config.CodeCheckConfigLoader;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class DockerMvndTestRunner implements TestRunner {

    private final Path repositoryRoot;

    private final CodeCheckConfigLoader configLoader;

    private final DockerCommandExecutor docker;

    private final Clock clock;

    private String containerId;

    private Instant lastUsed;

    @Inject
    public DockerMvndTestRunner(@Named(RepositoryPathProvider.REPOSITORY_DIRECTORY) Path repositoryRoot,
            CodeCheckConfigLoader configLoader, DockerCommandExecutor docker) {
        this(repositoryRoot, configLoader, docker, Clock.systemUTC());
    }

    DockerMvndTestRunner(Path repositoryRoot, CodeCheckConfigLoader configLoader, DockerCommandExecutor docker,
            Clock clock) {
        this.repositoryRoot = repositoryRoot.toAbsolutePath().normalize();
        this.configLoader = configLoader;
        this.docker = docker;
        this.clock = clock;
    }

    @Override
    public synchronized TestRunResult runTests(TestRunRequest request) {
        CodeCheckConfig.Maven config = configLoader.load().maven();
        String activeContainer = ensureContainer(config);
        List<String> command = mavenCommand(config, request);
        CommandResult result = docker.exec(activeContainer, repositoryRoot, command);
        lastUsed = clock.instant();
        return new TestRunResult(result.success(), result.exitCode(), result.stdout(), result.stderr(), activeContainer,
                command);
    }

    @Override
    public synchronized void stop() {
        if (containerId != null) {
            docker.stopContainer(containerId);
            containerId = null;
            lastUsed = null;
        }
    }

    public synchronized void reapIdleContainer() {
        if (containerId == null || lastUsed == null) {
            return;
        }
        if (lastUsed.plus(configLoader.load().maven().docker().containerIdleTimeout()).isBefore(clock.instant())
                || lastUsed.plus(configLoader.load().maven().docker().containerIdleTimeout()).equals(clock.instant())) {
            stop();
        }
    }

    private String ensureContainer(CodeCheckConfig.Maven config) {
        if (containerId == null || !docker.isRunning(containerId)) {
            docker.removeContainersForRepository(repositoryRoot);
            containerId = docker.startContainer(config.docker().image(), repositoryRoot, config.docker().mountM2());
        }
        lastUsed = clock.instant();
        return containerId;
    }

    private List<String> mavenCommand(CodeCheckConfig.Maven config, TestRunRequest request) {
        List<String> command = new ArrayList<>();
        command.add(config.preferMvnd() ? "mvnd" : "mvn");
        if (!request.modules().isEmpty()) {
            command.add("-pl");
            command.add(String.join(",", request.modules()));
        }
        command.addAll(config.args());
        command.addAll(request.additionalMavenArgs());
        if (!request.testClasses().isEmpty()) {
            command.add(config.targetedTestProperty() + "=" + String.join(",", request.testClasses()));
        }
        command.addAll(config.goals());
        if (!request.generateCoverageReport()) {
            command.removeIf(goal -> goal.toLowerCase(java.util.Locale.ROOT).contains("jacoco"));
        }
        return command;
    }

}
