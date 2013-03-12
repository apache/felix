package org.apache.felix.ipojo.runtime.core.api.components;

import org.apache.felix.ipojo.runtime.core.api.services.Foo;

public class FooImpl implements Foo {
    
   // private List<String> m_list = new ArrayList<String>();

    public void doSomething() {
       // Do something...
        System.out.println("Hello World !");
    }
    
    public FooImpl(String s) {
        _setIM(s);
    }
    
    public void _setIM(String s) {
        
    }

}
