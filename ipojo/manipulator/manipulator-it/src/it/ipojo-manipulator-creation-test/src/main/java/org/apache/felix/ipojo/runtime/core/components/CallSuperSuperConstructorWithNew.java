package org.apache.felix.ipojo.runtime.core.components;

public class CallSuperSuperConstructorWithNew extends ParentClass2 {

    public CallSuperSuperConstructorWithNew() {
        super(new String("test"));
        System.out.println("plop");
    }

}
