package de.zorro909.codecheck.utils;

import java.util.Optional;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;

public class MethodDeclarationExtensions {

    public static final String EXCLUSION_GETTER_PREFIX = "get";

    public static boolean isSimpleGetterOrSetter(MethodDeclaration method) {
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

}
