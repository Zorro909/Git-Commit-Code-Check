package de.zorro909.codecheck.config;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

public record CodeCheckConfig(Git git,
                              Daemon daemon,
                              JavaProject javaProject,
                              Maven maven,
                              Coverage coverage) {

    public static CodeCheckConfig defaults() {
        return new CodeCheckConfig(
                new Git(List.of("develop", "main", "master"), Pattern.compile("release/.*"),
                        true),
                new Daemon(Duration.ofMinutes(30), Duration.ofSeconds(5), Transport.WEBSOCKET),
                new JavaProject(25, GeneratedSourceDetection.MAVEN_DEFAULTS),
                new Maven(MavenRunner.DOCKER_MVND, true,
                          new Docker("team/mvnd-jdk25:latest", Duration.ofMinutes(10), true),
                          List.of("test", "jacoco:report"), List.of(), "-Dtest"),
                new Coverage(CoverageProvider.JACOCO, CoverageFreshnessMode.REUSE_IF_FRESH,
                             List.of("target/site/jacoco/jacoco.xml",
                                     "*/target/site/jacoco/jacoco.xml")));
    }

    public CodeCheckConfig withGit(Git git) {
        return new CodeCheckConfig(git, daemon, javaProject, maven, coverage);
    }

    public CodeCheckConfig withDaemon(Daemon daemon) {
        return new CodeCheckConfig(git, daemon, javaProject, maven, coverage);
    }

    public CodeCheckConfig withJavaProject(JavaProject javaProject) {
        return new CodeCheckConfig(git, daemon, javaProject, maven, coverage);
    }

    public CodeCheckConfig withMaven(Maven maven) {
        return new CodeCheckConfig(git, daemon, javaProject, maven, coverage);
    }

    public CodeCheckConfig withCoverage(Coverage coverage) {
        return new CodeCheckConfig(git, daemon, javaProject, maven, coverage);
    }

    public record Git(List<String> mainBranches,
                      Pattern releaseBranchPattern,
                      boolean restageAfterFix) {
    }

    public record Daemon(Duration inactivityTimeout,
                         Duration saveDebounce,
                         Transport transport) {
    }

    public record JavaProject(int languageLevel,
                              GeneratedSourceDetection generatedSourceDetection) {
    }

    public record Maven(MavenRunner runner,
                        boolean preferMvnd,
                        Docker docker,
                        List<String> goals,
                        List<String> args,
                        String targetedTestProperty) {
    }

    public record Docker(String image,
                         Duration containerIdleTimeout,
                         boolean mountM2) {
    }

    public record Coverage(CoverageProvider provider,
                           CoverageFreshnessMode freshnessMode,
                           List<String> reportPaths) {
    }

    public enum Transport {
        WEBSOCKET
    }

    public enum GeneratedSourceDetection {
        MAVEN_DEFAULTS
    }

    public enum MavenRunner {
        DOCKER_MVND
    }

    public enum CoverageProvider {
        JACOCO
    }

    public enum CoverageFreshnessMode {
        REUSE_IF_FRESH
    }
}
