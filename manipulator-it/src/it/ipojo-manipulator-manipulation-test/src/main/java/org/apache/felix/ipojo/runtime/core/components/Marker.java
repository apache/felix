package org.apache.felix.ipojo.runtime.core.components;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Marker {
    
    String name();
    
    String[] arrayOfObjects();
    
    SubMarker sub();
    
    SubMarker[] arrayOfAnnotations();
    
    Type type();
    
    public enum Type {FOO, BAR, BAZ};

}
