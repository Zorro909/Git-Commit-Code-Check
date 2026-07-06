package de.zorro909.codecheck.changeset;

import de.zorro909.codecheck.config.CodeCheckConfig;
import de.zorro909.codecheck.config.CodeCheckConfigLoader;
import jakarta.inject.Singleton;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Singleton
public class GitChangeSetService implements ChangeSetService {

    private final Path repositoryDirectory;
    private final CodeCheckConfigLoader configLoader;
    private final GitCommandRunner git;

    public GitChangeSetService(Path repositoryDirectory, CodeCheckConfigLoader configLoader) {
        this(repositoryDirectory, configLoader, new GitCommandRunner(repositoryDirectory));
    }

    GitChangeSetService(Path repositoryDirectory, CodeCheckConfigLoader configLoader,
                        GitCommandRunner git) {
        this.repositoryDirectory = repositoryDirectory.toAbsolutePath().normalize();
        this.configLoader = configLoader;
        this.git = git;
    }

    @Override
    public ChangeSet currentAssistantChangeSet() {
        List<ChangeSetEntry> entries = new ArrayList<>(branchAwareChangeSet().entries());
        entries.addAll(untrackedJavaEntries());
        return deduplicate(entries);
    }

    @Override
    public ChangeSet currentInteractiveCheckChangeSet() {
        return branchAwareChangeSet();
    }

    @Override
    public ChangeSet preCommitChangeSet() {
        return parseNameStatus(git.run("diff", "--cached", "--name-status", "--relative"),
                               true, false, "pre-commit staged path");
    }

    @Override
    public ChangeSet explicitFiles(Collection<Path> files) {
        return deduplicate(files.stream()
                                .map(this::repositoryRelative)
                                .map(path -> new ChangeSetEntry(path, GitFileStatus.UNKNOWN,
                                                                false, false, false, false,
                                                                "explicit file"))
                                .toList());
    }

    private Path repositoryRelative(Path path) {
        Path normalized = path.normalize();
        if (!normalized.isAbsolute()) {
            return normalized;
        }
        return repositoryDirectory.relativize(normalized);
    }

    private ChangeSet branchAwareChangeSet() {
        CodeCheckConfig.Git gitConfig = configLoader.load().git();
        String currentBranch = currentBranch();
        if (isMainLikeBranch(currentBranch, gitConfig)) {
            return mainBranchChangeSet();
        }

        return firstExistingMainBranch(gitConfig.mainBranches())
                .map(this::featureBranchChangeSet)
                .orElseGet(this::mainBranchChangeSet);
    }

    private ChangeSet mainBranchChangeSet() {
        List<ChangeSetEntry> entries = new ArrayList<>();
        entries.addAll(parseNameStatus(git.run("diff", "--name-status", "--relative"),
                                       false, true, "unstaged change").entries());
        entries.addAll(parseNameStatus(git.run("diff", "--cached", "--name-status", "--relative"),
                                       true, false, "staged change").entries());
        return deduplicate(entries);
    }

    private ChangeSet featureBranchChangeSet(String baseBranch) {
        return parseNameStatus(git.run("diff", "--name-status", "--relative", baseBranch),
                               false, true, "direct diff against " + baseBranch);
    }

    private List<ChangeSetEntry> untrackedJavaEntries() {
        return git.run("ls-files", "--others", "--exclude-standard")
                  .stream()
                  .filter(path -> path.endsWith(".java"))
                  .map(path -> new ChangeSetEntry(Path.of(path), GitFileStatus.UNTRACKED,
                                                  false, false, true, false,
                                                  "untracked java file"))
                  .toList();
    }

    private String currentBranch() {
        List<String> branches = git.run("branch", "--show-current");
        return branches.isEmpty() ? "" : branches.get(0);
    }

    private boolean isMainLikeBranch(String currentBranch, CodeCheckConfig.Git gitConfig) {
        Pattern releasePattern = gitConfig.releaseBranchPattern();
        return gitConfig.mainBranches().contains(currentBranch)
               || releasePattern.matcher(currentBranch).matches();
    }

    private java.util.Optional<String> firstExistingMainBranch(List<String> mainBranches) {
        return mainBranches.stream()
                           .filter(branch -> git.succeeds("rev-parse", "--verify", "--quiet",
                                                          branch))
                           .findFirst();
    }

    private ChangeSet parseNameStatus(List<String> lines, boolean staged, boolean unstaged,
                                      String originReason) {
        return deduplicate(lines.stream()
                                .map(line -> parseNameStatusLine(line, staged, unstaged,
                                                                 originReason))
                                .filter(entry -> !entry.deleted())
                                .toList());
    }

    private ChangeSetEntry parseNameStatusLine(String line, boolean staged, boolean unstaged,
                                               String originReason) {
        String[] parts = line.split("\t");
        if (parts.length < 2) {
            throw new GitCommandException("Unexpected git name-status output: " + line);
        }
        GitFileStatus status = status(parts[0]);
        Path path = Path.of(parts.length >= 3 && (status == GitFileStatus.RENAMED
                                                  || status == GitFileStatus.COPIED)
                            ? parts[2] : parts[1]);
        boolean deleted = status == GitFileStatus.DELETED;
        return new ChangeSetEntry(path, status, staged, unstaged, false, deleted, originReason);
    }

    private GitFileStatus status(String statusText) {
        return switch (statusText.charAt(0)) {
            case 'A' -> GitFileStatus.ADDED;
            case 'C' -> GitFileStatus.COPIED;
            case 'D' -> GitFileStatus.DELETED;
            case 'M' -> GitFileStatus.MODIFIED;
            case 'R' -> GitFileStatus.RENAMED;
            case 'T' -> GitFileStatus.TYPE_CHANGED;
            case 'U' -> GitFileStatus.UNMERGED;
            default -> GitFileStatus.UNKNOWN;
        };
    }

    private ChangeSet deduplicate(List<ChangeSetEntry> entries) {
        Map<Path, ChangeSetEntry> merged = new LinkedHashMap<>();
        for (ChangeSetEntry entry : entries) {
            merged.merge(entry.path(), entry, this::merge);
        }
        return new ChangeSet(merged.values().stream().filter(entry -> !entry.deleted()).toList());
    }

    private ChangeSetEntry merge(ChangeSetEntry left, ChangeSetEntry right) {
        return new ChangeSetEntry(left.path(), right.status(),
                                  left.staged() || right.staged(),
                                  left.unstaged() || right.unstaged(),
                                  left.untracked() || right.untracked(),
                                  left.deleted() && right.deleted(),
                                  left.originReason() + ", " + right.originReason());
    }
}
