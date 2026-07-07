package de.zorro909.codecheck.daemon;

import de.zorro909.codecheck.RepositoryPathProvider;
import de.zorro909.codecheck.ValidationCheckPipeline;
import de.zorro909.codecheck.config.CodeCheckConfig;
import de.zorro909.codecheck.config.CodeCheckConfigLoader;
import de.zorro909.codecheck.config.ConfigOverrides;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FileWatcherConfigTest {

    @Test
    void watchResolvesSaveDebounceFromConfigOnce(@TempDir Path tempDir) throws Exception {
        AtomicInteger loadCount = new AtomicInteger();
        CodeCheckConfigLoader countingLoader = new CodeCheckConfigLoader() {
            @Override
            public CodeCheckConfig load() {
                loadCount.incrementAndGet();
                return CodeCheckConfig.defaults();
            }

            @Override
            public CodeCheckConfig load(ConfigOverrides overrides) {
                return load();
            }
        };
        FileWatcher watcher = new FileWatcher(daemonServer(),
                                              repositoryPathProvider(tempDir),
                                              countingLoader);

        watcher.watch();

        assertThat(loadCount).hasValue(1);
    }

    private RepositoryPathProvider repositoryPathProvider(Path directory) {
        return new RepositoryPathProvider() {
            @Override
            public Path repositoryDirectory() {
                return directory;
            }
        };
    }

    private DaemonServer daemonServer() {
        return new DaemonServer(Stream::empty, () -> {
            ValidationCheckPipeline pipeline = new ValidationCheckPipeline();
            setField(pipeline, "codeChecker", List.of());
            return pipeline;
        });
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
