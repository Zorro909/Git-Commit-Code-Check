package de.zorro909.codecheck.infra.git;

import de.zorro909.codecheck.core.changeset.ChangeSetService;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GitChangeSetServiceBeanTest {

    @Test
    void changeSetServiceBeanResolvesGitImplementation() {
        try (ApplicationContext context = ApplicationContext.run()) {
            ChangeSetService service = context.getBean(ChangeSetService.class);

            assertThat(service).isInstanceOf(GitChangeSetService.class);
        }
    }

}
