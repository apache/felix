package org.apache.felix.ipojo.api.test;

public class Bar implements BarService {
    
    private FooService fs;

    public String getMessage() {
       return fs.getMessage();
    }
    
    

}
