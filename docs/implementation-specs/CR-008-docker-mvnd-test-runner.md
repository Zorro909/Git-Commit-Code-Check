# CR-008 Implementation Specification: Docker mvnd Test Runner

## Scope

This increment introduces a daemon-owned `TestRunner` abstraction and a Docker/mvnd implementation.
The Docker process execution is isolated behind `DockerCommandExecutor` so lifecycle and command
behavior are testable without requiring Docker in unit tests.

## Runner API

- `TestRunner.runTests(TestRunRequest request)`
- `TestRunner.stop()`
- `TestRunRequest` supports modules, specific test classes, retry-failing-tests flag, full-run flag,
  coverage report generation, and additional Maven args.
- `TestRunResult` returns success, exit code, stdout, stderr, container id, and the exact Maven
  command used.

## Docker mvnd Lifecycle

`DockerMvndTestRunner`:

- starts a Docker container on demand
- reuses the warm container for subsequent runs
- stops the container after configured idle timeout
- restarts the container if the executor reports it is no longer running
- runs Maven commands from the repository root

## Command Shape

Module-specific targeted tests produce:

```text
mvnd -pl <module> <args> <targetedTestProperty>=<tests> <goals>
```

Defaults come from typed config:

- image: `maven.docker.image`
- idle timeout: `maven.docker.containerIdleTimeout`
- goals: `maven.goals`
- args: `maven.args`
- targeted property: `maven.targetedTestProperty`
- prefer mvnd: `maven.preferMvnd`

## Tests

- The runner starts a Docker container on demand.
- A second run within the idle timeout reuses the warm container.
- Targeted test execution passes the configured targeted test property.
- Module-specific execution passes `-pl`.
- Idle reaping stops the container after configured inactivity.
