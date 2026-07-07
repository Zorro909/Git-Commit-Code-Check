# CR-003 Implementation Specification: Daemon Lifecycle and Transport

## Scope

This increment makes daemon lifecycle state explicit and per repository. It introduces stable
repository identity, cache metadata, dynamic localhost ports, local auth tokens, stale metadata
cleanup, attach-if-alive command behavior, and inactivity shutdown. The current transport remains
the existing JDK local server behind daemon metadata; the metadata uses the target `websocket`
transport name so the CR-003 WebSocket transport can replace the control server without changing
registry or command flow.

## Runtime Metadata

Metadata is stored under:

```text
~/.cache/git-commit-code-check/repos/<repo-id>/
  daemon.json
  daemon.pid
```

`repo-id` is a SHA-256 hash of the canonical repository path. The raw path is not used as a cache
directory name.

## Startup

1. Resolve current repository directory.
2. Compute the repository id and metadata path.
3. Read existing metadata.
4. If the recorded process is alive, treat the daemon as attached and return silently.
5. If metadata is stale, remove it.
6. Allocate a random localhost port and auth token.
7. Write metadata and start the daemon server.
8. If startup fails, remove metadata and print the failure through the command service.

## Transport Control

The server binds to `127.0.0.1` and a random available port. Control endpoints require the
`X-CodeCheck-Token` header:

- `GET /health`
- `GET /check`
- `POST /shutdown`

Unauthorized requests return `401`. An empty token never authorizes; a daemon cannot run without
authentication.

On POSIX file systems the metadata directory and files are readable and writable only by the
owning user, so the auth token is not exposed to other local users.

## Inactivity

The daemon receives the configured inactivity timeout. Authorized control requests and file updates
refresh activity. When idle time exceeds the timeout, the server stops.

## Tests

- Repository ids are stable hashes and metadata paths do not expose raw repository paths.
- Alive metadata is loaded and stale metadata is cleaned.
- New metadata uses localhost, a random port, a non-empty token, and writes `daemon.json` plus
  `daemon.pid`.
- Control endpoints reject missing tokens and accept the correct token.
- Empty-token metadata rejects every request.
- Metadata files are owner-only on POSIX file systems.
- The registry bean resolves through the qualified repository directory path.
