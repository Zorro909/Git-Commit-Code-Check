package de.zorro909.codecheck.command;

import de.zorro909.codecheck.config.CodeCheckConfig;
import de.zorro909.codecheck.config.CodeCheckConfigLoader;
import de.zorro909.codecheck.daemon.DaemonServer;
import de.zorro909.codecheck.daemon.DaemonMetadata;
import de.zorro909.codecheck.daemon.DaemonProcessRegistry;
import jakarta.inject.Singleton;

import java.io.PrintStream;

@Singleton
public class LocalAssistantDaemonController implements AssistantDaemonController {

    private final DaemonServer daemonServer;
    private final DaemonProcessRegistry daemonProcessRegistry;
    private final CodeCheckConfigLoader configLoader;

    public LocalAssistantDaemonController(DaemonServer daemonServer,
                                          DaemonProcessRegistry daemonProcessRegistry,
                                          CodeCheckConfigLoader configLoader) {
        this.daemonServer = daemonServer;
        this.daemonProcessRegistry = daemonProcessRegistry;
        this.configLoader = configLoader;
    }

    @Override
    public void startOrAttach() throws Exception {
        if (daemonProcessRegistry.aliveMetadata().isPresent()) {
            return;
        }

        CodeCheckConfig config = configLoader.load();
        DaemonMetadata metadata = daemonProcessRegistry.createMetadata();
        daemonProcessRegistry.write(metadata);
        try {
            daemonServer.run(metadata, config.daemon().inactivityTimeout());
        } catch (Exception e) {
            daemonProcessRegistry.clear();
            throw e;
        }
    }

    @Override
    public void printStatus(PrintStream out) {
        daemonProcessRegistry.aliveMetadata()
                             .ifPresentOrElse(
                                     metadata -> out.println("Assistant daemon running on "
                                                             + metadata.host() + ":"
                                                             + metadata.port()),
                                     () -> out.println("Assistant daemon is not running."));
    }

    @Override
    public void applyFix(String diagnosticId) {
        throw new UnsupportedOperationException(
                "Daemon-backed fix selection is not available yet for diagnostic " + diagnosticId);
    }
}
