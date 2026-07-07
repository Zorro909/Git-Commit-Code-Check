package de.zorro909.codecheck.infra.jacoco;

import de.zorro909.codecheck.core.coverage.ClassCoverage;
import de.zorro909.codecheck.core.coverage.CoverageSnapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapStructCoverageAttributor {

    private static final Pattern PACKAGE = Pattern.compile("package\\s+([\\w.]+)\\s*;");

    private static final Pattern TYPE = Pattern.compile("interface\\s+(\\w+)");

    public boolean isMapper(Path sourceFile) {
        try {
            String source = Files.readString(sourceFile);
            return source.contains("@Mapper") || source.contains("@org.mapstruct.Mapper");
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to read " + sourceFile, e);
        }
    }

    public Optional<ClassCoverage> attributedCoverage(Path mapperInterface, CoverageSnapshot snapshot) {
        String implName = implementationClassName(mapperInterface);
        return snapshot.classCoverage(implName).or(() -> snapshot.classCoverage(implName.replace('/', '.')));
    }

    public String implementationClassName(Path mapperInterface) {
        try {
            String source = Files.readString(mapperInterface);
            String packageName = match(PACKAGE, source).orElse("");
            String typeName = match(TYPE, source).orElseThrow();
            String qualified = packageName.isBlank() ? typeName + "Impl" : packageName + "." + typeName + "Impl";
            return qualified.replace('.', '/');
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to read " + mapperInterface, e);
        }
    }

    private Optional<String> match(Pattern pattern, String source) {
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

}
