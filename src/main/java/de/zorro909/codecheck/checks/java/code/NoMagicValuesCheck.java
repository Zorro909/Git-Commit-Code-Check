package de.zorro909.codecheck.checks.java.code;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import de.zorro909.codecheck.checks.ValidationError;
import de.zorro909.codecheck.checks.java.JavaChecker;
import jakarta.inject.Singleton;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Singleton
public class NoMagicValuesCheck extends JavaChecker {

    private static final String MAIN_FOLDER =
        "src" + File.separatorChar + "main" + File.separatorChar + "java";

    @Override
    public boolean isJavaResponsible(Path path) {
        return path.toString().contains(MAIN_FOLDER);
    }

    @Override
    public List<ValidationError> check(CompilationUnit javaUnit) {
        Path path = javaUnit.getStorage().get().getPath();

        return javaUnit.findAll(ClassOrInterfaceDeclaration.class)
            .stream().filter(clazz -> clazz.getAnnotationByName("Generated").isEmpty())
            .flatMap(clazz -> clazz.findAll(MethodCallExpr.class).stream())
            .flatMap(call -> call.getArguments().stream())
            .filter(expr -> (expr instanceof StringLiteralExpr) || (expr instanceof IntegerLiteralExpr))
            .map(expr -> new ValidationError(path,
                "Magic Values like '" + expr.toString() + "' are not allowed! Extract to Constant!",
                expr.getBegin().get().line, ValidationError.Severity.LOW)).toList();
    }
}
