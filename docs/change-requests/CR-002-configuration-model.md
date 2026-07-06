# CR-002: Configuration Model

## Status

Proposed.

## Context

The tool needs both personal settings and repository/team policy. User config is appropriate for personal preferences such as debounce and IDE command. Repo config is appropriate for branch policy, Maven parameters, rule expectations, and coverage thresholds.

Config changes should not be hot-reloaded into a running daemon because live reload can leave inconsistent parser, watcher, rule, and test-runner state.

## Goals

- Support both user-level and repo-local config.
- Let repository config override user config.
- Allow CLI flags to override config for a single command.
- Auto-restart the daemon immediately when relevant config changes.
- Have the CLI reattach after restart.

## Config Locations

Recommended defaults:

```text
~/.config/git-commit-code-check/config.yaml
<repo>/.codecheck.yaml
```

Runtime daemon metadata remains separate under cache and is covered by CR-003.

## Precedence

Configuration should be resolved in this order:

```text
built-in defaults < user config < repo config < CLI flags
```

Repo config overrides user config because team policy should be reproducible within the repository.

## Initial Config Surface

```yaml
git:
  mainBranches: [develop, main, master]
  releaseBranchPattern: "release/.*"
  restageAfterFix: true

daemon:
  inactivityTimeout: "30m"
  saveDebounce: "5s"
  transport: websocket

java:
  languageLevel: 25
  generatedSourceDetection: maven-defaults

maven:
  runner: docker-mvnd
  preferMvnd: true
  docker:
    image: "team/mvnd-jdk25:latest"
    containerIdleTimeout: "10m"
    mountM2: true
  goals: ["test", "jacoco:report"]
  args: []
  targetedTestProperty: "-Dtest"

coverage:
  provider: jacoco
  freshnessMode: reuse-if-fresh
  reportPaths:
    - "target/site/jacoco/jacoco.xml"
    - "*/target/site/jacoco/jacoco.xml"
```

The exact schema can evolve, but these are the important early domains.

## Config Change Behavior

- The daemon watches config files as high-priority context.
- When config changes, the daemon should restart immediately.
- The restart should be clean: shut down watcher, transport, parser state, and test-runner lifecycle.
- Attached CLI sessions should attempt to reconnect after restart.
- If restart fails, the CLI should print the failure.

## Requirements

- Missing config files must be acceptable.
- Invalid config must fail fast with actionable diagnostics.
- Config parsing should happen before daemon startup decisions that depend on it.
- Repo config must be optional.
- User config must be optional.
- Config schema should be typed internally rather than passed around as maps.

## Acceptance Criteria

- A repo can set `git.mainBranches` and override the user's default.
- A repo can configure Maven test goals and Docker image.
- A user can configure save debounce and daemon inactivity timeout.
- Changing repo config causes daemon restart and CLI reattach.
- Invalid config prevents startup with a clear error.

## Open Questions

- Should there be a config validation command such as `codecheck config validate`?
- Should config support environment-variable interpolation?
- Should repo config be allowed to disable Docker test execution, or is tool usage enough opt-in?

