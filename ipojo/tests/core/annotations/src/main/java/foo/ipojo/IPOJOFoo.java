package foo.ipojo;


import foo.RGB;

/**
 * Creates a simple annotation to create the processing of matching
 * annotations
 */
public @interface IPOJOFoo {
    String bar();

    RGB rgb() default RGB.BLUE;

    RGB[] colors() default {};
}
