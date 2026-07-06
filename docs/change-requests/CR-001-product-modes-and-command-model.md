# CR-001: Product Modes and Command Model

## Status

Proposed.

## Context

The project should primarily behave as an IDE assistant for fast local feedback. It should also support a pre-commit-hook use case, but the hook should only verify rules and must not require interactive fixing.

The current implementation is CLI-first: command-line options influence Micronaut bean selection and the command executes a short-lived validation pipeline. Daemon/watch mode exists, but it is not yet the main organizing architecture.

## Goals

- Make daemon-backed assistant mode the default product behavior.
- Keep hook and batch behavior deterministic and non-interactive.
- Share the same validation core across daemon, interactive CLI, and pre-commit modes.
- Avoid hidden environment detection for hooks. Users explicitly choose hook-friendly commands.
- Support Linux and Windows.

## Non-Goals

- Full IDE protocol integration in this change.
- JSON/SARIF output in this change.
- Gradle support.
- Plugin loading from external jars.

## Proposed Command Model

```text
codecheck
```

Starts or attaches to the per-repository daemon. This is the default assistant entry point.

```text
codecheck check
```

Runs an interactive one-shot check. It may attach to the daemon and may offer interactive fix actions.

```text
codecheck check --batch
```

Runs a non-interactive one-shot check. Interactive fixers are unavailable and matching findings remain failures according to severity policy.

```text
codecheck pre-commit
```

Starts or attaches to the daemon, invokes a full scan for staged paths, bypasses watcher debounce, and exits with hook-friendly status.

```text
codecheck status
```

Attaches to the daemon and prints current validation/test/coverage state.

```text
codecheck fix
```

Requests a user-selected fix for a known finding. This is explicitly user-triggered.

## Execution Modes

### Assistant Daemon

- Default mode.
- Keeps project state and parser/test/coverage state warm.
- Watches files that are relevant to active rules.
- Stores latest validation state only.
- Lists possible fixes but does not apply them automatically.

### Interactive Check

- Can attach to the daemon for warm state.
- Can invoke interactive fixers when the user chooses.
- Restages files created or modified by the tool after a full affected-file recheck passes.

### Batch Check

- Non-interactive.
- No prompt, editor launch, or user intervention.
- Interactive-only fixers are treated as unavailable.
- Exit code is determined by severity policy.

### Pre-Commit

- Non-interactive.
- Starts or attaches to daemon.
- Bypasses watcher/debounce by invoking a full scan directly.
- Uses staged paths for selection but validates working tree content.
- Only HIGH findings block.

## Requirements

- Bare `codecheck` must start or attach to the daemon for the current repository.
- Successful daemon startup must be silent.
- Daemon startup failures must be printed clearly.
- Pre-commit must not apply fixes automatically.
- The hook command must not rely on hook detection.
- CLI commands must be predictable and mode-specific.

## Acceptance Criteria

- A user can run `codecheck` in a repository and get daemon-backed assistant behavior.
- A pre-commit hook can call `codecheck pre-commit` and receive deterministic exit status.
- Batch mode never opens an editor or waits for user input.
- Interactive check can apply a user-selected fix and restage after successful recheck.

## Open Questions

- Should `codecheck` attach and remain connected, or attach, print status, and exit by default?
- Should `codecheck check` force a fresh scan even if daemon state is current?
- Should there be an explicit `codecheck daemon stop` command in the first daemon refactor?

