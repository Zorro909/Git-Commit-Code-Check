package de.zorro909.codecheck.legacy.checks.java.test;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import de.zorro909.codecheck.legacy.FileLoader;
import de.zorro909.codecheck.core.diagnostic.ValidationError;
import de.zorro909.codecheck.legacy.checks.java.JavaChecker;
import de.zorro909.codecheck.java.JavaParserService;
import de.zorro909.codecheck.java.ParseOutcome;
import de.zorro909.codecheck.legacy.utils.CompilationUnitExtensions;
import de.zorro909.codecheck.legacy.utils.MethodDeclarationExtensions;

/**
 * Checks that all Public Methods inside Impl Classes have Tests
 */
@Singleton
public class PublicMethodsAreTestedCheck extends JavaChecker {

    protected PublicMethodsAreTestedCheck(FileLoader fileLoader) {
        super(fileLoader);
    }

    @Inject
    public PublicMethodsAreTestedCheck(FileLoader fileLoader, JavaParserService javaParserService) {
        super(fileLoader, javaParserService);
    }

    @Override
    public boolean isJavaResponsible(Path path) {
        return path.endsWith("Impl.java");
    }

    @Override
    public List<ValidationError> check(CompilationUnit javaUnit) {

        List<MethodDeclaration> publicMethods = javaUnit.findAll(MethodDeclaration.class)
            .stream()
            .filter(MethodDeclaration::isPublic)
            .filter(Predicate.not(MethodDeclarationExtensions::isSimpleGetterOrSetter))
            .toList();

        Path filePath = getPath(javaUnit);
        filePath = Path.of(filePath.toString().replace("main", "test").replace("Impl.java", "ImplTest.java"));

        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }

        ParseOutcome testUnitResult = load(filePath);
        if (testUnitResult.compilationUnit().isEmpty()) {
            return new ArrayList<>();
        }

        CompilationUnit testUnit = testUnitResult.compilationUnit().get();

        testUnit.findAll(MethodCallExpr.class).stream().filter(MethodCallExpr::hasScope).forEach(callExpr -> {
            String name = callExpr.getNameAsString();
            Iterator<MethodDeclaration> methodDeclarationIterator = publicMethods.iterator();
            while (methodDeclarationIterator.hasNext()) {
                MethodDeclaration methodDeclaration = methodDeclarationIterator.next();
                if (methodDeclaration.getNameAsString().equals(name)) {
                    if (methodDeclaration.getTypeParameters()
                        .size() == callExpr.getTypeArguments().map(NodeList::size).orElse(0)) {
                        methodDeclarationIterator.remove();
                        return;
                    }
                }
            }
        });

        return publicMethods.stream()
            .map(method -> CompilationUnitExtensions.validationError(javaUnit, method, ValidationError.Severity.MEDIUM,
                    "Method '" + method.getNameAsString() + "' is not used in any Tests! Probably missing coverage."))
            .toList();
    }

}
