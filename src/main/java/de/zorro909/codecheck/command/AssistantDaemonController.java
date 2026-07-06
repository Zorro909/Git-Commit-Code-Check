package de.zorro909.codecheck.command;

import java.io.PrintStream;

public interface AssistantDaemonController {

    void startOrAttach() throws Exception;

    void printStatus(PrintStream out);

    void applyFix(String diagnosticId) throws Exception;
}
