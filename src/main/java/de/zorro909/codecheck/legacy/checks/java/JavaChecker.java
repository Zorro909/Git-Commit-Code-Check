package de.zorro909.codecheck.legacy.checks.java;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import de.zorro909.codecheck.legacy.FileLoader;
import de.zorro909.codecheck.legacy.checks.CodeCheck;
import de.zorro909.codecheck.core.diagnostic.ValidationError;
import de.zorro909.codecheck.core.project.DefaultJavaParserService;
import de.zorro909.codecheck.core.project.JavaParserService;
import de.zorro909.codecheck.core.project.MavenProjectModelService;
import de.zorro909.codecheck.core.project.ParseOutcome;
import de.zorro909.codecheck.core.diagnostic.Diagnostic;

public abstract class JavaChecker implements CodeCheck {

    public static final String JAVA_FILE_SUFFIX = ".java";

    public static final String COMP_UNIT_NO_STORAGE = "Invalid compilation unit: storage not found";

    public static final String GENERATED_ANNOTATION = "Generated";

    private static final String MAIN_FOLDER = "src" + File.separatorChar + "main" + File.separatorChar + "java";

    private static final String TEST_FOLDER = "src" + File.separatorChar + "test" + File.separatorChar + "java";

    protected final FileLoader fileLoader;

    private final JavaParserService javaParserService;

    protected JavaChecker(FileLoader fileLoader) {
        this(fileLoader, new DefaultJavaParserService(new MavenProjectModelService(Path.of(""))));
    }

    protected JavaChecker(FileLoader fileLoader, JavaParserService javaParserService) {
        this.fileLoader = fileLoader;
        this.javaParserService = javaParserService;
    }

    /**
     * Checks if a given file path is responsible for Java code validation.
     * @param path The file path to check.
     * @return True if the file path is responsible for Java code validation, false
     * otherwise.
     */
    public abstract boolean isJavaResponsible(Path path);

    @Override
    public boolean isResponsible(Path path) {
        if (path.getFileName().toString().endsWith(JAVA_FILE_SUFFIX)) {
            return isJavaResponsible(path);
        }
        return false;
    }

    /**
     * Abstract method for performing code validation on a CompilationUnit.
     * @param javaUnit The CompilationUnit to be validated.
     * @return A List of ValidationErrors representing the errors found during validation.
     */
    public abstract List<ValidationError> check(CompilationUnit javaUnit);

    @Override
    public List<ValidationError> check(Path path) {
        ParseOutcome parseOutcome = load(path);
        List<ValidationError> parserDiagnostics = parseOutcome.diagnostics()
            .stream()
            .map(Diagnostic::toValidationError)
            .toList();
        if (parseOutcome.compilationUnit().isEmpty()) {
            return parserDiagnostics;
        }
        List<ValidationError> checkDiagnostics = check(parseOutcome.compilationUnit().get());
        return Stream.concat(parserDiagnostics.stream(), checkDiagnostics.stream()).toList();
    }

    /**
     * Loads a Java source file from the specified path and returns a ParseResult of
     * CompilationUnit.
     * @param path The path of the Java source file to load.
     * @return The ParseOutcome representing the loaded Java source file.
     */
    public ParseOutcome load(Path path) {
        fileLoader.markFile(path);
        return javaParserService.parse(path);
    }

    @Override
    public void resetCache(Path path) {
        javaParserService.invalidate(path);
    }

    protected Path getPath(CompilationUnit javaUnit) {
        Optional<Path> pathOpt = javaUnit.getStorage().map(CompilationUnit.Storage::getPath);
        return pathOpt.orElseThrow(() -> new IllegalArgumentException(COMP_UNIT_NO_STORAGE));
    }

    protected Stream<ClassOrInterfaceDeclaration> filterGeneratedClasses(CompilationUnit javaUnit) {
        return javaUnit
            .findAll(ClassOrInterfaceDeclaration.class,
                    clazz -> clazz.getAnnotationByName(GENERATED_ANNOTATION).isEmpty())
            .stream();
    }

}
