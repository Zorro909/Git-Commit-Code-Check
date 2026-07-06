# CR-007: Watcher and Incremental State

## Status

Proposed.

## Context

Daemon/watch mode is core to the product. The watcher should not blindly watch the whole repository. It should watch files that active rules validate or consume as context. If a new rule starts caring about `pom.xml`, then `pom.xml` enters the watch plan.

The daemon stores latest state only; no historical trends are required.

## Goals

- Make watcher scope rule-driven.
- Debounce saved untracked/unstaged changes, defaulting to 5 seconds.
- Use higher debounce for expensive passing test/coverage reruns.
- Retry failing tests more often.
- Maintain latest validation state.
- Use explicit dependency invalidation.
- Restart daemon immediately on config changes rather than hot reload.

## Watch Plan

`RuleRegistry.watchPlan()` should combine:

- validated files
- context files
- project model files
- config files
- Maven build files needed by active rules
- generated context roots needed by rules

Examples:

- Java rules watch Java source roots.
- Java parser context watches generated source roots.
- Coverage rules watch JaCoCo reports and relevant test/source files.
- Maven project model watches `pom.xml` files.

## Debounce Policy

Default:

```yaml
daemon:
  saveDebounce: "5s"
```

Untracked and unstaged files should only be validated after the save debounce.

Test/coverage debounce is separate:

- Passing tests: longer debounce between reruns.
- Failing tests: shorter retry debounce.
- Exact defaults can be configured after implementation experience.

## Incremental State

The daemon stores:

- latest diagnostics by file
- stale/checking/current status by file
- latest project model
- latest test state
- latest coverage state
- available fixes by diagnostic

No history or trend storage is required.

## Dependency Graph

When validating file A reads context file B, changing B should cause A to be invalidated.

Store dependency edges in the direction needed for invalidation:

```text
context file B -> dependent validated file A
```

This replaces ambiguous `currentFile`-based mutable tracking.

## Config Changes

- Config files are watched.
- Config changes trigger immediate clean daemon restart.
- CLI attempts to reattach.
- Avoid hot reload of live rule/parser/watcher state.

## Requirements

- Watch scope must come from active rules and project model.
- Generated source roots may be watched as context but not as validation targets.
- Watcher must support Linux and Windows.
- Debounce must be configurable.
- Pre-commit/full scan must bypass watcher debounce.
- Dependency invalidation must be thread-safe.

## Acceptance Criteria

- Adding a rule that watches `pom.xml` adds POM files to the watch plan.
- Saving an unstaged Java file triggers validation after configured debounce.
- Changing generated context invalidates dependent validated files.
- Config change restarts daemon.
- Pre-commit performs an immediate full scan for selected files.

## Open Questions

- Should the first implementation watch directories recursively or specific files where possible?
- Should generated context changes always trigger revalidation, or only when dependency edges exist?
- Should file events during restart be ignored or queued?

