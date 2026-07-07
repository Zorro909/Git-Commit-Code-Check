package de.zorro909.codecheck.core.coverage;

import de.zorro909.codecheck.core.diagnostic.ValidationError;
import de.zorro909.codecheck.core.diagnostic.Diagnostic;
import de.zorro909.codecheck.core.diagnostic.DiagnosticKind;
import de.zorro909.codecheck.core.validation.rule.RuleId;
import de.zorro909.codecheck.core.diagnostic.SourcePosition;
import de.zorro909.codecheck.infra.jacoco.MapStructCoverageAttributor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CoverageDiagnosticService {

    private static final RuleId COVERAGE_RULE = new RuleId("coverage.jacoco");

    private final CoverageThresholdPolicy thresholdPolicy;

    private final MapStructCoverageAttributor mapStructCoverageAttributor;

    public CoverageDiagnosticService(CoverageThresholdPolicy thresholdPolicy,
            MapStructCoverageAttributor mapStructCoverageAttributor) {
        this.thresholdPolicy = thresholdPolicy;
        this.mapStructCoverageAttributor = mapStructCoverageAttributor;
    }

    public List<Diagnostic> diagnostics(Path sourceFile, String className, CoverageSnapshot snapshot) {
        CoverageThreshold threshold = thresholdPolicy.thresholdFor(sourceFile);
        ClassCoverage coverage = mapStructCoverageAttributor.isMapper(sourceFile)
                ? mapStructCoverageAttributor.attributedCoverage(sourceFile, snapshot).orElse(null)
                : snapshot.classCoverage(className).orElse(null);
        if (coverage == null) {
            return List.of();
        }
        List<Diagnostic> diagnostics = new ArrayList<>();
        if (coverage.line().ratio() < threshold.line()) {
            diagnostics.add(diagnostic(sourceFile,
                    "Line coverage " + coverage.line().ratio() + " is below " + threshold.line()));
        }
        if (coverage.branch().ratio() < threshold.branch()) {
            diagnostics.add(diagnostic(sourceFile,
                    "Branch coverage " + coverage.branch().ratio() + " is below " + threshold.branch()));
        }
        return diagnostics;
    }

    private Diagnostic diagnostic(Path sourceFile, String message) {
        return new Diagnostic(sourceFile, message, new SourcePosition(1, 1), ValidationError.Severity.HIGH,
                DiagnosticKind.COVERAGE_FAILURE, COVERAGE_RULE);
    }

}
