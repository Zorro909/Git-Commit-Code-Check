package de.zorro909.codecheck.legacy.actions;

import java.nio.file.Path;
import java.util.Set;

public interface PostAction {

    boolean perform(Set<Path> files);

}
