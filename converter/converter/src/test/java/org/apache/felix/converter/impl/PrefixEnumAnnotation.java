package org.apache.felix.converter.impl;

public @interface PrefixEnumAnnotation {
    static final String PREFIX_ = "com.acme.config.";

    enum Type { SINGLE, MULTI };

    long timeout() default 1000L;
    Type type() default Type.SINGLE;
}
