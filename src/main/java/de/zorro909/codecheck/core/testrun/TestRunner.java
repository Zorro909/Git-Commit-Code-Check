package de.zorro909.codecheck.core.testrun;

public interface TestRunner {

    TestRunResult runTests(TestRunRequest request);

    void stop();

}
