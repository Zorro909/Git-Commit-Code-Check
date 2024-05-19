package de.zorro909.codecheck.java;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import de.zorro909.codecheck.CodeCheck;
import de.zorro909.codecheck.ValidationError;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class JavaChecker implements CodeCheck {

    private static final Map<Path, ParseResult<CompilationUnit>> classCache = new HashMap<>();
    private static final JavaParser javaParser = new JavaParser();

    static {
        javaParser.getParserConfiguration()
                  .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
    }

    /**
     * Checks if a given file path is responsible for Java code validation.
     *
     * @param path The file path to check.
     * @return True if the file path is responsible for Java code validation, false otherwise.
     */
    public abstract boolean isJavaResponsible(Path path);

    @Override
    public boolean isResponsible(Path path) {
        if (path.getFileName().toString().endsWith(".java")) {
            return isJavaResponsible(path);
        }
        return false;
    }

    /**
     * Abstract method for performing code validation on a CompilationUnit.
     *
     * @param javaUnit The CompilationUnit to be validated.
     * @return A List of ValidationErrors representing the errors found during validation.
     */
    public abstract List<ValidationError> check(CompilationUnit javaUnit);

    @Override
    public List<ValidationError> check(Path path) {
        ParseResult<CompilationUnit> parseResult = load(path);
        if (!parseResult.isSuccessful()) {
            String errorMessage = "Failure parsing java file: " + parseResult.getProblems()
                                                                             .stream()
                                                                             .map(Problem::toString)
                                                                             .collect(
                                                                                     Collectors.joining(
                                                                                             ", "));
            return List.of(
                    new ValidationError(path, errorMessage, 0, ValidationError.Severity.HIGH));
        }
        return check(parseResult.getResult().get());
    }

    /**
     * Loads a Java source file from the specified path and returns a ParseResult of CompilationUnit.
     *
     * @param path The path of the Java source file to load.
     * @return The ParseResult of CompilationUnit representing the loaded Java source file.
     */
    public ParseResult<CompilationUnit> load(Path path) {
        try {
            if (classCache.containsKey(path)) {
                return classCache.get(path);
            }
            ParseResult<CompilationUnit> parseResult = javaParser.parse(path);
            classCache.put(path, parseResult);
            return parseResult;
        } catch (Exception e) {
            return new ParseResult<>(null, List.of(new Problem("Big Problem Exception", null, e)),
                                     null);
        }
    }

    @Override
    public void resetCache(Path path) {
        classCache.remove(path);
    }

}
