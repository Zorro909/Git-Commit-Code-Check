package de.zorro909.codecheck.runner;

import java.nio.file.Path;
import java.util.List;

public interface DockerCommandExecutor {

    String startContainer(String image, Path repositoryRoot, boolean mountM2);

    boolean isRunning(String containerId);

    CommandResult exec(String containerId, Path workingDirectory, List<String> command);

    void stopContainer(String containerId);
}
