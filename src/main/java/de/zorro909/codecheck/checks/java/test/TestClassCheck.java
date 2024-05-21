package de.zorro909.codecheck.checks.java.test;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import de.zorro909.codecheck.checks.ValidationError;
import de.zorro909.codecheck.checks.java.JavaChecker;
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

    @Override
    public boolean isJavaResponsible(Path path) {
        return path.toString().contains(TEST_FOLDER) && path.toString()
                                                            .endsWith(JAVA_TEST_FILE_ENDING);
    }

    @Override
    public List<ValidationError> check(CompilationUnit javaUnit) {
        List<ValidationError> errors = new ArrayList<>();


        javaUnit.findAll(ClassOrInterfaceDeclaration.class,
                         decl -> decl.getNameAsString().endsWith(TEST_CLASS_SUFFIX))
                .forEach(type -> {
                    if (type.getExtendedTypes().isEmpty()) {
                        errors.add(new ValidationError(getPath(javaUnit),
                                                       ERROR_TEST_CLASS_SHOULD_EXTEND,
                                                       type.getBegin(),
                                                       ValidationError.Severity.HIGH));
                    }

                    boolean containsTestsAnnotation = type.getAnnotationByName(
                            TESTS_ANNOTATION_NAME).isPresent();

                    if (!containsTestsAnnotation) {
                        errors.add(new ValidationError(getPath(javaUnit),
                                                       ERROR_TEST_CLASS_TESTS_ANNOTATION,
                                                       type.getBegin(),
                                                       ValidationError.Severity.HIGH));
                    }
                });

        return errors;
    }
}
