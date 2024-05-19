package de.zorro909.codecheck;

import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class is a JUnit test class for the GitCommitCodeCheckCommand class.
 */
@Tests(GitCommitCodeCheckCommand.class)
class GitCommitCodeCheckCommandTest extends TestBase {

    @Test
    public void testWithCommandLineOption() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
//
        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            String[] args = new String[]{"-v", "--no-exit-code"};
            PicocliRunner.run(GitCommitCodeCheckCommand.class, ctx, args);

            // git-commit-code-check
            assertTrue(baos.toString().contains("Hi!"));
        }
    }
}
