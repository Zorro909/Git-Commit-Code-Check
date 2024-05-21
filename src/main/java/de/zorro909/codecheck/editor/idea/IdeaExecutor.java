package de.zorro909.codecheck.editor.idea;


import com.github.javaparser.Position;
import de.zorro909.codecheck.editor.EditorExecutor;

import java.io.IOException;
import java.nio.file.Path;

public class IdeaExecutor implements EditorExecutor {

    private final String ideaExecutable;

    public IdeaExecutor(String ideaExecutable) {
        this.ideaExecutable = ideaExecutable;
    }

    @Override
    public boolean open(Path path, Position pos) {
        String[] params = new String[]{ideaExecutable, "--line", String.valueOf(
                pos.line), "--column", String.valueOf(pos.column), path.toString()};

        return exec(params);
    }

    @Override
    public boolean openAndWait(Path path, Position pos) {
        String[] params = new String[]{ideaExecutable, "--wait", "--line", String.valueOf(
                pos.line), "--column", String.valueOf(pos.column), path.toString()};

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
