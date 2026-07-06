# CR-005: Validation Engine, Rules, and Fixers

## Status

Proposed.

## Context

The current `ValidationCheckPipeline` performs orchestration, fixing, rechecking, and post actions. It returns grouped errors, but it prints in several paths and relies on nullable injected lists. Rules are directly coupled to JavaParser base behavior.

The target architecture needs a reusable validation core for daemon, interactive check, batch, and pre-commit.

## Goals

- Introduce a structured validation engine.
- Keep rules extensible through internal DI without central switch statements.
- Support rule-declared file interests for watcher planning.
- Support fixer metadata and user-triggered application.
- Prevent interactive fixers in batch/pre-commit.
- Restage tool-modified files after successful full affected-file recheck.

## Core Types

```java
interface Rule {
    RuleId id();
    RuleMetadata metadata();
    FileInterest validatedFiles();
    FileInterest contextFiles();
    List<Diagnostic> check(ValidationContext context, Path file);
}
```

```java
interface Fixer {
    FixerId id();
    FixerMetadata metadata();
    boolean canFix(Diagnostic diagnostic);
    FixPlan plan(Diagnostic diagnostic);
    FixResult apply(FixPlan plan);
}
```

```java
enum FixInteraction {
    NONE,
    INTERACTIVE
}
```

```java
record Diagnostic(
    Path file,
    String message,
    SourcePosition position,
    Severity severity,
    DiagnosticKind kind,
    RuleId ruleId
) {}
```

## Validation Engine

```java
interface ValidationEngine {
    ValidationResult validate(ChangeSet changeSet, ValidationMode mode);
    FileValidationResult validateFile(Path file, ValidationMode mode);
}
```

The engine:

- Does not print.
- Does not call `System.exit`.
- Does not open editors.
- Returns structured results.
- Uses active rule registry.
- Uses project model and parser services through context.

## Rule Registry

The rule registry should be populated through DI:

```java
interface RuleRegistry {
    List<Rule> activeRules();
    List<Fixer> activeFixers();
    WatchPlan watchPlan();
}
```

Adding a new internal rule should require adding a class and optional config defaults, not editing the core engine.

## Fix Semantics

Fixes are never applied automatically while the developer is typing.

User-triggered fix flow:

1. User selects a diagnostic/fix.
2. Daemon computes or retrieves a fix plan.
3. Fixer applies the fix.
4. Fix result reports created/modified files.
5. Validation engine performs a full recheck of affected files.
6. If recheck passes for affected files, Git service restages all files created or modified by the tool.
7. If recheck fails, files remain unstaged and remaining diagnostics are shown.

Interactive `codecheck check` uses the same semantics.

Batch/pre-commit:

- Interactive fixers are unavailable.
- Non-interactive fixers may be listed but should not be auto-applied unless an explicit command requests it.

## Requirements

- Validation results must be structured internally even if JSON output is deferred.
- Rules must declare validated and context file interests.
- Fixers must declare whether they require interaction.
- Fix application must report affected files.
- Restaging must happen after successful affected-file recheck.
- The engine must not depend on CLI state or Picocli objects.

## Acceptance Criteria

- `NoMagicValuesCheck` can be represented as a rule with Java main-source file interests.
- Manual IDE fixing is represented as an interactive fixer.
- Batch mode treats manual IDE fixing as unavailable.
- A user-triggered fix that edits two files causes both files to be restaged after successful recheck.
- Validation engine can be invoked by daemon and pre-commit without different rule logic.

## Open Questions

- Should non-interactive autofixes exist in the first refactor?
- Should rule ordering be explicit?
- Should fixers be allowed to create files outside the repository?

