package de.zorro909.codecheck.validation;

import java.util.List;

public record WatchPlan(List<FileInterest> validatedFiles, List<FileInterest> contextFiles) {

    public WatchPlan {
        validatedFiles = List.copyOf(validatedFiles);
        contextFiles = List.copyOf(contextFiles);
    }
}
