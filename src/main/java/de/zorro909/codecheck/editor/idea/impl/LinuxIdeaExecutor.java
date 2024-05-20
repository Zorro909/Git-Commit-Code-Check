package de.zorro909.codecheck.editor.idea.impl;

import de.zorro909.codecheck.editor.idea.IdeaExecutor;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

@Singleton
@Requires(os = Requires.Family.LINUX)
public class LinuxIdeaExecutor extends IdeaExecutor {

    private final static String LIN_IDEA = "idea";

    @Override
    protected String getIdeaExecutable() {
        return LIN_IDEA;
    }
}
