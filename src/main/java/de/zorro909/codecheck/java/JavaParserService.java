package de.zorro909.codecheck.java;

import com.github.javaparser.ast.CompilationUnit;

import java.nio.file.Path;
import java.util.Optional;

public interface JavaParserService {

    ParseOutcome parse(Path file);

    Optional<CompilationUnit> compilationUnit(Path file);

    void invalidate(Path file);

    void invalidateModule(ModuleId moduleId);

}
