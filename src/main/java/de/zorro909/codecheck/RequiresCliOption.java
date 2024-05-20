package de.zorro909.codecheck;

import io.micronaut.context.annotation.Requires;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Requires(condition = CliOptionCondition.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresCliOption {
    String value();
}

