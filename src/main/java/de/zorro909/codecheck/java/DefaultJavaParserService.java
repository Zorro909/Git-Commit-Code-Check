package de.zorro909.codecheck.java;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Position;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import de.zorro909.codecheck.checks.ValidationError;
import de.zorro909.codecheck.validation.Diagnostic;
import de.zorro909.codecheck.validation.DiagnosticKind;
import de.zorro909.codecheck.validation.RuleId;
import de.zorro909.codecheck.validation.SourcePosition;
import jakarta.inject.Singleton;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

@Singleton
public class DefaultJavaParserService implements JavaParserService {

    private static final RuleId JAVA_PARSER_RULE = new RuleId("java.parser");

    private final ProjectModelService projectModelService;
    private final ConcurrentMap<Path, ParseOutcome> parseCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<ModuleId, JavaParser> parserCache = new ConcurrentHashMap<>();

    public DefaultJavaParserService(ProjectModelService projectModelService) {
        this.projectModelService = projectModelService;
    }

    @Override
    public ParseOutcome parse(Path file) {
        Path absolute = file.toAbsolutePath().normalize();
        return parseCache.computeIfAbsent(absolute, this::parseUncached);
    }

    @Override
    public Optional<CompilationUnit> compilationUnit(Path file) {
        return parse(file).compilationUnit();
    }

    @Override
    public void invalidate(Path file) {
        parseCache.remove(file.toAbsolutePath().normalize());
    }

    @Override
    public void invalidateModule(ModuleId moduleId) {
        parserCache.remove(moduleId);
        ProjectModel model = projectModelService.currentModel();
        model.modules()
             .stream()
             .filter(module -> module.id().equals(moduleId))
             .findFirst()
             .ifPresent(module -> parseCache.keySet().removeIf(module::owns));
    }

    private ParseOutcome parseUncached(Path file) {
        ProjectModel model = projectModelService.currentModel();
        MavenModule module = model.moduleFor(file).orElseGet(() -> model.modules().getFirst());
        try {
            ParseResult<CompilationUnit> result = parserFor(module, model).parse(file);
            if (!result.isSuccessful() || result.getResult().isEmpty()) {
                return new ParseOutcome(file, Optional.empty(), parseDiagnostics(file, result));
            }
            CompilationUnit compilationUnit = result.getResult().get();
            List<Diagnostic> diagnostics = new ArrayList<>();
            diagnostics.addAll(parseDiagnostics(file, result));
            diagnostics.addAll(symbolDiagnostics(file, compilationUnit));
            return new ParseOutcome(file, Optional.of(compilationUnit), diagnostics);
        } catch (Exception e) {
            return new ParseOutcome(file, Optional.empty(), List.of(new Diagnostic(
                    file, "Failure parsing java file: " + e.getMessage(),
                    new SourcePosition(Position.FIRST_LINE, Position.FIRST_COLUMN),
                    ValidationError.Severity.HIGH, DiagnosticKind.PARSE_ERROR,
                    JAVA_PARSER_RULE)));
        }
    }

    private JavaParser parserFor(MavenModule module, ProjectModel model) {
        return parserCache.computeIfAbsent(module.id(), _ -> {
            CombinedTypeSolver typeSolver = new CombinedTypeSolver();
            typeSolver.add(new ReflectionTypeSolver());
            Stream.of(module.sourceRoots(), module.testRoots(), module.generatedSourceRoots(),
                      module.generatedTestSourceRoots())
                  .flatMap(List::stream)
                  .filter(Files::exists)
                  .forEach(root -> typeSolver.add(new JavaParserTypeSolver(root)));
            ParserConfiguration configuration = new ParserConfiguration()
                    .setLanguageLevel(languageLevel(model.languageLevel()))
                    .setSymbolResolver(new JavaSymbolSolver(typeSolver));
            return new JavaParser(configuration);
        });
    }

    private ParserConfiguration.LanguageLevel languageLevel(int languageLevel) {
        return switch (languageLevel) {
            case 8 -> ParserConfiguration.LanguageLevel.JAVA_8;
            case 9 -> ParserConfiguration.LanguageLevel.JAVA_9;
            case 10 -> ParserConfiguration.LanguageLevel.JAVA_10;
            case 11 -> ParserConfiguration.LanguageLevel.JAVA_11;
            case 12 -> ParserConfiguration.LanguageLevel.JAVA_12;
            case 13 -> ParserConfiguration.LanguageLevel.JAVA_13;
            case 14 -> ParserConfiguration.LanguageLevel.JAVA_14;
            case 15 -> ParserConfiguration.LanguageLevel.JAVA_15;
            case 16 -> ParserConfiguration.LanguageLevel.JAVA_16;
            case 17 -> ParserConfiguration.LanguageLevel.JAVA_17;
            case 18 -> ParserConfiguration.LanguageLevel.JAVA_18;
            case 19 -> ParserConfiguration.LanguageLevel.JAVA_19;
            case 20 -> ParserConfiguration.LanguageLevel.JAVA_20;
            case 21 -> ParserConfiguration.LanguageLevel.JAVA_21;
            default -> ParserConfiguration.LanguageLevel.JAVA_25;
        };
    }

    private List<Diagnostic> parseDiagnostics(Path file, ParseResult<CompilationUnit> result) {
        return result.getProblems()
                     .stream()
                     .map(problem -> parseDiagnostic(file, problem))
                     .toList();
    }

    private Diagnostic parseDiagnostic(Path file, Problem problem) {
        SourcePosition position = problem.getLocation()
                                         .map(location -> location.getBegin())
                                         .flatMap(token -> token.getRange())
                                         .map(range -> range.begin)
                                         .map(SourcePosition::from)
                                         .orElse(new SourcePosition(1, 1));
        return new Diagnostic(file, "Failure parsing java file: " + problem.getMessage(),
                              position, ValidationError.Severity.HIGH,
                              DiagnosticKind.PARSE_ERROR, JAVA_PARSER_RULE);
    }

    private List<Diagnostic> symbolDiagnostics(Path file, CompilationUnit compilationUnit) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (ClassOrInterfaceType type : compilationUnit.findAll(ClassOrInterfaceType.class)) {
            try {
                type.resolve();
            } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
                diagnostics.add(new Diagnostic(file,
                                               "Unresolved symbol '" + type.getNameAsString()
                                               + "': " + e.getMessage(),
                                               type.getBegin().map(SourcePosition::from)
                                                   .orElse(new SourcePosition(1, 1)),
                                               ValidationError.Severity.MEDIUM,
                                               DiagnosticKind.SYMBOL_WARNING,
                                               JAVA_PARSER_RULE));
            } catch (RuntimeException ignored) {
                // JavaParser can throw for constructs it cannot model yet. Those are not
                // unresolved-symbol diagnostics.
            }
        }
        return diagnostics;
    }
}
