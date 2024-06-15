package de.zorro909.codecheck.checks.java.doc;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.nodeTypes.NodeWithJavadoc;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.description.JavadocDescription;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import de.zorro909.codecheck.FileLoader;
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

    private static final String MAIN_FOLDER =
        "src" + File.separatorChar + "main" + File.separatorChar + "java";

    private static final String TEST_FOLDER =
        "src" + File.separatorChar + "test" + File.separatorChar + "java";

    public static final String ERROR_MESSAGE_CLASS =
        "Public class doesn't implement an interface" + " and doesn't have a javadoc comment";

    public static final String ERROR_MESSAGE_METHOD = "Public method doesn't have a @Override "
        + "annotation, is not a simple getter or setter, and doesn't have a javadoc comment";

    public static final String ERROR_MESSAGE_TEST_METHOD = "Test method doesn't have a javadoc comment";

    public static final String EXCLUSIION_MAPPER_CLASS_SUFFIX = "Mapper";

    public static final String EXCLUSION_OVERRIDE_ANNOTAION = "Override";

    public static final String EXCLUSION_GETTER_PREFIX = "get";

    public JavaDocCheck(FileLoader fileLoader) {
        super(fileLoader);
    }

    @Override
    public boolean isJavaResponsible(Path path) {
        return path.toString().contains(MAIN_FOLDER) || (path.toString().contains(TEST_FOLDER)
            && path.toString().endsWith("Test.java"));
    }

    @Override
    public List<ValidationError> check(CompilationUnit javaUnit) {
        List<ValidationError> errors = new LinkedList<>();

        javaUnit.findAll(TypeDeclaration.class,
                decl -> decl.getAccessSpecifier() == AccessSpecifier.PUBLIC)
            .stream()
            .filter(this::hasNoJavaDoc)
            .filter(coi -> hasNoImplements(javaUnit, coi))
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

        javaUnit.findAll(MethodDeclaration.class,
                method -> method.getAnnotationByName("Test").isPresent())
            .stream()
            .filter(node -> hasNoJavaDoc(node, true))
            .map(Node::getBegin)
            .map(pos -> new ValidationError(getPath(javaUnit), ERROR_MESSAGE_TEST_METHOD, pos,
                ValidationError.Severity.MEDIUM))
            .forEach(errors::add);

        return errors;
    }

    private boolean hasNoImplements(CompilationUnit unit,
        TypeDeclaration typeDeclaration) {
        if (!(typeDeclaration instanceof ClassOrInterfaceDeclaration)) {
            return true;
        }
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration =
            (ClassOrInterfaceDeclaration) typeDeclaration;

        NodeList<ClassOrInterfaceType> implementedTypes =
            classOrInterfaceDeclaration.getImplementedTypes();
        if (implementedTypes.isEmpty()) {
            return true;
        }

        String[] origPackages = unit.getPackageDeclaration().map(PackageDeclaration::getName).map(
            Name::asString).orElse("").split("\\.");
        if (origPackages.length < 5) {
            return false;
        }

        return implementedTypes.stream().noneMatch(coi -> {
            try {
                String qualifiedName = coi.resolve().asReferenceType().getQualifiedName();
                String[] packages = qualifiedName.split("\\.");
                for (int i = 0; i < 5; i++) {
                    if (!origPackages[i].equals(packages[i])) {
                        return false;
                    }
                }
                return true;
            } catch (UnsolvedSymbolException use) {
                return false;
            }
        });
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
        return hasNoJavaDoc(node, false);
    }

    boolean hasNoJavaDoc(NodeWithJavadoc<?> node, boolean ignoreExceptions) {
        Optional<Javadoc> optionalComment = node.getJavadoc();

        if (optionalComment.isEmpty()) {
            return true;
        }

        boolean hasNoDescription = optionalComment.map(Javadoc::getDescription)
            .map(JavadocDescription::toText)
            .map(text -> text.trim().length() < 6)
            .orElse(true);

        if (hasNoDescription) {
            return true;
        }

        Javadoc javadoc = optionalComment.get();
        if (node instanceof MethodDeclaration method) {
            List<String> parameters =
                method.getParameters()
                    .stream()
                    .map(Parameter::getName)
                    .map(SimpleName::asString)
                    .toList();

            if (!checkBlockTags(javadoc, parameters, JavadocBlockTag.Type.PARAM)) {
                return true;
            }
            List<String> exceptions =
                method.getThrownExceptions().stream().map(ReferenceType::asString).toList();

            if (!ignoreExceptions) {
                if (!checkBlockTags(javadoc, exceptions, JavadocBlockTag.Type.THROWS)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkBlockTags(Javadoc javadoc, List<String> nameChecks, JavadocBlockTag.Type type) {
        List<Boolean> checkedParams = javadoc.getBlockTags()
            .stream()
            .filter(tag -> tag.getType() == type)
            .map(JavadocBlockTag::getName)
            .map(name -> name.map(nameChecks::contains).orElse(false))
            .toList();

        if (checkedParams.size() != nameChecks.size()) {
            return false;
        }
        return !checkedParams.contains(Boolean.FALSE);
    }

}
