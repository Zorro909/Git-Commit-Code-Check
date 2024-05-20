package de.zorro909.codecheck.checks.java.doc;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.ReturnStmt;
import de.zorro909.codecheck.checks.ValidationError;
import de.zorro909.codecheck.checks.java.JavaChecker;
import jakarta.inject.Singleton;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is responsible for checking the correctness of Java code by enforcing JavaDoc comments for
 * public classes and methods that are not getter or setter methods or overridden methods. It extends the
 * {@link JavaChecker} abstract class.
 */
@Singleton
public final class JavaDocCheck extends JavaChecker {

    private static final String MAIN_FOLDER = "src" + File.separatorChar + "main" + File.separatorChar + "java";

    @Override
    public boolean isJavaResponsible(Path path) {
        return path.toString().contains(MAIN_FOLDER);
    }

    @Override
    public List<ValidationError> check(CompilationUnit javaUnit) {
        List<ValidationError> errors = new LinkedList<>();

        javaUnit.findAll(ClassOrInterfaceDeclaration.class,
                         decl -> decl.getAccessSpecifier() == AccessSpecifier.PUBLIC)
                .stream()
                .filter(decl -> !decl.hasJavaDocComment())
                .filter(decl -> decl.getImplementedTypes().isEmpty())
                .forEach(decl -> {
                    int line = decl.getBegin().map(pos -> pos.line).orElse(-1);
                    errors.add(new ValidationError(javaUnit.getStorage().get().getPath(),
                                                   "Public class doesn't implement an interface and doesn't have a javadoc comment",
                                                   line, ValidationError.Severity.MEDIUM));
                });

        javaUnit.findAll(ClassOrInterfaceDeclaration.class,
                         decl -> decl.getAccessSpecifier() == AccessSpecifier.PUBLIC && decl.isInterface())
                .stream()
                .filter(decl -> !decl.getNameAsString().endsWith("Mapper"))
                .flatMap(decl -> decl.findAll(MethodDeclaration.class).stream())
                .filter(method -> method.getAccessSpecifier() == AccessSpecifier.NONE)
                .filter(method -> !method.hasJavaDocComment())
                .filter(method -> method.getAnnotationByName("Override").isEmpty())
                .filter(method -> !isSimpleGetterOrSetter(method))
                .forEach(method -> {
                    int line = method.getBegin().map(pos -> pos.line).orElse(-1);
                    errors.add(new ValidationError(javaUnit.getStorage().get().getPath(),
                                                   "Public method doesn't have a @Override annotation, is not a simple getter or setter, and doesn't have a javadoc comment",
                                                   line, ValidationError.Severity.MEDIUM));
                });

        javaUnit.findAll(MethodDeclaration.class,
                         method -> method.getAccessSpecifier() == AccessSpecifier.PUBLIC)
                .stream()
                .filter(method -> !method.hasJavaDocComment())
                .filter(method -> method.getAnnotationByName("Override").isEmpty())
                .filter(method -> !isSimpleGetterOrSetter(method))
                .forEach(method -> {
                    int line = method.getBegin().map(pos -> pos.line).orElse(-1);
                    errors.add(new ValidationError(javaUnit.getStorage().get().getPath(),
                                                   "Public method doesn't have a @Override annotation, is not a simple getter or setter, and doesn't have a javadoc comment",
                                                   line, ValidationError.Severity.MEDIUM));
                });

        return errors;
    }

    boolean isSimpleGetterOrSetter(MethodDeclaration method) {
        if (method.getParameters().isEmpty()) {
            boolean isNamedGet = method.getNameAsString().startsWith("get");
            if (isNamedGet) {
                return true;
            }
            boolean hasDirectReturn = method.getBody()
                                            .map(block -> block.getStatement(0).isReturnStmt())
                                            .orElse(false);
            if (hasDirectReturn) {
                ReturnStmt returnStmt = method.getBody().get().getStatement(0).asReturnStmt();

                if (returnStmt.getExpression().isPresent()) {
                    String expression = returnStmt.getExpression().get().toString().trim();
                    if (expression.equalsIgnoreCase(
                            "this." + method.getNameAsString()) || expression.equalsIgnoreCase(
                            method.getNameAsString())) {
                        return true;
                    }
                }
            }
        }

        boolean isNamedSet = method.getNameAsString().startsWith("set");
        boolean hasVoidReturnType = method.getType().asString().equals("void");
        return method.getParameters().size() == 1 && isNamedSet && hasVoidReturnType;
    }

}
