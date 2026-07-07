package de.zorro909.codecheck.coverage;

import de.zorro909.codecheck.runner.TestRunRequest;
import de.zorro909.codecheck.runner.TestRunResult;
import de.zorro909.codecheck.runner.TestRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JacocoCoverageServiceTest {

    @Test
    void freshReportIsParsedWithoutRunningTests(@TempDir Path repo) throws Exception {
        Path source = write(repo.resolve("src/main/java/com/example/Foo.java"), "class Foo {}");
        Path report = write(repo.resolve("target/site/jacoco/jacoco.xml"),
                            report("com/example/Foo", 1, 3, 2, 2));
        Files.setLastModifiedTime(source, FileTime.from(Instant.parse("2026-01-01T00:00:00Z")));
        Files.setLastModifiedTime(report, FileTime.from(Instant.parse("2026-01-01T00:00:10Z")));
        RecordingRunner runner = new RecordingRunner(report, report("com/example/Foo", 0, 1, 0, 1));
        JacocoCoverageService service = new JacocoCoverageService(runner);

        CoverageSnapshot snapshot = service.currentCoverage(new CoverageRequest(
                List.of(source), List.of(), List.of(), List.of(),
                List.of(repo.resolve("missing.xml"), report)));

        assertThat(runner.requests).isEmpty();
        assertThat(snapshot.classCoverage("com/example/Foo"))
                .get()
                .extracting(coverage -> coverage.line().ratio())
                .isEqualTo(0.75);
    }

    @Test
    void staleReportRunsTestsAndParsesRefreshedReport(@TempDir Path repo) throws Exception {
        Path source = write(repo.resolve("src/main/java/com/example/Foo.java"), "class Foo { int value; }");
        Path report = write(repo.resolve("target/site/jacoco/jacoco.xml"),
                            report("com/example/Foo", 4, 0, 1, 0));
        Files.setLastModifiedTime(report, FileTime.from(Instant.parse("2026-01-01T00:00:00Z")));
        Files.setLastModifiedTime(source, FileTime.from(Instant.parse("2026-01-01T00:00:10Z")));
        RecordingRunner runner = new RecordingRunner(report, report("com/example/Foo", 1, 9, 0, 2));
        JacocoCoverageService service = new JacocoCoverageService(runner);

        CoverageSnapshot snapshot = service.currentCoverage(new CoverageRequest(
                List.of(source), List.of(), List.of(), List.of(), List.of(report)));

        assertThat(runner.requests).containsExactly(TestRunRequest.full());
        assertThat(snapshot.classCoverage("com/example/Foo"))
                .get()
                .extracting(coverage -> coverage.line().ratio(), coverage -> coverage.branch().ratio())
                .containsExactly(0.9, 1.0);
    }

    private static Path write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, content);
    }

    private static String report(String className, int lineMissed, int lineCovered,
                                 int branchMissed, int branchCovered) {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <!DOCTYPE report PUBLIC "-//JACOCO//DTD Report 1.1//EN" "report.dtd">
                <report name="unit">
                  <package name="com/example">
                    <class name="%s" sourcefilename="Foo.java">
                      <counter type="LINE" missed="%d" covered="%d"/>
                      <counter type="BRANCH" missed="%d" covered="%d"/>
                    </class>
                  </package>
                </report>
                """.formatted(className, lineMissed, lineCovered, branchMissed, branchCovered);
    }

    private static final class RecordingRunner implements TestRunner {
        private final Path report;
        private final String refreshedReport;
        private final List<TestRunRequest> requests = new java.util.ArrayList<>();

        private RecordingRunner(Path report, String refreshedReport) {
            this.report = report;
            this.refreshedReport = refreshedReport;
        }

        @Override
        public TestRunResult runTests(TestRunRequest request) {
            requests.add(request);
            try {
                Files.writeString(report, refreshedReport);
                Files.setLastModifiedTime(report, FileTime.from(Instant.parse("2026-01-01T00:01:00Z")));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return new TestRunResult(true, 0, "", "", "container", List.of("mvnd", "test", "jacoco:report"));
        }

        @Override
        public void stop() {
        }
    }
}
