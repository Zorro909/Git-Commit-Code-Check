# Migration Roadmap

This roadmap records the incremental path from the original command-driven checker to the
daemon-first validation architecture. The implementation is intentionally stacked so each phase
introduces one boundary before later phases depend on it.

## Current Stack

| Phase | Implementing CR | Branch | Primary Boundary |
| --- | --- | --- | --- |
| 1 | CR-001 | `cr-001-product-modes-command-model` | Product modes, command service, daemon controller |
| 2 | CR-002 | `cr-002-configuration-model` | Typed config loading and validation |
| 3 | CR-003 | `cr-003-daemon-lifecycle-transport` | Per-repo daemon registry and authenticated transport |
| 4 | CR-004 | `cr-004-git-change-set-policy` | Branch-aware Git change set selection |
| 5 | CR-005 | `cr-005-validation-engine-rules-fixers` | Structured validation engine, rule registry, fixer contracts |
| 6 | CR-006 | `cr-006-java-project-model-parsing` | Maven project model and parser service |
| 7 | CR-007 | `cr-007-watcher-incremental-state` | Watch scope, debounce, incremental invalidation |
| 8 | CR-008 | `cr-008-docker-mvnd-test-runner` | Daemon-owned Docker mvnd test runner |
| 9 | CR-009 | `cr-009-jacoco-coverage-mapstruct` | JaCoCo report freshness, thresholds, MapStruct attribution |
| 10 | CR-010 | `cr-010-severity-reporting-precommit` | Mode-aware severity rendering and exit policy |

## Phase Details

### Phase 1: Core Command Shape

CR-001 establishes mode-specific commands and isolates command orchestration from daemon control.
This keeps the CLI surface stable while later phases replace internals behind the command service.

Validation:

- command routing tests
- assistant/check/pre-commit mode tests

### Phase 2: Configuration

CR-002 introduces a typed configuration model with defaults, user/repo merge order, and validation
errors that fail early before daemon or validation work starts.

Validation:

- default config tests
- repo-over-user override tests
- invalid config diagnostics

### Phase 3: Daemon Registry And Transport

CR-003 adds per-repository daemon metadata, dynamic localhost ports, auth tokens, stale metadata
cleanup, and basic server lifecycle behavior.

Validation:

- metadata persistence tests
- stale process cleanup tests
- daemon server lifecycle tests

### Phase 4: Change Sets

CR-004 replaces selector-only behavior with a Git-backed `ChangeSetService`. It models pre-commit
staged paths, feature branch direct diffs, deleted-file exclusion, and shared Git command error
handling.

Validation:

- pre-commit staged path tests
- direct diff against configured main branch tests
- deleted file exclusion tests

### Phase 5: Validation Engine And Fixers

CR-005 extracts structured validation contracts from the legacy pipeline while preserving existing
checks. It adds rule metadata, fixer metadata, and a mode-aware fixer application service.

Validation:

- engine validation tests
- fixer registry/application tests
- rule metadata tests

### Phase 6: Java Project Model And Parsing

CR-006 moves Maven root detection and JavaParser state into dedicated services. Parse errors become
HIGH diagnostics and unresolved symbols become MEDIUM diagnostics.

Validation:

- Maven project model tests
- parser success/failure tests
- parse and symbol diagnostic severity tests

### Phase 7: Watcher And Incremental State

CR-007 adds rule-driven watch scopes, debounce scheduling, incremental validation state, dependency
invalidation, and config-change restart signaling.

Validation:

- watch scope tests
- debounce tests
- incremental invalidation tests
- config restart tests

### Phase 8: Docker mvnd Runner

CR-008 introduces the test runner abstraction and a daemon-owned Docker mvnd runner with warm
container reuse, idle shutdown, module targeting, and coverage-report goal support.

Validation:

- container startup/reuse tests
- targeted module/test command tests
- idle shutdown tests

### Phase 9: Coverage And MapStruct

CR-009 adds JaCoCo XML parsing, report freshness checks, test-runner refresh, threshold matching,
and MapStruct generated implementation attribution back to mapper interfaces.

Validation:

- fresh report reuse tests
- stale report refresh tests
- threshold matching tests
- MapStruct coverage diagnostic tests

### Phase 10: Severity And Reporting

CR-010 makes mode-specific severity behavior explicit. Pre-commit hides LOW diagnostics, prints
MEDIUM diagnostics, and blocks only on HIGH diagnostics. Batch mode displays all diagnostics and
blocks on HIGH. The renderer boundary is ready for future JSON or IDE output.

Validation:

- severity policy tests
- command-level pre-commit tests
- batch blocking tests
- validation result severity retention tests

## Deferred Work

The migration intentionally leaves the following items for future CRs:

- JSON and IDE renderers over the existing diagnostic model
- richer daemon status snapshots for test and coverage progress
- targeted test selection from historical coverage data
- user-configurable coverage threshold defaults
- host mvnd fallback if Docker is unavailable
- broader integration between coverage diagnostics and rule registry execution

Review follow-ups recorded while merging the stack:

- wire `WatchScopeService`, `DebouncedFileUpdateScheduler`, `IncrementalValidationState`, and
  `ConfigChangeRestartSignal` into `FileWatcher` and the daemon lifecycle (CR-007 scaffolding)
- wire `TestRunner`, `CoverageService`, and `CoverageDiagnosticService` into validation execution
  (CR-008/CR-009 scaffolding)
- consolidate the legacy `CodeCheckCommandService` constructors and inject the renderer beans
  instead of constructing them inline
- expose CLI flags that populate `ConfigOverrides` so command-scoped overrides are reachable
  end to end
- resolve the repository root from Git instead of the current working directory in
  `RepositoryPathProvider`

## Operational Guidance

Future changes should keep the same migration rules:

- introduce a boundary before replacing behavior behind it
- preserve the full Maven test suite at each stacked branch
- keep generated sources as context unless a future CR explicitly makes them validation targets
- keep command exit decisions outside the validation engine
- keep daemon state structured so renderers can evolve independently
