package de.zorro909.codecheck.core.changeset;

import java.nio.file.Path;

public record ChangeSetEntry(Path path,
                             GitFileStatus status,
                             boolean staged,
                             boolean unstaged,
                             boolean untracked,
                             boolean deleted,
                             String originReason) {
}
