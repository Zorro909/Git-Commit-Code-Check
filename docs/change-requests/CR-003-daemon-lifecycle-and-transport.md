# CR-003: Daemon Lifecycle and Transport

## Status

Proposed.

## Context

The assistant should behave similarly to Gradle and mvnd: CLI commands start or attach to a daemon. The daemon is per repository, works on Linux and Windows, and shuts down after inactivity.

Plain request/response HTTP is insufficient for assistant UX because it encourages polling. The CLI needs live updates from the daemon.

## Goals

- Provide a per-repository daemon.
- Support Linux and Windows.
- Use a bidirectional transport to avoid polling.
- Keep daemon startup silent unless it fails.
- Shut down after 30 minutes of inactivity by default.
- Restart immediately on config change and allow CLI reattach.

## Runtime Metadata

Use a cache directory keyed by repository identity:

```text
~/.cache/git-commit-code-check/repos/<repo-id>/
  daemon.json
  daemon.pid
```

`repo-id` should be a stable hash of canonical repository root and Git worktree identity. Do not use the raw path as a directory name.

Example `daemon.json`:

```json
{
  "pid": 12345,
  "repoRoot": "/path/to/repo",
  "transport": "websocket",
  "host": "127.0.0.1",
  "port": 49152,
  "token": "random-local-auth-token",
  "startedAt": "2026-07-06T20:00:00Z"
}
```

## Transport

Initial transport should be localhost WebSocket:

- Cross-platform.
- Bidirectional.
- Avoids polling.
- Easy for CLI attach sessions.
- Can coexist with a minimal control endpoint if useful.

The daemon must bind to `127.0.0.1`, not all interfaces.

The metadata token is required because localhost ports are not a complete trust boundary.

Future transport abstraction:

```java
interface DaemonTransport {
    void start();
    void stop();
    void publish(DaemonEvent event);
}
```

Potential later implementations:

- Unix domain socket on Linux.
- Named pipe on Windows.
- Localhost WebSocket fallback.

## WebSocket Message Model

Commands from CLI to daemon:

- `status.get`
- `scan.full`
- `scan.files`
- `fix.apply`
- `tests.run`
- `shutdown`

Events from daemon to CLI:

- `daemon.started`
- `daemon.stopping`
- `validation.started`
- `validation.file.updated`
- `validation.completed`
- `tests.started`
- `tests.completed`
- `coverage.updated`
- `config.changed`
- `daemon.restart.required`
- `daemon.restart.failed`

## Lifecycle

Startup:

1. CLI finds repository root.
2. CLI computes repo id.
3. CLI reads daemon metadata.
4. If daemon is alive, attach.
5. If daemon is not alive, spawn daemon.
6. CLI waits for metadata and transport readiness.
7. Successful startup remains silent unless the command requires output.

Inactivity:

- Default timeout: 30 minutes.
- Configurable.
- Any CLI connection, scan, fix, file event, or test execution refreshes activity.
- Long-running test execution prevents idle shutdown.

Config change:

- Daemon detects config change.
- Daemon restarts immediately.
- Attached CLI attempts to reattach.
- If restart fails, print failure.

## Requirements

- Daemon must be per repository.
- Successful daemon startup must not print noise.
- Stale metadata must be detected and cleaned.
- Transport auth token must be required.
- Random available port should be used instead of a hardcoded port.
- Daemon must shut down after inactivity by default.

## Acceptance Criteria

- Running `codecheck` twice in the same repo attaches to the same daemon.
- Running `codecheck` in two repos starts separate daemons.
- A CLI session receives validation events without polling.
- The daemon exits after configured inactivity.
- Config changes restart the daemon and the CLI reattaches.

## Open Questions

- Should daemon stdout/stderr be redirected to a file even though explicit logs are not required now?
- Should the daemon expose an emergency stop command in the first version?
- Should dead daemon detection rely on pid, transport health check, or both?

