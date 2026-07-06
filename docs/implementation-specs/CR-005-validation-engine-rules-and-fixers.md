# CR-005 Implementation Specification: Validation Engine, Rules, and Fixers

## Scope

This increment introduces a structured validation engine and rule/fixer contracts while preserving
the existing `CodeCheck`, `FixAction`, and `PostAction` implementations through adapters.

## Core Types

- `Rule`, `RuleId`, `RuleMetadata`, `FileInterest`, `ValidationContext`
- `Diagnostic`, `DiagnosticKind`, `SourcePosition`, `ValidationMode`
- `ValidationEngine`, `ValidationResult`, `FileValidationResult`
- `Fixer`, `FixerId`, `FixerMetadata`, `FixInteraction`, `FixPlan`, `FixResult`
- `RuleRegistry`, `WatchPlan`

## Migration Strategy

- Existing `CodeCheck` implementations expose default rule metadata and file interests.
- Existing `FixAction` implementations expose default fixer metadata.
- `DefaultRuleRegistry` adapts injected `CodeCheck` instances into rules and `FixAction`
  instances into fixers.
- `DefaultValidationEngine` depends only on `RuleRegistry`, `ChangeSet`, and validation types.
  It does not print, call `System.exit`, launch editors, or depend on Picocli.
- `ValidationCheckPipeline` delegates validation to the engine for checks and keeps its
  compatibility methods for existing command code and tests.

## Fix Semantics

- Interactive fixers are filtered out for `BATCH` and `PRE_COMMIT` modes.
- User-triggered fixing is represented by `FixApplicationService`.
- A fix result reports modified/created files.
- Affected files are fully rechecked after the fix.
- Post actions, including Git restaging, run only when the affected-file recheck passes.

## File Interests

`NoMagicValuesCheck` declares Java main-source validation interest:

```text
src/main/java/**/*.java
```

This interest is visible through the rule registry watch plan.

## Tests

- Existing validation checks are available as structured rules.
- `NoMagicValuesCheck` exposes Java main-source file interest.
- The validation engine returns structured diagnostics without printing.
- Manual editor fixing is represented as an interactive fixer.
- Interactive fixers are unavailable in batch mode.
- A user-triggered fix that affects two files restages both files after successful recheck.
