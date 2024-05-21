package de.zorro909.codecheck.checks.java.doc;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.nodeTypes.NodeWithJavadoc;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import de.zorro909.codecheck.checks.ValidationError;
import de.zorro909.codecheck.checks.java.JavaChecker;
import jakarta.inject.Singleton;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * This class is responsible for checking the correctness of Java code by enforcing JavaDoc comments for
 * public classes and methods that are not getter or setter methods or overridden methods. It extends the
 * {@link JavaChecker} abstract class.
 */
@Singleton
public final class JavaDocCheck extends JavaChecker {

    private static final String MAIN_FOLDER = "src" + File.separatorChar + "main" + File.separatorChar + "java";

    public static final String ERROR_MESSAGE_CLASS = "Public class doesn't implement an interface" + " and doesn't have a javadoc comment";

    public static final String ERROR_MESSAGE_METHOD = "Public method doesn't have a @Override " + "annotation, is not a simple getter or setter, and doesn't have a javadoc comment";
    public static final String EXCLUSIION_MAPPER_CLASS_SUFFIX = "Mapper";
    public static final String EXCLUSION_OVERRIDE_ANNOTAION = "Override";
    public static final String EXCLUSION_GETTER_PREFIX = "get";

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
                .filter(this::hasNoJavaDoc)
                .filter(decl -> decl.getImplementedTypes().isEmpty())
                .map(Node::getBegin)
                .map(pos -> new ValidationError(getPath(javaUnit), ERROR_MESSAGE_CLASS, pos,
                                                ValidationError.Severity.MEDIUM))
                .forEach(errors::add);

        javaUnit.findAll(ClassOrInterfaceDeclaration.class,
                         decl -> decl.getAccessSpecifier() == AccessSpecifier.PUBLIC && decl.isInterface())
                .stream()
                .filter(decl -> !decl.getNameAsString().endsWith(EXCLUSIION_MAPPER_CLASS_SUFFIX))
                .flatMap(decl -> decl.findAll(MethodDeclaration.class).stream())
                .filter(method -> method.getAccessSpecifier() == AccessSpecifier.NONE)
                .filter(this::hasNoJavaDoc)
                .filter(method -> method.getAnnotationByName(EXCLUSION_OVERRIDE_ANNOTAION)
                                        .isEmpty())
                .filter(method -> !isSimpleGetterOrSetter(method))
                .map(Node::getBegin)
                .map(pos -> new ValidationError(getPath(javaUnit), ERROR_MESSAGE_METHOD, pos,
                                                ValidationError.Severity.MEDIUM))
                .forEach(errors::add);

        javaUnit.findAll(MethodDeclaration.class,
                         method -> method.getAccessSpecifier() == AccessSpecifier.PUBLIC)
                .stream()
                .filter(this::hasNoJavaDoc)
                .filter(method -> method.getAnnotationByName(EXCLUSION_OVERRIDE_ANNOTAION)
                                        .isEmpty())
                .filter(method -> !isSimpleGetterOrSetter(method))
                .map(Node::getBegin)
                .map(pos -> new ValidationError(getPath(javaUnit), ERROR_MESSAGE_METHOD, pos,
                                                ValidationError.Severity.MEDIUM))
                .forEach(errors::add);

        return errors;
    }

    boolean isSimpleGetterOrSetter(MethodDeclaration method) {
        if (method.getParameters().isEmpty()) {
            boolean isNamedGet = method.getNameAsString().startsWith(EXCLUSION_GETTER_PREFIX);
            if (isNamedGet) {
                return true;
            }
            Optional<ReturnStmt> returnStmt = method.getBody()
                                                    .flatMap(block -> block.getStatements()
                                                                           .getFirst())
                                                    .filter(Statement::isReturnStmt)
                                                    .map(Statement::asReturnStmt);
            if (returnStmt.isPresent()) {
                Optional<Expression> firstExpression = returnStmt.get().getExpression();
                if (firstExpression.isPresent()) {
                    String expression = firstExpression.get().toString().trim();
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

    boolean hasNoJavaDoc(NodeWithJavadoc<?> node) {
        Optional<JavadocComment> optionalComment = node.getJavadocComment();
        return optionalComment.map(JavadocComment::getContent).map(String::isBlank).orElse(true);
    }

}
