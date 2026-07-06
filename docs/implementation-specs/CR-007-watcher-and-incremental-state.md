# CR-007 Implementation Specification: Watcher and Incremental State

## Scope

This increment introduces rule-driven watch planning, configurable debounce primitives, latest
validation state storage, dependency invalidation, and config-change restart signaling. The existing
JDK `WatchService` remains the file event source, but its debounce duration now comes from config.

## Watch Scope

`WatchScopeService` combines:

- active rule validated/context interests from `RuleRegistry.watchPlan()`
- Maven module source/test roots from `ProjectModel`
- generated source/test roots as context roots
- root and module `pom.xml` files
- user and repository config files

Generated roots are context watches, not validation watches.

## Debounce

`DebouncedFileUpdateScheduler` is a deterministic debounce primitive used by tests and intended for
the watcher. It tracks pending paths and releases them only after the configured save debounce.
Pre-commit/full scan paths bypass this scheduler by invoking `ChangeSetService` and
`ValidationEngine` directly.

## Incremental State

`IncrementalValidationState` stores latest diagnostics and status by file:

- `STALE`
- `CHECKING`
- `CURRENT`

No history is retained.

## Dependency Invalidation

`DependencyInvalidationGraph` stores edges as:

```text
context file -> dependent validated file
```

When a context file changes, all dependents are marked stale.

## Config Changes

`ConfigChangeRestartSignal` recognizes user and repo config paths and raises
`DaemonRestartRequiredException`. Later daemon lifecycle work can catch this and perform the clean
restart/reattach loop.

## Tests

- Rule interest in `pom.xml` adds POM files to the watch scope.
- Java source interests add module Java roots as validation watches.
- Generated roots are watched as context only.
- Debounced saves release paths only after configured debounce.
- Context changes mark dependent files stale.
- Config changes raise restart-required.
