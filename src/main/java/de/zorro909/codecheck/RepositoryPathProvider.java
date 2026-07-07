package de.zorro909.codecheck;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Named;

import java.nio.file.Path;

@Factory
public class RepositoryPathProvider {

    public static final String REPOSITORY_DIRECTORY = "repositoryDirectory";

    @Bean
    @Named(REPOSITORY_DIRECTORY)
    public Path repositoryDirectory(){
        return Path.of("");
    }

}
