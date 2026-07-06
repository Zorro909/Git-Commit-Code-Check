package de.zorro909.codecheck.validation;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

public record FileInterest(String description,
                           List<String> includeGlobs,
                           Predicate<Path> predicate) {

    public FileInterest {
        includeGlobs = List.copyOf(includeGlobs);
    }

    public static FileInterest any() {
        return new FileInterest("all files", List.of("**/*"), _ -> true);
    }

    public static FileInterest none() {
        return new FileInterest("no files", List.of(), _ -> false);
    }

    public static FileInterest javaMainSources() {
        return new FileInterest("Java main sources", List.of("src/main/java/**/*.java"),
                                path -> {
                                    String normalized = path.toString().replace('\\', '/');
                                    return normalized.contains("src/main/java/")
                                           && normalized.endsWith(".java");
                                });
    }

    public boolean matches(Path path) {
        return predicate.test(path);
    }
}
