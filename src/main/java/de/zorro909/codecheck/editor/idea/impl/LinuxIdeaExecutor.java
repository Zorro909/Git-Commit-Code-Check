package de.zorro909.codecheck.editor.idea.impl;

import de.zorro909.codecheck.editor.idea.IdeaExecutor;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;


/**
 * LinuxIdeaExecutor serves as a factory class for creating instances of IdeaExecutor on a Linux operating system.
 * <p>
 * The application must run on a Linux OS.
 *
 * @see de.zorro909.codecheck.editor.idea.IdeaExecutor
 */
@Factory
public class LinuxIdeaExecutor {

    private static final String LINUX_INTELLIJ_IDEA_EXECUTABLE = "idea";


    /**
     * Factory method for creating an instance of {@link IdeaExecutor} on a Linux operating system.
     * The application must run on a Linux OS.
     *
     * @return An instance of {@link IdeaExecutor} configured for Linux.
     *
     * @see LinuxIdeaExecutor
     * @see IdeaExecutor
     */
    @Singleton
    @Requires(os = Requires.Family.LINUX)
    public IdeaExecutor linuxIdeaExecutor() {
        return new IdeaExecutor(LINUX_INTELLIJ_IDEA_EXECUTABLE);
    }
}



