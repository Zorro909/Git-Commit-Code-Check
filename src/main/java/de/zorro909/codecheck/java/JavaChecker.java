package de.zorro909.codecheck.java;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import de.zorro909.codecheck.CodeCheck;
import de.zorro909.codecheck.ValidationError;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public abstract class JavaChecker implements CodeCheck {

    private static final Map<Path, CompilationUnit> classCache = new HashMap<>();
    private static final JavaParser javaParser = new JavaParser();

    public abstract boolean isJavaResponsible(Path path);

    @Override
    public boolean isResponsible(Path path) {
        if (path.getFileName().toString().endsWith(".java")) {
            return isJavaResponsible(path);
        }
        return false;
    }

    public abstract List<ValidationError> check(CompilationUnit javaUnit);

    @Override
    public List<ValidationError> check(Path path) {
        Optional<CompilationUnit> javaUnitOptional = load(path);
        if (javaUnitOptional.isEmpty()) {
            return List.of(new ValidationError(path.toString(), "Failure parsing java file", 0, ValidationError.Severity.HIGH));
        }
        return check(javaUnitOptional.get());
    }

    public Optional<CompilationUnit> load(Path path) {
        try {
            return javaParser.parse(path).getResult();
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
