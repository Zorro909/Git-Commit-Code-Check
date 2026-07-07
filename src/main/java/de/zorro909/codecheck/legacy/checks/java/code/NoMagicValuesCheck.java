package de.zorro909.codecheck.legacy.checks.java.code;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import de.zorro909.codecheck.legacy.FileLoader;
import de.zorro909.codecheck.core.diagnostic.ValidationError;
import de.zorro909.codecheck.legacy.checks.java.JavaChecker;
import de.zorro909.codecheck.java.JavaParserService;
import de.zorro909.codecheck.validation.FileInterest;
import de.zorro909.codecheck.validation.RuleId;
import de.zorro909.codecheck.validation.RuleMetadata;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The NoMagicValuesCheck class is responsible for checking Java code files for the
 * presence of magic values. Magic values are strings or integers that are hardcoded in
 * the code and are not represented by constants. It extends the JavaChecker class and
 * implements the CodeCheck interface. Only Java files located in the "src/main/java"
 * folder are considered for validation.
 */
@Singleton
public class NoMagicValuesCheck extends JavaChecker {

    private static final String MAIN_FOLDER = "src" + File.separatorChar + "main" + File.separatorChar + "java";

    public NoMagicValuesCheck(FileLoader fileLoader) {
        super(fileLoader);
    }

    @Inject
    public NoMagicValuesCheck(FileLoader fileLoader, JavaParserService javaParserService) {
        super(fileLoader, javaParserService);
    }

    @Override
    public RuleId ruleId() {
        return new RuleId("java.no-magic-values");
    }

    @Override
    public RuleMetadata ruleMetadata() {
        return new RuleMetadata("No magic values",
                "Rejects literal method and constructor arguments in main Java sources.");
    }

    @Override
    public FileInterest validatedFiles() {
        return FileInterest.javaMainSources();
    }

    @Override
    public boolean isJavaResponsible(Path path) {
        return path.toString().contains(MAIN_FOLDER);
    }

    @Override
    public List<ValidationError> check(CompilationUnit javaUnit) {
        List<ValidationError> errors = new ArrayList<>();

        filterGeneratedClasses(javaUnit).flatMap(Node::stream)
            .filter(node -> node instanceof MethodCallExpr || node instanceof ExplicitConstructorInvocationStmt
                    || node instanceof ObjectCreationExpr)
            .map(obj -> (NodeWithArguments<?>) obj)
            .filter(this::filterOutExceptions)
            .flatMap(call -> call.getArguments().stream())
            .filter(expr -> expr instanceof LiteralExpr && !(expr instanceof NullLiteralExpr)
                    && !(expr instanceof BooleanLiteralExpr))
            .map(expr -> new ValidationError(getPath(javaUnit),
                    "Magic Values like '" + expr + "' are not allowed! Extract to Constant!", expr.getBegin(),
                    ValidationError.Severity.LOW))
            .forEach(errors::add);

        return errors;
    }

    private boolean filterOutExceptions(NodeWithArguments<?> nodeWithArguments) {
        if (nodeWithArguments instanceof MethodCallExpr methodCallExpr) {
            for (MethodExclusion exclusion : MethodExclusion.values()) {
                if (exclusion.isMethodCall(methodCallExpr)) {
                    return false;
                }
            }
        }
        return true;
    }

    enum MethodExclusion {

        COLLECTORS_JOINING("Collectors", "joining");

        private final String scope;

        private final String method;

        MethodExclusion(String scope, String method) {
            this.scope = scope;
            this.method = method;
        }

        boolean isMethodCall(MethodCallExpr callExpr) {
            if (callExpr.getScope().isEmpty()) {
                return false;
            }
            String scope = callExpr.getScope().get().toString();
            String methodName = callExpr.getNameAsString();
            return this.scope.equals(scope) && this.method.equals(methodName);
        }

    }

}
