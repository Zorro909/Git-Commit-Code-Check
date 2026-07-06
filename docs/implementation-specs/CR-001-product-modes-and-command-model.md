# CR-001 Implementation Specification: Product Modes and Command Model

## Scope

This increment introduces explicit product modes at the command boundary while preserving the
current validation pipeline and file selector implementation for later refactors.

## Commands

- `git-commit-code-check`
  - Starts or attaches to the assistant daemon for the current repository.
  - Successful startup is silent.
  - Startup failures are printed to stderr and return a non-zero exit code.
- `git-commit-code-check check`
  - Runs an interactive one-shot validation.
  - May invoke fix actions.
  - Runs post actions after successful fixing.
- `git-commit-code-check check --batch`
  - Runs one-shot validation without invoking fix actions.
  - Returns non-zero when HIGH diagnostics are present.
- `git-commit-code-check pre-commit`
  - Runs deterministic non-interactive validation.
  - Does not invoke fix actions or post actions.
  - Hides LOW diagnostics, prints MEDIUM and HIGH diagnostics, and blocks only on HIGH.
- `git-commit-code-check status`
  - Delegates status reporting to the assistant daemon boundary.
- `git-commit-code-check fix`
  - Provides an explicit user-triggered fix command boundary for later daemon-backed fixing.

## Design

- Picocli command classes stay thin and delegate to `CodeCheckCommandService`.
- `CodeCheckCommandService` owns product-mode behavior, validation rendering, and exit-code
  decisions.
- `AssistantDaemonController` is the daemon boundary used by command modes. The CR-001
  implementation delegates to the existing in-process daemon server; CR-003 will replace this
  with start/attach metadata and WebSocket transport.
- The validation engine is not replaced in this increment. `ValidationCheckPipeline` remains the
  shared validation core until CR-005 extracts the structured engine.

## Mode Policy

| Mode | Fix actions | Printed diagnostics | Blocking diagnostics |
| --- | --- | --- | --- |
| Assistant daemon | none from CLI startup | daemon-owned | startup failure |
| Interactive check | yes | LOW, MEDIUM, HIGH | remaining diagnostics unless `--no-exit-code` |
| Batch check | no | LOW, MEDIUM, HIGH | HIGH |
| Pre-commit | no | MEDIUM, HIGH | HIGH |

## Tests

- Bare command mode delegates to daemon start/attach and stays silent on success.
- Daemon startup failures are clear and non-zero.
- Batch mode never invokes fix actions.
- Pre-commit hides LOW diagnostics and blocks only on HIGH.
- Interactive check still invokes fix actions and post actions after successful recheck.
