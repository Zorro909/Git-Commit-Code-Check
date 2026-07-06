# CR-010 Implementation Specification: Severity, Reporting, and Pre-Commit Behavior

## Scope

This increment makes mode-specific severity behavior explicit for command rendering and exit
decisions. Validation remains structured internally and the validation engine does not terminate the
process.

## Severity Policy

`ModeSeverityPolicy` defines visibility and blocking behavior by `ValidationMode`.

- Assistant and interactive modes display LOW, MEDIUM, and HIGH diagnostics.
- Batch mode displays all diagnostics, but only HIGH blocks.
- Pre-commit displays HIGH and MEDIUM diagnostics, hides LOW diagnostics, and only HIGH blocks.

## Terminal Reporting

`TerminalDiagnosticRenderer` renders human terminal output from structured diagnostics grouped by
file. It applies `ModeSeverityPolicy` before printing diagnostics and prints a concise blocking exit
reason when the mode will fail.

JSON and IDE renderers remain deferred, but mode filtering is isolated from command orchestration so
other renderers can reuse the same policy.

## Command Behavior

`CodeCheckCommandService` continues to return `CommandOutcome` instead of calling `System.exit`.
Batch and pre-commit exits are calculated from `ModeSeverityPolicy.blocks(...)`.

## Tests

- Pre-commit hides LOW diagnostics, prints MEDIUM diagnostics, and blocks on HIGH diagnostics.
- Batch exits nonzero when HIGH diagnostics exist.
- HIGH parse diagnostics from the validation engine block pre-commit.
- MEDIUM symbol diagnostics print in pre-commit without blocking.
- Assistant-mode validation results retain LOW, MEDIUM, and HIGH diagnostics.
