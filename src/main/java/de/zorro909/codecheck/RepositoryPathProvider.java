package de.zorro909.codecheck;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;

import java.nio.file.Path;

@Factory
public class RepositoryPathProvider {

    @Bean
    public Path repositoryDirectory(){
        return Path.of("");
    }

}
