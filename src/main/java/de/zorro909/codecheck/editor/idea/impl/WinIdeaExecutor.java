package de.zorro909.codecheck.editor.idea.impl;

import de.zorro909.codecheck.editor.idea.IdeaExecutor;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;

/**
 * The {@code WinIdeaExecutor} class is a factory for creating an instance of {@code IdeaExecutor} on
 * Windows operating system.
 * <p>
 * It provides methods to open and open and wait for IDE editor at a specific position in a file.
 * The class initializes the {@code IdeaExecutor} with the name of the IDEA executable for Windows.
 * </p>
 *
 * @see IdeaExecutor
 * @see EditorExecutor
 */
@Factory
public class WinIdeaExecutor {

    private final static String IDEA_WIN = "idea64.exe";

    /**
     * Creates an instance of {@code IdeaExecutor} specifically for the Windows operating system.
     *
     * @return an instance of {@code IdeaExecutor} for Windows
     * @see IdeaExecutor
     *
     */
    @Bean
    @Requires(os = Requires.Family.WINDOWS)
    public IdeaExecutor windowsIdeaExecutor() {
        return new IdeaExecutor(IDEA_WIN);
    }
}
