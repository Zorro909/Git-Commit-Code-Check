package de.zorro909.codecheck;


/**
 * This annotation is used to associate a test class with the class being tested.
 */
public @interface Tests {

    Class<?> value();

}
