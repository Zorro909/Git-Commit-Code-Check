package de.zorro909.codecheck.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

final class CodeCheckConfigParser {

    private static final Set<String> ROOT_KEYS = Set.of("git", "daemon", "java", "maven", "coverage");

    private static final Set<String> GIT_KEYS = Set.of("mainBranches", "releaseBranchPattern", "restageAfterFix");

    private static final Set<String> DAEMON_KEYS = Set.of("inactivityTimeout", "saveDebounce", "transport");

    private static final Set<String> JAVA_KEYS = Set.of("languageLevel", "generatedSourceDetection");

    private static final Set<String> MAVEN_KEYS = Set.of("runner", "preferMvnd", "docker", "goals", "args",
            "targetedTestProperty");

    private static final Set<String> DOCKER_KEYS = Set.of("image", "containerIdleTimeout", "mountM2");

    private static final Set<String> COVERAGE_KEYS = Set.of("provider", "freshnessMode", "reportPaths");

    private final Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));

    private final Consumer<String> warningConsumer;

    CodeCheckConfigParser(Consumer<String> warningConsumer) {
        this.warningConsumer = warningConsumer;
    }

    CodeCheckConfig applyIfPresent(Path configPath, CodeCheckConfig base) {
        if (!Files.exists(configPath)) {
            return base;
        }

        Object root;
        try (Reader reader = Files.newBufferedReader(configPath)) {
            root = yaml.load(reader);
        }
        catch (YAMLException e) {
            throw new ConfigException("Invalid config YAML in " + configPath + ": " + e.getMessage(), e);
        }
        catch (IOException e) {
            throw new ConfigException("Unable to read config " + configPath + ": " + e.getMessage(), e);
        }

        if (root == null) {
            return base;
        }
        if (!(root instanceof Map<?, ?> rootMap)) {
            throw invalid(configPath, "<root>", "expected a YAML object");
        }

        Map<String, Object> rootObject = stringKeyMap(rootMap, configPath, "<root>");
        warnUnknownKeys(rootObject, ROOT_KEYS, configPath, "<root>");
        CodeCheckConfig config = base;
        Map<String, Object> git = object(rootObject, "git", configPath, "git");
        if (git != null) {
            config = config.withGit(applyGit(configPath, git, config.git()));
        }
        Map<String, Object> daemon = object(rootObject, "daemon", configPath, "daemon");
        if (daemon != null) {
            config = config.withDaemon(applyDaemon(configPath, daemon, config.daemon()));
        }
        Map<String, Object> java = object(rootObject, "java", configPath, "java");
        if (java != null) {
            config = config.withJavaProject(applyJava(configPath, java, config.javaProject()));
        }
        Map<String, Object> maven = object(rootObject, "maven", configPath, "maven");
        if (maven != null) {
            config = config.withMaven(applyMaven(configPath, maven, config.maven()));
        }
        Map<String, Object> coverage = object(rootObject, "coverage", configPath, "coverage");
        if (coverage != null) {
            config = config.withCoverage(applyCoverage(configPath, coverage, config.coverage()));
        }
        return config;
    }

    private CodeCheckConfig.Git applyGit(Path configPath, Map<String, Object> node, CodeCheckConfig.Git base) {
        warnUnknownKeys(node, GIT_KEYS, configPath, "git");
        List<String> mainBranches = stringList(node, "mainBranches", configPath, "git.mainBranches",
                base.mainBranches());
        String releaseBranchPattern = pattern(node, "releaseBranchPattern", configPath, "git.releaseBranchPattern",
                base.releaseBranchPattern());
        Boolean restageAfterFix = bool(node, "restageAfterFix", configPath, "git.restageAfterFix");
        return new CodeCheckConfig.Git(mainBranches, releaseBranchPattern,
                restageAfterFix == null ? base.restageAfterFix() : restageAfterFix);
    }

    private CodeCheckConfig.Daemon applyDaemon(Path configPath, Map<String, Object> node, CodeCheckConfig.Daemon base) {
        warnUnknownKeys(node, DAEMON_KEYS, configPath, "daemon");
        Duration inactivityTimeout = duration(node, "inactivityTimeout", configPath, "daemon.inactivityTimeout",
                base.inactivityTimeout());
        Duration saveDebounce = duration(node, "saveDebounce", configPath, "daemon.saveDebounce", base.saveDebounce());
        CodeCheckConfig.Transport transport = enumValue(node, "transport", configPath, "daemon.transport",
                CodeCheckConfig.Transport.class, base.transport());
        return new CodeCheckConfig.Daemon(inactivityTimeout, saveDebounce, transport);
    }

    private CodeCheckConfig.JavaProject applyJava(Path configPath, Map<String, Object> node,
            CodeCheckConfig.JavaProject base) {
        warnUnknownKeys(node, JAVA_KEYS, configPath, "java");
        Integer languageLevel = integer(node, "languageLevel", configPath, "java.languageLevel");
        if (languageLevel != null && languageLevel < 8) {
            throw invalid(configPath, "java.languageLevel", "must be at least 8");
        }
        CodeCheckConfig.GeneratedSourceDetection generatedSourceDetection = enumValue(node, "generatedSourceDetection",
                configPath, "java.generatedSourceDetection", CodeCheckConfig.GeneratedSourceDetection.class,
                base.generatedSourceDetection());
        return new CodeCheckConfig.JavaProject(languageLevel == null ? base.languageLevel() : languageLevel,
                generatedSourceDetection);
    }

    private CodeCheckConfig.Maven applyMaven(Path configPath, Map<String, Object> node, CodeCheckConfig.Maven base) {
        warnUnknownKeys(node, MAVEN_KEYS, configPath, "maven");
        CodeCheckConfig.MavenRunner runner = enumValue(node, "runner", configPath, "maven.runner",
                CodeCheckConfig.MavenRunner.class, base.runner());
        Boolean preferMvnd = bool(node, "preferMvnd", configPath, "maven.preferMvnd");
        Map<String, Object> docker = object(node, "docker", configPath, "maven.docker");
        CodeCheckConfig.Docker dockerConfig = docker == null ? base.docker()
                : applyDocker(configPath, docker, base.docker());
        List<String> goals = stringList(node, "goals", configPath, "maven.goals", base.goals());
        List<String> args = stringList(node, "args", configPath, "maven.args", base.args());
        String targetedTestProperty = string(node, "targetedTestProperty", configPath, "maven.targetedTestProperty",
                base.targetedTestProperty());
        return new CodeCheckConfig.Maven(runner, preferMvnd == null ? base.preferMvnd() : preferMvnd, dockerConfig,
                goals, args, targetedTestProperty);
    }

    private CodeCheckConfig.Docker applyDocker(Path configPath, Map<String, Object> node, CodeCheckConfig.Docker base) {
        warnUnknownKeys(node, DOCKER_KEYS, configPath, "maven.docker");
        String image = string(node, "image", configPath, "maven.docker.image", base.image());
        Duration containerIdleTimeout = duration(node, "containerIdleTimeout", configPath,
                "maven.docker.containerIdleTimeout", base.containerIdleTimeout());
        Boolean mountM2 = bool(node, "mountM2", configPath, "maven.docker.mountM2");
        return new CodeCheckConfig.Docker(image, containerIdleTimeout, mountM2 == null ? base.mountM2() : mountM2);
    }

    private CodeCheckConfig.Coverage applyCoverage(Path configPath, Map<String, Object> node,
            CodeCheckConfig.Coverage base) {
        warnUnknownKeys(node, COVERAGE_KEYS, configPath, "coverage");
        CodeCheckConfig.CoverageProvider provider = enumValue(node, "provider", configPath, "coverage.provider",
                CodeCheckConfig.CoverageProvider.class, base.provider());
        CodeCheckConfig.CoverageFreshnessMode freshnessMode = enumValue(node, "freshnessMode", configPath,
                "coverage.freshnessMode", CodeCheckConfig.CoverageFreshnessMode.class, base.freshnessMode());
        List<String> reportPaths = stringList(node, "reportPaths", configPath, "coverage.reportPaths",
                base.reportPaths());
        return new CodeCheckConfig.Coverage(provider, freshnessMode, reportPaths);
    }

    private Map<String, Object> object(Map<String, Object> parent, String field, Path configPath, String path) {
        Object node = parent.get(field);
        if (node == null) {
            return null;
        }
        if (!(node instanceof Map<?, ?> map)) {
            throw invalid(configPath, path, "expected an object");
        }
        return stringKeyMap(map, configPath, path);
    }

    private Map<String, Object> stringKeyMap(Map<?, ?> map, Path configPath, String path) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw invalid(configPath, path, "expected string keys");
            }
            result.put(key, entry.getValue());
        }
        return result;
    }

    private String string(Map<String, Object> parent, String field, Path configPath, String path, String fallback) {
        Object node = parent.get(field);
        if (node == null) {
            return fallback;
        }
        if (!(node instanceof String value)) {
            throw invalid(configPath, path, "expected a string");
        }
        return value;
    }

    private List<String> stringList(Map<String, Object> parent, String field, Path configPath, String path,
            List<String> fallback) {
        Object node = parent.get(field);
        if (node == null) {
            return fallback;
        }
        if (!(node instanceof List<?> list)) {
            throw invalid(configPath, path, "expected a list of strings");
        }
        List<String> values = new ArrayList<>();
        for (Object element : list) {
            if (!(element instanceof String value)) {
                throw invalid(configPath, path, "expected a list of strings");
            }
            values.add(value);
        }
        return List.copyOf(values);
    }

    private Boolean bool(Map<String, Object> parent, String field, Path configPath, String path) {
        Object node = parent.get(field);
        if (node == null) {
            return null;
        }
        if (!(node instanceof Boolean value)) {
            throw invalid(configPath, path, "expected true or false");
        }
        return value;
    }

    private Integer integer(Map<String, Object> parent, String field, Path configPath, String path) {
        Object node = parent.get(field);
        if (node == null) {
            return null;
        }
        if (!(node instanceof Integer value)) {
            throw invalid(configPath, path, "expected an integer");
        }
        return value;
    }

    private Duration duration(Map<String, Object> parent, String field, Path configPath, String path,
            Duration fallback) {
        Object node = parent.get(field);
        if (node == null) {
            return fallback;
        }
        if (!(node instanceof String value)) {
            throw invalid(configPath, path, "expected a duration string");
        }
        return parseDuration(configPath, path, value);
    }

    private String pattern(Map<String, Object> parent, String field, Path configPath, String path, String fallback) {
        String value = string(parent, field, configPath, path, null);
        if (value == null) {
            return fallback;
        }
        try {
            Pattern.compile(value);
        }
        catch (PatternSyntaxException e) {
            throw invalid(configPath, path, "invalid regular expression: " + e.getDescription());
        }
        return value;
    }

    private <T extends Enum<T>> T enumValue(Map<String, Object> parent, String field, Path configPath, String path,
            Class<T> enumType, T fallback) {
        String value = string(parent, field, configPath, path, null);
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        try {
            return Enum.valueOf(enumType, normalized);
        }
        catch (IllegalArgumentException e) {
            throw invalid(configPath, path, "unknown value '" + value + "'");
        }
    }

    private Duration parseDuration(Path configPath, String path, String value) {
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        try {
            if (trimmed.endsWith("ms")) {
                return Duration.ofMillis(longPrefix(trimmed, 2));
            }
            if (trimmed.endsWith("s")) {
                return Duration.ofSeconds(longPrefix(trimmed, 1));
            }
            if (trimmed.endsWith("m")) {
                return Duration.ofMinutes(longPrefix(trimmed, 1));
            }
            if (trimmed.endsWith("h")) {
                return Duration.ofHours(longPrefix(trimmed, 1));
            }
            return Duration.parse(value);
        }
        catch (RuntimeException e) {
            throw invalid(configPath, path, "invalid duration '" + value + "'");
        }
    }

    private long longPrefix(String value, int suffixLength) {
        return Long.parseLong(value.substring(0, value.length() - suffixLength));
    }

    private void warnUnknownKeys(Map<String, Object> node, Set<String> knownKeys, Path configPath, String path) {
        for (String key : node.keySet()) {
            if (!knownKeys.contains(key)) {
                warningConsumer
                    .accept("Config warning in " + configPath + " at " + path + ": unknown key '" + key + "'");
            }
        }
    }

    private ConfigException invalid(Path configPath, String path, String message) {
        return new ConfigException("Invalid config " + configPath + " at " + path + ": " + message);
    }

}
