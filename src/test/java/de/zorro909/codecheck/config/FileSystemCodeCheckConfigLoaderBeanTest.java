package de.zorro909.codecheck.config;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileSystemCodeCheckConfigLoaderBeanTest {

    @Test
    void configLoaderBeanResolvesToFileSystemLoader() {
        try (ApplicationContext context = ApplicationContext.run()) {
            CodeCheckConfigLoader loader = context.getBean(CodeCheckConfigLoader.class);

            assertThat(loader).isInstanceOf(FileSystemCodeCheckConfigLoader.class);
        }
    }
}
