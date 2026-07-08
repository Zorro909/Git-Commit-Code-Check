package de.zorro909.codecheck.core.config;

import java.time.Duration;
import java.util.List;

public record ConfigOverrides(List<String> mainBranches, Duration inactivityTimeout, Duration saveDebounce) {

    public static ConfigOverrides none() {
        return new ConfigOverrides(null, null, null);
    }

    public CodeCheckConfig apply(CodeCheckConfig config) {
        CodeCheckConfig result = config;
        if (mainBranches != null) {
            result = result.withGit(new CodeCheckConfig.Git(List.copyOf(mainBranches),
                    result.git().releaseBranchPattern(), result.git().restageAfterFix()));
        }
        if (inactivityTimeout != null || saveDebounce != null) {
            result = result.withDaemon(new CodeCheckConfig.Daemon(
                    inactivityTimeout == null ? result.daemon().inactivityTimeout() : inactivityTimeout,
                    saveDebounce == null ? result.daemon().saveDebounce() : saveDebounce, result.daemon().transport()));
        }
        return result;
    }
}
