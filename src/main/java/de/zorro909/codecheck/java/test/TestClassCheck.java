package de.zorro909.codecheck.java.test;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import de.zorro909.codecheck.ValidationError;
import de.zorro909.codecheck.java.JavaChecker;
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

    @Override
    public boolean isJavaResponsible(Path path) {
        return path.toString().contains(TEST_FOLDER) && path.toString().endsWith("Test.java");
    }

    @Override
    public List<ValidationError> check(CompilationUnit javaUnit) {
        List<ValidationError> errors = new ArrayList<>();


        javaUnit.findAll(ClassOrInterfaceDeclaration.class,
                         decl -> decl.getNameAsString().endsWith("Test")).forEach(type -> {
            if (type.getExtendedTypes().isEmpty()) {
                errors.add(new ValidationError(javaUnit.getStorage().get().getPath(),
                                               "TestClass should extend a class.",
                                               type.getBegin().get().line,
                                               ValidationError.Severity.HIGH));
            }

            boolean containsTestsAnnotation = type.getAnnotationByName("Tests").isPresent();

            if (!containsTestsAnnotation) {
                errors.add(new ValidationError(javaUnit.getStorage().get().getPath(),
                                               "Test Class should contain a @Tests annotation.",
                                               type.getBegin().get().line,
                                               ValidationError.Severity.HIGH));
            }
        });

        return errors;
    }
}
