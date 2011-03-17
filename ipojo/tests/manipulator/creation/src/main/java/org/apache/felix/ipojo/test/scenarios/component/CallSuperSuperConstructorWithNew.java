package org.apache.felix.ipojo.test.scenarios.component;

public class CallSuperSuperConstructorWithNew extends ParentClass2 {

    public CallSuperSuperConstructorWithNew() {
        super(new String("test"));
        System.out.println("plop");
    }

}
