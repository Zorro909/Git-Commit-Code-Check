# CR-002 Implementation Specification: Configuration Model

## Scope

This increment adds typed configuration loading for built-in defaults, user config, repository
config, and command-scoped overrides. Later daemon and watcher branches will use the same loader
to restart on config file changes.

## Locations

- User config: `~/.config/git-commit-code-check/config.yaml`
- Repository config: `<repo>/.codecheck.yaml`

Missing files are accepted.

## Precedence

Configuration is resolved in this order:

```text
built-in defaults < user config < repo config < CLI overrides
```

Repo config wins over user config so repository policy is reproducible.

## Typed Model

The internal model is `CodeCheckConfig` with typed nested domains:

- `git`: main branches, release branch pattern, restage-after-fix
- `daemon`: inactivity timeout, save debounce, transport
- `java`: language level, generated source detection
- `maven`: runner, mvnd preference, Docker settings, goals, args, targeted test property
- `coverage`: provider, freshness mode, report paths

Durations are parsed into `java.time.Duration`. Enum values are parsed case-insensitively and
accept YAML names such as `docker-mvnd`, `maven-defaults`, and `reuse-if-fresh`.

## Validation

Invalid YAML, wrong node types, unknown enum values, invalid durations, and invalid integer ranges
raise `ConfigException` with the config file and field path in the message.

## Command Integration

`CodeCheckCommandService` loads configuration before daemon startup or validation commands. Invalid
configuration prevents the command from running and returns a non-zero exit code with a clear error.

## Tests

- Missing config files return built-in defaults.
- User config overrides defaults.
- Repo config overrides user config.
- Maven goals and Docker image are configurable from repo config.
- CLI overrides win over file config.
- Invalid config fails fast with field-specific diagnostics.
