package org.apache.felix.ipojo.api.test;

public class Foo implements FooService {
    
    private String m_message;
    
    public Foo() {
        System.out.println("Foo Created !");
    }

    public String getMessage() {
        return m_message;
    }
    
    public void start() {
        System.out.println("Foo2 started...");
    }
    
    public void stop() {
        System.out.println("Foo2 stopped...");
    }

}
