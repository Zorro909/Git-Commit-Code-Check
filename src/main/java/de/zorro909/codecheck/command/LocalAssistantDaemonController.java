package de.zorro909.codecheck.command;

import de.zorro909.codecheck.daemon.DaemonServer;
import jakarta.inject.Singleton;

import java.io.PrintStream;

@Singleton
public class LocalAssistantDaemonController implements AssistantDaemonController {

    private final DaemonServer daemonServer;

    public LocalAssistantDaemonController(DaemonServer daemonServer) {
        this.daemonServer = daemonServer;
    }

    @Override
    public void startOrAttach() throws Exception {
        daemonServer.run();
    }

    @Override
    public void printStatus(PrintStream out) {
        out.println("Assistant daemon status is not available yet.");
    }

    @Override
    public void applyFix(String diagnosticId) {
        throw new UnsupportedOperationException(
                "Daemon-backed fix selection is not available yet for diagnostic " + diagnosticId);
    }
}
