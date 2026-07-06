# CR-010: Severity, Reporting, and Pre-Commit Behavior

## Status

Proposed.

## Context

Severity must behave differently by mode. The daemon should show all useful feedback. Pre-commit should be strict enough to block serious problems but not noisy enough to block minor style issues.

The current `ValidationError` has LOW, MEDIUM, and HIGH, but exit behavior and output filtering need explicit policy.

## Severity Definitions

### HIGH

Blocking correctness or validity problem.

Examples:

- Java parse failure.
- Rule violation that must not be committed.
- Missing mandatory structure if configured as blocking.

### MEDIUM

Warning diagnostic or non-blocking rule issue.

Examples:

- Unresolved symbol diagnostic.
- Important rule feedback that should print in pre-commit but not block.

### LOW

Low-priority improvement.

Examples:

- Minor style or maintainability feedback.

## Mode Behavior

| Mode | HIGH | MEDIUM | LOW |
| --- | --- | --- | --- |
| Daemon assistant | Show | Show | Show |
| Interactive check | Show | Show | Show |
| Batch | Blocks | Prints | Optional display, non-blocking |
| Pre-commit | Blocks | Prints | Hidden |

Only HIGH findings block pre-commit.

MEDIUM findings print in pre-commit.

LOW findings do not print in pre-commit.

## Diagnostic Kinds

Severity and kind should be separate:

```java
enum DiagnosticKind {
    RULE_VIOLATION,
    PARSE_ERROR,
    SYMBOL_WARNING,
    TOOL_WARNING,
    COVERAGE_FAILURE
}
```

Unresolved symbols:

- Severity: MEDIUM.
- Kind: SYMBOL_WARNING.

Parse errors:

- Severity: HIGH.
- Kind: PARSE_ERROR.

## Reporting

Human terminal output is the primary format for now.

JSON and IDE integration are deferred, but results should be structured internally so renderers can be added later.

Pre-commit output should be concise:

- HIGH diagnostics.
- MEDIUM diagnostics.
- No LOW diagnostics.
- Clear final exit reason if blocking.

Daemon output/state can include all severities.

## Requirements

- Exit status must be based on mode and severity.
- Renderers must filter diagnostics by mode.
- The validation engine must not call `System.exit`.
- Pre-commit must hide LOW diagnostics.
- Batch mode must not require user input.

## Acceptance Criteria

- A HIGH parse error blocks pre-commit.
- A MEDIUM unresolved symbol prints but does not block pre-commit.
- A LOW magic-value finding does not print in pre-commit.
- The daemon stores and exposes LOW, MEDIUM, and HIGH diagnostics.
- Batch mode exits nonzero when HIGH findings exist.

## Open Questions

- Should batch mode block only HIGH like pre-commit, or should it optionally block MEDIUM by config?
- Should coverage failures default to HIGH or depend on policy?
- Should LOW diagnostics be printed in batch by default?

