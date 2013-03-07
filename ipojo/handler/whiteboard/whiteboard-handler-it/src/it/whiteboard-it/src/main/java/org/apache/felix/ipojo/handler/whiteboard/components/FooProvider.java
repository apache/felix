package org.apache.felix.ipojo.handler.whiteboard.components;

import org.apache.felix.ipojo.handler.whiteboard.services.FooService;

public class FooProvider implements FooService {
    
    public String foo;

    public void foo() { 
        if (foo.equals("foo")) {
            foo = "bar";
        } else {
            foo = "foo";
        }
    }
    
}
