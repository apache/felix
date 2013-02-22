package org.apache.felix.ipojo.runtime.core.components;

public class CallSuperConstructorWithNew extends ParentClass {
    
    public CallSuperConstructorWithNew() {
        super(new StringBuffer("test"));
        System.out.println("plop");
    } 

}
