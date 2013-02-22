package org.apache.felix.ipojo.runtime.core.components;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SubMarker {
    
    String subname();


}
