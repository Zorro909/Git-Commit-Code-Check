package de.zorro909.codecheck.java.doc;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import de.zorro909.codecheck.ValidationError;
import de.zorro909.codecheck.java.JavaChecker;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class JavaDocCheck extends JavaChecker {
    @Override
    public boolean isJavaResponsible(Path path) {
        return path.toString().contains("/src/main/java/");
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
                    errors.add(new ValidationError(javaUnit.getStorage().get().getPath().toString(),
                                                   "Public class doesn't implement an interface and doesn't have a javadoc comment",
                                                   line, ValidationError.Severity.HIGH));
                });


        javaUnit.findAll(MethodDeclaration.class,
                         method -> method.getAccessSpecifier() == AccessSpecifier.PUBLIC)
                .stream()
                .filter(method -> !method.hasJavaDocComment())
                .filter(method -> !method.getAnnotationByName("Override").isPresent())
                .filter(method -> !isSimpleGetterOrSetter(method))
                .forEach(method -> {
                    int line = method.getBegin().map(pos -> pos.line).orElse(-1);
                    errors.add(new ValidationError(javaUnit.getStorage().get().getPath().toString(),
                                                   "Public method doesn't have a @Override annotation, is not a simple getter or setter, and doesn't have a javadoc comment",
                                                   line, ValidationError.Severity.HIGH));
                });


        return List.of();
    }

    boolean isSimpleGetterOrSetter(MethodDeclaration method) {
        boolean isNamedGet = method.getNameAsString().startsWith("get");
        if (method.getParameters().isEmpty() && isNamedGet) {
            return true;
        }

        boolean isNamedSet = method.getNameAsString().startsWith("set");
        boolean hasVoidReturnType = method.getType().asString().equals("void");
        return method.getParameters().size() == 1 && isNamedSet && hasVoidReturnType;
    }

    @Override
    public void resetCache(Path file) {

    }
}
