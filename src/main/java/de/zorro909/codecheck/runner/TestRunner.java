package de.zorro909.codecheck.runner;

public interface TestRunner {

    TestRunResult runTests(TestRunRequest request);

    void stop();

}
