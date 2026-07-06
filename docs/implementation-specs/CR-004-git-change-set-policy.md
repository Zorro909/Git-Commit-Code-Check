# CR-004 Implementation Specification: Git Change-Set Policy

## Scope

This increment introduces an explicit `ChangeSetService` with path metadata and uses it from
command validation. Existing selector classes remain for compatibility with older daemon/check
internals until later refactors remove them.

## Change Sets

`ChangeSet` contains `ChangeSetEntry` records with:

- repository-relative path
- Git status
- staged flag
- unstaged flag
- untracked flag
- deleted flag
- origin reason

Deleted entries are represented by the parser but filtered out of command change sets.

## Policies

- Interactive and batch checks use branch-aware selection.
- Pre-commit uses staged paths from `git diff --cached --name-status --relative`.
- Assistant mode includes untracked Java files in addition to branch-aware changes.
- Main/release branches select staged plus unstaged changes.
- Feature branches select direct diff against the first configured main branch that exists.
- The first matching configured main branch wins.
- If no configured main branch exists, the service falls back to staged plus unstaged changes.

## Git Execution

Git commands are executed through `GitCommandRunner`, which waits for process exit, captures stderr,
and raises `GitCommandException` on non-zero exit codes. Rename output is parsed explicitly and uses
the new path.

## Command Integration

`CodeCheckCommandService` now validates paths from `ChangeSetService`:

- `runInteractiveCheck`: `currentInteractiveCheckChangeSet`
- `runBatchCheck`: `currentInteractiveCheckChangeSet`
- `runPreCommit`: `preCommitChangeSet`

## Tests

- Main branch selection includes staged and unstaged modified files.
- Deleted files are ignored.
- Feature branch selection uses direct diff against configured main branch tip.
- Main branch fallback honors configured branch order.
- Pre-commit uses staged paths.
- Assistant mode includes untracked Java files.
