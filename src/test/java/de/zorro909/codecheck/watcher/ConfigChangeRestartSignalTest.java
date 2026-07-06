package de.zorro909.codecheck.watcher;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigChangeRestartSignalTest {

    @Test
    void configChangeRaisesRestartRequired() {
        Path config = Path.of(".codecheck.yaml");
        ConfigChangeRestartSignal signal = new ConfigChangeRestartSignal(Set.of(config));

        assertThatThrownBy(() -> signal.handleChange(config))
                .isInstanceOf(DaemonRestartRequiredException.class)
                .hasMessageContaining("daemon restart required");
    }

    @Test
    void nonConfigChangeDoesNotRaiseRestartRequired() {
        ConfigChangeRestartSignal signal = new ConfigChangeRestartSignal(Set.of(Path.of(".codecheck.yaml")));

        assertThatCode(() -> signal.handleChange(Path.of("src/main/java/Example.java")))
                .doesNotThrowAnyException();
    }
}
