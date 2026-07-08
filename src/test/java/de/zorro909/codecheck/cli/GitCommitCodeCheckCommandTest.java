package de.zorro909.codecheck.cli;

import de.zorro909.codecheck.cli.CodeCheckCommandService;
import de.zorro909.codecheck.cli.CommandOutcome;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

class GitCommitCodeCheckCommandTest {

    @Test
    void bareCommandRoutesToAssistantDaemon() {
        RecordingCommandService service = new RecordingCommandService();

        int exitCode = commandLine(service).execute();

        assertThat(exitCode).isZero();
        assertThat(service.calledMode).isEqualTo("daemon");
    }

    @Test
    void checkCommandRoutesToInteractiveCheck() {
        RecordingCommandService service = new RecordingCommandService();

        int exitCode = commandLine(service).execute("check");

        assertThat(exitCode).isZero();
        assertThat(service.calledMode).isEqualTo("interactive");
    }

    @Test
    void batchCheckRoutesToBatchCheck() {
        RecordingCommandService service = new RecordingCommandService();

        int exitCode = commandLine(service).execute("check", "--batch");

        assertThat(exitCode).isZero();
        assertThat(service.calledMode).isEqualTo("batch");
    }

    @Test
    void preCommitCommandRoutesToPreCommit() {
        RecordingCommandService service = new RecordingCommandService();

        int exitCode = commandLine(service).execute("pre-commit");

        assertThat(exitCode).isZero();
        assertThat(service.calledMode).isEqualTo("pre-commit");
    }

    @Test
    void statusCommandRoutesToStatus() {
        RecordingCommandService service = new RecordingCommandService();

        int exitCode = commandLine(service).execute("status");

        assertThat(exitCode).isZero();
        assertThat(service.calledMode).isEqualTo("status");
    }

    @Test
    void fixCommandRoutesToExplicitFix() {
        RecordingCommandService service = new RecordingCommandService();

        int exitCode = commandLine(service).execute("fix", "diagnostic-1");

        assertThat(exitCode).isZero();
        assertThat(service.calledMode).isEqualTo("fix:diagnostic-1");
    }

    private CommandLine commandLine(RecordingCommandService service) {
        GitCommitCodeCheckCommand command = new GitCommitCodeCheckCommand();
        command.commandService = service;
        return new CommandLine(command);
    }

    private static final class RecordingCommandService extends CodeCheckCommandService {

        private String calledMode;

        private RecordingCommandService() {
            super(null, null, null);
        }

        @Override
        public CommandOutcome startAssistantDaemon() {
            calledMode = "daemon";
            return CommandOutcome.success();
        }

        @Override
        public CommandOutcome runInteractiveCheck(boolean noExitCode) {
            calledMode = "interactive";
            return CommandOutcome.success();
        }

        @Override
        public CommandOutcome runBatchCheck() {
            calledMode = "batch";
            return CommandOutcome.success();
        }

        @Override
        public CommandOutcome runPreCommit() {
            calledMode = "pre-commit";
            return CommandOutcome.success();
        }

        @Override
        public CommandOutcome printStatus() {
            calledMode = "status";
            return CommandOutcome.success();
        }

        @Override
        public CommandOutcome applyFix(String diagnosticId) {
            calledMode = "fix:" + diagnosticId;
            return CommandOutcome.success();
        }

    }

}
