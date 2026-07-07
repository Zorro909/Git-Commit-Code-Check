package de.zorro909.codecheck.coverage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CoverageThresholdPolicyTest {

    @Test
    void annotationThresholdWinsOverGlobThreshold(@TempDir Path repo) throws Exception {
        Path mapper = write(repo.resolve("src/main/java/com/example/mapper/UserMapper.java"), """
                package com.example.mapper;

                import org.mapstruct.Mapper;

                @Mapper
                interface UserMapper {}
                """);
        CoverageThreshold annotationThreshold = new CoverageThreshold(
                new CoverageThresholdMatch("org.mapstruct.Mapper", null), 0.95, 0.90);
        CoverageThreshold globThreshold = new CoverageThreshold(
                new CoverageThresholdMatch(null, "**/mapper/*.java"), 0.70, 0.60);
        CoverageThreshold fallback = new CoverageThreshold(new CoverageThresholdMatch(null, null), 0.50, 0.40);
        CoverageThresholdPolicy policy = new CoverageThresholdPolicy(
                List.of(globThreshold, annotationThreshold), fallback);

        assertThat(policy.thresholdFor(mapper)).isEqualTo(annotationThreshold);
    }

    @Test
    void globThresholdMatchesSourcePath(@TempDir Path repo) throws Exception {
        Path service = write(repo.resolve("src/main/java/com/example/service/UserService.java"),
                             "package com.example.service; class UserService {}");
        CoverageThreshold globThreshold = new CoverageThreshold(
                new CoverageThresholdMatch(null, "**/service/*.java"), 0.80, 0.75);
        CoverageThreshold fallback = new CoverageThreshold(new CoverageThresholdMatch(null, null), 0.50, 0.40);
        CoverageThresholdPolicy policy = new CoverageThresholdPolicy(List.of(globThreshold), fallback);

        assertThat(policy.thresholdFor(service)).isEqualTo(globThreshold);
    }

    @Test
    void classThresholdWinsOverPackageThreshold(@TempDir Path repo) throws Exception {
        Path service = write(repo.resolve("src/main/java/com/example/service/UserService.java"),
                             "package com.example.service; class UserService {}");
        CoverageThreshold packageThreshold = new CoverageThreshold(
                new CoverageThresholdMatch(null, null, null, "com.example.service..*"), 0.80, 0.75);
        CoverageThreshold classThreshold = new CoverageThreshold(
                new CoverageThresholdMatch(null, null, "com.example.service.UserService", null), 0.95, 0.90);
        CoverageThreshold fallback = new CoverageThreshold(new CoverageThresholdMatch(null, null), 0.50, 0.40);
        CoverageThresholdPolicy policy = new CoverageThresholdPolicy(
                List.of(packageThreshold, classThreshold), fallback);

        assertThat(policy.thresholdFor(service)).isEqualTo(classThreshold);
    }

    @Test
    void thresholdWithMultipleCriteriaRequiresAllOfThemToMatch(@TempDir Path repo)
            throws Exception {
        Path service = write(repo.resolve("src/main/java/com/example/service/UserService.java"),
                             "package com.example.service; class UserService {}");
        CoverageThreshold combined = new CoverageThreshold(
                new CoverageThresholdMatch("org.mapstruct.Mapper", null, null,
                                           "com.example.service..*"), 0.95, 0.90);
        CoverageThreshold fallback = new CoverageThreshold(new CoverageThresholdMatch(null, null),
                                                           0.50, 0.40);
        CoverageThresholdPolicy policy = new CoverageThresholdPolicy(List.of(combined), fallback);

        assertThat(policy.thresholdFor(service)).isEqualTo(fallback);
    }

    private static Path write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, content);
    }
}
