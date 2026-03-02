package de.zorro909.codecheck.checks.java.test;

import com.github.javaparser.ast.CompilationUnit;
import de.zorro909.codecheck.FileLoader;
import de.zorro909.codecheck.checks.ValidationError;
import de.zorro909.codecheck.checks.java.JavaChecker;
import de.zorro909.codecheck.utils.CompilationUnitExtensions;
import jakarta.inject.Singleton;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The TestClassCheck class is responsible for checking Java test classes
 * for specific conditions and validating them.
 */
@Singleton
public class TestClassCheck extends JavaChecker {

    private static final String TEST_FOLDER = "src" + File.separatorChar + "test" + File.separatorChar + "java";
    public static final String JAVA_TEST_FILE_ENDING = "Test.java";
    public static final String TEST_CLASS_SUFFIX = "Test";
    public static final String TESTS_ANNOTATION_NAME = "Tests";
    public static final String ERROR_TEST_CLASS_SHOULD_EXTEND = "TestClass should extend a class.";
    public static final String ERROR_TEST_CLASS_TESTS_ANNOTATION = "Test Class should contain a @Tests annotation.";

    public TestClassCheck(FileLoader fileLoader) {
        super(fileLoader);
    }

    @Override
    public boolean isJavaResponsible(Path path) {
        return path.toString().contains(TEST_FOLDER) && path.toString()
                                                            .endsWith(JAVA_TEST_FILE_ENDING);
    }

    /**
     * Checks the specified CompilationUnit for specific conditions and returns a list of validation errors.
     *
     * @param javaUnit The CompilationUnit to be checked.
     * @return A list of ValidationErrors representing the errors found during validation.
     */
    @Override
    public List<ValidationError> check(CompilationUnit javaUnit) {
        List<ValidationError> errors = new ArrayList<>();

        // Creates a HIGH severity Validation Erroor for Test Classes that do not extend another
        // Class
        CompilationUnitExtensions.findAllClassesWithEnds(javaUnit, TEST_CLASS_SUFFIX)
                .filter(type -> type.getExtendedTypes().isEmpty())
                .map(type -> CompilationUnitExtensions.validationError(javaUnit, type,
                        ValidationError.Severity.HIGH, ERROR_TEST_CLASS_SHOULD_EXTEND))
                .forEach(errors::add);

        // Creates a HIGH severity Validation Error for Test Classes without the @Tests annotation
        CompilationUnitExtensions.findAllClassesWithEnds(javaUnit, TEST_CLASS_SUFFIX)
                .filter(type -> type.getAnnotationByName(TESTS_ANNOTATION_NAME).isEmpty())
                .map(type -> CompilationUnitExtensions.validationError(javaUnit, type,
                        ValidationError.Severity.HIGH, ERROR_TEST_CLASS_TESTS_ANNOTATION))
                .forEach(errors::add);

        return errors;
    }
}
