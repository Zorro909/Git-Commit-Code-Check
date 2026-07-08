package de.zorro909.codecheck.infra.docker;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ProcessDockerCommandExecutorTest {

    @Test
    void largeStderrOutputDoesNotDeadlockCommandExecution() {
        assumeTrue(!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win"));
        ProcessDockerCommandExecutor executor = new ProcessDockerCommandExecutor();

        CommandResult result = assertTimeoutPreemptively(Duration.ofSeconds(20),
                () -> executor.run(List.of("/bin/sh", "-c",
                        "i=0; while [ $i -lt 4000 ]; do" + " echo 'stderr filler line to exceed the pipe buffer' 1>&2;"
                                + " i=$((i+1)); done; echo done")));

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("done");
        assertThat(result.stderr()).contains("stderr filler line");
    }

}
