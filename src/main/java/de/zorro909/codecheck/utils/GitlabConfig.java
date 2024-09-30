package de.zorro909.codecheck.utils;

import de.zorro909.codecheck.RequiresCliOption;
import jakarta.inject.Singleton;
import org.gitlab4j.api.DiscussionsApi;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.MergeRequestApi;

@RequiresCliOption("--gitlab-report")
@Singleton
public class GitlabConfig {

    @Singleton
    public GitLabApi gitLabApi(String gitlabUrl, String personalAccessToken) {
        return new GitLabApi(gitlabUrl, personalAccessToken);
    }

    @Singleton
    public DiscussionsApi discussionsApi(GitLabApi gitLabApi) {
        return new DiscussionsApi(gitLabApi);
    }

    @Singleton
    public MergeRequestApi mergeRequestApi(GitLabApi gitLabApi) {
        return new MergeRequestApi(gitLabApi);
    }

}
