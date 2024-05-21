package de.zorro909.codecheck.checks.java.test;

import com.github.javaparser.Position;
import de.zorro909.codecheck.RequiresCliOption;
import de.zorro909.codecheck.checks.CodeCheck;
import de.zorro909.codecheck.checks.ValidationError;
import jakarta.inject.Singleton;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Singleton
@RequiresCliOption("--experimental")
public class ImplClassesHaveTestsCheck implements CodeCheck {

    private static final String MAIN_FOLDER = "src" + File.separatorChar + "main" + File.separatorChar + "java";
    private static final String TEST_FOLDER = "src" + File.separatorChar + "test" + File.separatorChar + "java";

    @Override
    public boolean isResponsible(Path path) {
        return path.toString().contains(MAIN_FOLDER) && path.toString().endsWith("Impl.java");
    }

    @Override
    public List<ValidationError> check(Path file) {
        String filePath = file.toAbsolutePath().toString();
        String testFilePath = filePath.replace(MAIN_FOLDER, TEST_FOLDER)
                                      .replace("Impl.java", "ImplTest.java");

        Path testFile = Paths.get(testFilePath);
        if (!Files.exists(testFile)) {
            ValidationError noTestClassError = new ValidationError(file,
                                                                   "The Implementation " + "Class '" + file.getFileName()
                                                                                                           .toString() + "' has no Tests!",
                                                                   new Position(1, 1),
                                                                   ValidationError.Severity.MEDIUM);
            return List.of(noTestClassError);
        }

        return List.of();
    }

    @Override
    public void resetCache(Path file) {
    }
}
