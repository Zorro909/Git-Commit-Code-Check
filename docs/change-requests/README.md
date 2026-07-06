# Git Commit Code Check - Change Request Index

This directory captures the agreed architecture direction for evolving Git Commit Code Check from a CLI-centric checker into a daemon-backed IDE assistant that can also run as a pre-commit verifier.

The main product is a per-repository local assistant daemon. The CLI starts or attaches to that daemon by default. Hook and batch modes reuse the same validation core, but run with stricter non-interactive behavior.

## Change Requests

| ID | Topic | Summary | Detail |
| --- | --- | --- | --- |
| CR-001 | Product modes and command model | Defines daemon-first assistant behavior, one-shot interactive checks, and non-interactive pre-commit verification. | [CR-001 Product Modes and Command Model](./CR-001-product-modes-and-command-model.md) |
| CR-002 | Configuration model | Defines built-in, user, repo, and CLI config precedence, restart behavior, and initial config surface. | [CR-002 Configuration Model](./CR-002-configuration-model.md) |
| CR-003 | Daemon lifecycle and transport | Defines per-repository daemon identity, cache metadata, WebSocket transport, auth token, inactivity shutdown, and restart behavior. | [CR-003 Daemon Lifecycle and Transport](./CR-003-daemon-lifecycle-and-transport.md) |
| CR-004 | Git change-set policy | Defines branch-aware file selection, pre-commit staged-path behavior, deleted-file handling, and untracked-file inclusion. | [CR-004 Git Change-Set Policy](./CR-004-git-change-set-policy.md) |
| CR-005 | Validation engine, rules, and fixers | Defines the core validation engine, rule/fixer extension contracts, batch restrictions, and post-fix restaging. | [CR-005 Validation Engine, Rules, and Fixers](./CR-005-validation-engine-rules-and-fixers.md) |
| CR-006 | Java project model and parsing | Defines Maven-only project modeling, JavaParser ownership, generated-source context, symbol diagnostics, and multi-module support. | [CR-006 Java Project Model and Parsing](./CR-006-java-project-model-and-parsing.md) |
| CR-007 | Watcher and incremental state | Defines rule-driven watch scope, debounce policy, latest-state storage, dependency invalidation, and config-change restart handling. | [CR-007 Watcher and Incremental State](./CR-007-watcher-and-incremental-state.md) |
| CR-008 | Docker mvnd test runner | Defines daemon-owned long-lived Docker/mvnd lifecycle, Maven command configuration, targeted test support, and timeouts. | [CR-008 Docker mvnd Test Runner](./CR-008-docker-mvnd-test-runner.md) |
| CR-009 | JaCoCo coverage and MapStruct | Defines coverage freshness, rerun policy, threshold modeling, MapStruct generated-implementation attribution, and report consumption. | [CR-009 JaCoCo Coverage and MapStruct](./CR-009-jacoco-coverage-and-mapstruct.md) |
| CR-010 | Severity, reporting, and pre-commit behavior | Defines HIGH/MEDIUM/LOW behavior across daemon, interactive check, batch, and pre-commit modes. | [CR-010 Severity, Reporting, and Pre-Commit Behavior](./CR-010-severity-reporting-and-precommit.md) |
| CR-011 | Migration roadmap | Proposes an incremental refactor path from the current codebase to the target architecture. | [CR-011 Migration Roadmap](./CR-011-migration-roadmap.md) |

## Cross-Cutting Decisions

- The default command starts or attaches to a per-repository daemon.
- The daemon is silent on successful startup and reports only failures.
- The CLI attaches through a bidirectional transport; initial target is localhost WebSocket with a local auth token.
- Repo config overrides user config.
- Config changes should trigger a clean daemon restart rather than hot reload.
- Pre-commit starts or attaches to the daemon, invokes a full scan, and bypasses watcher debounce.
- Only HIGH findings block pre-commit and batch mode.
- MEDIUM findings print in pre-commit but do not block.
- LOW findings are hidden in pre-commit.
- Interactive fixes are only applied after explicit user action.
- Restaging happens after a full affected-file recheck passes, and includes files created or modified by the tool.
- Maven is the only supported build system for now.
- Tests and coverage run inside a daemon-owned long-lived Docker container using mvnd.
- Generated sources are not validated directly, but may be used for parsing, symbol resolution, introspection, and coverage mapping.
- MapStruct mapper coverage is first-class and calculated from mapper interfaces plus generated implementations.
- Unresolved symbols are MEDIUM warning diagnostics for now.

