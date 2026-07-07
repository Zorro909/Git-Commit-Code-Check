package de.zorro909.codecheck.watcher;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WatchScopeServiceBeanTest {

    @Test
    void watchScopeServiceBeanResolvesWithRepositoryDirectory() {
        try (ApplicationContext context = ApplicationContext.builder().singletons((Object) new String[0]).start()) {
            WatchScopeService service = context.getBean(WatchScopeService.class);

            assertThat(service.watchScope().paths()).isNotEmpty();
        }
    }

}
