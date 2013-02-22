package org.apache.felix.ipojo.runtime.core.components;

public class CallSuperConstructor extends ParentClass {
    
    public CallSuperConstructor() {
        super("test");
        System.out.println("plop");
    } 

}
