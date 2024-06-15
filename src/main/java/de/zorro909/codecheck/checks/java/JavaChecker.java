package de.zorro909.codecheck.checks.java;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Position;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import de.zorro909.codecheck.FileLoader;
import de.zorro909.codecheck.checks.CodeCheck;
import de.zorro909.codecheck.checks.ValidationError;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class JavaChecker implements CodeCheck {

    private static final Map<Path, ParseResult<CompilationUnit>> classCache = new HashMap<>();

    private static final JavaParser javaParser = new JavaParser();

    public static final String JAVA_FILE_SUFFIX = ".java";

    public static final String PARSE_EXCEPTION = "Big Problem Exception";

    public static final String COMP_UNIT_NO_STORAGE = "Invalid compilation unit: storage not found";

    public static final String GENERATED_ANNOTATION = "Generated";

    private static final String MAIN_FOLDER =
        "src" + File.separatorChar + "main" + File.separatorChar + "java";

    private static final String TEST_FOLDER =
        "src" + File.separatorChar + "test" + File.separatorChar + "java";

    protected final FileLoader fileLoader;

    static {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(MAIN_FOLDER));
        combinedTypeSolver.add(new JavaParserTypeSolver(TEST_FOLDER));

        javaParser.getParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
            .setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
    }

    public JavaChecker(FileLoader fileLoader) {
        this.fileLoader = fileLoader;
    }

    /**
     * Checks if a given file path is responsible for Java code validation.
     * @param path The file path to check.
     * @return True if the file path is responsible for Java code validation, false otherwise.
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
        ParseResult<CompilationUnit> parseResult = load(path);
        if (!parseResult.isSuccessful() || parseResult.getResult().isEmpty()) {
            String errorMessage = "Failure parsing java file: " + parseResult.getProblems()
                .stream()
                .map(Problem::toString)
                .collect(
                    Collectors.joining(
                        ", "));
            return List.of(new ValidationError(path, errorMessage, new Position(Position.FIRST_LINE,
                Position.FIRST_COLUMN),
                ValidationError.Severity.HIGH));
        }
        return check(parseResult.getResult().get());
    }

    /**
     * Loads a Java source file from the specified path and returns a ParseResult of CompilationUnit.
     * @param path The path of the Java source file to load.
     * @return The ParseResult of CompilationUnit representing the loaded Java source file.
     */
    public ParseResult<CompilationUnit> load(Path path) {
        try {
            if (classCache.containsKey(path)) {
                return classCache.get(path);
            }
            fileLoader.markFile(path);
            ParseResult<CompilationUnit> parseResult = javaParser.parse(path);
            classCache.put(path, parseResult);
            return parseResult;
        } catch (Exception e) {
            System.err.println("Can't parse File '" + path.toString() + "'! (" + e.toString() + ")");
            return new ParseResult<>(null, List.of(new Problem(PARSE_EXCEPTION, null, e)), null);
        }
    }

    @Override
    public void resetCache(Path path) {
        classCache.remove(path);
    }

    protected Path getPath(CompilationUnit javaUnit) {
        Optional<Path> pathOpt = javaUnit.getStorage().map(CompilationUnit.Storage::getPath);
        return pathOpt.orElseThrow(() -> new IllegalArgumentException(COMP_UNIT_NO_STORAGE));
    }

    protected Stream<ClassOrInterfaceDeclaration> filterGeneratedClasses(CompilationUnit javaUnit) {
        return javaUnit.findAll(ClassOrInterfaceDeclaration.class,
                clazz -> clazz.getAnnotationByName(GENERATED_ANNOTATION).isEmpty())
            .stream();
    }

}
