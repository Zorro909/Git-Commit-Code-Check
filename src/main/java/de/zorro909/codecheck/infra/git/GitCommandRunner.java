package de.zorro909.codecheck.infra.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public class GitCommandRunner {

    private final Path repositoryDirectory;

    public GitCommandRunner(Path repositoryDirectory) {
        this.repositoryDirectory = repositoryDirectory.toAbsolutePath().normalize();
    }

    public List<String> run(String... args) {
        ProcessBuilder builder = new ProcessBuilder(command(args));
        builder.directory(repositoryDirectory.toFile());
        try {
            Process process = builder.start();
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new GitCommandException(
                        "git " + String.join(" ", args) + " failed with exit code " + exitCode + ": " + stderr.strip());
            }
            return stdout.lines().filter(line -> !line.isBlank()).toList();
        }
        catch (IOException e) {
            throw new GitCommandException("Failed to execute git " + String.join(" ", args), e);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GitCommandException("Interrupted while executing git " + String.join(" ", args), e);
        }
    }

    public boolean succeeds(String... args) {
        ProcessBuilder builder = new ProcessBuilder(command(args));
        builder.directory(repositoryDirectory.toFile());
        try {
            Process process = builder.start();
            process.getInputStream().readAllBytes();
            process.getErrorStream().readAllBytes();
            return process.waitFor() == 0;
        }
        catch (IOException e) {
            throw new GitCommandException("Failed to execute git " + String.join(" ", args), e);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GitCommandException("Interrupted while executing git " + String.join(" ", args), e);
        }
    }

    private List<String> command(String... args) {
        List<String> command = new java.util.ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));
        return command;
    }

}
