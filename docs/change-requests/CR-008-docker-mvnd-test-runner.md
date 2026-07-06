# CR-008: Docker mvnd Test Runner

## Status

Proposed.

## Context

Coverage and test-driven checks need a sandboxed test execution environment. The desired runner uses Docker with the project and default Maven config mounted, and prefers mvnd to benefit from Maven daemon behavior.

The Docker/mvnd lifecycle should be owned by the codecheck daemon and should remain warm for a shorter idle timeout than the main daemon.

## Goals

- Run Maven tests in Docker.
- Prefer mvnd.
- Keep the test container long-lived to leverage mvnd daemon benefits.
- Default test-container idle timeout: 10 minutes.
- Support root-level multi-module Maven execution with `-pl`.
- Support targeted test execution.
- Make Maven goals, arguments, Docker image, and test property configurable.

## Test Runner Interface

```java
interface TestRunner {
    TestRunResult runTests(TestRunRequest request);
    void stop();
}
```

`TestRunRequest` should support:

- modules
- specific test classes
- retry failing tests
- full test run
- coverage report generation
- additional Maven args

## Docker mvnd Runner

The daemon owns the lifecycle:

- Start container on demand.
- Keep it warm while active.
- Stop after configured idle timeout.
- Restart if container exits unexpectedly.

Default timeout:

```yaml
maven:
  docker:
    containerIdleTimeout: "10m"
```

Mounts:

- repository root into container
- `~/.m2` into container by default
- optional extra mounts from config later

The runner should execute from repository root.

## Maven Command Shape

For module-specific targeted tests:

```text
mvnd -pl <module> -Dtest=<TestA>,<TestB> test jacoco:report
```

Actual goals and args are configurable:

```yaml
maven:
  runner: docker-mvnd
  preferMvnd: true
  goals: ["test", "jacoco:report"]
  args: ["-DskipITs"]
  targetedTestProperty: "-Dtest"
```

## Targeted Test Strategies

Initial strategies:

- Run changed test classes directly.
- Infer tests for changed production classes by naming convention.
- Retry previously failing tests first.
- Run module-specific tests with `-pl`.

Future strategies:

- Use previous coverage data to map tests to changed production classes.

## Requirements

- Docker/mvnd runner must be daemon-owned.
- Test container must be long-lived by default.
- Test runner must support full and targeted runs.
- Maven execution must occur at repository root with `-pl` for modules.
- Runner configuration must be repo-configurable.
- Verbose mode should show the exact command.

## Acceptance Criteria

- The daemon starts a Docker container for tests on demand.
- A second test run within 10 minutes reuses the same warm container.
- Targeted test execution passes `-Dtest`.
- Module-specific execution passes `-pl`.
- The container stops after configured inactivity.

## Open Questions

- What default Docker image should be used?
- Should Docker be mandatory when coverage rules are enabled, or should host mvnd be allowed as a fallback?
- How should Windows path mounting be normalized?
- Should container startup failures produce HIGH diagnostics or tool errors?

