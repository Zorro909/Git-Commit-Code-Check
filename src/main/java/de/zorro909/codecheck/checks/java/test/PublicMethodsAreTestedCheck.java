package de.zorro909.codecheck.checks.java.test;

import jakarta.inject.Singleton;
import lombok.experimental.ExtensionMethod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.google.common.collect.Lists;

import de.zorro909.codecheck.FileLoader;
import de.zorro909.codecheck.checks.ValidationError;
import de.zorro909.codecheck.checks.java.JavaChecker;
import de.zorro909.codecheck.utils.CompilationUnitExtensions;
import de.zorro909.codecheck.utils.MethodDeclarationExtensions;

/**
 * Checks that all Public Methods inside Impl Classes have Tests
 */
@ExtensionMethod(CompilationUnitExtensions.class)
@Singleton
public class PublicMethodsAreTestedCheck extends JavaChecker {

    protected PublicMethodsAreTestedCheck(FileLoader fileLoader) {
        super(fileLoader);
    }

    @Override
    public boolean isJavaResponsible(Path path) {
        return path.endsWith("Impl.java");
    }

    @Override
    public List<ValidationError> check(CompilationUnit javaUnit) {

        List<MethodDeclaration> publicMethods =
            javaUnit.findAll(MethodDeclaration.class).stream().filter(MethodDeclaration::isPublic).filter(
                Predicate.not(MethodDeclarationExtensions::isSimpleGetterOrSetter)).toList();

        Path filePath = getPath(javaUnit);
        filePath =
            Paths.get(filePath.toString().replace("main", "test").replace("Impl.java", "ImplTest.java"));

        if (!Files.exists(filePath)) {
            return Lists.newArrayList();
        }

        ParseResult<CompilationUnit> testUnitResult = load(filePath);
        if (!testUnitResult.isSuccessful()) {
            return Lists.newArrayList();
        }

        CompilationUnit testUnit = testUnitResult.getResult().get();

        testUnit.findAll(MethodCallExpr.class).stream().filter(MethodCallExpr::hasScope).forEach(callExpr -> {
            String name = callExpr.getNameAsString();
            Iterator<MethodDeclaration> methodDeclarationIterator = publicMethods.iterator();
            while (methodDeclarationIterator.hasNext()) {
                MethodDeclaration methodDeclaration = methodDeclarationIterator.next();
                if (methodDeclaration.getNameAsString().equals(name)) {
                    if (methodDeclaration.getTypeParameters().size() == callExpr.getTypeArguments().map(
                        NodeList::size).orElse(0)) {
                        methodDeclarationIterator.remove();
                        return;
                    }
                }
            }
        });

        return publicMethods.stream().map(method ->
            javaUnit.validationError(method, ValidationError.Severity.MEDIUM,
                "Method '" + method.getNameAsString()
                    + "' is not used in any Tests! Probably missing coverage.")
        ).toList();
    }
}
