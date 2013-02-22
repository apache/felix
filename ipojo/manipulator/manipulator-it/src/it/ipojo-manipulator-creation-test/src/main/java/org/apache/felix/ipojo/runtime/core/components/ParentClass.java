package org.apache.felix.ipojo.runtime.core.components;

public class ParentClass {
    
    private String name;

    public ParentClass(final String n) {
        name = n;
    }
    
    public ParentClass(final StringBuffer n) {
        name = n.toString();
    } 

}
