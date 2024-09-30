package de.zorro909.codecheck.actions.post;

import de.zorro909.codecheck.actions.PostAction;
import de.zorro909.codecheck.checks.ValidationError;
import de.zorro909.codecheck.selector.impl.GitDiffSelector;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Order;
import jakarta.inject.Singleton;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Order(99)
@Singleton
@Requires(bean = GitDiffSelector.class)
public class GitStageAction implements PostAction {

    private final Path repositoryPath;

    public GitStageAction(Path repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    @Override
    public boolean perform(Map<Path, List<ValidationError>> validationErrors) {
        Set<Path> files = validationErrors.keySet();

        int startIndex = 0;
        int filesPerBatch = 10; // number of files to process at a time

        while (startIndex < files.size()) {
            try {
                List<String> commandList = new ArrayList<>();
                commandList.add("git");
                commandList.add("add");

                // Constructing the command list
                files.stream()
                     .skip(startIndex)
                     .limit(filesPerBatch)
                     .map(Path::toString)
                     .forEach(commandList::add);

                ProcessBuilder builder = new ProcessBuilder(commandList);
                builder.directory(repositoryPath.toAbsolutePath().toFile());
                Process process = builder.start();
                process.waitFor();
                startIndex += filesPerBatch;
            } catch (Exception ex) {
                if (filesPerBatch > 1) {
                    filesPerBatch /= 2; // decrease the number of files to process at a time
                } else {
                    System.err.println("Failed to add file");
                    ex.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }
}
