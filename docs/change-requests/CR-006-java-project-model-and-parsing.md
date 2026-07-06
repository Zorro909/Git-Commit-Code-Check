# CR-006: Java Project Model and Parsing

## Status

Proposed.

## Context

The current Java parsing logic lives in `JavaChecker`. It uses static mutable maps for parse and parser caches, derives source roots through string matching, and lowercases paths. This is acceptable for simple one-shot use, but not for a daemon with concurrent scans, multi-module Maven support, generated-source context, and test/coverage integration.

Maven is the only required build system for now.

## Goals

- Introduce a Maven-aware project model.
- Move JavaParser ownership into a dedicated service.
- Support multi-module Maven projects.
- Use generated sources as context but not validation targets.
- Treat parse failures as HIGH diagnostics.
- Treat unresolved symbols as MEDIUM warning diagnostics.
- Support source-only and dependency-aware symbol solving.

## Project Model

```java
interface ProjectModelService {
    ProjectModel currentModel();
    ProjectModel refresh();
}
```

`ProjectModel` should include:

- repository root
- Maven root
- modules
- module source roots
- module test roots
- generated source roots
- generated test source roots
- resource roots if needed later
- detected Java language level
- classpath/dependency metadata when available

## Maven Scope

Initial support:

- root `pom.xml`
- multi-module Maven reactors
- module-relative source roots
- module-relative test roots
- Maven default generated source directories

Generated source defaults:

```text
target/generated-sources/annotations
target/generated-test-sources/test-annotations
```

Generated sources are context roots, not validation roots.

## Parser Service

```java
interface JavaParserService {
    ParseOutcome parse(Path file);
    Optional<CompilationUnit> compilationUnit(Path file);
    void invalidate(Path file);
    void invalidateModule(ModuleId moduleId);
}
```

The parser service:

- Owns parser and parse caches.
- Uses daemon-safe synchronization or concurrent collections.
- Does not use static mutable state.
- Does not lowercase paths.
- Builds type solvers per module.
- Supports source-only mode and dependency-aware mode.
- Emits structured diagnostics for parse and symbol problems.

## Diagnostics

Parse failure:

- Severity: HIGH.
- Kind: validation diagnostic.
- Blocks batch/pre-commit according to severity policy.

Unresolved symbols:

- Severity: MEDIUM.
- Kind: warning diagnostic.
- Does not block pre-commit.
- Should be visible in daemon and interactive check.

## Requirements

- Multi-module Maven projects must resolve source/test roots correctly.
- Generated sources must not produce direct validation errors by default.
- Generated sources must be available for introspection and symbol solving.
- JavaParser cache invalidation must be safe for daemon/watch mode.
- Path handling must be case-sensitive where the filesystem is case-sensitive.

## Acceptance Criteria

- A module source file is parsed with its module source roots.
- A module test file can resolve main source types when dependencies are available.
- A generated MapStruct implementation can be loaded as context.
- A parse error becomes a HIGH diagnostic for that file.
- An unresolved symbol becomes a MEDIUM warning diagnostic.

## Open Questions

- Should dependency classpath be obtained from Maven commands, parsed POMs, or both?
- Should Java language level default to config or Maven compiler properties?
- Should parser service expose ASTs directly to rules or wrap common queries in higher-level APIs?

