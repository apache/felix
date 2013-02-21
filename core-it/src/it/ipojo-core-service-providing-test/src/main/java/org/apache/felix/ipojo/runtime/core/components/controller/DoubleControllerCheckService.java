package org.apache.felix.ipojo.runtime.core.components.controller;

import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.apache.felix.ipojo.runtime.core.services.FooService;

import java.util.Properties;

public class DoubleControllerCheckService implements FooService, CheckService {
    
    
    private boolean controllerFoo;
    private boolean controllerCS;

    public boolean foo() {
        controllerFoo = ! controllerFoo;
        return controllerFoo;
    }

    public Properties fooProps() {
        Properties props = new Properties();
        props.put("controller", new Boolean(controllerFoo));
        
        controllerCS = true;
        controllerFoo = true;
        
        return props;
    }

    public boolean getBoolean() {
        return false;
    }

    public double getDouble() {
        return 0;
    }

    public int getInt() {
        return 0;
    }

    public long getLong() {
        return 0;
    }

    public Boolean getObject() {
        return null;
    }

    public boolean check() {
        controllerCS = ! controllerCS;
        return controllerCS;
    }

    public Properties getProps() {
        Properties props = new Properties();
        props.put("controller", new Boolean(controllerCS));
        
        // Invert both
        controllerCS = ! controllerCS;
        controllerFoo = ! controllerFoo;
        
        return props;
        
    }

}
