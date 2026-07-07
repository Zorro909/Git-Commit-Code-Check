package de.zorro909.codecheck.coverage;

import de.zorro909.codecheck.runner.TestRunRequest;
import de.zorro909.codecheck.runner.TestRunner;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Singleton
public class JacocoCoverageService implements CoverageService {

    private final TestRunner testRunner;

    private final JacocoXmlParser parser;

    public JacocoCoverageService(TestRunner testRunner) {
        this(testRunner, new JacocoXmlParser());
    }

    JacocoCoverageService(TestRunner testRunner, JacocoXmlParser parser) {
        this.testRunner = testRunner;
        this.parser = parser;
    }

    @Override
    public CoverageSnapshot currentCoverage(CoverageRequest request) {
        CoverageFreshness freshness = freshness(request);
        if (freshness == CoverageFreshness.FRESH) {
            return parser.parse(newestReport(existing(request.reportPaths())));
        }
        return refreshCoverage(request);
    }

    @Override
    public CoverageFreshness freshness(CoverageRequest request) {
        List<Path> existingReports = existing(request.reportPaths());
        if (existingReports.isEmpty()) {
            return CoverageFreshness.ABSENT;
        }
        Instant newestReport = modified(newestReport(existingReports));
        List<Path> inputs = java.util.stream.Stream
            .of(request.sourceFiles(), request.testFiles(), request.contextFiles(), request.buildFiles())
            .flatMap(List::stream)
            .filter(Files::exists)
            .toList();
        if (inputs.isEmpty()) {
            return CoverageFreshness.FRESH;
        }
        Instant newestInput = inputs.stream().map(this::modified).max(Comparator.naturalOrder()).orElse(Instant.EPOCH);
        return newestReport.isBefore(newestInput) ? CoverageFreshness.STALE : CoverageFreshness.FRESH;
    }

    @Override
    public CoverageSnapshot refreshCoverage(CoverageRequest request) {
        var result = testRunner.runTests(TestRunRequest.full());
        if (!result.success()) {
            throw new IllegalStateException(
                    "Unable to refresh JaCoCo coverage; test run exited with " + result.exitCode());
        }
        return parser.parse(newestReport(existing(request.reportPaths())));
    }

    private List<Path> existing(List<Path> paths) {
        return paths.stream().filter(Files::exists).toList();
    }

    private Path newestReport(List<Path> reports) {
        return reports.stream()
            .max(Comparator.comparing(this::modified))
            .orElseThrow(() -> new IllegalStateException("No JaCoCo report available"));
    }

    private Instant modified(Path path) {
        try {
            FileTime time = Files.getLastModifiedTime(path);
            return time.toInstant();
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to inspect " + path, e);
        }
    }

}
