package de.zorro909.codecheck.actions.report;

import de.zorro909.codecheck.RequiresCliOption;
import de.zorro909.codecheck.actions.PostAction;
import de.zorro909.codecheck.checks.ValidationError;
import jakarta.inject.Singleton;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.gitlab4j.api.DiscussionsApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.MergeRequestApi;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.MergeRequestFilter;
import org.gitlab4j.api.models.MergeRequestVersion;
import org.gitlab4j.api.models.Position;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiresCliOption("--gitlab-report")
@Singleton
public class GitlabReportAction implements PostAction {

    private final MergeRequestApi mergeRequestApi;
    private final DiscussionsApi discussionsApi;
    private final Path repositoryPath;

    public GitlabReportAction(MergeRequestApi mergeRequestApi, DiscussionsApi discussionsApi, Path repositoryPath) {
        this.mergeRequestApi = mergeRequestApi;
        this.discussionsApi = discussionsApi;
        this.repositoryPath = repositoryPath;
    }

    @Override
    public boolean perform(Map<Path, List<ValidationError>> validationErrors) {
        Optional<MergeRequest> mrOptional = findMR();

        if (mrOptional.isEmpty()) {
            return false;
        }
        MergeRequest mergeRequest = mrOptional.get();

        MergeRequestVersion version;
        try {
            version = mergeRequestApi.getDiffVersions(mergeRequest.getProjectId(), mergeRequest.getIid())
                                     .stream()
                                     .max(Comparator.comparingLong(MergeRequestVersion::getId))
                                     .orElseThrow();
        } catch (GitLabApiException e) {
            throw new RuntimeException(e);
        }

        String sourceSha = version.getBaseCommitSha();
        String headSha = version.getHeadCommitSha();

        List<DiffEntry> diffs = List.of();
        try (Git git = Git.open(repositoryPath.toFile())) {
            Repository repository = git.getRepository();
            ObjectReader reader = repository.newObjectReader();

            CanonicalTreeParser sourceTreeParser = new CanonicalTreeParser();
            ObjectId sourceTreeId = repository.resolve(sourceSha + "^{tree}");
            sourceTreeParser.reset(reader, sourceTreeId);

            CanonicalTreeParser headTreeParser = new CanonicalTreeParser();
            ObjectId headTreeId = repository.resolve(headSha + "^{tree}");
            headTreeParser.reset(reader, headTreeId);

            diffs = git.diff().setNewTree(headTreeParser).setOldTree(sourceTreeParser).call();
        } catch (GitAPIException | IOException e) {
            throw new RuntimeException(e);
        }

        for (Path path : validationErrors.keySet()) {

            String oldPath = diffs.stream()
                                  .filter(entry -> path.toString().equalsIgnoreCase(entry.getNewPath()))
                                  .filter(diff -> diff.getChangeType() == DiffEntry.ChangeType.RENAME)
                                  .map(DiffEntry::getOldPath)
                                  .findFirst()
                                  .orElse(path.toString());

            for (ValidationError error : validationErrors.get(path)) {
                Position position = new Position().withPositionType(Position.PositionType.TEXT)
                                                  .withBaseSha(version.getBaseCommitSha())
                                                  .withHeadSha(version.getHeadCommitSha())
                                                  .withStartSha(version.getStartCommitSha())
                                                  .withNewPath(path.toString())
                                                  .withOldPath(oldPath)
                                                  .withNewLine(error.position().line);

                try {
                    discussionsApi.createMergeRequestDiscussion(mergeRequest.getProjectId(), mergeRequest.getIid(),
                                                                error.errorMessage(), new Date(), null, position);
                } catch (GitLabApiException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }

    public Optional<MergeRequest> findMR() {
        try {
            String sourceBranch = executeGitBranchCommand().findFirst().orElseThrow();

            if (sourceBranch.isEmpty() || sourceBranch.equals("develop")) {
                return Optional.empty();
            }

            MergeRequestFilter filter = new MergeRequestFilter();
            filter.setTargetBranch("develop");
            filter.setSourceBranch(sourceBranch);

            List<MergeRequest> mergeRequests = this.mergeRequestApi.getMergeRequests(filter);
            if (mergeRequests.size() != 1) {
                System.out.println("Found multiple MRs: '" + mergeRequests.stream()
                                                                          .map(MergeRequest::getId)
                                                                          .map(String::valueOf)
                                                                          .collect(Collectors.joining(",")) + "'");
                return Optional.empty();
            }
            return Optional.of(mergeRequests.getFirst());
        } catch (IOException | InterruptedException | GitLabApiException e) {
            throw new RuntimeException(e);
        }
    }

    private Stream<String> executeGitBranchCommand() throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("git", "branch", "--show-current");
        builder.directory(repositoryPath.toAbsolutePath().toFile());
        Process process = builder.start();
        return new BufferedReader(new InputStreamReader(process.getInputStream())).lines();
    }


    enum DiffType {
        ADD, REM, UNCHANGED
    }

}
