package de.zorro909.codecheck.runner;

import jakarta.inject.Singleton;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class ProcessDockerCommandExecutor implements DockerCommandExecutor {

    @Override
    public String startContainer(String image, Path repositoryRoot, boolean mountM2) {
        List<String> command = new ArrayList<>(List.of("docker", "run", "-d", "-w", "/workspace",
                                                       "-v", repositoryRoot.toAbsolutePath()
                                                                            + ":/workspace"));
        if (mountM2) {
            command.add("-v");
            command.add(Path.of(System.getProperty("user.home"), ".m2").toAbsolutePath()
                        + ":/root/.m2");
        }
        command.add(image);
        command.add("sleep");
        command.add("infinity");
        CommandResult result = run(command);
        if (!result.success()) {
            throw new IllegalStateException("Failed to start Docker test container: "
                                            + result.stderr());
        }
        return result.stdout().strip();
    }

    @Override
    public boolean isRunning(String containerId) {
        return run(List.of("docker", "inspect", "-f", "{{.State.Running}}", containerId))
                .stdout()
                .strip()
                .equals("true");
    }

    @Override
    public CommandResult exec(String containerId, Path workingDirectory, List<String> command) {
        List<String> dockerCommand = new ArrayList<>(List.of("docker", "exec", "-w",
                                                             "/workspace", containerId));
        dockerCommand.addAll(command);
        return run(dockerCommand);
    }

    @Override
    public void stopContainer(String containerId) {
        run(List.of("docker", "rm", "-f", containerId));
    }

    private CommandResult run(List<String> command) {
        try {
            Process process = new ProcessBuilder(command).start();
            String stdout = new String(process.getInputStream().readAllBytes(),
                                       StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(),
                                       StandardCharsets.UTF_8);
            return new CommandResult(process.waitFor(), stdout, stderr);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to execute " + String.join(" ", command), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while executing "
                                            + String.join(" ", command), e);
        }
    }
}
