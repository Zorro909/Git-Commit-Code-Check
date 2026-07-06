package de.zorro909.codecheck.config;

import jakarta.inject.Singleton;

import java.nio.file.Path;

@Singleton
public class FileSystemCodeCheckConfigLoader implements CodeCheckConfigLoader {

    static final String REPO_CONFIG_FILE = ".codecheck.yaml";

    private final Path repoDirectory;
    private final Path userConfig;
    private final CodeCheckConfigParser parser;

    public FileSystemCodeCheckConfigLoader(Path repoDirectory) {
        this(repoDirectory, defaultUserConfigPath());
    }

    public FileSystemCodeCheckConfigLoader(Path repoDirectory, Path userConfig) {
        this.repoDirectory = repoDirectory.toAbsolutePath().normalize();
        this.userConfig = userConfig.toAbsolutePath().normalize();
        this.parser = new CodeCheckConfigParser();
    }

    @Override
    public CodeCheckConfig load() {
        return load(ConfigOverrides.none());
    }

    @Override
    public CodeCheckConfig load(ConfigOverrides overrides) {
        CodeCheckConfig config = CodeCheckConfig.defaults();
        config = parser.applyIfPresent(userConfig, config);
        config = parser.applyIfPresent(repoDirectory.resolve(REPO_CONFIG_FILE), config);
        return overrides.apply(config);
    }

    private static Path defaultUserConfigPath() {
        return Path.of(System.getProperty("user.home"), ".config", "git-commit-code-check",
                       "config.yaml");
    }
}
