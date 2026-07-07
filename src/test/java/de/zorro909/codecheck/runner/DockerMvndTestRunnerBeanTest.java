package de.zorro909.codecheck.runner;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DockerMvndTestRunnerBeanTest {

    @Test
    void testRunnerBeanResolvesDockerImplementation() {
        try (ApplicationContext context = ApplicationContext.run()) {
            TestRunner runner = context.getBean(TestRunner.class);

            assertThat(runner).isInstanceOf(DockerMvndTestRunner.class);
        }
    }
}
