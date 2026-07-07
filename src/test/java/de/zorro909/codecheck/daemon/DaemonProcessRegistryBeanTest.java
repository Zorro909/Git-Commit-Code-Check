package de.zorro909.codecheck.daemon;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DaemonProcessRegistryBeanTest {

    @Test
    void registryBeanResolvesWithRepositoryDirectory() {
        try (ApplicationContext context = ApplicationContext.run()) {
            DaemonProcessRegistry registry = context.getBean(DaemonProcessRegistry.class);

            assertThat(registry.repoId()).matches("[0-9a-f]{64}");
        }
    }

}
