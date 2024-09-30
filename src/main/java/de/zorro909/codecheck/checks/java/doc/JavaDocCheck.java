package de.zorro909.codecheck.checks.java.doc;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.nodeTypes.NodeWithJavadoc;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.description.JavadocDescription;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import de.zorro909.codecheck.FileLoader;
import de.zorro909.codecheck.checks.ValidationError;
import de.zorro909.codecheck.checks.java.JavaChecker;
import de.zorro909.codecheck.utils.MethodDeclarationExtensions;
import jakarta.inject.Singleton;
import lombok.experimental.ExtensionMethod;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * This class is responsible for checking the correctness of Java code by enforcing JavaDoc comments for
 * public classes and methods that are not getter or setter methods or overridden methods. It extends the
 * {@link JavaChecker} abstract class.
 */
@Singleton
@ExtensionMethod(MethodDeclarationExtensions.class)
public final class JavaDocCheck extends JavaChecker {

    public static final String ERROR_MESSAGE_CLASS = "Public class doesn't implement an interface" + " and doesn't have a javadoc comment";
    public static final String ERROR_MESSAGE_METHOD = "Public method doesn't have a @Override " + "annotation, is not a simple getter or setter, and doesn't have a javadoc comment";
    public static final String ERROR_MESSAGE_TEST_METHOD = "Test method doesn't have a javadoc comment";
    public static final String EXCLUSIION_MAPPER_CLASS_SUFFIX = "Mapper";
    public static final String EXCLUSION_OVERRIDE_ANNOTAION = "Override";
    private static final String MAIN_FOLDER = "src" + File.separatorChar + "main" + File.separatorChar + "java";
    private static final String TEST_FOLDER = "src" + File.separatorChar + "test" + File.separatorChar + "java";

    public JavaDocCheck(FileLoader fileLoader) {
        super(fileLoader);
    }

    @Override
    public boolean isJavaResponsible(Path path) {
        String pathString = path.toString();

        boolean isMainJavaFile = pathString.contains(MAIN_FOLDER) && pathString.endsWith(".java");
        boolean isTestJavaFile = pathString.contains(TEST_FOLDER) && pathString.endsWith("Test.java");

        return isMainJavaFile || isTestJavaFile;
    }

    @Override
    public List<ValidationError> check(CompilationUnit javaUnit) {
        List<ValidationError> errors = new LinkedList<>();

        javaUnit.findAll(TypeDeclaration.class, decl -> decl.getAccessSpecifier() == AccessSpecifier.PUBLIC)
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
                .filter(method -> method.getAnnotationByName(EXCLUSION_OVERRIDE_ANNOTAION).isEmpty())
                .filter(MethodDeclarationExtensions::isSimpleGetterOrSetter)
                .map(Node::getBegin)
                .map(pos -> new ValidationError(getPath(javaUnit), ERROR_MESSAGE_METHOD, pos,
                                                ValidationError.Severity.MEDIUM))
                .forEach(errors::add);

        javaUnit.findAll(MethodDeclaration.class, method -> method.getAccessSpecifier() == AccessSpecifier.PUBLIC)
                .stream()
                .filter(this::hasNoJavaDoc)
                .filter(method -> method.getAnnotationByName(EXCLUSION_OVERRIDE_ANNOTAION).isEmpty())
                .filter(method -> !method.isSimpleGetterOrSetter())
                .map(Node::getBegin)
                .map(pos -> new ValidationError(getPath(javaUnit), ERROR_MESSAGE_METHOD, pos,
                                                ValidationError.Severity.MEDIUM))
                .forEach(errors::add);

        javaUnit.findAll(MethodDeclaration.class, method -> method.getAnnotationByName("Test").isPresent())
                .stream()
                .filter(node -> hasNoJavaDoc(node, true))
                .map(Node::getBegin)
                .map(pos -> new ValidationError(getPath(javaUnit), ERROR_MESSAGE_TEST_METHOD, pos,
                                                ValidationError.Severity.MEDIUM))
                .forEach(errors::add);

        return errors;
    }

    private boolean hasNoImplements(CompilationUnit unit, TypeDeclaration typeDeclaration) {
        if (!(typeDeclaration instanceof ClassOrInterfaceDeclaration classOrInterfaceDeclaration)) {
            return true;
        }

        NodeList<ClassOrInterfaceType> implementedTypes = classOrInterfaceDeclaration.getImplementedTypes();
        if (implementedTypes.isEmpty()) {
            return true;
        }

        String[] origPackages = unit.getPackageDeclaration()
                                    .map(PackageDeclaration::getName)
                                    .map(Name::asString)
                                    .orElse("")
                                    .split("\\.");
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
            Stream<String> parameterNames = method.getParameters().stream().map(Parameter::getNameAsString);

            Stream<String> typeParameterNames = method.getTypeParameters().stream().map(TypeParameter::getNameAsString);

            List<String> parameters = Stream.concat(parameterNames, typeParameterNames).toList();

            if (!checkBlockTags(javadoc, parameters, JavadocBlockTag.Type.PARAM)) {
                return true;
            }
            List<String> exceptions = method.getThrownExceptions().stream().map(ReferenceType::asString).toList();

            if (!ignoreExceptions) {
                return !checkBlockTags(javadoc, exceptions, JavadocBlockTag.Type.THROWS);
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
