# CR-011: Migration Roadmap

## Status

Proposed.

## Context

The current codebase already has useful pieces: command entry point, selectors, validation checks, fix actions, Git staging, JavaParser use, tests, and daemon/watch experiments. The target architecture should be reached incrementally without discarding working behavior.

## Goals

- Preserve current tests while extracting clearer boundaries.
- Reduce risk by introducing abstractions before large behavior changes.
- Make daemon-first behavior possible without coupling it to rule implementation details.

## Phase 1: Core Validation Extraction

- Introduce `Diagnostic` or evolve `ValidationError` toward structured diagnostics.
- Extract `ValidationEngine` from `ValidationCheckPipeline`.
- Move printing and exit behavior out of the engine.
- Convert field injection to constructor injection for testability.
- Add mode-aware result rendering.

Success criteria:

- Existing check tests still pass.
- Engine can be called without CLI.
- No core validation path calls `System.exit`.

## Phase 2: Change Set Service

- Replace CLI-option-selected `FileSelector` with `ChangeSetService`.
- Implement pre-commit staged-path policy.
- Implement branch-aware assistant/check policy.
- Factor shared Git command execution with exit/stderr handling.

Success criteria:

- Pre-commit file selection is explicit.
- Feature branch direct diff against configured main branch works.
- Deleted files are ignored.

## Phase 3: Config Loading

- Add typed config model.
- Load built-in defaults, user config, repo config, CLI overrides.
- Add config validation.
- Use config for main branch list and debounce values.

Success criteria:

- Repo config overrides user config.
- Invalid config fails clearly.
- Existing defaults match current behavior where possible.

## Phase 4: Daemon Registry and Transport

- Add per-repo daemon metadata under `~/.cache`.
- Use dynamic localhost WebSocket transport with auth token.
- Implement start/attach behavior.
- Add inactivity shutdown.

Success criteria:

- `codecheck` starts or attaches per repo.
- Two repos get separate daemons.
- CLI receives events without polling.

## Phase 5: Project Model and Parser Service

- Introduce Maven `ProjectModelService`.
- Move JavaParser cache from `JavaChecker` to `JavaParserService`.
- Remove static mutable parser/cache maps.
- Add generated source context handling.
- Emit parse and symbol diagnostics.

Success criteria:

- Multi-module Maven roots are modeled.
- Generated sources are context-only.
- Parse failure is HIGH.
- Unresolved symbols are MEDIUM warnings.

## Phase 6: Rule Interests and Watcher Rewrite

- Add rule-declared validated/context file interests.
- Build watch plan from active rules and project model.
- Add configurable debounce.
- Add dependency invalidation graph.
- Restart daemon on config changes.

Success criteria:

- Watcher scope changes when active rules change.
- Untracked/unstaged saves validate after debounce.
- Config change restarts daemon and CLI reattaches.

## Phase 7: Fixer Metadata and Restaging

- Add fixer interaction metadata.
- Disallow interactive fixers in batch/pre-commit.
- Track created/modified files from fix results.
- Full recheck affected files.
- Restage after successful recheck.

Success criteria:

- Manual editor fixer is interactive.
- Batch mode does not invoke it.
- Interactive check restages tool-modified files after passing recheck.

## Phase 8: Docker mvnd Runner

- Add daemon-owned Docker mvnd runner.
- Keep container warm with 10 minute timeout.
- Support root-level Maven execution with `-pl`.
- Support targeted test requests.

Success criteria:

- Test runner can run targeted tests in Docker.
- Container is reused within idle timeout.
- Runner shuts down after idle timeout.

## Phase 9: JaCoCo and MapStruct Coverage

- Consume JaCoCo XML.
- Detect stale reports.
- Trigger test runner when needed.
- Add threshold policy matching.
- Add first-class MapStruct mapper coverage attribution.

Success criteria:

- Fresh reports are reused.
- Stale reports trigger tests.
- MapStruct mapper coverage reports against mapper interfaces.

## Phase 10: UX Polish

- Improve human terminal rendering.
- Add `status`, `fix`, and daemon control commands.
- Prepare internal renderer boundary for future JSON/IDE output.

Success criteria:

- CLI remains concise.
- Daemon state is inspectable.
- Future JSON/IDE support can be added without changing validation core.

