package de.zorro909.codecheck.runner;

import de.zorro909.codecheck.config.CodeCheckConfig;
import de.zorro909.codecheck.config.CodeCheckConfigLoader;
import de.zorro909.codecheck.config.ConfigOverrides;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DockerMvndTestRunnerTest {

    @Test
    void startsDockerContainerOnDemand(@TempDir Path repo) {
        FakeDockerExecutor docker = new FakeDockerExecutor();
        DockerMvndTestRunner runner = runner(repo, docker, new MutableClock());

        TestRunResult result = runner.runTests(TestRunRequest.full());

        assertThat(result.success()).isTrue();
        assertThat(result.containerId()).isEqualTo("container-1");
        assertThat(docker.startedImages).containsExactly("team/mvnd-jdk25:latest");
        assertThat(result.mavenCommand()).startsWith("mvnd");
    }

    @Test
    void secondRunWithinIdleTimeoutReusesWarmContainer(@TempDir Path repo) {
        FakeDockerExecutor docker = new FakeDockerExecutor();
        MutableClock clock = new MutableClock();
        DockerMvndTestRunner runner = runner(repo, docker, clock);

        runner.runTests(TestRunRequest.full());
        clock.advance(Duration.ofMinutes(5));
        runner.runTests(TestRunRequest.full());

        assertThat(docker.startedImages).hasSize(1);
        assertThat(docker.executedCommands).hasSize(2);
    }

    @Test
    void targetedModuleRunPassesTestPropertyAndModuleList(@TempDir Path repo) {
        FakeDockerExecutor docker = new FakeDockerExecutor();
        DockerMvndTestRunner runner = runner(repo, docker, new MutableClock());

        TestRunResult result = runner.runTests(TestRunRequest.targeted(
                List.of("service-a"), List.of("UserTest", "OrderTest")));

        assertThat(result.mavenCommand()).containsSubsequence("mvnd", "-pl", "service-a");
        assertThat(result.mavenCommand()).contains("-Dtest=UserTest,OrderTest");
        assertThat(result.mavenCommand()).contains("test", "jacoco:report");
    }

    @Test
    void requestAdditionalArgsAreIncludedAfterConfiguredArgs(@TempDir Path repo) {
        FakeDockerExecutor docker = new FakeDockerExecutor();
        DockerMvndTestRunner runner = runner(repo, docker, new MutableClock());

        TestRunResult result = runner.runTests(new TestRunRequest(
                List.of(), List.of(), false, true, false, List.of("-DskipITs")));

        assertThat(result.mavenCommand()).containsSubsequence("mvnd", "-DskipITs", "test");
        assertThat(result.mavenCommand()).doesNotContain("jacoco:report");
    }

    @Test
    void idleContainerStopsAfterConfiguredTimeout(@TempDir Path repo) {
        FakeDockerExecutor docker = new FakeDockerExecutor();
        MutableClock clock = new MutableClock();
        DockerMvndTestRunner runner = runner(repo, docker, clock);
        runner.runTests(TestRunRequest.full());

        clock.advance(Duration.ofMinutes(10));
        runner.reapIdleContainer();

        assertThat(docker.stoppedContainers).containsExactly("container-1");
    }

    @Test
    void staleContainersAreRemovedBeforeStartingNewContainer(@TempDir Path repo) {
        FakeDockerExecutor docker = new FakeDockerExecutor();
        DockerMvndTestRunner runner = runner(repo, docker, new MutableClock());

        runner.runTests(TestRunRequest.full());

        assertThat(docker.removedRepositories)
                .containsExactly(repo.toAbsolutePath().normalize());
        assertThat(docker.startedImages).hasSize(1);
    }

    @Test
    void exitedContainerIsRestartedOnNextRun(@TempDir Path repo) {
        FakeDockerExecutor docker = new FakeDockerExecutor();
        DockerMvndTestRunner runner = runner(repo, docker, new MutableClock());
        runner.runTests(TestRunRequest.full());
        docker.running = false;

        TestRunResult result = runner.runTests(TestRunRequest.full());

        assertThat(result.containerId()).isEqualTo("container-2");
        assertThat(docker.startedImages).hasSize(2);
    }

    private DockerMvndTestRunner runner(Path repo, FakeDockerExecutor docker, MutableClock clock) {
        return new DockerMvndTestRunner(repo, configLoader(), docker, clock);
    }

    private CodeCheckConfigLoader configLoader() {
        return new CodeCheckConfigLoader() {
            @Override
            public CodeCheckConfig load() {
                return CodeCheckConfig.defaults();
            }

            @Override
            public CodeCheckConfig load(ConfigOverrides overrides) {
                return overrides.apply(CodeCheckConfig.defaults());
            }
        };
    }

    private static final class FakeDockerExecutor implements DockerCommandExecutor {

        private final List<String> startedImages = new ArrayList<>();
        private final List<List<String>> executedCommands = new ArrayList<>();
        private final List<String> stoppedContainers = new ArrayList<>();
        private final List<Path> removedRepositories = new ArrayList<>();
        private boolean running = true;

        @Override
        public String startContainer(String image, Path repositoryRoot, boolean mountM2) {
            startedImages.add(image);
            running = true;
            return "container-" + startedImages.size();
        }

        @Override
        public boolean isRunning(String containerId) {
            return running;
        }

        @Override
        public CommandResult exec(String containerId, Path workingDirectory, List<String> command) {
            executedCommands.add(List.copyOf(command));
            return new CommandResult(0, "ok", "");
        }

        @Override
        public void stopContainer(String containerId) {
            stoppedContainers.add(containerId);
            running = false;
        }

        @Override
        public void removeContainersForRepository(Path repositoryRoot) {
            removedRepositories.add(repositoryRoot);
        }
    }

    private static final class MutableClock extends Clock {

        private Instant now = Instant.parse("2026-07-07T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        private void advance(Duration duration) {
            now = now.plus(duration);
        }
    }
}
