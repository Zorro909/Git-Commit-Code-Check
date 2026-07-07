package de.zorro909.codecheck.config;

import de.zorro909.codecheck.RepositoryPathProvider;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.nio.file.Path;
import java.util.function.Consumer;

@Singleton
public class FileSystemCodeCheckConfigLoader implements CodeCheckConfigLoader {

    static final String REPO_CONFIG_FILE = ".codecheck.yaml";

    private final Path repoDirectory;
    private final Path userConfig;
    private final CodeCheckConfigParser parser;

    @Inject
    public FileSystemCodeCheckConfigLoader(
            @Named(RepositoryPathProvider.REPOSITORY_DIRECTORY) Path repoDirectory) {
        this(repoDirectory, defaultUserConfigPath());
    }

    public FileSystemCodeCheckConfigLoader(Path repoDirectory, Path userConfig) {
        this(repoDirectory, userConfig, System.err::println);
    }

    public FileSystemCodeCheckConfigLoader(Path repoDirectory, Path userConfig,
                                           Consumer<String> warningConsumer) {
        this.repoDirectory = repoDirectory.toAbsolutePath().normalize();
        this.userConfig = userConfig.toAbsolutePath().normalize();
        this.parser = new CodeCheckConfigParser(warningConsumer);
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
