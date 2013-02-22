package org.apache.felix.ipojo.runtime.core.components;

public class SuperParentClass {

    private String name;

    public SuperParentClass(final String n) {
        System.out.println("Hello from super super !");
        name = n;
    }

    public SuperParentClass(final StringBuffer n) {
        name = n.toString();
    }


}
