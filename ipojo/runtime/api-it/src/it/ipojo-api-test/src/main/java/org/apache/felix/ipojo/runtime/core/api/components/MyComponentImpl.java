package org.apache.felix.ipojo.runtime.core.api.components;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.runtime.core.api.services.Foo;

/**
 * This class is marked as a component to be manipulated.
 */
@Component(name="do-not-use-this-factory", public_factory = false)
public class MyComponentImpl {
    
    private Foo myFoo;
    
    private int anInt;
    
    public MyComponentImpl() {
        anInt = 2;
    }
    
    public MyComponentImpl(int i) {
        anInt = i;
    }

    public void start() {
       myFoo.doSomething();
       if (anInt > 0) {
           System.out.println("Set int to " + anInt);
       }
    }

}
