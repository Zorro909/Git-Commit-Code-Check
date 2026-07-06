# CR-004: Git Change-Set Policy

## Status

Proposed.

## Context

File selection should reflect developer workflow. Main-like branches should validate local modifications. Feature branches should validate against the configured main branch. Pre-commit should use staged paths, but validating working tree content is acceptable.

The current `FileSelector` model is too small and too coupled to CLI option based DI.

## Goals

- Replace selector bean selection with explicit change-set policy.
- Support branch-aware selection.
- Include untracked files in assistant mode after debounce.
- Ignore deleted files.
- Use direct diff against the configured main branch tip for feature branches.
- Keep pre-commit deterministic.

## Branch Policy

Configured main branch list:

```yaml
git:
  mainBranches: [develop, main, master]
  releaseBranchPattern: "release/.*"
```

Selection behavior:

- If current branch is first matching main branch, another listed main branch, or `release/*`, select staged plus unstaged modified files.
- If current branch is another branch, direct diff against the first configured main branch that exists.
- The first branch in `git.mainBranches` wins when multiple candidates exist.
- Deleted files are ignored because they cannot contain code relevant to the new state.

Direct diff means comparing against branch tip, not merge-base.

## Pre-Commit Policy

- Use staged paths for selection.
- Validate working tree content for those paths.
- Ignore deleted paths.
- Bypass watcher debounce and invoke full scan.
- LOW findings are hidden.
- MEDIUM findings print.
- HIGH findings block.

## Assistant Policy

- Include staged files.
- Include unstaged modified files.
- Include untracked relevant files after save debounce.
- Use rule interest to decide relevance.

## Proposed Service

```java
interface ChangeSetService {
    ChangeSet currentAssistantChangeSet();
    ChangeSet currentInteractiveCheckChangeSet();
    ChangeSet preCommitChangeSet();
    ChangeSet explicitFiles(Collection<Path> files);
}
```

`ChangeSet` should carry path metadata:

- path
- git status
- staged/unstaged/untracked
- deleted flag
- origin reason

## Git Command Requirements

- Commands must wait for process exit.
- stderr and exit code must be inspected.
- Rename output must be handled explicitly.
- Paths should be repository-relative.
- Git failures should become tool diagnostics, not silent empty scans.

## Acceptance Criteria

- On `develop`, modified staged and unstaged files are selected.
- On feature branch, files changed compared to `develop` tip are selected.
- If `develop` does not exist and `main` exists, `main` is used when ordered next.
- Pre-commit uses staged paths but reads current working tree content.
- Deleted files are ignored.
- Untracked relevant Java files are included by assistant mode after debounce.

## Open Questions

- Should renamed files be reported under the new path only?
- Should file mode changes without content changes be ignored?
- Should ignored files ever be included if explicitly passed on the CLI?

