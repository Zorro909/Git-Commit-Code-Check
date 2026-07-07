package de.zorro909.codecheck.core.changeset;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public record ChangeSet(List<ChangeSetEntry> entries) {

    public ChangeSet {
        entries = List.copyOf(entries);
    }

    public static ChangeSet empty() {
        return new ChangeSet(List.of());
    }

    public Stream<Path> paths() {
        return entries.stream()
                      .filter(entry -> !entry.deleted())
                      .map(ChangeSetEntry::path)
                      .distinct();
    }
}
