package de.zorro909.codecheck.editor.idea;


import de.zorro909.codecheck.editor.EditorExecutor;

import java.io.IOException;
import java.nio.file.Path;


public abstract class IdeaExecutor implements EditorExecutor {

    protected abstract String getIdeaExecutable();

    @Override
    public boolean open(Path path, Integer line) {
        String[] params = new String[]{getIdeaExecutable(), "--line", String.valueOf(
                line), path.toString()};

        return exec(params);
    }

    @Override
    public boolean openAndWait(Path path, Integer line) {
        String[] params = new String[]{getIdeaExecutable(), "--wait", "--line", String.valueOf(
                line), path.toString()};

        return exec(params);
    }

    private boolean exec(String[] params) {
        try {
            Process process = new ProcessBuilder(params).start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

}
