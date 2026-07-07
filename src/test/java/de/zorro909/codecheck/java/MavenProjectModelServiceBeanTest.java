package de.zorro909.codecheck.java;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MavenProjectModelServiceBeanTest {

    @Test
    void projectModelServiceBeanResolvesMavenImplementation() {
        try (ApplicationContext context = ApplicationContext.run()) {
            ProjectModelService service = context.getBean(ProjectModelService.class);

            assertThat(service).isInstanceOf(MavenProjectModelService.class);
        }
    }

}
