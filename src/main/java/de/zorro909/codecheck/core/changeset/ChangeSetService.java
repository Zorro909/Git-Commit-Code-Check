package de.zorro909.codecheck.core.changeset;

import java.nio.file.Path;
import java.util.Collection;

public interface ChangeSetService {

    ChangeSet currentAssistantChangeSet();

    ChangeSet currentInteractiveCheckChangeSet();

    ChangeSet preCommitChangeSet();

    ChangeSet explicitFiles(Collection<Path> files);
}
