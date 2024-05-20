package de.zorro909.codecheck.editor.idea.impl;

import de.zorro909.codecheck.editor.idea.IdeaExecutor;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

@Singleton
@Requires(os = Requires.Family.WINDOWS)
public class WinIdeaExecutor extends IdeaExecutor {

    private final static String IDEA_WIN = "idea64.exe";

    @Override
    protected String getIdeaExecutable() {
        return IDEA_WIN;
    }
}
